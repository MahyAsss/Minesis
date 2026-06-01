# Getting Started with Mimesis Mod Development

## Setup Instructions

### 1. Clone/Open the Project
```bash
# If using git
git clone <repository-url>
cd Mimesis

# Or open existing folder in IDE
```

### 2. Generate IDE Configuration
Choose your IDE:

**IntelliJ IDEA:**
```bash
./gradlew genIntellijRuns
```
Then:
1. Open the project
2. Right-click on project root
3. Select "Reload Gradle Project"

**Eclipse:**
```bash
./gradlew genEclipseRuns
```
Then:
1. File → Import → Existing Projects into Workspace
2. Select the Mimesis folder

**VS Code:**
1. Install Extension Pack for Java
2. Open the folder
3. Extensions will auto-detect the Gradle project

### 3. Configure Java 17
The mod requires Java 17. Ensure your IDE is configured:

- **IntelliJ**: File → Project Structure → Set Project SDK to Java 17
- **Eclipse**: Right-click Project → Properties → Java Build Path → Set JDK 17
- **VS Code**: Ctrl+Shift+P → "Java: Configure Java Runtime"

### 4. Building

**Development Build (Debug):**
```bash
./gradlew build
```

**Run Client (In-Game Testing):**
```bash
./gradlew runClient
```
The mod will launch Minecraft with the dev environment.

**Run Server:**
```bash
./gradlew runServer
```

**Create Release JAR:**
```bash
./gradlew build
# JAR will be in: build/libs/mimesis-1.0.0.jar
```

## Project Overview

### Main Components

1. **MimesisEntity** (`entity/MimesisEntity.java`)
   - The main entity that mimics players
   - Manages behavior phases (follow → hostile)
   - Syncs with clients
   - Handles voice playback

2. **Voice System** (`voice/`)
   - `VoiceManager`: Main integration point
   - `VoiceCaptureHandler`: Per-player audio capture
   - `VoiceStorage`: Audio clip storage and retrieval

3. **AI Goals** (`ai/`)
   - `MimesisFollowGoal`: Tracking and following behavior
   - `MimesisAttackGoal`: Combat behavior

4. **Rendering** (`client/`)
   - `MimesisEntityRenderer`: Custom entity renderer
   - Copies player skin dynamically

### File Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/mimesis/
│   │       ├── MimesisMod.java              ← Mod entry point
│   │       ├── ServerEvents.java            ← Server events
│   │       ├── entity/                      ← Entity code
│   │       ├── ai/                          ← AI behavior
│   │       ├── voice/                       ← Voice integration
│   │       ├── client/                      ← Client rendering
│   │       ├── commands/                    ← Chat commands
│   │       └── utils/                       ← Utilities
│   └── resources/
│       ├── META-INF/
│       │   └── mods.toml                    ← Mod metadata
│       └── assets/mimesis/                  ← Game assets
```

## Common Tasks

### Add a New AI Goal
1. Create new file in `ai/` folder
2. Extend `Goal` class
3. Implement `canUse()` and `tick()`
4. Register in `MimesisEntity.registerGoals()`

### Add a Chat Command
1. Create class extending command builder
2. Register in `MimesisMod`
3. Use Brigadier syntax

### Modify Behavior Timing
1. Edit `MimesisEntity.java`:
```java
private int hostileActivationTime = 600; // Change here (in ticks)
```

### Add Configuration Options
1. Edit `utils/MimesisConfig.java`
2. Add static final fields
3. Create getters/setters

## Testing the Mod

### In-Game Testing
1. Launch with `./gradlew runClient`
2. Create new world
3. Use command: `/mimesis spawn`
4. A clone will appear near you

### Behavior Testing
- **0-30 seconds**: Entity follows you, replays voice
- **30+ seconds**: Entity attacks you

### Voice Testing
1. Ensure Simple Voice Chat is installed
2. Talk in voice chat
3. Watch for voice playback (visual indicators)

## Troubleshooting

### Build Fails
- Check Java version: `java -version` should be 17+
- Clean build: `./gradlew clean build`
- Check gradle: `./gradlew --version`

### Entity Doesn't Appear
- Check server logs for errors
- Verify entity registry in `MimesisEntities.java`
- Ensure player is in correct game mode

### Rendering Issues
- Verify renderer is registered in `ClientSetup.java`
- Check texture paths in `MimesisEntityRenderer.java`
- Ensure skin loading works

### Voice Doesn't Work
- Install Simple Voice Chat mod
- Check `VoiceManager.init()` is called
- Verify audio data in `VoiceStorage`

## Next Steps

1. Read [DEVELOPMENT.md](DEVELOPMENT.md) for detailed architecture
2. Explore [README.md](README.md) for features
3. Check `MimesisMod` main class
4. Run `./gradlew runClient` to test

## IDE Shortcuts

### IntelliJ
- `Ctrl+N` - Find class
- `Ctrl+Shift+F` - Find in files
- `Ctrl+B` - Go to definition
- `Shift+F6` - Refactor/rename
- `Alt+Enter` - Quick fixes

### VS Code
- `Ctrl+P` - Find file
- `Ctrl+Shift+F` - Find in files
- `F12` - Go to definition
- `Ctrl+Shift+L` - Select all occurrences
- `F2` - Rename

## Resources

- [Minecraft Forge Documentation](https://docs.minecraftforge.net/)
- [Java 17 Documentation](https://docs.oracle.com/en/java/javase/17/)
- [Gradle Documentation](https://docs.gradle.org/)

## Questions?

Check the main classes or run in debug mode to understand the flow better!
