package com.mimesis.voice;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mimesis.entity.MimesisEntity;
import com.mimesis.utils.AudioUtils;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.server.level.ServerLevel;

/**
 * Handles playback of stored voice clips through Voice Chat API
 */
public class VoicePlaybackService {
    private static final Map<UUID, AudioPlayer> activePlayers = new ConcurrentHashMap<>();
    private static VoicechatServerApi voicechatApi = null;
    // remember last clip hash per mimesis entity to avoid repeats
    private static final Map<UUID, Integer> lastClipHashByEntity = new ConcurrentHashMap<>();

    public static void setVoicechatApi(VoicechatServerApi api) {
        voicechatApi = api;
    }

    /**
     * Play a voice clip from a stored source through a Mimesis entity
     */
    public static void playVoiceClip(MimesisEntity mimesisEntity, UUID sourcePlayerUUID, VoicechatServerApi api) {
        if (api == null || mimesisEntity == null || mimesisEntity.level().isClientSide) {
            return;
        }

        byte[] audioData = null;
        int attempts = 0;
        int lastHash = lastClipHashByEntity.getOrDefault(mimesisEntity.getUUID(), 0);
        while (attempts < 6) {
            audioData = VoiceStorage.getRandomVoiceClip(sourcePlayerUUID);
            if (audioData == null || audioData.length == 0) {
                return;
            }
            int h = java.util.Arrays.hashCode(audioData);
            if (h != lastHash) {
                lastClipHashByEntity.put(mimesisEntity.getUUID(), h);
                break;
            }
            attempts++;
        }
        if (audioData == null || audioData.length == 0) return;

        try {
            // Create entity audio channel
            UUID channelId = UUID.randomUUID();
            de.maxhenkel.voicechat.api.Entity voicechatEntity = api.fromEntity(mimesisEntity);
            AudioChannel audioChannel = api.createEntityAudioChannel(channelId, voicechatEntity);
            if (audioChannel == null) {
                return;
            }

            // Encode audio to Opus
            OpusEncoder encoder = api.createEncoder();
            if (encoder == null || encoder.isClosed()) {
                return;
            }

            // Convert PCM bytes to shorts
            short[] pcmShorts = AudioUtils.bytesToShorts(audioData);
            if (mimesisEntity.isHostileModeActive()) {
                pcmShorts = applyHostileVoiceEffect(pcmShorts);
            }

            // Create audio player with direct audio data
            AudioPlayer player = api.createAudioPlayer(audioChannel, encoder, pcmShorts);
            if (player == null) {
                encoder.close();
                return;
            }

            // Store and start playback
            UUID playerId = UUID.randomUUID();
            activePlayers.put(playerId, player);

            player.setOnStopped(() -> {
                activePlayers.remove(playerId);
                encoder.close();
            });

            player.startPlaying();
        } catch (Exception e) {
            System.err.println("[Mimesis] Error playing voice clip: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void stopAllPlayback() {
        for (AudioPlayer player : activePlayers.values()) {
            if (player != null && player.isPlaying()) {
                player.stopPlaying();
            }
        }
        activePlayers.clear();
    }

    public static int getActivePlayerCount() {
        return activePlayers.size();
    }

    private static short[] applyHostileVoiceEffect(short[] pcmShorts) {
        short[] transformed = new short[pcmShorts.length];
        int previous = 0;
        int[] reverbTail = new int[2205];

        for (int i = 0; i < pcmShorts.length; i++) {
            int sample = pcmShorts[i];
            int delayed = reverbTail[i % reverbTail.length];
            int filtered = (int) (previous * 0.68D + sample * 0.32D);
            filtered = (int) (filtered * 0.84D) + (delayed / 3);
            filtered = (int) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, filtered));

            if ((i & 63) == 0) {
                filtered = (int) (filtered * 0.92D);
            }

            transformed[i] = (short) filtered;
            previous = filtered;
            reverbTail[i % reverbTail.length] = (int) (filtered * 0.45D);
        }

        return transformed;
    }
}
