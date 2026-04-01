package io.github.pkgde;

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

    public static final float WORLD_WIDTH = 1280;
    public static final float WORLD_HEIGHT = 720;
    public static final float FLOOR_OFFSET = 120f;

    private final MapManager mapManager;
    private final Player player;
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<LootDrop> lootDrops = new ArrayList<>();

    private final ArrayList<Rectangle> boundaries;
    private final ArrayList<Polygon> collisionPolygons;

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
        for (int i = lootDrops.size() - 1; i >= 0; i--) {
            LootDrop drop = lootDrops.get(i);
            drop.update(delta);
            if (player.getBounds().overlaps(drop.getBounds())) {
                if (drop.getType() == LootDrop.Type.HEALTH) {
                    player.addHealth(25f);
                } else if (drop.getType() == LootDrop.Type.TORCH) {
                    player.addTorch();
                }
                lootDrops.remove(i);
            }
        }
    }

    private void spawnLoot(float x, float y) {
        float roll = MathUtils.random();
        if (roll < 0.35f) { // 35% chance health
            lootDrops.add(new LootDrop(LootDrop.Type.HEALTH, x, y));
        } else if (roll < 0.60f) { // 25% chance torch
            lootDrops.add(new LootDrop(LootDrop.Type.TORCH, x, y));
        }
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

    public Player getPlayer() { return player; }

    public Enemy getEnemy() {
        // Helper method added to support legacy single-enemy calls if they exist elsewhere.
        // Returns the first enemy, or null if empty.
        return enemies.isEmpty() ? null : enemies.get(0);
    }

    public ArrayList<Enemy> getEnemies() { return enemies; }
    public ArrayList<LootDrop> getLootDrops() { return lootDrops; }
    public MapManager getMapManager() { return mapManager; }
    public ArrayList<Rectangle> getBoundaries() { return boundaries; }

    public void dispose() {
        player.dispose();
        for (Enemy e : enemies) {
            e.dispose();
        }
    }
}
