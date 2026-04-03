package io.github.pkgde;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.objects.*;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;

/**
 * Handles TiledMap loading, object extraction (spawns, collisions, interactables),
 * and rendering. Provides fallback generation for missing map objects.
 */
public class MapManager {

    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;

    private final Vector2 playerSpawn = new Vector2();
    private final ArrayList<Vector2> enemySpawns = new ArrayList<>();
    private final ArrayList<Rectangle> collisionRects = new ArrayList<>();
    private final ArrayList<Polygon> collisionPolygons = new ArrayList<>();
    private final ArrayList<Rectangle> torchRects = new ArrayList<>();
    private final ArrayList<Rectangle> chestRects = new ArrayList<>();
    private final ArrayList<Rectangle> exitGateRects = new ArrayList<>();
    private final ArrayList<Interactable> interactables = new ArrayList<>();

    private static final float UNIT_SCALE = 1f;

    public void load(String path) {
        map = new TmxMapLoader().load(path);
        renderer = new OrthogonalTiledMapRenderer(map, UNIT_SCALE);

        clearData();

        loadCollisions();
        loadSpawns();
        loadTorches();
        loadChests();
        loadExitGates();
        loadInteractables();
    }

    private void clearData() {
        enemySpawns.clear();
        collisionRects.clear();
        collisionPolygons.clear();
        torchRects.clear();
        chestRects.clear();
        exitGateRects.clear();
        interactables.clear();
    }

    public float getMapWidth() {
        return map.getProperties().get("width", Integer.class) *
            map.getProperties().get("tilewidth", Integer.class);
    }

    public float getMapHeight() {
        return map.getProperties().get("height", Integer.class) *
            map.getProperties().get("tileheight", Integer.class);
    }

