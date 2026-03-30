package io.github.pkgde;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.objects.*;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;

public class MapManager {

    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;

    private Vector2 playerSpawn = new Vector2();
    private ArrayList<Vector2> enemySpawns = new ArrayList<>();
    private ArrayList<Rectangle> collisionRects = new ArrayList<>();
    private ArrayList<Rectangle> torchRects = new ArrayList<>();
    private ArrayList<Rectangle> chestRects = new ArrayList<>();

    private static final float UNIT_SCALE = 1f;

    public void load(String path) {
        map = new TmxMapLoader().load(path);
        renderer = new OrthogonalTiledMapRenderer(map, UNIT_SCALE);

        enemySpawns.clear();
        collisionRects.clear();
        torchRects.clear();
        chestRects.clear();

        loadCollisions();
        loadSpawns();
        loadTorches();
        loadChests();
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
        MapLayer layer = getObjectLayer();
        if (layer == null) return;

        for (MapObject obj : layer.getObjects()) {
            Vector2 pos = extractObjectPosition(obj);
            if (pos == null) continue;

            if (isObjectTag(obj, "playerSpawn")) {
                playerSpawn.set(pos);
            } else if (isObjectTag(obj, "enemySpawn")) {
                enemySpawns.add(new Vector2(pos));
            }
        }
    }

    private void loadCollisions() {
        MapLayer objectLayer = getObjectLayer();
        if (objectLayer != null) {
            for (MapObject obj : objectLayer.getObjects()) {

                if (isObjectTag(obj, "wall") || isObjectTag(obj, "water") || isObjectTag(obj, "centerFire")) {
                    Rectangle r = extractObjectBounds(obj);
                    if (r != null) {
                        collisionRects.add(r);
                    }
                }
            }
            return;
        }

        // Fallback for older maps that store collisions in separate layers.
        String[] legacyLayers = { "wall", "water", "centerFire" };
        for (String layerName : legacyLayers) {
            MapLayer layer = map.getLayers().get(layerName);
            if (layer == null) continue;

            for (MapObject obj : layer.getObjects()) {
                Rectangle r = extractObjectBounds(obj);
                if (r != null) {
                    collisionRects.add(r);
                }
            }
        }
    }

    private MapLayer getObjectLayer() {
        MapLayer layer = map.getLayers().get("object");
        if (layer != null) {
            return layer;
        }
        return map.getLayers().get("objects");
    }

    private void loadTorches() {
        MapLayer layer = getObjectLayer();
        if (layer == null) return;

        for (MapObject obj : layer.getObjects()) {
            if (!isObjectTag(obj, "torch")) {
                continue;
            }

            Rectangle bounds = extractObjectBounds(obj);
            if (bounds != null) {
                torchRects.add(bounds);
            }
        }
    }

    private void loadChests() {
        MapLayer layer = getObjectLayer();
        if (layer == null) return;

        for (MapObject obj : layer.getObjects()) {
            if (!isObjectTag(obj, "chest")) {
                continue;
            }

            Rectangle bounds = extractObjectBounds(obj);
            if (bounds != null) {
                chestRects.add(bounds);
            }
        }
    }

    private boolean isObjectTag(MapObject obj, String expectedTag) {
        String name = obj.getName();
        if (expectedTag.equals(name)) {
            return true;
        }

        Object type = obj.getProperties().get("type");
        return expectedTag.equals(type);
    }

    private Vector2 extractObjectPosition(MapObject obj) {
        if (obj instanceof RectangleMapObject) {
            Rectangle rect = ((RectangleMapObject) obj).getRectangle();
            return new Vector2(rect.x, rect.y);
        }

        if (obj instanceof EllipseMapObject) {
            Circle c = ellipseAsCircle((EllipseMapObject) obj);
            return new Vector2(c.x, c.y);
        }

        return null;
    }

    private Rectangle extractObjectBounds(MapObject obj) {
        if (obj instanceof RectangleMapObject) {
            Rectangle rect = ((RectangleMapObject) obj).getRectangle();
            float rotation = obj.getProperties().get("rotation", 0f, Float.class);

            if (MathUtils.isZero(rotation, 0.001f)) {
                return new Rectangle(rect);
            }

            float radians = rotation * MathUtils.degreesToRadians;
            float cos = MathUtils.cos(radians);
            float sin = MathUtils.sin(radians);

            float x0 = rect.x;
            float y0 = rect.y;

            float x1 = x0 + rect.width * cos;
            float y1 = y0 + rect.width * sin;

            float x2 = x0 + rect.width * cos - rect.height * sin;
            float y2 = y0 + rect.width * sin + rect.height * cos;

            float x3 = x0 - rect.height * sin;
            float y3 = y0 + rect.height * cos;

            float minX = Math.min(Math.min(x0, x1), Math.min(x2, x3));
            float maxX = Math.max(Math.max(x0, x1), Math.max(x2, x3));
            float minY = Math.min(Math.min(y0, y1), Math.min(y2, y3));
            float maxY = Math.max(Math.max(y0, y1), Math.max(y2, y3));

            return new Rectangle(minX, minY, maxX - minX, maxY - minY);
        }

        if (obj instanceof EllipseMapObject) {
            Circle c = ellipseAsCircle((EllipseMapObject) obj);
            return new Rectangle(c.x - c.radius, c.y - c.radius, c.radius * 2f, c.radius * 2f);
        }

        return null;
    }

    private Circle ellipseAsCircle(EllipseMapObject obj) {
        float x = obj.getEllipse().x;
        float y = obj.getEllipse().y;
        float w = obj.getEllipse().width;
        float h = obj.getEllipse().height;
        float radius = Math.max(w, h) * 0.5f;
        return new Circle(x + w * 0.5f, y + h * 0.5f, radius);
    }

    public void render(OrthographicCamera camera) {
        renderer.setView(camera);
        renderer.render();
    }

    public Vector2 getPlayerSpawn() { return playerSpawn; }
    public ArrayList<Vector2> getEnemySpawns() { return enemySpawns; }
    public ArrayList<Rectangle> getCollisionRects() { return collisionRects; }
    public ArrayList<Rectangle> getTorchRects() { return torchRects; }
    public ArrayList<Rectangle> getChestRects() { return chestRects; }

    public void dispose() {
        map.dispose();
        renderer.dispose();
    }
}
