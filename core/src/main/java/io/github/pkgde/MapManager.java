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

    private Vector2 playerSpawn = new Vector2();
    private ArrayList<Vector2> enemySpawns = new ArrayList<>();
    private ArrayList<Rectangle> collisionRects = new ArrayList<>();

    private static final float UNIT_SCALE = 1f;

    public void load(String path) {
        map = new TmxMapLoader().load(path);
        renderer = new OrthogonalTiledMapRenderer(map, UNIT_SCALE);

        loadCollisions();
        loadSpawns();
    }

    public float getMapWidth() {
        return map.getProperties().get("width", Integer.class) *
            map.getProperties().get("tilewidth", Integer.class);
    }

    public float getMapHeight() {
        return map.getProperties().get("height", Integer.class) *
            map.getProperties().get("tileheight", Integer.class);
    }

    private void loadSpawns() {
        MapLayer layer = map.getLayers().get("objects");
        if (layer == null) return;

        for (MapObject obj : layer.getObjects()) {
            if (obj instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                String name = obj.getName();

                if ("playerSpawn".equals(name)) {
                    playerSpawn.set(rect.x, rect.y);
                } else if ("enemySpawn".equals(name)) {
                    enemySpawns.add(new Vector2(rect.x, rect.y));
                }
            }
        }
    }

    private void loadCollisions() {
        String[] layers = { "wall", "water", "centerFire" };

        for (String name : layers) {
            MapLayer layer = map.getLayers().get(name);
            if (layer == null) continue;

            for (MapObject obj : layer.getObjects()) {
                if (obj instanceof RectangleMapObject) {
                    Rectangle r = ((RectangleMapObject) obj).getRectangle();
                    collisionRects.add(new Rectangle(r));
                }
            }
        }
    }

    public void render(OrthographicCamera camera) {
        renderer.setView(camera);
        renderer.render();
    }

    public Vector2 getPlayerSpawn() { return playerSpawn; }
    public ArrayList<Vector2> getEnemySpawns() { return enemySpawns; }
    public ArrayList<Rectangle> getCollisionRects() { return collisionRects; }

    public void dispose() {
        map.dispose();
        renderer.dispose();
    }
}