    private MapLayer getObjectLayer() {
        MapLayer layer = map.getLayers().get("object");
        if (layer != null) return layer;
        return map.getLayers().get("objects");
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
                    processCollisionObject(obj);
                }
            }
            return;
        }

        // Legacy/Fallback Layer Processing
        String[] legacyLayers = {"wall", "water", "centerFire"};
        for (String layerName : legacyLayers) {
            MapLayer layer = map.getLayers().get(layerName);
            if (layer == null) continue;
            for (MapObject obj : layer.getObjects()) {
                processCollisionObject(obj);
            }
        }
    }

    private void processCollisionObject(MapObject obj) {
        if (obj instanceof PolygonMapObject) {
            collisionPolygons.add(((PolygonMapObject) obj).getPolygon());
        } else {
            Rectangle r = extractObjectBounds(obj);
            if (r != null) collisionRects.add(r);
        }
    }

    private void loadTorches() {
        MapLayer layer = getObjectLayer();
        if (layer != null) {
            for (MapObject obj : layer.getObjects()) {
                if (isObjectTag(obj, "torch")) {
                    Rectangle bounds = extractObjectBounds(obj);
                    if (bounds != null) torchRects.add(bounds);
                }
            }
        }

        // If no torches in map, generate random valid positions
        if (torchRects.isEmpty()) {
            generateRandomTorches(5);
        }
    }

    private void generateRandomTorches(int count) {
        float mapW = getMapWidth();
        float mapH = getMapHeight();
        for (int i = 0; i < count; i++) {
            for (int attempt = 0; attempt < 50; attempt++) {
                float x = MathUtils.random(60f, mapW - 60f);
                float y = MathUtils.random(60f, mapH - 60f);
                Rectangle r = new Rectangle(x, y, 32f, 32f);

                if (!isPositionColliding(r)) {
                    torchRects.add(r);
                    break;
                }
            }
        }
    }

    private boolean isPositionColliding(Rectangle r) {
        for (Rectangle wall : collisionRects) {
            if (wall.overlaps(r)) return true;
        }
        for (Polygon poly : collisionPolygons) {
            if (poly.getBoundingRectangle().overlaps(r)) return true;
        }
        return false;
    }

    private void loadChests() {
        MapLayer layer = getObjectLayer();
        if (layer == null) return;
        for (MapObject obj : layer.getObjects()) {
            if (isObjectTag(obj, "chest")) {
                Rectangle bounds = extractObjectBounds(obj);
                if (bounds != null) chestRects.add(bounds);
            }
        }
    }

    private void loadExitGates() {
        MapLayer layer = getObjectLayer();
        if (layer != null) {
            for (MapObject obj : layer.getObjects()) {
                if (isObjectTag(obj, "exitGate")) {
                    Rectangle bounds = extractObjectBounds(obj);
                    if (bounds != null) exitGateRects.add(bounds);
                }
            }
        }
        if (exitGateRects.isEmpty()) {
            float mapW = getMapWidth();
            float mapH = getMapHeight();
            exitGateRects.add(new Rectangle(mapW * 0.5f - 40f, mapH * 0.75f - 40f, 80f, 80f));
        }
    }

    private void loadInteractables() {
        MapLayer layer = getObjectLayer();
        if (layer != null) {
            for (MapObject obj : layer.getObjects()) {
                Rectangle b = extractObjectBounds(obj);
                if (b == null) continue;

                if (isObjectTag(obj, "chest")) {
                    interactables.add(new Interactable(Interactable.Type.CHEST, b.x, b.y, b.width, b.height));
                } else if (isObjectTag(obj, "sign")) {
                    Object textProp = obj.getProperties().get("text");
                    String text = (textProp != null) ? textProp.toString() : "";
                    interactables.add(new Interactable(Interactable.Type.SIGN, b.x, b.y, b.width, b.height, text));
                } else if (isObjectTag(obj, "barrel")) {
                    interactables.add(new Interactable(Interactable.Type.BARREL, b.x, b.y, b.width, b.height));
                }
            }
        }
        if (interactables.isEmpty()) {
            generateFallbackInteractables();
        }
    }

    private void generateFallbackInteractables() {
        float mapW = getMapWidth();
        float mapH = getMapHeight();
        interactables.add(new Interactable(Interactable.Type.CHEST, mapW * 0.3f, mapH * 0.6f, 40f, 35f));
        interactables.add(new Interactable(Interactable.Type.CHEST, mapW * 0.7f, mapH * 0.4f, 40f, 35f));
        interactables.add(new Interactable(Interactable.Type.SIGN, mapW * 0.5f, mapH * 0.8f, 30f, 40f, "Find the exit gate!"));
        interactables.add(new Interactable(Interactable.Type.BARREL, mapW * 0.2f, mapH * 0.3f, 32f, 38f));
        interactables.add(new Interactable(Interactable.Type.BARREL, mapW * 0.8f, mapH * 0.7f, 32f, 38f));
    }

    private boolean isObjectTag(MapObject obj, String expectedTag) {
        String name = obj.getName();
        if (expectedTag.equals(name)) return true;
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

            if (MathUtils.isZero(rotation, 0.001f)) return new Rectangle(rect);

            float radians = rotation * MathUtils.degreesToRadians;
            float cos = MathUtils.cos(radians);
            float sin = MathUtils.sin(radians);

            float x0 = rect.x, y0 = rect.y;
            float x1 = x0 + rect.width * cos, y1 = y0 + rect.width * sin;
            float x2 = x0 + rect.width * cos - rect.height * sin, y2 = y0 + rect.width * sin + rect.height * cos;
            float x3 = x0 - rect.height * sin, y3 = y0 + rect.height * cos;

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
        float x = obj.getEllipse().x, y = obj.getEllipse().y;
        float w = obj.getEllipse().width, h = obj.getEllipse().height;
        float radius = Math.max(w, h) * 0.5f;
        return new Circle(x + w * 0.5f, y + h * 0.5f, radius);
    }

    public void render(OrthographicCamera camera) {
        renderer.setView(camera);
        renderer.render();
    }

    // ===== GETTERS =====
    public Vector2 getPlayerSpawn() { return playerSpawn; }
    public ArrayList<Vector2> getEnemySpawns() { return enemySpawns; }
    public ArrayList<Rectangle> getCollisionRects() { return collisionRects; }
    public ArrayList<Polygon> getCollisionPolygons() { return collisionPolygons; }
    public ArrayList<Rectangle> getTorchRects() { return torchRects; }
    public ArrayList<Rectangle> getChestRects() { return chestRects; }
    public ArrayList<Rectangle> getExitGateRects() { return exitGateRects; }
    public ArrayList<Interactable> getInteractables() { return interactables; }

    public void dispose() {
        if (renderer != null) renderer.dispose();
        if (map != null) map.dispose();
    }
}
