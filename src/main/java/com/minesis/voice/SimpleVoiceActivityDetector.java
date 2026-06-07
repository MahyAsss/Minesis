package com.minesis.voice;

/**
 * Simplified Voice Activity Detection
 */
public class SimpleVoiceActivityDetector {
    private static final float SILENCE_THRESHOLD = 0.03f;
    
    public static boolean isSpeech(byte[] audioData) {
        if (audioData == null || audioData.length < 2) return false;
        
        long sum = 0;
        for (byte b : audioData) {
            sum += (long) b * b;
        }
        double rms = Math.sqrt((double) sum / audioData.length);
        return (rms / 128.0) > SILENCE_THRESHOLD;
    }
    
    public static byte[] trimSilence(byte[] audioData) {
        if (audioData == null || audioData.length < 100) return audioData;
        return audioData; // Placeholder
    }
}
