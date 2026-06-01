# Mimesis Mod Development Guide

## Project Structure

```
mimesis/
├── src/main/
│   ├── java/com/mimesis/
│   │   ├── MimesisMod.java                 # Main mod class
│   │   ├── ServerEvents.java               # Server-side events
│   │   ├── entity/
│   │   │   ├── MimesisEntity.java          # Custom entity implementation
│   │   │   └── MimesisEntities.java        # Entity registry
│   │   ├── ai/
│   │   │   ├── MimesisFollowGoal.java      # Follow/tracking behavior
│   │   │   └── MimesisAttackGoal.java      # Attack behavior
│   │   ├── voice/
│   │   │   ├── VoiceManager.java           # Voice chat integration
│   │   │   ├── VoiceCaptureHandler.java    # Audio capture per player
│   │   │   └── VoiceStorage.java           # Audio clip storage
│   │   ├── client/
│   │   │   ├── ClientSetup.java            # Client-side registration
│   │   │   └── MimesisEntityRenderer.java  # Custom renderer
│   │   └── commands/
│   │       └── MimesisCommand.java         # Chat commands
│   └── resources/
│       ├── META-INF/
│       │   ├── mods.toml                   # Mod metadata
│       │   └── MANIFEST.MF                 # JAR manifest
│       └── assets/mimesis/                 # Assets (textures, models, etc.)
├── build.gradle                            # Gradle build configuration
├── gradle.properties                       # Gradle properties
├── settings.gradle                         # Gradle settings
└── README.md                              # This file
```

## Building

### Prerequisites
- Java 17+
- Gradle (or use gradlew)

### Build Commands

```bash
# Generate IDE files (optional, for IntelliJ/Eclipse)
./gradlew genIntellijRuns  # IntelliJ
./gradlew genEclipseRuns   # Eclipse

# Build the mod
./gradlew build

# Run in-game (development)
./gradlew runClient        # Run client
./gradlew runServer        # Run server

# Build JAR
./gradlew build            # Creates mimesis-1.0.0.jar in build/libs/
```

## Key Classes

### MimesisEntity
- Extends `Monster`
- Stores target player UUID and name
- Manages behavior phases (follow → hostile)
- Synced data: UUID, name, timer
- Auto-saves to NBT

### VoiceStorage
Thread-safe audio storage system:
- `storeVoiceClip()`: Store audio for a player
- `getRandomVoiceClip()`: Retrieve random clip
- `hasVoiceClips()`: Check if clips exist
- `clearVoiceClips()`: Clear player audio

### VoiceCaptureHandler
Receives audio from Simple Voice Chat:
- Activated when player starts speaking
- Buffers audio data
- Passes to VoiceStorage
- Per-player instance

## Simple Voice Chat API Integration

The mod is designed to work with the Simple Voice Chat mod. The API allows:

1. **Audio Stream Access**: Intercept player voice streams
2. **Audio Playback**: Play audio from custom entities
3. **Voice Group Management**: Entity can join/leave voice groups

### Example Integration Points

```java
// In VoiceCaptureHandler
public void onAudioData(byte[] audioData) {
    VoiceStorage.storeVoiceClip(this.playerUUID, audioData);
}

// In MimesisEntity
private void attemptVoicePlayback() {
    UUID playerUUID = this.getTargetPlayerUUID();
    if (playerUUID != null) {
        VoiceStorage.playStoredVoiceClip(playerUUID, this);
    }
}
```

## Customization

### Behavior Timing
Edit in `MimesisEntity.java`:
```java
private int hostileActivationTime = 600; // Change this value (in ticks)
private static final int VOICE_REPLAY_INTERVAL = 100; // Voice replay frequency
```

### Audio Storage Limits
Edit in `VoiceStorage.java`:
```java
private static final int MAX_CLIPS_PER_PLAYER = 5;        // Max clips to store
private static final int VOICE_CLIP_MAX_DURATION = 3000;  // Max clip length (ms)
```

### Spawn Distance
Edit in `ServerEvents.java`:
```java
double distance = 10 + Math.random() * 10; // Spawn 10-20 blocks away
```

## Debugging

### Enable Debug Logging
In `gradle.properties`, ensure debug is enabled:
```properties
org.gradle.jvmargs=-Xmx3G
```

### Check Logs
- Client: `logs/latest.log`
- Server: console output

### Test Commands
```
/mimesis spawn          # Spawn one entity
/mimesis clear          # Clear all entities
```

## Dependencies

### Required
- Minecraft Forge 1.20.1 (47.2.0+)
- Minecraft 1.20.1

### Optional
- Simple Voice Chat 2.4.32+ (for full voice features)

## License

MIT License - See LICENSE file (to be created)

## Future Enhancements

- [ ] Advanced voice modulation (pitch shift, echo)
- [ ] Multiple clones per player
- [ ] Configurable difficulty/behavior
- [ ] Sound event integration
- [ ] Custom attack patterns
- [ ] NBT persistence for clones
- [ ] Admin controls for entity management
