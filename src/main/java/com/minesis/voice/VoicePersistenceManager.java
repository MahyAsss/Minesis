package com.minesis.voice;

import com.minesis.utils.AudioUtils;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Saves voice clips to disk as OGG Opus files and handles 24-hour cleanup.
 * Files are written to <world>/minesis/voices/<player-uuid>/<timestamp>_<context>.ogg
 */
public class VoicePersistenceManager {
    private static final Logger LOGGER = LogManager.getLogger();

    static final long CLEANUP_INTERVAL_MS = 24L * 60L * 60L * 1000L; // 24 h

    private static volatile Path              voicesDir     = null;
    private static volatile VoicechatServerApi voicechatApi = null;
    private static volatile long              lastCleanupMs = 0L;

    // Single background thread for all disk I/O — never blocks the audio thread
    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Minesis-VoiceSave");
        t.setDaemon(true);
        return t;
    });

    // ── OGG CRC32 (polynomial 0x04C11DB7) ─────────────────────────────────

    private static final int[] OGG_CRC = buildCrcTable();

    private static int[] buildCrcTable() {
        int[] t = new int[256];
        for (int i = 0; i < 256; i++) {
            int c = i << 24;
            for (int j = 0; j < 8; j++) c = (c < 0) ? (c << 1) ^ 0x04C11DB7 : (c << 1);
            t[i] = c;
        }
        return t;
    }

    private static int oggCrc(byte[] data) {
        int crc = 0;
        for (byte b : data) crc = (crc << 8) ^ OGG_CRC[((crc >>> 24) ^ (b & 0xFF)) & 0xFF];
        return crc;
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────

    public static void setApi(VoicechatServerApi api) {
        voicechatApi = api;
    }

    public static void init(MinecraftServer server) {
        voicesDir = server.getWorldPath(LevelResource.ROOT).toAbsolutePath()
                         .resolve("minesis").resolve("voices");
        try {
            Files.createDirectories(voicesDir);
        } catch (IOException e) {
            LOGGER.error("[Minesis] Cannot create voices directory: {}", e.getMessage());
        }
        lastCleanupMs = System.currentTimeMillis();
        loadAllClips();
    }

    /**
     * Loads all raw PCM clips saved to disk back into VoiceStorage.
     * Called on server start so clips survive restarts.
     */
    public static void loadAllClips() {
        Path dir = voicesDir;
        if (dir == null || !Files.exists(dir)) return;
        int loaded = 0;
        try {
            for (Path playerDir : Files.list(dir).filter(Files::isDirectory).toList()) {
                UUID playerUUID;
                try { playerUUID = UUID.fromString(playerDir.getFileName().toString()); }
                catch (IllegalArgumentException e) { continue; }

                try {
                    for (Path pcmFile : Files.list(playerDir)
                            .filter(f -> f.getFileName().toString().endsWith(".pcm"))
                            .toList()) {
                        try {
                            String name = pcmFile.getFileName().toString();
                            // filename: <timestamp>_<CONTEXT>.pcm
                            String base = name.substring(0, name.length() - 4); // strip .pcm
                            int sep = base.lastIndexOf('_');
                            VoiceContext ctx = VoiceContext.IDLE;
                            if (sep >= 0) {
                                try { ctx = VoiceContext.valueOf(base.substring(sep + 1)); }
                                catch (IllegalArgumentException ignored) {}
                            }
                            byte[] pcm = Files.readAllBytes(pcmFile);
                            VoiceStorage.loadVoiceClip(playerUUID, pcm, ctx);
                            loaded++;
                        } catch (Exception e) {
                            LOGGER.debug("[Minesis] Skipping corrupt PCM file {}: {}", pcmFile, e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.debug("[Minesis] Error reading voice dir for {}: {}", playerUUID, e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("[Minesis] Failed to list voices directory: {}", e.getMessage());
        }
        if (loaded > 0)
            LOGGER.info("[Minesis] Loaded {} voice clip(s) from disk.", loaded);
        else
            LOGGER.debug("[Minesis] No PCM clips found on disk (first run or clips recorded before this version).");
    }

    // ── Save ────────────────────────────────────────────────────────────────

    public static void saveClip(UUID playerUUID, byte[] pcm, VoiceContext context) {
        Path dir = voicesDir;
        if (dir == null || pcm == null || pcm.length == 0) return;

        VoicechatServerApi api = voicechatApi;
        SAVE_EXECUTOR.submit(() -> {
            try {
                Path playerDir = dir.resolve(playerUUID.toString());
                Files.createDirectories(playerDir);
                String base = System.currentTimeMillis() + "_" + context.name();

                // Raw PCM — always saved, used for reload on next startup
                Files.write(playerDir.resolve(base + ".pcm"), pcm);

                // OGG Opus — saved when SVC API is available (for external playback)
                if (api != null) {
                    byte[] oggData = encodeOggOpus(pcm, api);
                    if (oggData != null) Files.write(playerDir.resolve(base + ".ogg"), oggData);
                }
            } catch (Exception e) {
                LOGGER.debug("[Minesis] Failed to save voice clip for {}: {}", playerUUID, e.getMessage());
            }
        });
    }

    // ── Cleanup ─────────────────────────────────────────────────────────────

    public static boolean shouldCleanup() {
        return lastCleanupMs > 0 && System.currentTimeMillis() - lastCleanupMs >= CLEANUP_INTERVAL_MS;
    }

    public static void cleanup() {
        LOGGER.info("[Minesis] 24h voice cleanup — wiping in-memory clips and disk files.");
        lastCleanupMs = System.currentTimeMillis();
        VoiceStorage.clearAllClips();
        Path dir = voicesDir;
        if (dir == null) return;
        SAVE_EXECUTOR.submit(() -> {
            try {
                deleteDir(dir.toFile());
                Files.createDirectories(dir);
                LOGGER.info("[Minesis] Voice directory cleared.");
            } catch (IOException e) {
                LOGGER.error("[Minesis] Error during voice cleanup: {}", e.getMessage());
            }
        });
    }

    // ── OGG Opus encoding ───────────────────────────────────────────────────

    private static final int FRAME_SAMPLES = 960; // 20 ms @ 48 kHz
    private static final int PRE_SKIP      = 312; // standard Opus pre-skip @ 48 kHz

    private static byte[] encodeOggOpus(byte[] pcm, VoicechatServerApi api) {
        OpusEncoder encoder = api.createEncoder();
        if (encoder == null || encoder.isClosed()) return null;
        try {
            short[] shorts = AudioUtils.bytesToShorts(pcm);
            int serial = (int)(System.nanoTime() & 0xFFFFFFFFL);
            int seq = 0;
            ByteArrayOutputStream out = new ByteArrayOutputStream(pcm.length / 4);

            // Header pages (BOS + comment)
            writeOggPage(out, 0x02, 0L, serial, seq++, new byte[][]{opusHead()});
            writeOggPage(out, 0x00, 0L, serial, seq++, new byte[][]{opusTags()});

            // Encode PCM frames to Opus packets (pad last partial frame with zeros)
            List<byte[]> packets = new ArrayList<>((shorts.length / FRAME_SAMPLES) + 1);
            for (int i = 0; i < shorts.length; i += FRAME_SAMPLES) {
                short[] frame = new short[FRAME_SAMPLES];
                System.arraycopy(shorts, i, frame, 0, Math.min(FRAME_SAMPLES, shorts.length - i));
                byte[] enc = encoder.encode(frame);
                if (enc != null && enc.length > 0) packets.add(enc);
            }

            // Write audio pages (up to 50 packets each)
            int pi = 0, framesWritten = 0, total = packets.size();
            while (pi < total) {
                int end = Math.min(pi + 50, total);
                framesWritten += (end - pi);
                long gp = (long) framesWritten * FRAME_SAMPLES + PRE_SKIP;
                byte[][] pagePackets = packets.subList(pi, end).toArray(new byte[0][]);
                writeOggPage(out, end == total ? 0x04 : 0x00, gp, serial, seq++, pagePackets);
                pi = end;
            }
            if (total == 0) {
                writeOggPage(out, 0x04, PRE_SKIP, serial, seq, new byte[0][]);
            }

            return out.toByteArray();
        } catch (Exception e) {
            LOGGER.debug("[Minesis] OGG Opus encoding error: {}", e.getMessage());
            return null;
        } finally {
            encoder.close();
        }
    }

    private static byte[] opusHead() {
        // https://wiki.xiph.org/OggOpus#ID_Header
        byte[] h = new byte[19];
        h[0]='O'; h[1]='p'; h[2]='u'; h[3]='s'; h[4]='H'; h[5]='e'; h[6]='a'; h[7]='d';
        h[8] = 1;                                          // version
        h[9] = 1;                                          // channels (mono)
        h[10] = (byte)(PRE_SKIP & 0xFF);                   // pre-skip LE
        h[11] = (byte)((PRE_SKIP >> 8) & 0xFF);
        h[12] = (byte)0x80; h[13] = (byte)0xBB;           // sample rate 48000 LE (0x0000BB80)
        // h[14..15] = output gain 0, h[16..18] already 0 (gain high + mapping family)
        return h;
    }

    private static byte[] opusTags() {
        // https://wiki.xiph.org/OggOpus#Comment_Header
        byte[] vendor = "Minesis".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] h = new byte[8 + 4 + vendor.length + 4];
        h[0]='O'; h[1]='p'; h[2]='u'; h[3]='s'; h[4]='T'; h[5]='a'; h[6]='g'; h[7]='s';
        int p = 8;
        h[p++] = (byte)(vendor.length & 0xFF);
        h[p++] = (byte)((vendor.length >> 8) & 0xFF);
        h[p++] = (byte)((vendor.length >> 16) & 0xFF);
        h[p++] = (byte)((vendor.length >> 24) & 0xFF);
        System.arraycopy(vendor, 0, h, p, vendor.length);
        // last 4 bytes are 0 → user_comment_list_length = 0
        return h;
    }

    private static void writeOggPage(ByteArrayOutputStream out, int headerType, long granule,
            int serial, int seq, byte[][] packets) throws IOException {
        // Build segment table (OGG lacing) and page body
        ByteArrayOutputStream seg  = new ByteArrayOutputStream();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (byte[] pkt : packets) {
            int rem = pkt.length;
            while (rem >= 255) { seg.write(255); rem -= 255; }
            seg.write(rem); // terminal segment (0 for exact multiples of 255)
            body.write(pkt);
        }
        byte[] segs = seg.toByteArray();
        byte[] data = body.toByteArray();

        // Page header with checksum placeholder = 0
        ByteArrayOutputStream page = new ByteArrayOutputStream(27 + segs.length + data.length);
        page.write(new byte[]{'O','g','g','S'});   // capture_pattern
        page.write(0x00);                           // stream_structure_version
        page.write(headerType & 0xFF);              // header_type_flag
        writeLong64(page, granule);                 // absolute_granule_position (8 bytes LE)
        writeLE32(page, serial);                    // stream_serial_number
        writeLE32(page, seq);                       // page_sequence_no
        writeLE32(page, 0);                         // page_checksum (zeroed for CRC input)
        page.write(segs.length & 0xFF);             // number_of_page_segments
        page.write(segs);                           // segment_table
        page.write(data);                           // page body

        // Compute OGG CRC and insert at bytes 22-25
        byte[] raw = page.toByteArray();
        int crc = oggCrc(raw);
        raw[22] = (byte)(crc        & 0xFF);
        raw[23] = (byte)((crc >> 8)  & 0xFF);
        raw[24] = (byte)((crc >> 16) & 0xFF);
        raw[25] = (byte)((crc >> 24) & 0xFF);
        out.write(raw);
    }

    private static void writeLE32(OutputStream s, int v) throws IOException {
        s.write(v & 0xFF); s.write((v >> 8) & 0xFF);
        s.write((v >> 16) & 0xFF); s.write((v >> 24) & 0xFF);
    }

    private static void writeLong64(OutputStream s, long v) throws IOException {
        for (int i = 0; i < 8; i++) s.write((int)(v >> (i * 8)) & 0xFF);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static void deleteDir(File f) {
        if (f == null || !f.exists()) return;
        File[] children = f.listFiles();
        if (children != null) for (File c : children) {
            if (c.isDirectory()) deleteDir(c); else c.delete();
        }
        f.delete();
    }
}
