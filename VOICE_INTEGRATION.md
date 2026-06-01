# Simple Voice Chat API Integration Guide

## Overview

The Mimesis mod is designed to integrate with the **Simple Voice Chat** mod to capture player voices and replay them through the cloned Mimesis entities. This guide explains the integration architecture and how to implement voice capture.

## Simple Voice Chat API Basics

Simple Voice Chat provides a plugin API that allows mods to:
- Capture voice streams from players
- Access audio data in real-time
- Playback custom audio through the voice chat system
- Manage voice groups and channels

## Integration Architecture

### Current Implementation

The mod provides the following components for voice integration:

#### 1. VoiceManager (`voice/VoiceManager.java`)
Central coordination point for voice features:
```java
// Check if Simple Voice Chat is available
VoiceManager.isVoiceChatAvailable()

// Register a capture handler for a player
VoiceManager.registerCaptureHandler(playerUUID, handler)

// Get capture handler for a player
VoiceManager.getCaptureHandler(playerUUID)

// Stop all captures
VoiceManager.stopAllCaptures()
```

#### 2. VoiceCaptureHandler (`voice/VoiceCaptureHandler.java`)
Per-player voice capture:
```java
// Create handler for a player
VoiceCaptureHandler handler = new VoiceCaptureHandler(playerUUID, playerName);

// Process incoming audio
handler.onAudioData(audioBytes)

// Control capture
handler.start()
handler.stop()
```

#### 3. VoiceStorage (`voice/VoiceStorage.java`)
Persistent audio storage:
```java
// Store audio clip
VoiceStorage.storeVoiceClip(playerUUID, audioData)

// Retrieve random clip
byte[] clip = VoiceStorage.getRandomVoiceClip(playerUUID)

// Check if clips exist
VoiceStorage.hasVoiceClips(playerUUID)
```

#### 4. SimpleVoiceChatIntegration (`voice/SimpleVoiceChatIntegration.java`)
API-level integration (framework for implementation):
```java
// Check if mod is loaded
SimpleVoiceChatIntegration.isSimpleVoiceChatLoaded()

// Register voice capture
SimpleVoiceChatIntegration.registerPlayerVoiceCapture(playerUUID)

// Playback voice
SimpleVoiceChatIntegration.playbackVoice(playerUUID, audioData)
```

## Implementing Voice Capture

### Step 1: Add Event Listener

Create a new file `src/main/java/com/mimesis/client/VoiceEventHandler.java`:

```java
package com.mimesis.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import de.maxhenkel.voicechat.api.events.AudioReceivedEvent;

@Mod.EventBusSubscriber(modid = "mimesis", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class VoiceEventHandler {
    
    @SubscribeEvent
    public static void onAudioReceived(AudioReceivedEvent event) {
        // This is called when voice chat audio is received
        byte[] audioData = event.getAudioData();
        UUID playerUUID = event.getPlayerUUID();
        
        // Store the audio
        VoiceStorage.storeVoiceClip(playerUUID, audioData);
    }
}
```

### Step 2: Register with Simple Voice Chat API

Implement `VoicechatPlugin` interface:

```java
package com.mimesis.voice;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;

public class MimesisVoicechatPlugin implements VoicechatPlugin {
    
    @Override
    public String getPluginId() {
        return "mimesis";
    }
    
    @Override
    public void registerEvents(EventRegistration registration) {
        // Register voice events with Simple Voice Chat
        registration.registerEventListener(new MimesisVoiceEventListener());
    }
}
```

### Step 3: Handle Voice Playback

Implement playback through entities:

```java
// In MimesisEntity.java
private void attemptVoicePlayback() {
    UUID playerUUID = this.getTargetPlayerUUID();
    if (playerUUID != null) {
        byte[] audioData = VoiceStorage.getRandomVoiceClip(playerUUID);
        if (audioData != null) {
            // Send to client for playback
            SimpleVoiceChatIntegration.playbackVoice(playerUUID, audioData);
        }
    }
}
```

## Audio Data Format

Simple Voice Chat uses:
- **Sample Rate**: 48 kHz
- **Bit Depth**: 16-bit PCM
- **Channels**: Mono
- **Format**: Raw byte array

### Working with Audio Data

Use `AudioUtils` helper class:

