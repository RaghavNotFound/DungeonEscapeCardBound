package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;

/**
 * F3 Debug Overlay — provides visual debugging overlays for:
 *   [1] Hitboxes   — Player, enemy, sword, arrow hitboxes
 *   [2] Boundaries — Map border, world bounds visualization
 *   [3] Collisions — IntGrid collision rects, chest/exit hitboxes, spawn markers
 *   [4] Info       — FPS counter, entity counts, player stats text panel
 *
 * Toggle the entire overlay with F3.
 * Toggle individual sections with 1-4 while the overlay is open.
 */
public class DebugOverlay {

    private boolean visible = false;

    // Individual section toggles (all default ON when overlay opens)
    private boolean showHitboxes = true;
    private boolean showBoundaries = true;
    private boolean showCollisions = true;
    private boolean showInfo = true;

    // Colors
    private static final Color COLOR_PLAYER_HITBOX = new Color(0.2f, 1f, 0.3f, 0.85f);
    private static final Color COLOR_ENEMY_HITBOX = new Color(1f, 0.2f, 0.8f, 0.85f);
    private static final Color COLOR_SWORD_HITBOX = new Color(1f, 1f, 0.2f, 0.85f);
    private static final Color COLOR_ARROW_HITBOX = new Color(0.2f, 0.8f, 1f, 0.85f);
    private static final Color COLOR_COLLISION = new Color(1f, 0.15f, 0.15f, 0.5f);
    private static final Color COLOR_COLLISION_OUTLINE = new Color(1f, 0.3f, 0.3f, 0.8f);
    private static final Color COLOR_BOUNDARY = new Color(1f, 0.6f, 0f, 0.9f);
    private static final Color COLOR_CHEST = new Color(1f, 0.85f, 0.1f, 0.8f);
    private static final Color COLOR_EXIT = new Color(0.1f, 1f, 0.7f, 0.8f);
    private static final Color COLOR_PLAYER_SPAWN = new Color(0.3f, 0.6f, 1f, 0.9f);
    private static final Color COLOR_ENEMY_SPAWN = new Color(1f, 0.35f, 0.35f, 0.9f);
    private static final Color COLOR_PANEL_BG = new Color(0.05f, 0.05f, 0.12f, 0.88f);
    private static final Color COLOR_PANEL_BORDER = new Color(0.3f, 0.3f, 0.5f, 0.9f);
    private static final Color COLOR_ACTIVE_TAB = new Color(0.25f, 0.85f, 0.55f, 1f);
    private static final Color COLOR_INACTIVE_TAB = new Color(0.55f, 0.55f, 0.55f, 1f);
    private static final Color COLOR_TEXT = new Color(0.9f, 0.92f, 0.95f, 1f);
    private static final Color COLOR_LABEL = new Color(0.6f, 0.65f, 0.7f, 1f);

    private final GlyphLayout layout = new GlyphLayout();

    public boolean isVisible() { return visible; }

