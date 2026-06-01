package com.mimesis.voice;

import java.util.UUID;

/**
 * Handles voice capture from Simple Voice Chat API
 * Receives audio streams and intelligently processes them
 * Uses SmartAudioClipManager for optimal audio chunk management
 */
public class VoiceCaptureHandler {
    private final UUID playerUUID;
    private final String playerName;
    private boolean isCapturing = false;
    private boolean isPhrasePending = false;
    private int silenceFrames = 0;
    private static final int SILENCE_THRESHOLD_FRAMES = 10; // ~100ms at 10ms per frame

    public VoiceCaptureHandler(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
    }

    /**
     * Called when audio data is received from a player
     * Uses SmartAudioClipManager for intelligent chunking
     */
    public void onAudioData(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return;
        }

        if (!isCapturing) {
            return;
        }

        // Check if this chunk contains speech
        if (VoiceActivityDetector.isSpeech(audioData)) {
            // Speech detected
            if (!isPhrasePending) {
                // Start new phrase
                SmartAudioClipManager.startRecording(this.playerUUID, this.playerName);
                isPhrasePending = true;
            }

            // Add chunk to recording
            SmartAudioClipManager.addAudioChunk(this.playerUUID, audioData);
            silenceFrames = 0; // Reset silence counter
        } else {
            // Silence detected
            silenceFrames++;

            // If we have an active phrase, add silence chunks
            if (isPhrasePending) {
                SmartAudioClipManager.addAudioChunk(this.playerUUID, audioData);

                // If silence exceeds threshold, finalize phrase
                if (silenceFrames >= SILENCE_THRESHOLD_FRAMES) {
                    finalizePhrase();
                }
            }
        }
    }

    /**
     * Start capturing voice from the player
     */
    public void start() {
        this.isCapturing = true;
        this.isPhrasePending = false;
        this.silenceFrames = 0;
        SmartAudioClipManager.startRecording(this.playerUUID, this.playerName);
    }

    /**
     * Stop capturing voice from the player
     */
    public void stop() {
        this.isCapturing = false;
        if (isPhrasePending) {
            finalizePhrase();
        }
    }

    /**
     * Finalize current phrase and store as clip
     */
    private void finalizePhrase() {
        if (isPhrasePending) {
            SmartAudioClipManager.stopRecording(this.playerUUID);
            isPhrasePending = false;
            silenceFrames = 0;
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

    /**
     * Get stored clips count
     */
    public int getStoredClipCount() {
        return VoiceStorage.getClipCount(this.playerUUID);
    }

    /**
     * Get current recording energy level
     */
    public float getCurrentEnergyLevel() {
        return SmartAudioClipManager.getRecordingEnergyLevel(this.playerUUID);
    }
}
