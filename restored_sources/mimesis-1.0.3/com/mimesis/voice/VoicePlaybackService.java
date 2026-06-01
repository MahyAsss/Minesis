/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  de.maxhenkel.voicechat.api.Entity
 *  de.maxhenkel.voicechat.api.VoicechatServerApi
 *  de.maxhenkel.voicechat.api.audiochannel.AudioChannel
 *  de.maxhenkel.voicechat.api.audiochannel.AudioPlayer
 *  de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel
 *  de.maxhenkel.voicechat.api.opus.OpusEncoder
 */
package com.mimesis.voice;

import com.mimesis.entity.MimesisEntity;
import com.mimesis.utils.AudioUtils;
import com.mimesis.voice.VoiceStorage;
import de.maxhenkel.voicechat.api.Entity;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VoicePlaybackService {
    private static final Map<UUID, AudioPlayer> activePlayers = new ConcurrentHashMap<UUID, AudioPlayer>();
    private static VoicechatServerApi voicechatApi = null;
    private static final Map<UUID, Integer> lastClipHashByEntity = new ConcurrentHashMap<UUID, Integer>();

    public static void setVoicechatApi(VoicechatServerApi api) {
        voicechatApi = api;
    }

    public static void playVoiceClip(MimesisEntity mimesisEntity, UUID sourcePlayerUUID, VoicechatServerApi api) {
        if (api == null || mimesisEntity == null || mimesisEntity.m_9236_().f_46443_) {
            return;
        }
        byte[] audioData = null;
        int lastHash = lastClipHashByEntity.getOrDefault(mimesisEntity.m_20148_(), 0);
        for (int attempts = 0; attempts < 6; ++attempts) {
            audioData = VoiceStorage.getRandomVoiceClip(sourcePlayerUUID);
            if (audioData == null || audioData.length == 0) {
                return;
            }
            int h = Arrays.hashCode(audioData);
            if (h == lastHash) continue;
            lastClipHashByEntity.put(mimesisEntity.m_20148_(), h);
            break;
        }
        if (audioData == null || audioData.length == 0) {
            return;
        }
        try {
            AudioPlayer player;
            UUID channelId = UUID.randomUUID();
            Entity voicechatEntity = api.fromEntity((Object)mimesisEntity);
            EntityAudioChannel audioChannel = api.createEntityAudioChannel(channelId, voicechatEntity);
            if (audioChannel == null) {
                return;
            }
            OpusEncoder encoder = api.createEncoder();
            if (encoder == null || encoder.isClosed()) {
                return;
            }
            short[] pcmShorts = AudioUtils.bytesToShorts(audioData);
            if (mimesisEntity.isHostileModeActive()) {
                pcmShorts = VoicePlaybackService.applyHostileVoiceEffect(pcmShorts);
            }
            if ((player = api.createAudioPlayer((AudioChannel)audioChannel, encoder, pcmShorts)) == null) {
                encoder.close();
                return;
            }
            UUID playerId = UUID.randomUUID();
            activePlayers.put(playerId, player);
            player.setOnStopped(() -> {
                activePlayers.remove(playerId);
                encoder.close();
            });
            player.startPlaying();
        }
        catch (Exception e) {
            System.err.println("[Mimesis] Error playing voice clip: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void stopAllPlayback() {
        for (AudioPlayer player : activePlayers.values()) {
            if (player == null || !player.isPlaying()) continue;
            player.stopPlaying();
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
        for (int i = 0; i < pcmShorts.length; ++i) {
            short sample = pcmShorts[i];
            int delayed = reverbTail[i % reverbTail.length];
            int filtered = (int)((double)previous * 0.68 + (double)sample * 0.32);
            filtered = (int)((double)filtered * 0.84) + delayed / 3;
            filtered = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, filtered));
            if ((i & 0x3F) == 0) {
                filtered = (int)((double)filtered * 0.92);
            }
            transformed[i] = (short)filtered;
            previous = filtered;
            reverbTail[i % reverbTail.length] = (int)((double)filtered * 0.45);
        }
        return transformed;
    }
}

