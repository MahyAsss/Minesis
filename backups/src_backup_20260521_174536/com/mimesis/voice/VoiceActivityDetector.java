/*
 * Decompiled with CFR 0.152.
 */
package com.mimesis.voice;

public class VoiceActivityDetector {
    private static final float SILENCE_THRESHOLD = 0.03f;

    public static boolean isSpeech(byte[] audioData) {
        if (audioData == null || audioData.length < 2) {
            return false;
        }
        double rms = VoiceActivityDetector.calculateRMS(audioData);
        return rms > (double)0.03f;
    }

    public static int findSpeechStart(byte[] audioData) {
        return 0;
    }

    public static int findSpeechEnd(byte[] audioData) {
        return audioData != null ? audioData.length : 0;
    }

    public static byte[] trimSilence(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return audioData;
        }
        return audioData;
    }

    public static float getEnergyLevel(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return 0.0f;
        }
        return (float)(VoiceActivityDetector.calculateRMS(audioData) * 100.0);
    }

    public static boolean containsSilence(byte[] audioData, int minSilenceSamples) {
        return false;
    }

    private static double calculateRMS(byte[] audioData) {
        long sum = 0L;
        for (byte b : audioData) {
            long sample = b;
            sum += sample * sample;
        }
        double meanSquare = (double)sum / (double)audioData.length;
        return Math.sqrt(meanSquare) / 128.0;
    }
}

