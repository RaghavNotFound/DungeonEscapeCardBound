package io.github.pkgde;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;

public class GameWorld {

    private Player player;
    private Enemy enemy;

    private ArrayList<Rectangle> boundaries;

    public GameWorld(MapManager mapManager) {

        player = new Player();
        enemy = new Enemy();

        boundaries = mapManager.getCollisionRects();
        player.setBoundaries(boundaries);

        // 🔥 APPLY SPAWN
        player.getPosition().set(mapManager.getPlayerSpawn());

        // (for now single enemy)
        if (!mapManager.getEnemySpawns().isEmpty()) {
            enemy.getBounds().setPosition(mapManager.getEnemySpawns().get(0));
        }
    }

    public void update(float delta, OrthographicCamera camera) {
        player.update(delta, camera);
        enemy.update(delta, player);
    }

    // 🔥 FIXED METHOD
    public boolean isPlayerNearEnemy() {

        Vector2 playerPos = player.getPosition();

        // ✅ FIX: Rectangle has no getPosition()
        Vector2 enemyPos = new Vector2(
            enemy.getBounds().x,
            enemy.getBounds().y
        );

        float distance = playerPos.dst(enemyPos);

        return distance < 150f;
    }

    public ArrayList<Rectangle> getBoundaries() {
        return boundaries;
    }

    public Player getPlayer() {
        return player;
    }

    public Enemy getEnemy() {
        return enemy;
    }

    public void dispose() {
        player.dispose();
        enemy.dispose();
    }
}
