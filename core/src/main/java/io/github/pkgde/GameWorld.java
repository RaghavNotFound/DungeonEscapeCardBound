package io.github.pkgde;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;

public class GameWorld {

    private Player player;
    private Enemy enemy;

    // 🔥 WORLD SIZE
    public static final float WORLD_WIDTH = 1280;
    public static final float WORLD_HEIGHT = 720;
    public static final float FLOOR_OFFSET = 120f;

    // 🔥 BOUNDARIES
    private static ArrayList<Rectangle> boundaries;

    public GameWorld() {

        player = new Player();
        enemy = new Enemy();

        boundaries = new ArrayList<>();

        float t = 10f; // thickness

        // LEFT
        boundaries.add(new Rectangle(-t, 0, t, WORLD_HEIGHT));

        // RIGHT
        boundaries.add(new Rectangle(WORLD_WIDTH, 0, t, WORLD_HEIGHT));

        // BOTTOM
        boundaries.add(new Rectangle(0, 0, WORLD_WIDTH, FLOOR_OFFSET));

        // TOP
        boundaries.add(new Rectangle(0, WORLD_HEIGHT, WORLD_WIDTH, t));
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

    public static ArrayList<Rectangle> getBoundaries() {
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
