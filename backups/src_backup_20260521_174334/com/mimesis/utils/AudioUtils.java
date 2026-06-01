package com.mimesis.utils;

import java.nio.ByteBuffer;

/**
 * Utility class for audio processing
 */
public class AudioUtils {
    
    /**
     * Convert byte array to short array (16-bit PCM)
     */
    public static short[] bytesToShorts(byte[] bytes) {
        short[] shorts = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes).asShortBuffer().get(shorts);
        return shorts;
    }
    
    /**
     * Convert short array to byte array (16-bit PCM)
     */
    public static byte[] shortsToBytes(short[] shorts) {
        byte[] bytes = new byte[shorts.length * 2];
        ByteBuffer.wrap(bytes).asShortBuffer().put(shorts);
        return bytes;
    }
    
    /**
     * Get audio duration in milliseconds
     * Assumes 48kHz sample rate (Minecraft standard)
     */
    public static long getAudioDuration(byte[] audioData) {
        int sampleCount = audioData.length / 2; // 16-bit samples
        int sampleRate = 48000; // Minecraft uses 48kHz
        return (sampleCount * 1000L) / sampleRate;
    }
    
    /**
     * Check if audio data is valid
     */
    public static boolean isValidAudioData(byte[] audioData) {
        return audioData != null && audioData.length > 0 && audioData.length % 2 == 0;
    }
    
    /**
     * Trim audio to maximum duration
     */
    public static byte[] trimAudio(byte[] audioData, int maxDurationMs) {
        if (audioData == null) {
            return null;
        }
        
        long durationMs = getAudioDuration(audioData);
        if (durationMs <= maxDurationMs) {
            return audioData;
        }
        
        int sampleRate = 48000;
        int bytesToKeep = (maxDurationMs * sampleRate * 2) / 1000;
        
        byte[] trimmed = new byte[bytesToKeep];
        System.arraycopy(audioData, 0, trimmed, 0, bytesToKeep);
        return trimmed;
    }
}
