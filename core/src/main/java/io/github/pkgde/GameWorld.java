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

    private boolean isTutorialBossWaveActive = false;
    private int tutorialWavePhase = 0;
    private final ArrayList<Vector2> tutorialWaveSpawns = new ArrayList<>();
    private final ArrayList<Enemy> enemiesToSpawn = new ArrayList<>();

    // Shared A* pathfinder for all enemies
    private AStar pathfinder;

    // Reusable temp object — avoids per-hit allocations
    private final Vector2 tmpDir = new Vector2();

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

        // Spawn player properly!
        player.getPosition().set(mapManager.getPlayerSpawn());

        Vector2 bossSpawn = mapManager.getBossSpawn();
        if (bossSpawn.x != -1 && bossSpawn.y != -1) {
            Enemy boss = new Enemy();
            boss.setBoundaries(boundaries);
            boss.setWorldBounds(0f, 0f, mapManager.getMapWidth(), mapManager.getMapHeight());
            boss.setPathfinder(pathfinder);
            boss.setPosition(bossSpawn.x, bossSpawn.y);
            boss.setBoss(true);
            boss.forceChase(Float.MAX_VALUE);
            enemies.add(boss);
        }

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
    }

    public void update(float delta, OrthographicCamera camera, com.badlogic.gdx.graphics.glutils.ShapeRenderer shape) {
        lightingManager.update(delta);
        player.update(delta, camera);

        // --- LAVA TICK DAMAGE --- (skip while player is airborne from a jump)
        if (player.isAlive() && !DebugOverlay.godMode && !player.isAirborne()) {
            Rectangle pBounds = player.getBounds();
            boolean touchingLava = false;
            for (Rectangle lava : mapManager.getLavaRects()) {
                if (pBounds.overlaps(lava)) {
                    // Calculate horizontal overlap
                    float overlapXStart = Math.max(pBounds.x, lava.x);
                    float overlapXEnd = Math.min(pBounds.x + pBounds.width, lava.x + lava.width);
                    float overlapWidth = overlapXEnd - overlapXStart;

                    // Calculate vertical overlap
                    float overlapYStart = Math.max(pBounds.y, lava.y);
                    float overlapYEnd = Math.min(pBounds.y + pBounds.height, lava.y + lava.height);
                    float overlapHeight = overlapYEnd - overlapYStart;

                    // Apply tick damage when significantly overlapping lava
                    if (overlapWidth > pBounds.width * 0.5f && overlapHeight > 0 && MathUtils.isEqual(overlapYStart, pBounds.y, 1f)) {
                        touchingLava = true;
                        player.setInLava(true);
                        player.applyLavaDamage(delta);
                        break;
                    }
                }
            }
            // Reset tick timer when player leaves lava
            if (!touchingLava && player.isInLava()) {
                player.resetLavaDamage();
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

        for (Enemy e : enemies) {
            e.update(delta, player);

            if (e.isAlive() && e.isBoss() && !bossFightTriggered) {
                float bossCX = e.getBounds().x + e.getBounds().width / 2f;
                float bossCY = e.getBounds().y + e.getBounds().height / 2f;
                float dx = pcx - bossCX;
                float dy = pcy - bossCY;

                if (dx * dx + dy * dy <= (150f * 150f)) {
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

                    tmpDir.set(e.getBounds().x - player.getBounds().x, e.getBounds().y - player.getBounds().y);
                    e.applyKnockback(tmpDir, 400f);
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
                tmpDir.set(player.getBounds().x - e.getBounds().x, player.getBounds().y - e.getBounds().y);
                player.applyKnockback(tmpDir, 500f);
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

        // Enemies always drop a card, plus random chance loot
        lootDrops.add(new LootDrop(LootDrop.Type.CARD, cx, cy));
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

        for (Interactable interactable : interactables) {
            interactable.update(Gdx.graphics.getDeltaTime());

            if (interactable.isPlayerInRange(player) && Gdx.input.isKeyJustPressed(Input.Keys.G)) {
                if (interactable.getType() == Interactable.Type.CENTER_FIRE) {
                    if (!lightingManager.isLit()) {
                        if (player.hasTorch()) {
                            player.removeTorch();
                            interactable.interact(player); // Hides the "Press [G]" prompt
                            Rectangle b = interactable.getBounds();
                            lightingManager.triggerLighting(b.x + b.width / 2f, b.y + b.height / 2f);
                        } else {
                            System.out.println("You need a torch to light the center fire!");
                        }
                    }
                } else if (interactable.interact(player)) {
                    Rectangle b = interactable.getBounds();
                    float cx = b.x + b.width / 2f;
                    float cy = b.y + b.height / 2f;

                    if (interactable.getType() == Interactable.Type.CHEST) {
                        lootDrops.add(new LootDrop(LootDrop.Type.HEALTH, cx - 15f, cy + 20f));
                        if (MathUtils.random() < 0.5f) lootDrops.add(new LootDrop(LootDrop.Type.TORCH, cx + 15f, cy + 20f));
                        if (MathUtils.random() < 0.3f) lootDrops.add(new LootDrop(LootDrop.Type.STAMINA, cx, cy + 35f));
                    } else if (interactable.getType() == Interactable.Type.BARREL) {
                        float roll = MathUtils.random();
                        if (roll < 0.4f) lootDrops.add(new LootDrop(LootDrop.Type.HEALTH, cx, cy + 15f));
                        else if (roll < 0.7f) lootDrops.add(new LootDrop(LootDrop.Type.STAMINA, cx, cy + 15f));
                        else lootDrops.add(new LootDrop(LootDrop.Type.ARROW, cx, cy + 15f));
                    }
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

    public void dispose() {
        player.dispose();
        for (Enemy e : enemies) e.dispose();
        if (lightingManager != null) lightingManager.dispose();
    }
}
