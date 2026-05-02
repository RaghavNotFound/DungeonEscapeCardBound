package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;
import java.util.ArrayList;

/**
 * The central hub for game logic. Manages the player, enemies, loot, and map interactions.
 * Handles collision detection, combat calculations, and objective tracking.
 */
public class GameWorld {

    private static final float NEAR_ENEMY_DISTANCE = 150f;
    private static final float NEAR_ENEMY_DISTANCE_SQ = NEAR_ENEMY_DISTANCE * NEAR_ENEMY_DISTANCE;
    private static final float ARROW_DAMAGE = 20f;

    public static final float WORLD_WIDTH = 1280;
    public static final float WORLD_HEIGHT = 720;
    public static final float FLOOR_OFFSET = 120f;

    private final MapManager mapManager;
    private final LightingManager lightingManager;
    private final Player player;
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<LootDrop> lootDrops = new ArrayList<>();
    private final ArrayList<Interactable> interactables;

    private final ArrayList<Rectangle> boundaries;
    private final ArrayList<Polygon> collisionPolygons = new ArrayList<>();

    private boolean levelComplete = false; // flag for level completion
    private boolean bossFightTriggered = false;
    private boolean quizBlocked = false;

    private boolean isTutorialBossWaveActive = false;
    private int tutorialWavePhase = 0;
    private final ArrayList<Vector2> tutorialWaveSpawns = new ArrayList<>();
    private final ArrayList<Enemy> enemiesToSpawn = new ArrayList<>();

    // Shared A* pathfinder for all enemies
    private AStar pathfinder;

    public GameWorld(MapManager mapManager) {
        this.mapManager = mapManager;
        this.lightingManager = new LightingManager();
        this.player = new Player();

        this.boundaries = mapManager.getCollisionRects();

        // Build shared A* grid from map collision data
        int gridSize = mapManager.getGridSize();
        if (gridSize <= 0) gridSize = 16;
        this.pathfinder = new AStar(mapManager.getMapWidth(), mapManager.getMapHeight(), gridSize, boundaries);

        player.setBoundaries(boundaries);
        player.setWorldBounds(0f, 0f, mapManager.getMapWidth(), mapManager.getMapHeight());

        player.getPosition().set(mapManager.getPlayerSpawn());

        // Nudge player out of collision if spawned inside a wall
        nudgeOutOfCollision(player.getPosition(), boundaries, mapManager.getMapWidth(), mapManager.getMapHeight());

        System.out.println("[GameWorld] Loading Level: " + mapManager.getCurrentLevelIndex() + " Path: " + mapManager.getCurrentMapPath());

        Vector2 bossSpawn = mapManager.getBossSpawn();
        boolean forceBoss = mapManager.getCurrentMapPath().equals("Maps/final_map.ldtk")
            && (mapManager.getCurrentLevelIndex() == 4 || mapManager.getCurrentLevelIndex() >= 6);

        // Spawn enemies
        for (Vector2 spawn : mapManager.getEnemySpawns()) {
            Enemy e = new Enemy();
            e.setBoundaries(boundaries);
            e.setWorldBounds(0f, 0f, mapManager.getMapWidth(), mapManager.getMapHeight());
            e.setPathfinder(pathfinder);
            e.setPosition(spawn.x, spawn.y);
            enemies.add(e);
        }

        if (mapManager.getCurrentMapPath().equals("Maps/tutorial.ldtk") && mapManager.getCurrentLevelIndex() == 3) {
            isTutorialBossWaveActive = true;
            tutorialWavePhase = 1;
            tutorialWaveSpawns.addAll(mapManager.getEnemySpawns());
        }

        this.interactables = mapManager.getInteractables();

        // --- BOSS SPAWN (Forced) ---
        // Trigger in map.ldtk level 4 where BossSpawn entity is placed
        boolean isFinalMap = mapManager.getCurrentMapPath().equals("Maps/final_map.ldtk");
        boolean isEndGameRoom = mapManager.getCurrentLevelIndex() == 4 || mapManager.getCurrentLevelIndex() >= 6;
        
        if (forceBoss || (isFinalMap && isEndGameRoom)) {
            float spawnX = bossSpawn.x, spawnY = bossSpawn.y;
            if (spawnX <= 0) {
                // No BossSpawn entity — spawn near player like debug menu does
                spawnX = player.getPosition().x + 80f;
                spawnY = player.getPosition().y;
            }
            Enemy boss = new Enemy();
            boss.setBoundaries(boundaries);
            boss.setWorldBounds(0f, 0f, mapManager.getMapWidth(), mapManager.getMapHeight());
            boss.setPathfinder(pathfinder);
            boss.setPosition(spawnX, spawnY);
            boss.setBoss(true);
            boss.forceChase(Float.MAX_VALUE);
            enemies.add(boss);
            System.out.println("[GameWorld] BOSS FORCED at " + spawnX + ", " + spawnY + " | Path: " + mapManager.getCurrentMapPath());
        }
    }

