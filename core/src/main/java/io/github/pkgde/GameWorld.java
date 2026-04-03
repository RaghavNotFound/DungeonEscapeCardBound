package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;

public class GameWorld {

    private static final float NEAR_ENEMY_DISTANCE = 150f;
    private static final float NEAR_ENEMY_DISTANCE_SQ = NEAR_ENEMY_DISTANCE * NEAR_ENEMY_DISTANCE;
    private static final float ARROW_DAMAGE = 20f;

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

        player = new Player();

        boundaries = mapManager.getCollisionRects();
        collisionPolygons = mapManager.getCollisionPolygons();

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

        interactables = mapManager.getInteractables();
    }

    public void update(float delta, OrthographicCamera camera) {
        player.update(delta, camera);
        for (Enemy e : enemies) {
            e.update(delta, player);
        }

        ArrayList<Rectangle> torches = mapManager.getTorchRects();
        for (int i = torches.size() - 1; i >= 0; i--) {
            if (player.getBounds().overlaps(torches.get(i))) {
                torches.remove(i);
                player.addTorch();
            }
        }

        if (player.canDealSwordDamage()) {
            for (Enemy e : enemies) {
                if (e.isAlive() && player.getSwordHitbox().overlaps(e.getBounds())) {
                    boolean wasAlive = e.isAlive();
                    e.takeDamage(player.getSwordDamage());
                    if (wasAlive && !e.isAlive()) {
                        player.incrementEnemiesKilled();
                        spawnLoot(e.getBounds().x + e.getBounds().width / 2f, e.getBounds().y + e.getBounds().height / 2f);
                    }
                    Vector2 dir = new Vector2(
                        e.getBounds().x - player.getBounds().x,
                        e.getBounds().y - player.getBounds().y
                    );
                    e.applyKnockback(dir, 400f);
                }
            }
            player.consumeSwordDamage();
        }

        for (Enemy e : enemies) {
            if (e.canDealDamage() && e.isPlayerInAttackRadius(player)) {
                player.takeDamage(e.getDamage());
                Vector2 dir = new Vector2(
                    player.getBounds().x - e.getBounds().x,
                    player.getBounds().y - e.getBounds().y
                );
                player.applyKnockback(dir, 500f);
                e.consumeAttackDamage();
            }
        }

        ArrayList<Arrow> arrows = player.getArrows();
        if (!arrows.isEmpty()) {
            for (int i = arrows.size() - 1; i >= 0; i--) {
                Arrow arrow = arrows.get(i);
                boolean hit = false;
                for (Enemy e : enemies) {
                    if (e.isAlive() && arrow.getBounds().overlaps(e.getBounds())) {
                        boolean wasAlive = e.isAlive();
                        e.takeDamage(ARROW_DAMAGE);
                        if (wasAlive && !e.isAlive()) {
                            player.incrementEnemiesKilled();
                            spawnLoot(e.getBounds().x + e.getBounds().width / 2f, e.getBounds().y + e.getBounds().height / 2f);
                        }
                        Vector2 hitDir = new Vector2(
                            e.getBounds().x - player.getBounds().x,
                            e.getBounds().y - player.getBounds().y
                        );
                        e.applyKnockback(hitDir, 350f);
                        hit = true;
                        break;
                    }
                }
                if (hit) {
                    arrows.remove(i);
                }
            }
        }

        // --- GROUP AGGRO MECHANIC ---
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
                            if (dx * dx + dy * dy < 480f * 480f) { // Alert radius
                                slacker.forceChase(3.5f);
                            }
                        }
                    }
                }
            }
        }

        // --- LOOT DROPS LOGIC ---
        float playerCX = player.getBounds().x + player.getBounds().width / 2f;
        float playerCY = player.getBounds().y + player.getBounds().height / 2f;

        for (int i = lootDrops.size() - 1; i >= 0; i--) {
            LootDrop drop = lootDrops.get(i);
            drop.update(delta);

            // Remove expired drops
            if (drop.isExpired()) {
                lootDrops.remove(i);
                continue;
            }

            // Magnet attraction
            drop.attractToward(playerCX, playerCY, delta);

            // Pickup check
            if (player.getBounds().overlaps(drop.getBounds())) {
                switch (drop.getType()) {
                    case HEALTH:
                        player.addHealth(25f);
                        break;
                    case TORCH:
                        player.addTorch();
                        break;
                    case ARROW:
                        // Give a small stamina boost as arrow ammo
                        player.addStamina(15f);
                        break;
                    case STAMINA:
                        player.addStamina(30f);
                        break;
                }
                lootDrops.remove(i);
            }
        }

        // --- INTERACTABLES ---
        for (Interactable interactable : interactables) {
            interactable.update(delta);

            if (interactable.isPlayerInRange(player)
                && Gdx.input.isKeyJustPressed(Input.Keys.G)
                && (!interactable.isInteracted() || interactable.getType() == Interactable.Type.SIGN)) {

                if (interactable.interact(player)) {
                    // Spawn loot from chests and barrels
                    if (interactable.getType() == Interactable.Type.CHEST) {
                        Rectangle b = interactable.getBounds();
                        float cx = b.x + b.width / 2f;
                        float cy = b.y + b.height / 2f;
                        // Chests give guaranteed + bonus loot
                        lootDrops.add(new LootDrop(LootDrop.Type.HEALTH, cx - 15f, cy + 20f));
                        if (MathUtils.random() < 0.5f) {
                            lootDrops.add(new LootDrop(LootDrop.Type.TORCH, cx + 15f, cy + 20f));
                        }
                        if (MathUtils.random() < 0.3f) {
                            lootDrops.add(new LootDrop(LootDrop.Type.STAMINA, cx, cy + 35f));
                        }
                    } else if (interactable.getType() == Interactable.Type.BARREL) {
                        Rectangle b = interactable.getBounds();
                        float cx = b.x + b.width / 2f;
                        float cy = b.y + b.height / 2f;
                        // Barrels give random single drop
                        float roll = MathUtils.random();
                        if (roll < 0.4f) {
                            lootDrops.add(new LootDrop(LootDrop.Type.HEALTH, cx, cy + 15f));
                        } else if (roll < 0.7f) {
                            lootDrops.add(new LootDrop(LootDrop.Type.STAMINA, cx, cy + 15f));
                        } else {
                            lootDrops.add(new LootDrop(LootDrop.Type.ARROW, cx, cy + 15f));
                        }
                    }
                }
            }
        }

        // --- EXIT GATE ---
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
                // Trigger when player center is within 60px of gate center
                if (dx * dx + dy * dy < 60f * 60f) {
                    exitGateReached = true;
                    break;
                }
            }
        }
    }

    private void spawnLoot(float x, float y) {
        float roll = MathUtils.random();
        if (roll < 0.30f) { // 30% health
            lootDrops.add(new LootDrop(LootDrop.Type.HEALTH, x, y));
        } else if (roll < 0.50f) { // 20% torch
            lootDrops.add(new LootDrop(LootDrop.Type.TORCH, x, y));
        } else if (roll < 0.65f) { // 15% stamina
            lootDrops.add(new LootDrop(LootDrop.Type.STAMINA, x, y));
        } else if (roll < 0.78f) { // 13% arrow
            lootDrops.add(new LootDrop(LootDrop.Type.ARROW, x, y));
        }
        // 22% nothing
    }

    public boolean isPlayerNearEnemy() {
        Vector2 p = player.getPosition();
        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;

            Rectangle b = e.getBounds();
            float ex = b.x + b.width * 0.5f;
            float ey = b.y + b.height * 0.5f;

            float dx = p.x - ex;
            float dy = p.y - ey;
            if (dx * dx + dy * dy < NEAR_ENEMY_DISTANCE_SQ) {
                return true;
            }
        }
        return false;
    }

    // ===== EXIT GATE =====
    public boolean isExitGateUnlocked() { return exitGateUnlocked; }
    public boolean isExitGateReached() { return exitGateReached; }

    public Player getPlayer() { return player; }

    public Enemy getEnemy() {
        return enemies.isEmpty() ? null : enemies.get(0);
    }

    public ArrayList<Enemy> getEnemies() { return enemies; }
    public ArrayList<LootDrop> getLootDrops() { return lootDrops; }
    public ArrayList<Interactable> getInteractables() { return interactables; }
    public MapManager getMapManager() { return mapManager; }
    public ArrayList<Rectangle> getBoundaries() { return boundaries; }

    public void dispose() {
        player.dispose();
        for (Enemy e : enemies) {
            e.dispose();
        }
    }
}
