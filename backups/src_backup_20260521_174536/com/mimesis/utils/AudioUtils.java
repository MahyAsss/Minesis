/*
 * Decompiled with CFR 0.152.
 */
package com.mimesis.utils;

import java.nio.ByteBuffer;

public class AudioUtils {
    public static short[] bytesToShorts(byte[] bytes) {
        short[] shorts = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes).asShortBuffer().get(shorts);
        return shorts;
    }

    public static byte[] shortsToBytes(short[] shorts) {
        byte[] bytes = new byte[shorts.length * 2];
        ByteBuffer.wrap(bytes).asShortBuffer().put(shorts);
        return bytes;
    }

    public static long getAudioDuration(byte[] audioData) {
        int sampleCount = audioData.length / 2;
        int sampleRate = 48000;
        return (long)sampleCount * 1000L / (long)sampleRate;
    }

    public static boolean isValidAudioData(byte[] audioData) {
        return audioData != null && audioData.length > 0 && audioData.length % 2 == 0;
    }

    public static byte[] trimAudio(byte[] audioData, int maxDurationMs) {
        if (audioData == null) {
            return null;
        }
        long durationMs = AudioUtils.getAudioDuration(audioData);
        if (durationMs <= (long)maxDurationMs) {
            return audioData;
        }
        int sampleRate = 48000;
        int bytesToKeep = maxDurationMs * sampleRate * 2 / 1000;
        byte[] trimmed = new byte[bytesToKeep];
        System.arraycopy(audioData, 0, trimmed, 0, bytesToKeep);
        return trimmed;
    }
}