    public void update(float delta, OrthographicCamera camera, com.badlogic.gdx.graphics.glutils.ShapeRenderer shape) {
        if (quizBlocked) return;

        lightingManager.update(delta);
        player.update(delta, camera);

        // --- LAVA DEATH LOGIC ---
        if (player.isAlive() && !DebugOverlay.godMode) {
            Rectangle pBounds = player.getBounds();
            // Create a small rectangle representing only the bottom line (legs) of the player
            Rectangle footLine = new Rectangle(pBounds.x, pBounds.y, pBounds.width, 2f);
            for (Rectangle lava : mapManager.getLavaRects()) {
                if (footLine.overlaps(lava)) {
                    if (!player.isJumping()) {
                        player.triggerLavaDeath();
                    }
                    break;
                }
            }
        }

        // --- ENEMY LAVA DEATH LOGIC ---
        for (Enemy e : enemies) {
            if (e.isAlive() && !e.isBoss()) {
                Rectangle eBounds = e.getBounds();
                for (Rectangle lava : mapManager.getLavaRects()) {
                    if (eBounds.overlaps(lava)) {
                        boolean wasAlive = e.isAlive();
                        e.takeDamage(e.getHealth()); // Instant death
                        if (wasAlive && !e.isAlive()) {
                            handleEnemyDeath(e);
                        }
                        break;
                    }
                }
            }
        }

        // Reverted to the old, simpler lighting logic.
        // The light is always on and centered on the player's hitbox.
        Rectangle pBounds = player.getBounds();
        float pcx = pBounds.x + pBounds.width / 2f;
        float pcy = pBounds.y + pBounds.height / 2f;
        lightingManager.updatePlayerLight(pcx, pcy);

        // Update the lighting FBO here, before main rendering starts
        lightingManager.updateLightFbo(camera, shape);

        // --- REMOVE DEAD ENEMIES after death animation finishes ---
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (!e.isAlive() && e.isDeathAnimationFinished()) {
                enemies.remove(i);
                e.dispose();
            }
        }

        for (Enemy e : enemies) {
            e.update(delta, player);

            if (e.isAlive() && e.isBoss() && !bossFightTriggered) {
                float bossCX = e.getBounds().x + e.getBounds().width / 2f;
                float bossCY = e.getBounds().y + e.getBounds().height / 2f;
                float dx = pcx - bossCX;
                float dy = pcy - bossCY;

                if (dx * dx + dy * dy <= (22f * 22f)) {
                    bossFightTriggered = true;
                }
            }
        }

        // --- WORLD TORCHES (Map Objects) ---
        ArrayList<Rectangle> torches = mapManager.getTorchRects();
        for (int i = torches.size() - 1; i >= 0; i--) {
            if (player.getBounds().overlaps(torches.get(i))) {
                torches.remove(i);
                player.addTorch();
            }
        }

        // --- SWORD COMBAT ---
        if (player.canDealSwordDamage()) {
            for (Enemy e : enemies) {
                if (e.isAlive() && player.getSwordHitbox().overlaps(e.getBounds())) {
                    boolean wasAlive = e.isAlive();
                    e.takeDamage(player.getSwordDamage());

                    if (wasAlive && !e.isAlive()) {
                        handleEnemyDeath(e);
                    }

                    Vector2 dir = new Vector2(e.getBounds().x - player.getBounds().x, e.getBounds().y - player.getBounds().y);
                    e.applyKnockback(dir, 200f); // Reduced from 400f
                }
            }
            player.consumeSwordDamage();
        }

