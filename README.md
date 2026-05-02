# Dungeon Escape: Cardbound
 
> A tactical hybrid 2D game blending real-time dungeon exploration with turn-based deck-building card combat — built from scratch in Java with LibGDX.
 
![Java](https://img.shields.io/badge/Java-JDK%208+-orange?style=flat-square&logo=java)
![LibGDX](https://img.shields.io/badge/LibGDX-Framework-red?style=flat-square)
![Gradle](https://img.shields.io/badge/Build-Gradle-02303A?style=flat-square&logo=gradle)
![License](https://img.shields.io/badge/License-Academic-blue?style=flat-square)
 
---
 
## Table of Contents
 
- [About](#about)
- [Gameplay](#gameplay)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Running the Game](#running-the-game)
  - [Building a Standalone JAR](#building-a-standalone-jar)
- [Project Structure](#project-structure)
- [Core Systems](#core-systems)
- [Team](#team)
- [Known Issues & Roadmap](#known-issues--roadmap)
---
 
## About
 
**Dungeon Escape: Cardbound** is a B.Tech Computer Science project developed at the University of Petroleum and Energy Studies (UPES), Dehradun, for the Object-Oriented Programming (Java) / Software Engineering Lab course (AY 2025–2026).
 
The game merges two distinct gameplay paradigms:
- A **real-time, top-down dungeon exploration** phase across three LDtk-designed map stages
- A **turn-based, deck-building card battle** (inspired by *Slay the Spire*) for the final boss encounter against The Demonic Monk
The project was developed by a six-member team using an Agile Jira Kanban workflow, with each developer owning independent subsystems integrated through well-defined interfaces.
 
---
 
## Gameplay
 
1. **Explore** three dungeon stages (Tutorial → Safe Room → Final Map), navigate enemies, open chests, and collect loot.
2. **Fight** enemies in real time using melee and bow combat. Enemies navigate using a custom A\* pathfinding AI.
3. **Face the Boss** — on reaching the final trigger, the game switches to a full card-battle UI where you use a deck of cards and 3 energy per turn to defeat The Demonic Monk.
---
 
## Features
 
- 🗺️ **Three LDtk-driven map stages** with collision, entity spawning, and stage transitions
- 🤖 **Custom A\* pathfinding AI** with wall-adjacency penalty to prevent corner-clipping
- 🔦 **Dynamic FBO lighting** — torch, campfire, and lava tile radial light falloff via OpenGL Frame Buffer Objects
- 🃏 **Full deck-builder boss fight** with card types, energy economy, block mechanics, and three boss attack patterns
- 💾 **JSON save/load system** — three save slots, persisting player position, health, stamina, inventory, and enemy states
- 🐛 **F3 Developer Debug Overlay** — hitbox view, teleport, God Mode, and boundary toggles
- 🎨 **BlurShader** GLSL post-processing for glass-morphism pause/settings menus
- 📦 **Distributable standalone JAR** via Gradle — no IDE required to run
---
 
## Tech Stack
 
| Component | Technology |
|---|---|
| Language | Java (JDK 8+) |
| Game Framework | LibGDX |
| Desktop Backend | LWJGL3 |
| Build Tool | Gradle |
| Level Design | LDtk (Level Designer Toolkit) |
| Asset Creation | Aseprite |
| Project Management | Jira Software (Agile Kanban) |
 
---
 
## Getting Started
 
### Prerequisites
 
- Java JDK 8 or higher installed
- Gradle (or use the included `gradlew` wrapper)
### Running the Game
 
Clone the repository and run via Gradle:
 
```bash
git clone https://github.com/RaghavNotFound/DungeonEscapeCardBound.git
cd DungeonEscapeCardBound
 
# On Windows
gradlew.bat lwjgl3:run
 
# On macOS/Linux
./gradlew lwjgl3:run
```
 
### Building a Standalone JAR
 
```bash
./gradlew lwjgl3:jar
```
 
The distributable `.jar` will be output to `lwjgl3/build/libs/`. Run it on any machine with Java installed — no IDE or development environment required.
 
---
 
## Project Structure
 
```
DungeonEscapeCardBound/
├── lwjgl3/          # Desktop launcher — LWJGL3 context, window config, Gradle packaging
├── android/         # Android launcher (scaffolded, in development)
├── assets/
│   ├── Maps/        # LDtk .ldtk map files
│   ├── Player_sprite/
│   ├── Enemy_Sprite/
│   └── UI/          # Fonts, card art .png files
└── core/src/.../pkgde/   # All core game logic
```
 
### Core Class Reference
 
| Class | Responsibility |
|---|---|
| `Main.java` | Entry point; initialises AssetManager and screen stack |
| `ExplorationScreen.java` | Real-time dungeon loop — input, physics, rendering, HUD |
| `BossFightScreen.java` | Turn-based deck-builder — card UI, energy, phase machine, boss AI |
| `GameWorld.java` | Central simulation: player, enemies, loot, collision, aggro |
| `GameRenderer.java` | Render coordinator: map layers, depth-sorted entities, lighting |
| `MapManager.java` | LDtk JSON parser: tile layers, entity spawning, collision boundaries |
| `Player.java` | Player state, wall-slide movement, stamina, melee & bow combat |
| `Enemy.java` | Enemy FSM, animations, A\* path following, attack range detection |
| `AStar.java` / `Node.java` | Custom A\* pathfinding with wall-adjacency movement penalty |
| `LightingManager.java` | FBO-based dynamic lighting: torch, campfire, lava glow |
| `SaveManager.java` | JSON serialisation/deserialisation and save slot management |
| `SaveState.java` | DTO holding all serialisable game state fields |
| `BlurShader.java` | Custom GLSL shader for pause/settings background blur |
| `DebugOverlay.java` | F3 developer tools: God Mode, teleport, hitbox/boundary toggles |
 
---
 
## Core Systems
 
### A\* Pathfinding
Enemy navigation uses a custom A\* implementation (`AStar.java`, `Node.java`) with the cost function `F = G + H` (Manhattan distance heuristic). A wall-adjacency movement penalty discourages corner-hugging, producing natural, organic enemy movement without geometry clipping.
 
### FBO Lighting
`LightingManager.java` renders a dark overlay to an off-screen Frame Buffer Object, then applies OpenGL's `GL_DST_COLOR / GL_ZERO` blend equation to punch transparent radial gradients at light source positions. Hardware-accelerated and constant-cost regardless of light source count.
 
### Save System
`SaveManager.java` uses a Data Transfer Object pattern — all live game state is serialised to JSON via LibGDX's `Json` library and written to disk. On load, the `SaveState` DTO is deserialised and all game objects are reconstructed, supporting clean cross-level transitions.
 
### Boss Fight
`BossFightScreen.java` implements a three-phase state machine (`PLAYER_TURN → BOSS_THINKING → BOSS_TURN`), a shuffled card deck with 3 energy per turn, block mechanics, and three distinct boss attack patterns.
 
---
 
## Known Issues & Roadmap
 
### Known Issues
- [ ] Combat balance: boss *Black Flash* attack deals disproportionate burst damage vs. available block values
- [ ] Screen transition between `ExplorationScreen` and `BossFightScreen` is an instant cut (fade-to-black designed but not yet integrated)
### Future Scope
- [ ] **Procedural Dungeon Generation** — BSP-tree or cellular automata to generate unique layouts per run
- [ ] **Expanded Deck-Building** — discover cards from chests during exploration to build a pre-boss deck
- [ ] **Audio System** — positional ambient sounds, attack SFX, and dynamic boss battle music via LibGDX Sound/Music APIs
- [ ] **Expanded Boss Roster** — add The Lich King as a mid-game boss encounter
- [ ] **Android Port** — complete the scaffolded Android launcher module
---
 
*Developed for the Object-Oriented Programming (Java) — UPES Dehradun, AY 2025–2026*
 
