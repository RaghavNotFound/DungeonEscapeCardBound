package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Handles LDtk map loading, entity extraction, IntGrid collision,
 * and tile layer rendering. Replaces the old TMX-based MapManager.
 */
public class MapManager {

    // Map dimensions in pixels
    private float mapWidth;
    private float mapHeight;
    private int gridSize;

    // Extracted game objects
    private final Vector2 playerSpawn = new Vector2();
    private final ArrayList<Vector2> enemySpawns = new ArrayList<>();
    private final ArrayList<Rectangle> collisionRects = new ArrayList<>();
    private final ArrayList<Rectangle> torchRects = new ArrayList<>();
    private final ArrayList<Rectangle> chestRects = new ArrayList<>();
    private final ArrayList<Rectangle> exitGateRects = new ArrayList<>();
    private final ArrayList<Rectangle> lavaRects = new ArrayList<>();
    private final ArrayList<Interactable> interactables = new ArrayList<>();

    // Tile rendering data
    private final ArrayList<TileLayerData> tileLayers = new ArrayList<>();
    private final HashMap<Integer, Texture> tilesetTextures = new HashMap<>();

    // Cached tileset info from LDtk defs
    private JsonValue tilesetDefs;

    private String currentMapPath;
    private int currentLevelIndex;

    /**
     * Represents one renderable tile layer (Ground, Wall, Objects, Campfire).
     */
    private static class TileLayerData {
        String identifier;
        ArrayList<TileInstance> tiles = new ArrayList<>();
    }

    /**
     * Represents one tile instance within a layer.
     */
    private static class TileInstance {
        float dstX, dstY;       // destination position in world (Y-flipped)
        float srcX, srcY;       // source position in tileset texture
        float width, height;    // tile size (gridSize)
        int flipFlags;          // 0=none, 1=flipX, 2=flipY, 3=both
        int tilesetUid;         // which tileset this tile uses
    }

    public void load(String path) {
        load(path, 0);
    }

    public void load(String path, int levelIndex) {
        clearData();
        this.currentMapPath = path;
        this.currentLevelIndex = levelIndex;

        // Parse the LDtk JSON file
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(Gdx.files.internal(path));

        // Cache tileset definitions for later lookup
        tilesetDefs = root.get("defs").get("tilesets");

        JsonValue levels = root.get("levels");
        if (levelIndex >= levels.size) {
            levelIndex = 0;
            // You might want to handle this better in production
        }

        JsonValue level = levels.get(levelIndex);
        mapWidth = level.getInt("pxWid");
        mapHeight = level.getInt("pxHei");

        // Determine grid size from the first layer
        JsonValue layers = level.get("layerInstances");
        gridSize = layers.get(0).getInt("__gridSize", 16);

        // Process layers (they come top-to-bottom in LDtk; we reverse for rendering)
        for (int i = layers.size - 1; i >= 0; i--) {
            JsonValue layer = layers.get(i);
            String type = layer.getString("__type");
            String identifier = layer.getString("__identifier");

            switch (type) {
                case "IntGrid":
                    if ("Collision".equals(identifier)) {
                        loadIntGridCollision(layer);
                    }
                    break;
                case "Entities":
                    loadEntities(layer);
                    break;
                case "Tiles":
                    loadTileLayer(layer);
                    break;
            }
        }

        // Add torch only if it's the safeRoom level
        if (currentMapPath.contains("safeRoom.ldtk") && torchRects.isEmpty()) {
            torchRects.add(new Rectangle(mapWidth * 0.5f, mapHeight * 0.5f, 8f, 8f));
        }

        // Generate fallback interactables if none found
        if (interactables.isEmpty()) {
            generateFallbackInteractables();
        }

        // Load tileset textures
        loadTilesetTextures(path);
    }

    private void clearData() {
        playerSpawn.setZero();
        enemySpawns.clear();
        collisionRects.clear();
        torchRects.clear();
        chestRects.clear();
        exitGateRects.clear();
        lavaRects.clear();
        interactables.clear();
        tileLayers.clear();
        tilesetTextures.values().forEach(Texture::dispose);
        tilesetTextures.clear();
    }