        // --- ENEMY ATTACKS ---
        for (Enemy e : enemies) {
            if (e.canDealDamage() && e.isPlayerInAttackRadius(player)) {
                if (!DebugOverlay.godMode) {
                    player.takeDamage(e.getDamage());
                }
                Vector2 dir = new Vector2(player.getBounds().x - e.getBounds().x, player.getBounds().y - e.getBounds().y);
                player.applyKnockback(dir, 250f); // Reduced from 500f
                e.consumeAttackDamage();
            }
        }

        // --- PROJECTILE COMBAT ---
        ArrayList<Arrow> arrows = player.getArrows();
        for (int i = arrows.size() - 1; i >= 0; i--) {
            Arrow arrow = arrows.get(i);
            for (Enemy e : enemies) {
                if (e.isAlive() && arrow.getBounds().overlaps(e.getBounds())) {
                    boolean wasAlive = e.isAlive();
                    e.takeDamage(ARROW_DAMAGE);
                    e.forceChase(10f); // Aggro triggered by projectile hit

                    if (wasAlive && !e.isAlive()) {
                        handleEnemyDeath(e);
                    }

                    arrows.remove(i);
                    break;
                }
            }
        }

        // --- GROUP AGGRO MECHANIC ---
        updateGroupAggro();

        // --- LOOT DROPS LOGIC ---
        updateLootDrops(delta);

        // --- INTERACTABLES ---
        updateInteractables();

        // --- TUTORIAL BOSS WAVES ---
        if (isTutorialBossWaveActive && tutorialWavePhase == 1) {
            boolean allDead = true;
            for (Enemy e : enemies) {
                if (e.isAlive()) {
                    allDead = false;
                    break;
                }
            }
            if (allDead) {
                tutorialWavePhase = 2;
                for (Vector2 spawn : tutorialWaveSpawns) {
                    Enemy waveEnemy = new Enemy();
                    waveEnemy.setBoundaries(boundaries);
                    waveEnemy.setWorldBounds(0f, 0f, mapManager.getMapWidth(), mapManager.getMapHeight());
                    waveEnemy.setPathfinder(pathfinder);
                    waveEnemy.setPosition(spawn.x, spawn.y);
                    enemiesToSpawn.add(waveEnemy);
                }
            }
        }

