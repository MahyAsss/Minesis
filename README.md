# Mimesis - Minecraft Forge Mod 1.20.1

A horror mod for Minecraft Forge 1.20.1 that spawns entity clones that mimic player voices and behavior using Simple Voice Chat API integration.

## Features

### Core Entity
- **MimesisEntity**: A custom monster entity that copies player appearance and behavior
- Copies player skin dynamically
- Inherits player name and display
- Spawns in proximity to target player

### Voice Integration (Simple Voice Chat API)
- Records and stores player voice clips (audio streams)
- Stores audio by player UUID
- Replays voice clips periodically
- Entity can "speak" using recorded player voices
- Audio storage with size limits (max 5 clips per player)

### Behavior
- **Phase 1 (0-30 seconds)**: Entity follows player without attacking
  - Tracks player position
  - Faces player
  - Occasionally replays voice clips
  
- **Phase 2 (30+ seconds)**: Entity becomes hostile
  - Switches to melee attack mode
  - Attacks the target player
  - Fully aggressive behavior

### Technical Implementation

#### Custom Goals
- `MimesisFollowGoal`: Navigation and tracking logic
- `MimesisAttackGoal`: Hostile behavior and attack logic

#### Voice Management
- `VoiceManager`: Central voice chat integration point
- `VoiceCaptureHandler`: Per-player voice capture
- `VoiceStorage`: Audio clip storage and retrieval

#### Rendering
- `MimesisEntityRenderer`: Custom renderer that copies player skin
- Dynamic skin loading from player UUID

## Commands

```
/mimesis spawn        - Spawn a Mimesis entity near you
/mimesis clear        - Remove all Mimesis entities
/mimesis help         - Show command help
```

## Configuration

Voice clips configuration:
- **MAX_CLIPS_PER_PLAYER**: 5 clips (keep recent recordings)
- **VOICE_CLIP_MAX_DURATION**: 3000ms (3 seconds per clip)
- **VOICE_REPLAY_INTERVAL**: 100 ticks (5 seconds between replays)
- **HOSTILE_ACTIVATION_TIME**: 600 ticks (30 seconds to become hostile)

## Requirements

- Minecraft 1.20.1
- Forge 47.2.0+
- Java 17+
- Simple Voice Chat mod (optional, but recommended for full voice features)

## Installation

1. Download the Mimesis mod JAR
2. Place in your `mods` folder
3. Run Minecraft with Forge
4. The mod will auto-detect Simple Voice Chat if installed

## Audio Stream Integration

The mod is designed to work with Simple Voice Chat API. When a player speaks:

1. Audio is captured from the voice chat stream
2. Short clips are stored in memory (5 per player)
3. MimesisEntity replays these clips periodically
4. Clips are cleared when entity dies or player leaves

## Entity Statistics

- **Type**: Monster
- **Size**: 0.6 x 1.8 blocks (human-like)
- **Tracking Range**: 10 blocks
- **Update Interval**: 3 ticks
- **XP Reward**: 50 XP on death

## Mod ID

`mimesis`

## Version

1.0.0 - Initial Release