    public String getCurrentMapPath() {
        return currentMapPath;
    }

    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }

    // ===== COLLISION (IntGrid) =====

    private void loadIntGridCollision(JsonValue layer) {
        int layerGridSize = layer.getInt("__gridSize", gridSize);
        int cWid = layer.getInt("__cWid");
        JsonValue csv = layer.get("intGridCsv");

        for (int idx = 0; idx < csv.size; idx++) {
            if (csv.getInt(idx) != 0) {
                int col = idx % cWid;
                int row = idx / cWid;

                // LDtk Y-down → LibGDX Y-up
                float x = col * layerGridSize;
                float y = mapHeight - (row + 1) * layerGridSize;

                collisionRects.add(new Rectangle(x, y, layerGridSize, layerGridSize));
            }
        }
    }

    // ===== ENTITIES =====

    private void loadEntities(JsonValue layer) {
        JsonValue entities = layer.get("entityInstances");
        if (entities == null) return;

        for (JsonValue entity : entities) {
            String id = entity.getString("__identifier");
            JsonValue px = entity.get("px");
            float ldtkX = px.getInt(0);
            float ldtkY = px.getInt(1);
            int eWidth = entity.getInt("width", 16);
            int eHeight = entity.getInt("height", 16);

            JsonValue pivot = entity.get("__pivot");
            float pivotX = pivot != null ? pivot.getFloat(0) : 0f;
            float pivotY = pivot != null ? pivot.getFloat(1) : 0f;

            // LDtk px is at the pivot. Calculate top-left in LDtk:
            float leftLdtk = ldtkX - (pivotX * eWidth);
            float topLdtk = ldtkY - (pivotY * eHeight);

            // Convert Top-Left to LibGDX Y-up (bottom-left)
            float x = leftLdtk;
            float y = mapHeight - topLdtk - eHeight;

            System.out.println("[MapManager] Entity: " + id + " ldtk=(" + ldtkX + "," + ldtkY + ") -> libgdx=(" + x + "," + y + ") size=" + eWidth + "x" + eHeight);

            switch (id) {
                case "PlayerSpawn":
                    playerSpawn.set(x, y);
                    System.out.println("[MapManager] PlayerSpawn set to (" + x + ", " + y + ")");
                    break;
                case "Enemy":
                case "EnemySpawn":
                case "Enemy_spawn":
                    enemySpawns.add(new Vector2(x, y));
                    break;
                case "Chest":
                    chestRects.add(new Rectangle(x, y, eWidth, eHeight));
                    interactables.add(new Interactable(Interactable.Type.CHEST, x, y, eWidth, eHeight));
                    break;
                case "BossSpawn":
                    interactables.add(new Interactable(Interactable.Type.BOSS_TRIGGER, x, y, eWidth, eHeight));
                    break;
                case "Exit":
                case "ExitDoor":
                    exitGateRects.add(new Rectangle(x, y, eWidth, eHeight));
                    break;
                case "LavaDamage":
                    lavaRects.add(new Rectangle(x, y, eWidth, eHeight));
                    break;
            }
        }
    }

    // ===== TILE LAYERS =====

    private void loadTileLayer(JsonValue layer) {
        TileLayerData tld = new TileLayerData();
        tld.identifier = layer.getString("__identifier");

        int layerGridSize = layer.getInt("__gridSize", gridSize);
        int tilesetUid = layer.getInt("__tilesetDefUid", -1);

        // Override tileset if specified
        int overrideUid = layer.getInt("overrideTilesetUid", -1);
        if (overrideUid != -1) tilesetUid = overrideUid;

        JsonValue gridTiles = layer.get("gridTiles");
        if (gridTiles != null) {
            for (JsonValue tile : gridTiles) {
                TileInstance ti = new TileInstance();
                JsonValue dstPx = tile.get("px");
                JsonValue srcPx = tile.get("src");

                ti.dstX = dstPx.getInt(0);
                // LDtk Y-down → LibGDX Y-up
                ti.dstY = mapHeight - dstPx.getInt(1) - layerGridSize;
                ti.srcX = srcPx.getInt(0);
                ti.srcY = srcPx.getInt(1);
                ti.width = layerGridSize;
                ti.height = layerGridSize;
                ti.flipFlags = tile.getInt("f", 0);
                ti.tilesetUid = tilesetUid;

                tld.tiles.add(ti);
            }
        }

        if (!tld.tiles.isEmpty()) {
            tileLayers.add(tld);
        }
    }

    // ===== TILESET LOADING =====

    private void loadTilesetTextures(String mapPath) {
        if (tilesetDefs == null) return;

        // Determine the directory the LDtk file is in
        String dir = "";
        int lastSlash = mapPath.lastIndexOf('/');
        if (lastSlash >= 0) dir = mapPath.substring(0, lastSlash + 1);

        for (JsonValue ts : tilesetDefs) {
            int uid = ts.getInt("uid");
            String relPath = ts.getString("relPath", null);
            String embedAtlas = ts.getString("embedAtlas", null);

            // Skip embedded atlases (internal LDtk icons) and null paths
            if (embedAtlas != null || relPath == null) continue;

            // The relPath in the LDtk file is relative to the .ldtk file location.
            // But it may contain absolute-like paths. We try loading:
            // 1. Filename only from the Maps/ directory
            // 2. The relative path as-is (fallback)
            String filename = relPath;
            int lastSep = Math.max(relPath.lastIndexOf('/'), relPath.lastIndexOf('\\'));
            if (lastSep >= 0) filename = relPath.substring(lastSep + 1);

            String assetPath = dir + filename;

            try {
                if (Gdx.files.internal(assetPath).exists()) {
                    Texture tex = new Texture(Gdx.files.internal(assetPath));
                    tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                    tilesetTextures.put(uid, tex);
                } else {
                    System.out.println("[MapManager] Tileset not found: " + assetPath + " (uid=" + uid + ")");
                }
            } catch (Exception e) {
                System.out.println("[MapManager] Failed to load tileset: " + assetPath + " — " + e.getMessage());
            }
        }
    }

    // ===== RENDERING =====

    public void render(SpriteBatch batch, OrthographicCamera camera) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (TileLayerData tld : tileLayers) {
            for (TileInstance tile : tld.tiles) {
                Texture tex = tilesetTextures.get(tile.tilesetUid);
                if (tex == null) continue;

                boolean flipX = (tile.flipFlags & 1) != 0;
                boolean flipY = (tile.flipFlags & 2) != 0;

                TextureRegion region = new TextureRegion(tex,
                    (int) tile.srcX, (int) tile.srcY,
                    (int) tile.width, (int) tile.height);
                region.flip(flipX, flipY);

                batch.draw(region, tile.dstX, tile.dstY, tile.width, tile.height);
            }
        }

        batch.end();
    }

    // ===== FALLBACK GENERATORS =====





    private void generateFallbackInteractables() {
        interactables.add(new Interactable(Interactable.Type.SIGN, mapWidth * 0.5f, mapHeight * 0.8f, 8f, 10f, "Explore the dungeon!"));
        interactables.add(new Interactable(Interactable.Type.BARREL, mapWidth * 0.2f, mapHeight * 0.3f, 8f, 10f));
        interactables.add(new Interactable(Interactable.Type.BARREL, mapWidth * 0.8f, mapHeight * 0.7f, 8f, 10f));
    }

    // --- Getters for Extracted Data ---

    public float getMapWidth() { return mapWidth; }
    public float getMapHeight() { return mapHeight; }
    public int getGridSize() { return gridSize; }
    public Vector2 getPlayerSpawn() { return playerSpawn; }
    public ArrayList<Vector2> getEnemySpawns() { return enemySpawns; }
    public ArrayList<Rectangle> getCollisionRects() { return collisionRects; }
    public ArrayList<Rectangle> getTorchRects() { return torchRects; }
    public ArrayList<Rectangle> getChestRects() { return chestRects; }
    public ArrayList<Rectangle> getExitGateRects() { return exitGateRects; }
    public ArrayList<Rectangle> getLavaRects() { return lavaRects; }
    public ArrayList<Interactable> getInteractables() { return interactables; }

    public void dispose() {
        for (Texture tex : tilesetTextures.values()) {
            tex.dispose();
        }
        tilesetTextures.clear();
    }
}
