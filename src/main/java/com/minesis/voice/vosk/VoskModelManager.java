package com.minesis.voice.vosk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads and extracts the appropriate small Vosk model on first run.
 *
 * French : vosk-model-small-fr-fr-0.22  (~94 MB)  → <gameDir>/minesis-vosk/model-fr/
 * English: vosk-model-small-en-us-0.15  (~40 MB)  → <gameDir>/minesis-vosk/model-en/
 */
public class VoskModelManager {

    private static final Logger LOGGER = LogManager.getLogger("Minesis/Vosk");

    public enum Language { FRENCH, ENGLISH }

    private static final String FR_MODEL_NAME = "vosk-model-small-fr-fr-0.22";
    private static final String EN_MODEL_NAME = "vosk-model-small-en-us-0.15";

    private static final String BASE_URL = "https://alphacephei.com/vosk/models/";

    /**
     * Ensures the model for the given language is downloaded and extracted.
     * Returns the path to the ready model directory, or null on failure.
     */
    public static String ensureModel(File gameDir, Language language) {
        String modelName  = language == Language.ENGLISH ? EN_MODEL_NAME : FR_MODEL_NAME;
        String subDir     = language == Language.ENGLISH ? "model-en"    : "model-fr";
        String langLabel  = language == Language.ENGLISH ? "English"     : "French";
        long   approxMB   = language == Language.ENGLISH ? 40            : 94;

        File modelRoot = new File(gameDir, "minesis-vosk/" + subDir);
        File modelDir  = new File(modelRoot, modelName);

        if (new File(modelDir, "conf/model.conf").exists()) {
            LOGGER.info("[Minesis Vosk] {} model already present: {}", langLabel, modelDir.getAbsolutePath());
            return modelDir.getAbsolutePath();
        }

        modelRoot.mkdirs();
        File zipFile = new File(modelRoot, modelName + ".zip");

        if (!zipFile.exists()) {
            LOGGER.info("[Minesis Vosk] Downloading {} model (~{} MB) — this only happens once...",
                    langLabel, approxMB);
            try {
                downloadFile(BASE_URL + modelName + ".zip", zipFile);
            } catch (IOException e) {
                LOGGER.error("[Minesis Vosk] Failed to download {} model", langLabel, e);
                return null;
            }
        }

        LOGGER.info("[Minesis Vosk] Extracting {} model...", langLabel);
        try {
            extractZip(zipFile, modelRoot);
            zipFile.delete();
        } catch (IOException e) {
            LOGGER.error("[Minesis Vosk] Failed to extract {} model", langLabel, e);
            return null;
        }

        if (!new File(modelDir, "conf/model.conf").exists()) {
            LOGGER.error("[Minesis Vosk] {} model extraction incomplete — expected: {}", langLabel, modelDir);
            return null;
        }

        LOGGER.info("[Minesis Vosk] {} model ready: {}", langLabel, modelDir.getAbsolutePath());
        return modelDir.getAbsolutePath();
    }

    private static void downloadFile(String urlStr, File dest) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(300_000);
        conn.setRequestProperty("User-Agent", "Minesis-Mod/1.0");
        conn.setInstanceFollowRedirects(true);
        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_MOVED_PERM
                || status == 307 || status == 308) {
            conn.disconnect();
            downloadFile(conn.getHeaderField("Location"), dest);
            return;
        }
        if (status != HttpURLConnection.HTTP_OK)
            throw new IOException("HTTP " + status + " from " + urlStr);
        long total = conn.getContentLengthLong();
        try (InputStream in  = conn.getInputStream();
             OutputStream out = new BufferedOutputStream(new FileOutputStream(dest))) {
            byte[] buf = new byte[65536];
            long done = 0; int n; long lastLog = 0;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                done += n;
                long now = System.currentTimeMillis();
                if (total > 0 && now - lastLog > 3000) {
                    LOGGER.info("[Minesis Vosk] Downloading model... {}/{} MB",
                            done / 1_048_576, total / 1_048_576);
                    lastLog = now;
                }
            }
        } finally { conn.disconnect(); }
        LOGGER.info("[Minesis Vosk] Download complete: {}", dest.getName());
    }

    private static void extractZip(File zipFile, File destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            long lastLog = 0;
            int count = 0;
            while ((entry = zis.getNextEntry()) != null) {
                File out = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    out.mkdirs();
                } else {
                    out.getParentFile().mkdirs();
                    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(out))) {
                        byte[] buf = new byte[65536];
                        int n;
                        while ((n = zis.read(buf)) != -1) os.write(buf, 0, n);
                    }
                    count++;
                    long now = System.currentTimeMillis();
                    if (now - lastLog > 2000) {
                        LOGGER.info("[Minesis Vosk] Extracting model... {} files", count);
                        lastLog = now;
                    }
                }
                zis.closeEntry();
            }
        }
    }
}
