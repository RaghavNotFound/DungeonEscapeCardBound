package io.github.pkgde;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.objects.*;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;

public class MapManager {

    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;

    // ===== SPAWN DATA =====
    private Vector2 playerSpawn = new Vector2();
    private ArrayList<Vector2> enemySpawns = new ArrayList<>();

    // ===== COLLISION =====
    private ArrayList<Rectangle> collisionRects = new ArrayList<>();

    // 🔥 KEEP THIS FIXED
    private static final float UNIT_SCALE = 1f;

    // ================================
    // LOAD MAP
    // ================================
    public void load(String path) {
        map = new TmxMapLoader().load(path);

        renderer = new OrthogonalTiledMapRenderer(map, UNIT_SCALE);

        loadCollisions();
        loadSpawns();
    }

    // ================================
    // MAP SIZE (IMPORTANT)
    // ================================
    public float getMapWidth() {
        return map.getProperties().get("width", Integer.class) *
            map.getProperties().get("tilewidth", Integer.class);
    }

    public float getMapHeight() {
        return map.getProperties().get("height", Integer.class) *
            map.getProperties().get("tileheight", Integer.class);
    }

    // ================================
    // LOAD SPAWNS
    // ================================
    private void loadSpawns() {

        MapLayer layer = map.getLayers().get("objects");

        if (layer == null) return;

        for (MapObject obj : layer.getObjects()) {

            if (obj instanceof RectangleMapObject) {

                Rectangle rect = ((RectangleMapObject) obj).getRectangle();

                String name = obj.getName();

                float x = rect.x * UNIT_SCALE;
                float y = rect.y * UNIT_SCALE;

                if ("playerSpawn".equals(name)) {
                    playerSpawn.set(x, y);
                }
                else if ("enemySpawn".equals(name)) {
                    enemySpawns.add(new Vector2(x, y));
                }
            }
        }
    }

    // ================================
    // LOAD COLLISIONS
    // ================================
    private void loadCollisions() {

        String[] collisionLayers = { "wall", "water", "centerFire" };

        for (String layerName : collisionLayers) {

            MapLayer layer = map.getLayers().get(layerName);

            if (layer == null) continue;

            for (MapObject obj : layer.getObjects()) {

                if (obj instanceof RectangleMapObject) {

                    Rectangle rect = ((RectangleMapObject) obj).getRectangle();

                    collisionRects.add(new Rectangle(
                        rect.x * UNIT_SCALE,
                        rect.y * UNIT_SCALE,
                        rect.width * UNIT_SCALE,
                        rect.height * UNIT_SCALE
                    ));
                }
            }
        }
    }

    // ================================
    // RENDER
    // ================================
    public void render(OrthographicCamera camera) {

        renderer.setView(
            camera.combined,
            camera.position.x - camera.viewportWidth / 2,
            camera.position.y - camera.viewportHeight / 2,
            camera.viewportWidth,
            camera.viewportHeight
        );

        renderer.render();
    }

    // ================================
    // GETTERS
    // ================================
    public Vector2 getPlayerSpawn() {
        return playerSpawn;
    }

    public ArrayList<Vector2> getEnemySpawns() {
        return enemySpawns;
    }

    public ArrayList<Rectangle> getCollisionRects() {
        return collisionRects;
    }

    // ================================
    // CLEANUP
    // ================================
    public void dispose() {
        if (map != null) map.dispose();
        if (renderer != null) renderer.dispose();
    }
}