        if (!enemiesToSpawn.isEmpty()) {
            enemies.addAll(enemiesToSpawn);
            enemiesToSpawn.clear();
        }
    }

    private void handleEnemyDeath(Enemy e) {
        player.incrementEnemiesKilled();
        float cx = e.getBounds().x + e.getBounds().width / 2f;
        float cy = e.getBounds().y + e.getBounds().height / 2f;

        // Enemies have a 35% chance to drop a card, plus random chance loot
        if (MathUtils.random() < 0.35f) {
            lootDrops.add(new LootDrop(LootDrop.Type.CARD, cx, cy));
        }
        spawnLoot(cx, cy);
    }

    private void updateGroupAggro() {
        boolean anyChasing = false;
        for (Enemy e : enemies) {
            if (e.isAlive() && e.isChasing()) {
                anyChasing = true;
                break;
            }
        }

        if (anyChasing) {
            for (Enemy chaser : enemies) {
                if (chaser.isAlive() && chaser.isChasing()) {
                    for (Enemy slacker : enemies) {
                        if (slacker.isAlive() && !slacker.isChasing() && slacker != chaser) {
                            float dx = slacker.getBounds().x - chaser.getBounds().x;
                            float dy = slacker.getBounds().y - chaser.getBounds().y;
                            if (dx * dx + dy * dy < 480f * 480f) {
                                slacker.forceChase(3.5f);
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateLootDrops(float delta) {
        float pCX = player.getBounds().x + player.getBounds().width / 2f;
        float pCY = player.getBounds().y + player.getBounds().height / 2f;

        for (int i = lootDrops.size() - 1; i >= 0; i--) {
            LootDrop drop = lootDrops.get(i);
            drop.update(delta);

            if (drop.isExpired()) {
                lootDrops.remove(i);
                continue;
            }

            drop.attractToward(pCX, pCY, delta);

            if (player.getBounds().overlaps(drop.getBounds())) {
                switch (drop.getType()) {
                    case HEALTH -> player.addHealth(25f);
                    case TORCH -> player.addTorch();
                    case ARROW -> player.addStamina(15f);
                    case STAMINA -> player.addStamina(30f);
                    case CARD -> player.addCard();
                }
                lootDrops.remove(i);
            }
        }
    }

    private void updateInteractables() {
        // --- LEVEL EXIT LOGIC ---
        for (Rectangle exit : mapManager.getExitGateRects()) {
            if (player.getBounds().overlaps(exit)) {
                io.github.pkgde.quiz.DoorEntity door = getDoorAt(exit);
                if (door != null && door.isLocked()) {
                    continue; // Door is locked, wait for quiz to unlock it
                }

                Vector2 spawn = mapManager.getPlayerSpawn();
                float dx = player.getPosition().x - spawn.x;
                float dy = player.getPosition().y - spawn.y;
                float distSq = dx * dx + dy * dy;

                // Prevent immediate exit if player spawned on or too close to the door
                if (distSq < 60f * 60f) {
                    continue;
                }

                boolean allEnemiesDead = true;
                for (Enemy e : enemies) {
                    if (e.isAlive()) {
                        allEnemiesDead = false;
                        break;
                    }
                }
                if (allEnemiesDead) {
                    levelComplete = true; // flag to be read by ExplorationScreen
                }
            }
        }

        boolean interactedThisFrame = false;
        for (Interactable interactable : interactables) {
            interactable.update(Gdx.graphics.getDeltaTime());

            if (!interactedThisFrame && interactable.isPlayerInRange(player) && Gdx.input.isKeyJustPressed(Input.Keys.G)) {
                if (interactable.getType() == Interactable.Type.CENTER_FIRE) {
                    if (!lightingManager.isLit()) {
                        if (player.hasTorch()) {
                            player.removeTorch();
                            interactable.interact(player); // Hides the "Press [G]" prompt
                            Rectangle b = interactable.getBounds();
                            lightingManager.triggerLighting(b.x + b.width / 2f, b.y + b.height / 2f);

                            // Spawn a boss near the player (same as debug menu [B] spawn)
                            Enemy boss = new Enemy();
                            boss.setBoundaries(boundaries);
                            boss.setWorldBounds(0f, 0f, mapManager.getMapWidth(), mapManager.getMapHeight());
                            boss.setPathfinder(pathfinder);
                            boss.setPosition(player.getPosition().x + 80f, player.getPosition().y);
                            boss.setBoss(true);
                            boss.forceChase(Float.MAX_VALUE);
                            enemies.add(boss);
                            System.out.println("[GameWorld] Boss spawned from Center Fire!");
                            interactedThisFrame = true;
                        } else {
                            System.out.println("You need a torch to light the center fire!");
                        }
                    }
                } else if (interactable.interact(player)) {
                    Rectangle b = interactable.getBounds();
                    float cx = b.x + b.width / 2f;
                    float cy = b.y + b.height / 2f;

                    if (interactable.getType() == Interactable.Type.CHEST) {
                        lootDrops.add(new LootDrop(LootDrop.Type.TORCH, cx, cy + 20f));
                    } else if (interactable.getType() == Interactable.Type.BARREL) {
                        float roll = MathUtils.random();
                        if (roll < 0.4f) lootDrops.add(new LootDrop(LootDrop.Type.HEALTH, cx, cy + 15f));
                        else if (roll < 0.7f) lootDrops.add(new LootDrop(LootDrop.Type.STAMINA, cx, cy + 15f));
                        else lootDrops.add(new LootDrop(LootDrop.Type.ARROW, cx, cy + 15f));
                    }
                    interactedThisFrame = true;
                }
            }
        }
    }


    private void spawnLoot(float x, float y) {
        float roll = MathUtils.random();
        if (roll < 0.30f) lootDrops.add(new LootDrop(LootDrop.Type.HEALTH, x, y));
        else if (roll < 0.50f) lootDrops.add(new LootDrop(LootDrop.Type.TORCH, x, y));
        else if (roll < 0.65f) lootDrops.add(new LootDrop(LootDrop.Type.STAMINA, x, y));
        else if (roll < 0.78f) lootDrops.add(new LootDrop(LootDrop.Type.ARROW, x, y));
    }

    public boolean isPlayerNearEnemy() {
        Vector2 p = player.getPosition();
        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;
            Rectangle b = e.getBounds();
            float dx = p.x - (b.x + b.width * 0.5f);
            float dy = p.y - (b.y + b.height * 0.5f);
            if (dx * dx + dy * dy < NEAR_ENEMY_DISTANCE_SQ) return true;
        }
        return false;
    }

    /**
     * Nudges a position out of collision rects by trying offsets in 8 directions.
     * Used to fix spawning inside walls.
     */
    private void nudgeOutOfCollision(Vector2 pos, ArrayList<Rectangle> collisions, float mapW, float mapH) {
        float testW = 55f * Player.ENTITY_SCALE;  // player foot hitbox width
        float testH = 28f * Player.ENTITY_SCALE;  // player foot hitbox height
        float offX = (128f - 55f) / 2f * Player.ENTITY_SCALE;
        float offY = 19f * Player.ENTITY_SCALE;

        Rectangle test = new Rectangle(pos.x + offX, pos.y + offY, testW, testH);

        // Check if currently colliding
        boolean colliding = false;
        for (Rectangle r : collisions) {
            if (test.overlaps(r)) { colliding = true; break; }
        }
        if (!colliding) return;

        // Try nudging in 8 directions at increasing distances
        float[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        for (float dist = 8f; dist <= 128f; dist += 8f) {
            for (float[] d : dirs) {
                float nx = pos.x + d[0] * dist;
                float ny = pos.y + d[1] * dist;
                if (nx < 0 || ny < 0 || nx > mapW || ny > mapH) continue;
                test.set(nx + offX, ny + offY, testW, testH);
                boolean ok = true;
                for (Rectangle r : collisions) {
                    if (test.overlaps(r)) { ok = false; break; }
                }
                if (ok) {
                    System.out.println("[GameWorld] Nudged player spawn from (" + pos.x + "," + pos.y + ") to (" + nx + "," + ny + ")");
                    pos.set(nx, ny);
                    return;
                }
            }
        }
    }

    public io.github.pkgde.quiz.DoorEntity getDoorAt(Rectangle rect) {
        if (mapManager == null || mapManager.getDoors() == null) return null;
        for (io.github.pkgde.quiz.DoorEntity door : mapManager.getDoors()) {
            if (door.getBounds().overlaps(rect)) {
                return door;
            }
        }
        return null;
    }

    // ===== GETTERS & SETTERS =====
    public Player getPlayer() { return player; }
    public ArrayList<Enemy> getEnemies() { return enemies; }
    public LightingManager getLightingManager() { return lightingManager; }
    public ArrayList<LootDrop> getLootDrops() { return lootDrops; }
    public ArrayList<Interactable> getInteractables() { return interactables; }
    public MapManager getMapManager() { return mapManager; }
    public ArrayList<Rectangle> getBoundaries() { return boundaries; }
    public boolean isLevelComplete() { return levelComplete; } // Getter for levelComplete
    public boolean isBossFightTriggered() { return bossFightTriggered; }
    public void setBossFightTriggered(boolean val) { this.bossFightTriggered = val; }
    public boolean isQuizBlocked() { return quizBlocked; }
    public void setQuizBlocked(boolean val) { this.quizBlocked = val; }

    public void dispose() {
        player.dispose();
        for (Enemy e : enemies) e.dispose();
        if (lightingManager != null) lightingManager.dispose();
    }
}