    /**
     * Call once per frame BEFORE update logic. Returns true if the overlay consumed the F3 key press.
     */
    public boolean handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            visible = !visible;
            return true;
        }

        if (!visible) return false;

        // Section toggles 1-4
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) showHitboxes = !showHitboxes;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) showBoundaries = !showBoundaries;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) showCollisions = !showCollisions;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) showInfo = !showInfo;

        return false;
    }

    /**
     * Render the debug overlay on top of the game world.
     */
    public void render(ShapeRenderer shape, SpriteBatch batch, BitmapFont font,
                       OrthographicCamera camera, GameWorld world, MapManager mapManager) {
        if (!visible) return;

        shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // ===== WORLD-SPACE OVERLAYS (drawn in camera/world coordinates) =====

        if (showCollisions) {
            drawCollisionRects(shape, mapManager);
            drawChestHitboxes(shape, mapManager);
            drawExitHitboxes(shape, mapManager);
            drawSpawnMarkers(shape, mapManager);
        }

        if (showBoundaries) {
            drawMapBorder(shape, mapManager);
        }

        if (showHitboxes) {
            drawEntityHitboxes(shape, world);
        }

        // ===== SCREEN-SPACE HUD (drawn relative to camera) =====

        if (showInfo) {
            drawInfoPanel(shape, batch, font, camera, world, mapManager);
        }

        // Always draw the tab bar when overlay is visible
        drawTabBar(shape, batch, font, camera);

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ==================== COLLISION RECTS ====================

    private void drawCollisionRects(ShapeRenderer shape, MapManager mapManager) {
        if (mapManager == null) return;
        ArrayList<Rectangle> rects = mapManager.getCollisionRects();

        // Filled semi-transparent
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COLOR_COLLISION);
        for (Rectangle r : rects) {
            shape.rect(r.x, r.y, r.width, r.height);
        }
        shape.end();

        // Outlines
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(COLOR_COLLISION_OUTLINE);
        for (Rectangle r : rects) {
            shape.rect(r.x, r.y, r.width, r.height);
        }
        shape.end();
    }

    // ==================== CHEST HITBOXES ====================

    private void drawChestHitboxes(ShapeRenderer shape, MapManager mapManager) {
        if (mapManager == null) return;
        ArrayList<Rectangle> chests = mapManager.getChestRects();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COLOR_CHEST.r, COLOR_CHEST.g, COLOR_CHEST.b, 0.25f);
        for (Rectangle r : chests) {
            shape.rect(r.x, r.y, r.width, r.height);
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shape.setColor(COLOR_CHEST);
        for (Rectangle r : chests) {
            shape.rect(r.x, r.y, r.width, r.height);
            // Draw an X marker to identify chests
            shape.line(r.x, r.y, r.x + r.width, r.y + r.height);
            shape.line(r.x + r.width, r.y, r.x, r.y + r.height);
        }
        Gdx.gl.glLineWidth(1f);
        shape.end();

        // Label
        // (Labels are drawn in info panel instead — world labels require SpriteBatch)
    }

    // ==================== EXIT HITBOXES ====================

    private void drawExitHitboxes(ShapeRenderer shape, MapManager mapManager) {
        if (mapManager == null) return;
        ArrayList<Rectangle> exits = mapManager.getExitGateRects();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COLOR_EXIT.r, COLOR_EXIT.g, COLOR_EXIT.b, 0.2f);
        for (Rectangle r : exits) {
            shape.rect(r.x, r.y, r.width, r.height);
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shape.setColor(COLOR_EXIT);
        for (Rectangle r : exits) {
            shape.rect(r.x, r.y, r.width, r.height);
            // Draw arrow-like chevron to indicate exit
            float cx = r.x + r.width / 2f;
            float cy = r.y + r.height / 2f;
            float s = Math.min(r.width, r.height) * 0.3f;
            shape.line(cx - s, cy - s, cx, cy);
            shape.line(cx, cy, cx - s, cy + s);
            shape.line(cx, cy - s, cx + s, cy);
            shape.line(cx + s, cy, cx, cy + s);
        }
        Gdx.gl.glLineWidth(1f);
        shape.end();
    }

    // ==================== SPAWN MARKERS ====================

    private void drawSpawnMarkers(ShapeRenderer shape, MapManager mapManager) {
        if (mapManager == null) return;

        // Player Spawn — draw a filled diamond
        Vector2 ps = mapManager.getPlayerSpawn();
        if (ps.x != 0 || ps.y != 0) {
            float size = 6f;
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(COLOR_PLAYER_SPAWN);
            shape.triangle(ps.x, ps.y - size, ps.x - size, ps.y, ps.x, ps.y + size);
            shape.triangle(ps.x, ps.y - size, ps.x + size, ps.y, ps.x, ps.y + size);
            shape.end();

            // Cross-hair ring
            shape.begin(ShapeRenderer.ShapeType.Line);
            Gdx.gl.glLineWidth(2f);
            shape.setColor(COLOR_PLAYER_SPAWN);
            shape.circle(ps.x, ps.y, size + 3f, 24);
            Gdx.gl.glLineWidth(1f);
            shape.end();
        }

        // Enemy Spawns — draw red circles with X
        ArrayList<Vector2> es = mapManager.getEnemySpawns();
        for (Vector2 spawn : es) {
            float size = 5f;
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(COLOR_ENEMY_SPAWN.r, COLOR_ENEMY_SPAWN.g, COLOR_ENEMY_SPAWN.b, 0.4f);
            shape.circle(spawn.x, spawn.y, size, 16);
            shape.end();

            shape.begin(ShapeRenderer.ShapeType.Line);
            Gdx.gl.glLineWidth(2f);
            shape.setColor(COLOR_ENEMY_SPAWN);
            shape.circle(spawn.x, spawn.y, size + 2f, 16);
            shape.line(spawn.x - size, spawn.y - size, spawn.x + size, spawn.y + size);
            shape.line(spawn.x + size, spawn.y - size, spawn.x - size, spawn.y + size);
            Gdx.gl.glLineWidth(1f);
            shape.end();
        }
    }

    // ==================== MAP BORDER ====================

    private void drawMapBorder(ShapeRenderer shape, MapManager mapManager) {
        if (mapManager == null) return;

        float w = mapManager.getMapWidth();
        float h = mapManager.getMapHeight();

        // Thick orange border around the entire map
        shape.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(3f);
        shape.setColor(COLOR_BOUNDARY);
        shape.rect(0, 0, w, h);

        // Corner marks
        float markSize = 12f;
        // Bottom-left
        shape.line(0, 0, markSize, 0);
        shape.line(0, 0, 0, markSize);
        // Bottom-right
        shape.line(w, 0, w - markSize, 0);
        shape.line(w, 0, w, markSize);
        // Top-left
        shape.line(0, h, markSize, h);
        shape.line(0, h, 0, h - markSize);
        // Top-right
        shape.line(w, h, w - markSize, h);
        shape.line(w, h, w, h - markSize);

        Gdx.gl.glLineWidth(1f);
        shape.end();

        // Dim fill outside the map is not needed since camera is locked to map
    }

    // ==================== ENTITY HITBOXES ====================

    private void drawEntityHitboxes(ShapeRenderer shape, GameWorld world) {
        Player player = world.getPlayer();

        // Player hitbox
        shape.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shape.setColor(COLOR_PLAYER_HITBOX);
        Rectangle pb = player.getBounds();
        shape.rect(pb.x, pb.y, pb.width, pb.height);

        // Sword hitbox (when active)
        if (player.canDealSwordDamage()) {
            shape.setColor(COLOR_SWORD_HITBOX);
            Rectangle sb = player.getSwordHitbox();
            shape.rect(sb.x, sb.y, sb.width, sb.height);
        }

        // Arrow hitboxes
        shape.setColor(COLOR_ARROW_HITBOX);
        for (Arrow a : player.getArrows()) {
            Rectangle ab = a.getBounds();
            shape.rect(ab.x, ab.y, ab.width, ab.height);
        }

        // Enemy hitboxes
        shape.setColor(COLOR_ENEMY_HITBOX);
        for (Enemy e : world.getEnemies()) {
            Rectangle eb = e.getBounds();
            shape.rect(eb.x, eb.y, eb.width, eb.height);

            // Attack range circle when attacking
            if (e.getAttackTimer() > 0f) {
                shape.setColor(1f, 0.2f, 0.2f, 0.6f);
                float cx = eb.x + eb.width / 2f;
                float cy = eb.y + eb.height / 2f;
                shape.circle(cx, cy, Enemy.ATTACK_RANGE, 24);
                shape.setColor(COLOR_ENEMY_HITBOX);
            }
        }

        Gdx.gl.glLineWidth(1f);
        shape.end();
    }

    // ==================== INFO PANEL (screen-space HUD) ====================

    private void drawInfoPanel(ShapeRenderer shape, SpriteBatch batch, BitmapFont font,
                               OrthographicCamera camera, GameWorld world, MapManager mapManager) {
        // Panel position — top right of the viewport
        float panelW = 145f;
        float panelH = 130f;
        float panelX = camera.position.x + camera.viewportWidth / 2f - panelW - 6f;
        float panelY = camera.position.y + camera.viewportHeight / 2f - panelH - 20f;

        // Panel background
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COLOR_PANEL_BG);
        shape.rect(panelX, panelY, panelW, panelH);
        shape.end();

        // Panel border
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(COLOR_PANEL_BORDER);
        shape.rect(panelX, panelY, panelW, panelH);
        shape.end();

        // Text content
        batch.begin();
        float textScale = 0.38f;
        font.getData().setScale(textScale);

        float lineH = 10f;
        float tx = panelX + 6f;
        float ty = panelY + panelH - 8f;

        // Title
        font.setColor(COLOR_ACTIVE_TAB);
        font.draw(batch, "DEBUG INFO", tx, ty);
        ty -= lineH + 2f;

        // FPS
        font.setColor(COLOR_LABEL);
        font.draw(batch, "FPS:", tx, ty);
        font.setColor(COLOR_TEXT);
        font.draw(batch, String.valueOf(Gdx.graphics.getFramesPerSecond()), tx + 38f, ty);
        ty -= lineH;

        // Player position
        Player p = world.getPlayer();
        font.setColor(COLOR_LABEL);
        font.draw(batch, "Pos:", tx, ty);
        font.setColor(COLOR_TEXT);
        font.draw(batch, String.format("%.0f, %.0f", p.getPosition().x, p.getPosition().y), tx + 38f, ty);
        ty -= lineH;

        // Player health
        font.setColor(COLOR_LABEL);
        font.draw(batch, "HP:", tx, ty);
        font.setColor(p.getHealthRatio() > 0.5f ? COLOR_ACTIVE_TAB : new Color(1f, 0.3f, 0.3f, 1f));
        font.draw(batch, String.format("%.0f/%.0f", p.getHealth(), p.getMaxHealth()), tx + 38f, ty);
        ty -= lineH;

        // Stamina
        font.setColor(COLOR_LABEL);
        font.draw(batch, "STA:", tx, ty);
        font.setColor(COLOR_TEXT);
        font.draw(batch, String.format("%.0f/%.0f", p.getStamina(), p.getMaxStamina()), tx + 38f, ty);
        ty -= lineH;

        // Enemies
        int alive = 0;
        for (Enemy e : world.getEnemies()) if (e.isAlive()) alive++;
        font.setColor(COLOR_LABEL);
        font.draw(batch, "Enemies:", tx, ty);
        font.setColor(COLOR_TEXT);
        font.draw(batch, alive + "/" + world.getEnemies().size(), tx + 56f, ty);
        ty -= lineH;

        // Map dimensions
        if (mapManager != null) {
            font.setColor(COLOR_LABEL);
            font.draw(batch, "Map:", tx, ty);
            font.setColor(COLOR_TEXT);
            font.draw(batch, String.format("%.0fx%.0f", mapManager.getMapWidth(), mapManager.getMapHeight()), tx + 38f, ty);
            ty -= lineH;
        }

        // Collision rects count
        if (mapManager != null) {
            font.setColor(COLOR_LABEL);
            font.draw(batch, "Walls:", tx, ty);
            font.setColor(COLOR_TEXT);
            font.draw(batch, String.valueOf(mapManager.getCollisionRects().size()), tx + 45f, ty);
            ty -= lineH;
        }

        // Chests / Exits
        if (mapManager != null) {
            font.setColor(COLOR_LABEL);
            font.draw(batch, "Chests:", tx, ty);
            font.setColor(COLOR_CHEST);
            font.draw(batch, String.valueOf(mapManager.getChestRects().size()), tx + 50f, ty);
            font.setColor(COLOR_LABEL);
            font.draw(batch, "Exits:", tx + 68f, ty);
            font.setColor(COLOR_EXIT);
            font.draw(batch, String.valueOf(mapManager.getExitGateRects().size()), tx + 105f, ty);
            ty -= lineH;
        }

        // Arrows
        font.setColor(COLOR_LABEL);
        font.draw(batch, "Arrows:", tx, ty);
        font.setColor(COLOR_ARROW_HITBOX);
        font.draw(batch, String.valueOf(p.getArrows().size()), tx + 50f, ty);
        ty -= lineH;

        // Kills
        font.setColor(COLOR_LABEL);
        font.draw(batch, "Kills:", tx, ty);
        font.setColor(COLOR_TEXT);
        font.draw(batch, String.valueOf(p.getEnemiesKilled()), tx + 42f, ty);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // ==================== TAB BAR ====================

    private void drawTabBar(ShapeRenderer shape, SpriteBatch batch, BitmapFont font,
                            OrthographicCamera camera) {
        float barW = 195f;
        float barH = 14f;
        float barX = camera.position.x - camera.viewportWidth / 2f + 6f;
        float barY = camera.position.y + camera.viewportHeight / 2f - barH - 4f;

        // Bar background
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COLOR_PANEL_BG);
        shape.rect(barX, barY, barW, barH);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(COLOR_PANEL_BORDER);
        shape.rect(barX, barY, barW, barH);
        shape.end();

        // Tab labels
        batch.begin();
        float textScale = 0.35f;
        font.getData().setScale(textScale);

        float tx = barX + 4f;
        float ty = barY + barH - 3f;

        String[] tabs = { "[1]Hit", "[2]Bnd", "[3]Col", "[4]Inf" };
        boolean[] states = { showHitboxes, showBoundaries, showCollisions, showInfo };

        for (int i = 0; i < tabs.length; i++) {
            font.setColor(states[i] ? COLOR_ACTIVE_TAB : COLOR_INACTIVE_TAB);
            font.draw(batch, tabs[i], tx, ty);
            tx += 48f;
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }
}
