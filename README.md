# Minesis — Minecraft Forge Mod 1.20.1

A horror mod for Minecraft Forge 1.20.1 that spawns entity clones mimicking player appearance, voice, and behavior using Simple Voice Chat API integration.

> **All Rights Reserved** — © Clément Mahy (MahyAss)

---

## Features

### Core Entity
- Copies the target player's skin, name, and inventory appearance dynamically
- Mimics player-like movement: wanders, interacts with chests, crafts, chops wood, mines ores
- Avoids water and only moves on solid ground
- Plays a dramatic 3-second transformation when turning hostile
- Custom hunt model with procedural animations (walk, attack, transform, tentacles)

### Voice Integration (Simple Voice Chat)
- Records and stores player voice clips via the Simple Voice Chat API
- Replays recorded clips periodically as ambient speech
- Plays direct voice responses when the player speaks nearby
- Configurable RMS noise threshold to filter out background noise
- Clips are cleared when the entity dies or the player disconnects

### Behavior Phases

**Phase 1 — Passive (configurable, default 1–3 minutes after spawn):**
- Wanders the area, mimicking daily activities (chest interactions, crafting, chopping, mining)
- Replays voice clips autonomously every 10–20 minutes
- Does not attack

**Phase 2 — Hostile (after the delay):**
- 3-second freeze with screen darkening, particles, and lightning
- Hunt model revealed with transform animation as screen clears
- Lunges toward the player, melee attacks
- Breaks or opens doors/obstacles to reach the player
- Teleports if stuck for too long (enderman-style)

### World Interaction
- Opens chests and barrels; 30% chance to deposit an *Echoes Below* music disc
- Chops trees, mines ores, interacts with crafting tables and furnaces
- Places cobblestone bridges when blocked in hostile mode
- Never breaks containers, ore blocks, or log blocks as "obstacles"

---

## Commands

All commands require operator permissions (level 2+).

| Command | Description |
|---|---|
| `/minesis me` | Spawn a Minesis clone of yourself |
| `/minesis spawnname <player>` | Spawn a clone of another player (by name) |
| `/minesis voicestatus` | Show voice clip count and API status for your player |
| `/minesis test` | Debug: display voice stats |
| `/minesis fakevoiceclip` | Debug: inject a synthetic voice clip |

---

## Configuration

Config file: `config/minesis-common.toml` (created automatically on first launch).

### `[hostile_behavior]`

| Key | Default | Description |
|---|---|---|
| `min_hostile_delay_seconds` | `60` | Minimum seconds before Minesis can turn hostile |
| `max_hostile_delay_seconds` | `180` | Maximum seconds before Minesis turns hostile |
| `transformation_screen_effects` | `true` | Apply blindness/darkness/nausea to nearby players during transformation |

### `[voice]`

| Key | Default | Description |
|---|---|---|
| `voice_replay_min_seconds` | `600` | Minimum seconds between ambient voice replays |
| `voice_replay_max_seconds` | `1200` | Maximum seconds between ambient voice replays |
| `speech_rms_threshold` | `0.02` | RMS energy threshold — clips below this are treated as silence |

### `[world_interaction]`

| Key | Default | Description |
|---|---|---|
| `chest_disc_drop_chance` | `0.30` | Probability Minesis deposits an *Echoes Below* disc when opening a chest |
| `natural_appearance_min_clips` | `50` | Minimum recorded voice clips required before Minesis can spawn naturally |

---

## Requirements

- Minecraft 1.20.1
- Forge 47.2.0+
- Java 17+
- [Simple Voice Chat](https://www.curseforge.com/minecraft/mc-mods/simple-voice-chat) (optional — voice features require it)

## Installation

1. Download the Minesis mod JAR
2. Place it in your `mods/` folder
3. Launch Minecraft with Forge 1.20.1
4. Adjust `config/minesis-common.toml` to your preference
5. Simple Voice Chat is auto-detected if installed

---

## Entity Statistics

| Property | Value |
|---|---|
| Type | Monster (MISC) |
| Size | 0.6 × 1.8 blocks |
| Base Health | 20 HP |
| Tracking Range | 10 blocks |
| Update Interval | 3 ticks |
| XP on death | None |

---

## Mod ID

`minesis`

## Version

`1.2.0`
