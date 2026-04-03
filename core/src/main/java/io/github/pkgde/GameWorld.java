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
    private final Player player;
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<LootDrop> lootDrops = new ArrayList<>();
    private final ArrayList<Interactable> interactables;

    private final ArrayList<Rectangle> boundaries;
    private final ArrayList<Polygon> collisionPolygons;

    // ===== EXIT GATE =====
    private boolean exitGateUnlocked = false;
    private boolean exitGateReached = false;

    public GameWorld(MapManager mapManager) {
        this.mapManager = mapManager;
        this.player = new Player();

        this.boundaries = mapManager.getCollisionRects();
        this.collisionPolygons = mapManager.getCollisionPolygons();

        player.setBoundaries(boundaries);
        player.setCollisionPolygons(collisionPolygons);
        player.setWorldBounds(0f, 0f, mapManager.getMapWidth(), mapManager.getMapHeight());
        player.getPosition().set(mapManager.getPlayerSpawn());

        for (Vector2 spawn : mapManager.getEnemySpawns()) {
            Enemy e = new Enemy();
            e.setBoundaries(boundaries);
            e.setCollisionPolygons(collisionPolygons);
            e.setWorldBounds(0f, 0f, mapManager.getMapWidth(), mapManager.getMapHeight());
            e.setPosition(spawn.x, spawn.y);
            enemies.add(e);
        }

        this.interactables = mapManager.getInteractables();
    }

    public void update(float delta, OrthographicCamera camera) {
        player.update(delta, camera);

        for (Enemy e : enemies) {
            e.update(delta, player);
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
                    e.applyKnockback(dir, 400f);
                }
            }
            player.consumeSwordDamage();
        }

        // --- ENEMY ATTACKS ---
        for (Enemy e : enemies) {
            if (e.canDealDamage() && e.isPlayerInAttackRadius(player)) {
                player.takeDamage(e.getDamage());
                Vector2 dir = new Vector2(player.getBounds().x - e.getBounds().x, player.getBounds().y - e.getBounds().y);
                player.applyKnockback(dir, 500f);
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

                    Vector2 hitDir = new Vector2(e.getBounds().x - player.getBounds().x, e.getBounds().y - player.getBounds().y);
                    e.applyKnockback(hitDir, 350f);
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

        // --- EXIT GATE LOGIC ---
        checkExitGateStatus();
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
        for (Interactable interactable : interactables) {
            interactable.update(Gdx.graphics.getDeltaTime());

            if (interactable.isPlayerInRange(player) && Gdx.input.isKeyJustPressed(Input.Keys.G)) {
                if (interactable.interact(player)) {
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

    private void checkExitGateStatus() {
        boolean allEnemiesDead = true;
        for (Enemy e : enemies) {
            if (e.isAlive()) {
                allEnemiesDead = false;
                break;
            }
        }

        exitGateUnlocked = enemies.isEmpty() || allEnemiesDead;

        if (exitGateUnlocked) {
            Rectangle pBounds = player.getBounds();
            float pcx = pBounds.x + pBounds.width / 2f;
            float pcy = pBounds.y + pBounds.height / 2f;

            for (Rectangle gateRect : mapManager.getExitGateRects()) {
                float gcx = gateRect.x + gateRect.width / 2f;
                float gcy = gateRect.y + gateRect.height / 2f;
                float dx = pcx - gcx;
                float dy = pcy - gcy;

                if (dx * dx + dy * dy < 60f * 60f) {
                    exitGateReached = true;
                    break;
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
    public boolean isExitGateUnlocked() { return exitGateUnlocked; }
    public boolean isExitGateReached() { return exitGateReached; }
    public Player getPlayer() { return player; }
    public ArrayList<Enemy> getEnemies() { return enemies; }
    public ArrayList<LootDrop> getLootDrops() { return lootDrops; }
    public ArrayList<Interactable> getInteractables() { return interactables; }
    public MapManager getMapManager() { return mapManager; }
    public ArrayList<Rectangle> getBoundaries() { return boundaries; }

    public void dispose() {
        player.dispose();
        for (Enemy e : enemies) e.dispose();
    }
}
