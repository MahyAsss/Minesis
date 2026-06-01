# Mimesis Mod - Quick Reference

## Installation & Setup (2 minutes)

```bash
# 1. Navigate to project
cd c:\Users\MahyAss\Desktop\Mimesis

# 2. Generate IDE files
./gradlew genIntellijRuns    # For IntelliJ
# or
./gradlew genEclipseRuns     # For Eclipse

# 3. Open in IDE and reload
# IntelliJ: Reload Gradle Project
# Eclipse: Refresh project

# 4. Configure Java 17 (Project Structure → SDK)
```

## Common Commands

```bash
# Build the mod
./gradlew build

# Run in-game
./gradlew runClient

# Run dedicated server
./gradlew runServer

# Clean and rebuild
./gradlew clean build

# Check for errors
./gradlew check
```

## File Locations

| Purpose | File |
|---------|------|
| Main mod class | `src/main/java/com/mimesis/MimesisMod.java` |
| Entity logic | `src/main/java/com/mimesis/entity/MimesisEntity.java` |
| Voice capture | `src/main/java/com/mimesis/voice/VoiceStorage.java` |
| AI behavior | `src/main/java/com/mimesis/ai/` |
| Rendering | `src/main/java/com/mimesis/client/MimesisEntityRenderer.java` |
| Configuration | `src/main/java/com/mimesis/utils/MimesisConfig.java` |
| Metadata | `src/main/resources/META-INF/mods.toml` |

## In-Game Testing

```
Command: /mimesis spawn
├── Spawns entity 10-20 blocks away
├── Entity follows you
├── After 30 seconds → becomes hostile
└── Replays your voice every 5 seconds
```

## Key Code Snippets

### Spawn Entity Manually
```java
ServerEvents.spawnMimesisNearPlayer(player);
```

### Check Voice Clips
```java
int clipCount = VoiceStorage.getClipCount(playerUUID);
boolean hasClips = VoiceStorage.hasVoiceClips(playerUUID);
```

### Adjust Timing
Edit `MimesisEntity.java`:
```java
private int hostileActivationTime = 600;      // Ticks until hostile
private int VOICE_REPLAY_INTERVAL = 100;      // Ticks between replays
```

### Configuration
Edit `utils/MimesisConfig.java`:
```java
// Max clips stored per player
MAX_VOICE_CLIPS_PER_PLAYER = 5

// Seconds before entity attacks
HOSTILE_ACTIVATION_TIME_MIN = 600
HOSTILE_ACTIVATION_TIME_MAX = 2400
```

## Mod Properties

| Property | Value |
|----------|-------|
| Mod ID | `mimesis` |
| Version | `1.0.0` |
| MC Version | `1.20.1` |
| Forge Version | `47.2.0+` |
| Java | `17+` |

## Architecture Overview

```
┌─────────────────────────────────────┐
│      Minecraft Forge 1.20.1         │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │   MimesisEntity (Monster)   │   │
│  │  ├─ Follow Goal             │   │
│  │  ├─ Attack Goal             │   │
│  │  └─ Voice Playback          │   │
│  └─────────────────────────────┘   │
│           ▲        ▲                 │
│           │        │                 │
│  ┌────────┴──┐  ┌──┴─────────────┐  │
│  │VoiceStorage│  │EntityRenderer  │  │
│  │ Audio clips│  │ Skin copying   │  │
│  └────────────┘  └────────────────┘  │
│           │                           │
│  ┌────────▼──────────────────────┐   │
│  │  Simple Voice Chat API        │   │
│  │  (Audio Capture & Playback)   │   │
│  └───────────────────────────────┘   │
└─────────────────────────────────────┘
```

## Behavior Timeline

```
Time:  0s          30s         Infinity
State: [Following] [Transitioning] [Hostile]
       - Quiet     - Building   - Attacks
       - Follows   - Tension    - Damage
       - Watches   - Last calls  - Kill
Voice: Every 5s    Every 3s     Silent
```

## Debugging Tips

### Enable Console Logging
```bash
./gradlew runClient --args "nogui"
```

### Check Voice Storage
```java
// Add to any method
System.out.println("Clips: " + VoiceStorage.getClipCount(playerUUID));
```

### Monitor Entity Behavior
In-game: Check entity data with command block
```
/data get entity @e[type=mimesis:mimesis_entity] Data
```

## Integration Checklist

- [x] Entity registration
- [x] AI goals setup
- [x] Rendering framework
- [x] Voice storage system
- [x] Network synchronization
- [x] Commands registered
- [ ] Simple Voice Chat event listeners
- [ ] Audio capture implementation
- [ ] Audio playback setup

## Performance Considerations

- **Voice Clips**: Max 5 per player in memory
- **Entity Count**: Spawn throttling (optional)
- **Audio Data**: ~3 second max clips
- **Update Rate**: 3 ticks (60ms)
- **Tracking Range**: 10 blocks client-side

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Entity doesn't spawn | Check error logs, verify entity registry |
| Entity doesn't move | Check navigation system, verify AI goals |
| No rendering | Verify renderer registration in ClientSetup |
| Voice not working | Install Simple Voice Chat mod |
| Compilation error | Ensure Java 17, reload Gradle |

## Documentation Files

| Document | Purpose |
|----------|---------|
| README.md | Feature overview |
| GETTING_STARTED.md | Setup guide |
| DEVELOPMENT.md | Architecture details |
| VOICE_INTEGRATION.md | Voice implementation |
| PROJECT_SUMMARY.md | Complete project info |
| QUICK_REFERENCE.md | This file |

## Useful Links

- [Minecraft Forge Docs](https://docs.minecraftforge.net/)
- [Simple Voice Chat](https://github.com/henkelmax/simple-voice-chat)
- [Java 17 Docs](https://docs.oracle.com/en/java/javase/17/)
- [Gradle Guide](https://docs.gradle.org/)

## IDE Shortcuts

### IntelliJ
- `Ctrl+N` → Find class
- `Ctrl+B` → Go to definition
- `Ctrl+H` → Find usages
- `Shift+F6` → Rename

### VS Code
- `Ctrl+P` → Find file
- `F12` → Go to definition
- `Shift+H` → Find all references
- `F2` → Rename

## Next: Voice Integration

See `VOICE_INTEGRATION.md` for:
1. Simple Voice Chat API setup
2. Event listener implementation
3. Audio capture configuration
4. Playback testing

---

**Ready to develop?**
1. Run `./gradlew genIntellijRuns`
2. Open in IDE
3. Run `./gradlew runClient`
4. Type `/mimesis spawn` in-game

Happy modding! 🎮
