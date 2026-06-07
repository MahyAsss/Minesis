package com.minesis.voice;

import java.util.UUID;

public class VoiceCaptureHandler {
    private final UUID playerUUID;
    private final String playerName;
    private boolean isCapturing    = false;
    private boolean isPhrasePending = false;
    private int silenceFrames      = 0;
    private VoiceContext phraseContext = VoiceContext.IDLE;

    // ~100 ms of silence (each frame ≈ 10 ms) before finalizing a phrase
    private static final int SILENCE_THRESHOLD_FRAMES = 10;

    public VoiceCaptureHandler(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
    }

    /**
     * Called on every audio packet.
     * @param context what the player was doing at this instant (computed by the plugin).
     */
    public void onAudioData(byte[] audioData, VoiceContext context) {
        if (audioData == null || audioData.length == 0 || !isCapturing) return;

        if (VoiceActivityDetector.isSpeech(audioData)) {
            if (!isPhrasePending) {
                // Phrase starts: lock the context for the whole phrase
                this.phraseContext = context;
                SmartAudioClipManager.startRecording(playerUUID, playerName, phraseContext);
                isPhrasePending = true;
            }
            SmartAudioClipManager.addAudioChunk(playerUUID, audioData);
            silenceFrames = 0;
        } else {
            silenceFrames++;
            if (isPhrasePending) {
                SmartAudioClipManager.addAudioChunk(playerUUID, audioData);
                if (silenceFrames >= SILENCE_THRESHOLD_FRAMES) {
                    finalizePhrase();
                }
            }
        }
    }

    public void start() {
        this.isCapturing    = true;
        this.isPhrasePending = false;
        this.silenceFrames  = 0;
        this.phraseContext  = VoiceContext.IDLE;
    }

    public void stop() {
        this.isCapturing = false;
        if (isPhrasePending) finalizePhrase();
    }

    private void finalizePhrase() {
        if (isPhrasePending) {
            SmartAudioClipManager.stopRecording(playerUUID);
            isPhrasePending = false;
            silenceFrames   = 0;
        }
    }

    public boolean isCapturing()     { return isCapturing; }
    public boolean isPhrasePending() { return isPhrasePending; }
    public UUID getPlayerUUID()      { return playerUUID; }
    public String getPlayerName()    { return playerName; }

    public int getStoredClipCount() {
        return VoiceStorage.getClipCount(playerUUID);
    }

    public float getCurrentEnergyLevel() {
        return SmartAudioClipManager.getRecordingEnergyLevel(playerUUID);
    }
}
