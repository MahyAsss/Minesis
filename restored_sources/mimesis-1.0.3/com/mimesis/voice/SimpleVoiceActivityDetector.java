/*
 * Decompiled with CFR 0.152.
 */
package com.mimesis.voice;

public class SimpleVoiceActivityDetector {
    private static final float SILENCE_THRESHOLD = 0.03f;

    public static boolean isSpeech(byte[] audioData) {
        if (audioData == null || audioData.length < 2) {
            return false;
        }
        long sum = 0L;
        for (byte b : audioData) {
            sum += (long)b * (long)b;
        }
        double rms = Math.sqrt((double)sum / (double)audioData.length);
        return rms / 128.0 > (double)0.03f;
    }

    public static byte[] trimSilence(byte[] audioData) {
        if (audioData == null || audioData.length < 100) {
            return audioData;
        }
        return audioData;
    }
}

