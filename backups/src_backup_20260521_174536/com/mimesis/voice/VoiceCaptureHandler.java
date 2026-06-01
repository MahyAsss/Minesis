/*
 * Decompiled with CFR 0.152.
 */
package com.mimesis.voice;

import com.mimesis.voice.SmartAudioClipManager;
import com.mimesis.voice.VoiceActivityDetector;
import com.mimesis.voice.VoiceStorage;
import java.util.UUID;

public class VoiceCaptureHandler {
    private final UUID playerUUID;
    private final String playerName;
    private boolean isCapturing = false;
    private boolean isPhrasePending = false;
    private int silenceFrames = 0;
    private static final int SILENCE_THRESHOLD_FRAMES = 10;

    public VoiceCaptureHandler(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
    }

    public void onAudioData(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return;
        }
        if (!this.isCapturing) {
            return;
        }
        if (VoiceActivityDetector.isSpeech(audioData)) {
            if (!this.isPhrasePending) {
                SmartAudioClipManager.startRecording(this.playerUUID, this.playerName);
                this.isPhrasePending = true;
            }
            SmartAudioClipManager.addAudioChunk(this.playerUUID, audioData);
            this.silenceFrames = 0;
        } else {
            ++this.silenceFrames;
            if (this.isPhrasePending) {
                SmartAudioClipManager.addAudioChunk(this.playerUUID, audioData);
                if (this.silenceFrames >= 10) {
                    this.finalizePhrase();
                }
            }
        }
    }

    public void start() {
        this.isCapturing = true;
        this.isPhrasePending = false;
        this.silenceFrames = 0;
        SmartAudioClipManager.startRecording(this.playerUUID, this.playerName);
    }

    public void stop() {
        this.isCapturing = false;
        if (this.isPhrasePending) {
            this.finalizePhrase();
        }
    }

    private void finalizePhrase() {
        if (this.isPhrasePending) {
            SmartAudioClipManager.stopRecording(this.playerUUID);
            this.isPhrasePending = false;
            this.silenceFrames = 0;
        }
    }

    public boolean isCapturing() {
        return this.isCapturing;
    }

    public boolean isPhrasePending() {
        return this.isPhrasePending;
    }

    public UUID getPlayerUUID() {
        return this.playerUUID;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public int getStoredClipCount() {
        return VoiceStorage.getClipCount(this.playerUUID);
    }

    public float getCurrentEnergyLevel() {
        return SmartAudioClipManager.getRecordingEnergyLevel(this.playerUUID);
    }
}

