package io.github.pkgde;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;

public class GameWorld {

    private static final float NEAR_ENEMY_DISTANCE = 150f;
    private static final float NEAR_ENEMY_DISTANCE_SQ = NEAR_ENEMY_DISTANCE * NEAR_ENEMY_DISTANCE;

    private final Player player;
    private final Enemy enemy;

    private final ArrayList<Rectangle> boundaries;

    public GameWorld(MapManager mapManager) {

        player = new Player();
        enemy = new Enemy();

        boundaries = mapManager.getCollisionRects();
        player.setBoundaries(boundaries);
        player.setWorldBounds(0f, 0f, mapManager.getMapWidth(), mapManager.getMapHeight());
        enemy.setBoundaries(boundaries);
        enemy.setWorldBounds(0f, 0f, mapManager.getMapWidth(), mapManager.getMapHeight());

        player.getPosition().set(mapManager.getPlayerSpawn());

        if (!mapManager.getEnemySpawns().isEmpty()) {
            Vector2 enemySpawn = mapManager.getEnemySpawns().get(0);
            enemy.setPosition(enemySpawn.x, enemySpawn.y);
        }
    }

    public void update(float delta, OrthographicCamera camera) {
        player.update(delta, camera);
        enemy.update(delta, player);

        ArrayList<Arrow> arrows = player.getArrows();
        if (arrows.isEmpty()) {
            return;
        }

        Rectangle enemyBounds = enemy.getBounds();
        for (int i = arrows.size() - 1; i >= 0; i--) {
            Arrow arrow = arrows.get(i);
            if (arrow.getBounds().overlaps(enemyBounds)) {
                arrows.remove(i);
            }
        }
    }

    public boolean isPlayerNearEnemy() {
        Vector2 p = player.getPosition();
        Rectangle b = enemy.getBounds();
        float ex = b.x + b.width * 0.5f;
        float ey = b.y + b.height * 0.5f;

        float dx = p.x - ex;
        float dy = p.y - ey;
        return dx * dx + dy * dy < NEAR_ENEMY_DISTANCE_SQ;
    }

    public Player getPlayer() { return player; }
    public Enemy getEnemy() { return enemy; }
    public ArrayList<Rectangle> getBoundaries() { return boundaries; }

    public void dispose() {
        player.dispose();
        enemy.dispose();
    }
}
