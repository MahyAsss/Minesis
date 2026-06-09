package com.minesis.voice;

public class VoiceClip {
    public final byte[] audio;
    public final VoiceContext context;
    public final float rms;        // normalized [0,1]; computed once at store time
    public String transcript;      // set asynchronously via ClipAnnotationPacket

    public VoiceClip(byte[] audio, VoiceContext context) {
        this.audio      = audio;
        this.context    = context;
        this.rms        = com.minesis.utils.AudioUtils.computeRms(audio);
        this.transcript = "";
    }

    /** True if this clip has enough energy to represent real speech (not white noise). */
    public boolean isSpeech() {
        return rms >= com.minesis.utils.MinesisConfig.SPEECH_RMS_THRESHOLD.get().floatValue();
    }
}
