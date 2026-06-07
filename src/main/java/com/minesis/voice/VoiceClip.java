package com.minesis.voice;

public class VoiceClip {
    public final byte[] audio;
    public final VoiceContext context;

    public VoiceClip(byte[] audio, VoiceContext context) {
        this.audio = audio;
        this.context = context;
    }
}