```java
// Get duration of audio in milliseconds
long durationMs = AudioUtils.getAudioDuration(audioData);

// Convert bytes to shorts (16-bit samples)
short[] samples = AudioUtils.bytesToShorts(audioData);

// Trim to maximum duration
byte[] trimmed = AudioUtils.trimAudio(audioData, 3000); // 3 seconds
```

## Network Synchronization

Voice data is synchronized using the `VoiceNetworking` system:

```java
// Send voice playback packet to client
VoiceNetworking.INSTANCE.sendToPlayer(
    new VoiceNetworking.VoicePlaybackPacket(entityId, audioData),
    player
);
```

## Configuration

Voice settings are managed in `utils/MimesisConfig.java`:

```java
// Maximum clips stored per player
MAX_VOICE_CLIPS_PER_PLAYER = 5

// Maximum duration per clip (milliseconds)
MAX_VOICE_CLIP_DURATION_MS = 3000

// How often to replay voice (ticks)
VOICE_REPLAY_INTERVAL = 100

// When entity becomes hostile (ticks)
HOSTILE_ACTIVATION_TIME_MIN = 600
```

## Simple Voice Chat Dependency

### Adding to build.gradle

The mod already includes Simple Voice Chat API in `build.gradle`:

```gradle
// Simple Voice Chat API
compileOnly 'de.maxhenkel.voicechat:voicechat_api:1.0.6'
runtimeOnly fg.deobf('de.maxhenkel.voicechat:voicechat:1.20.1-2.4.32:api')
```

### Version Compatibility

- **Simple Voice Chat**: 2.4.32+ for 1.20.1
- **Minecraft**: 1.20.1
- **Forge**: 47.2.0+

## Testing Voice Integration

### 1. Setup Test Environment
```bash
./gradlew runClient
```

### 2. Install Simple Voice Chat Mod
Add Simple Voice Chat to the mods folder in your run directory.

### 3. Test Voice Capture
1. Create world
2. Enable voice chat in game
3. Talk into microphone
4. Check if audio is being stored in `VoiceStorage`

### 4. Test Voice Playback
1. Use command: `/mimesis spawn`
2. Wait for entity to spawn
3. Listen for your own voice replayed by entity

## Debugging

### Enable Logging
Add to your logger configuration:

```java
// In MimesisVoiceEventListener
if (audioData != null) {
    System.out.println("Captured audio: " + audioData.length + " bytes");
    System.out.println("Duration: " + AudioUtils.getAudioDuration(audioData) + "ms");
}
```

### Check Voice Storage
```java
// In-game debug command
int clips = VoiceStorage.getClipCount(playerUUID);
System.out.println("Stored clips for player: " + clips);
```

## Common Issues

### Voice Not Being Captured
- Check if Simple Voice Chat is installed
- Verify `isSimpleVoiceChatLoaded()` returns true
- Check event registration

### Voice Not Playing Back
- Verify audio data is stored in `VoiceStorage`
- Check network synchronization
- Ensure entity is within hearing distance

### Audio Quality Issues
- Check sample rate (should be 48kHz)
- Verify audio data is properly formatted
- Trim long clips to avoid memory issues

## Future Enhancements

Potential improvements to voice integration:

1. **Audio Effects**
   - Echo/reverb effects
   - Pitch shifting
   - Distortion/corruption

2. **Advanced Storage**
   - Persistent audio file storage
   - Compression
   - Cloud sync

3. **Voice Recognition**
   - Detect specific words
   - Emotion analysis
   - Speaker identification

4. **Multi-Entity Voices**
   - Multiple clones with different voice patterns
   - Chorus effects
   - Spatial audio

## Resources

- [Simple Voice Chat GitHub](https://github.com/henkelmax/simple-voice-chat)
- [Simple Voice Chat API Docs](https://github.com/henkelmax/simple-voice-chat/wiki)
- [Minecraft Audio Formats](https://wiki.vg/Protocol#Sound_Effect)
- [Java Audio API](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/module-summary.html)

## Implementation Checklist

- [ ] Verify Simple Voice Chat dependency in build.gradle
- [ ] Implement VoicechatPlugin interface
- [ ] Create event listener for voice capture
- [ ] Test voice capture and storage
- [ ] Implement voice playback
- [ ] Test full integration
- [ ] Fine-tune audio quality and timing
- [ ] Optimize memory usage
- [ ] Add configuration options
- [ ] Document custom integrations
