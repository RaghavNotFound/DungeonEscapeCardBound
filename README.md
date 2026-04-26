# Dungeon Escape: Cardbound

A tactical 2D roguelike deck-builder built with [libGDX](https://libgdx.com/). 

Navigate a pixel-art dungeon filled with treacherous lava and deadly enemies. Once you reach the end of the dungeon, engage the boss (The Lich King) in a "Slay the Spire" style turn-based card battle!

## Features
- **Exploration & Combat:** Navigate through an LDtk-generated dungeon, dodging lava, collecting loot, and fighting off basic enemies using real-time sword and bow combat.
- **Card-Based Boss Fights:** When you reach the boss (The Lich King), combat transitions into a turn-based deck-building system. Draw cards, manage energy and block, and survive the Boss's devastating Ultimate attacks.
- **Dynamic Lighting:** A custom frame-buffer lighting engine ensures torches and campfires realistically illuminate your surroundings in the dark dungeon.
- **Full Save/Load System:** Save your progress mid-run, including player position, health, inventory, and cleared rooms.
- **Developer Debug Tools:** Press `F3` to open a robust debug overlay featuring God Mode, hitbox rendering, and instant teleportation.

## Architecture
- `core`: Contains the main application logic, game loop (`GameWorld`), and screens (`ExplorationScreen`, `BossFightScreen`, etc.). 
- `lwjgl3`: The desktop launcher module. Handles window creation, focus management, and packaging for PC platforms.
- `android`: The mobile launcher module (currently in development).

### File Structure (Core Module)
```text
core/src/main/java/io/github/pkgde/
├── Main.java                 # Main game entry point & AssetManager initialization
├── ExplorationScreen.java    # Real-time dungeon exploration game loop and UI rendering
├── BossFightScreen.java      # Turn-based deck-building card combat loop
├── GameWorld.java            # Central simulation manager (player, enemies, loot, collision, aggro)
├── GameRenderer.java         # Master rendering coordinator (draws map, entities, lighting)
├── MapManager.java           # LDtk map loader, parses layers, tiles, and spawn coordinates
├── Player.java               # Player state, movement, stamina, and combat actions
├── Enemy.java                # Enemy AI, stats, animations, and A* pathfinding logic
├── AStar.java / Node.java    # Custom pathfinding algorithms for enemy navigation
├── LightingManager.java      # FrameBuffer-based dynamic lighting (torches, campfires, lava glow)
├── Arrow.java                # Player bow projectile entity
├── Interactable.java         # Chests, Signs, Barrels, and level Exit logic
├── LootDrop.java             # Health/Stamina/Arrows generated upon enemy deaths and chest opens
├── SaveManager.java          # Handles JSON serialization, saving/loading, and cross-level transitions
├── SaveState.java            # Data Transfer Object (DTO) for game saves
├── WindowModeManager.java    # Fullscreen / Borderless Window toggle logic
├── BlurShader.java           # Custom GLSL shader used to blur the background during pause menus
├── GameTimeManager.java      # Keeps track of the total time in the game
├── LoadingScreen.java        # Loading Screen shown between parts of the game
├── InputHandler.java         # Handler for standard user inputs
├── KeyBindings.java          # Utility storing the user mapped inputs
├── HomeScreen.java           # Main menu launch screen
├── PauseOverlay.java         # Mid-game pause UI
├── SettingsOverlay.java      # Audio/Visual configuration UI
├── SaveLoadOverlay.java      # Menu UI to load, overwrite, and delete runs
├── InventoryOverlay.java     # Player's collected items and active cards view
├── DebugOverlay.java         # F3 Developer tooling (God Mode, Teleport, Hitboxes)
├── GameOverOverlay.java      # Player death screen
└── VictoryScreen.java        # Game completion screen with run statistics
```

## Running the Game (Desktop)
This project uses [Gradle](https://gradle.org/) to manage dependencies. Run the game from the root directory using the Gradle wrapper:

**Windows:**
```cmd
gradlew.bat lwjgl3:run
```

**macOS / Linux:**
```bash
./gradlew lwjgl3:run
```

## Building Executables
To build a standalone runnable `.jar` file that can be distributed and played without an IDE:
```bash
./gradlew lwjgl3:jar
```
The compiled file will be located at `lwjgl3/build/libs/`.

## Debug Controls
* **F3:** Toggle Debug Overlay
* **1:** Toggle Hitboxes
* **2:** Toggle Boundaries
* **3:** Toggle Collision Rectangles
* **4:** Toggle Info Panel
* **8:** Teleport to Mouse
* **9:** Spawn Enemy at Mouse
* **0:** Skip Level (Kill all enemies and teleport to exit)
* **B:** Instant Boss Trigger Cheat
* **L:** Toggle God Mode (Lava/Damage Immunity)

## Map Editing
Dungeon Escape uses [LDtk (Level Designer Toolkit)](https://ldtk.io/) for map creation. 
The maps are located in `assets/Maps/`. The primary game flow uses `final_map.ldtk`. To edit the maps, open the `.ldtk` file in the LDtk application and simply hit "Save"; the game's `MapManager` parses the JSON file dynamically at runtime.
