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

public class DebugOverlay {

    private boolean visible = false;

    private boolean showHitboxes = true;
    private boolean showBoundaries = true;
    private boolean showCollisions = true;
    private boolean showInfo = true;

    private boolean tpMode = false;
    private boolean spawnMode = false;

    private static final Color COLOR_PLAYER_HITBOX = new Color(0.2f, 1f, 0.3f, 0.85f);
    private static final Color COLOR_ENEMY_HITBOX = new Color(1f, 0.2f, 0.8f, 0.85f);
    private static final Color COLOR_SWORD_HITBOX = new Color(1f, 1f, 0.2f, 0.85f);
    private static final Color COLOR_ARROW_HITBOX = new Color(0.2f, 0.8f, 1f, 0.85f);
    private static final Color COLOR_COLLISION = new Color(1f, 0.15f, 0.15f, 0.5f);
    private static final Color COLOR_COLLISION_OUTLINE = new Color(1f, 0.3f, 0.3f, 0.8f);
    private static final Color COLOR_BOUNDARY = new Color(1f, 0.6f, 0f, 0.9f);
    private static final Color COLOR_CHEST = new Color(1f, 0.85f, 0.1f, 0.8f);
    private static final Color COLOR_PLAYER_SPAWN = new Color(0.3f, 0.6f, 1f, 0.9f);
    private static final Color COLOR_ENEMY_SPAWN = new Color(1f, 0.35f, 0.35f, 0.9f);
    private static final Color COLOR_LAVA = new Color(1f, 0.4f, 0f, 0.6f);
    private static final Color COLOR_PANEL_BG = new Color(0.05f, 0.05f, 0.12f, 0.88f);
    private static final Color COLOR_PANEL_BORDER = new Color(0.3f, 0.3f, 0.5f, 0.9f);
    private static final Color COLOR_ACTIVE_TAB = new Color(0.25f, 0.85f, 0.55f, 1f);
    private static final Color COLOR_INACTIVE_TAB = new Color(0.55f, 0.55f, 0.55f, 1f);
    private static final Color COLOR_TEXT = new Color(0.9f, 0.92f, 0.95f, 1f);
    private static final Color COLOR_LABEL = new Color(0.6f, 0.65f, 0.7f, 1f);

    private final GlyphLayout layout = new GlyphLayout();

    public boolean isVisible() { return visible; }

    public boolean handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            visible = !visible;
            return true;
        }
        if (!visible) return false;

        boolean consumed = false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) { showHitboxes = !showHitboxes; consumed = true; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) { showBoundaries = !showBoundaries; consumed = true; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) { showCollisions = !showCollisions; consumed = true; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) { showInfo = !showInfo; consumed = true; }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_8)) { tpMode = !tpMode; consumed = true; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_9)) { spawnMode = !spawnMode; consumed = true; }

        return consumed;
    }

    public void handleCheats(OrthographicCamera camera, GameWorld world, MapManager mapManager) {
        if (!visible) return;

        // Skip Level Cheat: '0'
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0)) {
            for (Enemy e : world.getEnemies()) {
                e.setHealth(0);
            }
            if (!mapManager.getExitGateRects().isEmpty()) {
                Rectangle exit = mapManager.getExitGateRects().get(0);
                world.getPlayer().setPosition(exit.x, exit.y);
            }
        }

        // INSTANT BOSS SPAWN CHEAT: 'B'
        // Lets you test the boss fight anywhere, anytime without having to walk to room 4!
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            Enemy boss = new Enemy();
            boss.setBoundaries(world.getBoundaries());
            boss.setWorldBounds(0f, 0f, mapManager.getMapWidth(), mapManager.getMapHeight());
            boss.setPosition(world.getPlayer().getPosition().x + 80f, world.getPlayer().getPosition().y);
            boss.setBoss(true);
            boss.forceChase(Float.MAX_VALUE);
            world.getEnemies().add(boss);
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            com.badlogic.gdx.math.Vector3 mousePos = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mousePos);

            if (tpMode) {
                world.getPlayer().setPosition(mousePos.x, mousePos.y);
                tpMode = false;
            } else if (spawnMode) {
                Enemy e = new Enemy();
                e.setBoundaries(world.getBoundaries());
                e.setWorldBounds(0f, 0f, mapManager.getMapWidth(), mapManager.getMapHeight());
                e.setPosition(mousePos.x, mousePos.y);
                world.getEnemies().add(e);
                spawnMode = false;
            }
        }
    }

    public void render(ShapeRenderer shape, SpriteBatch batch, BitmapFont font,
                       OrthographicCamera camera, GameWorld world, MapManager mapManager) {
        if (!visible) return;

        shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        if (showCollisions) {
            drawCollisionRects(shape, mapManager);
            drawChestHitboxes(shape, mapManager);
            drawSpawnMarkers(shape, mapManager);
            drawLavaRects(shape, mapManager);
        }
        if (showBoundaries) drawMapBorder(shape, mapManager);
        if (showHitboxes) drawEntityHitboxes(shape, world);

        if (showInfo) drawInfoPanel(shape, batch, font, camera, world, mapManager);

        drawTabBar(shape, batch, font, camera);

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawCollisionRects(ShapeRenderer shape, MapManager mapManager) {
        if (mapManager == null) return;
        ArrayList<Rectangle> rects = mapManager.getCollisionRects();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COLOR_COLLISION);
        for (Rectangle r : rects) shape.rect(r.x, r.y, r.width, r.height);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(COLOR_COLLISION_OUTLINE);
        for (Rectangle r : rects) shape.rect(r.x, r.y, r.width, r.height);
        shape.end();
    }

    private void drawChestHitboxes(ShapeRenderer shape, MapManager mapManager) {
        if (mapManager == null) return;
        ArrayList<Rectangle> chests = mapManager.getChestRects();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COLOR_CHEST.r, COLOR_CHEST.g, COLOR_CHEST.b, 0.25f);
        for (Rectangle r : chests) shape.rect(r.x, r.y, r.width, r.height);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shape.setColor(COLOR_CHEST);
        for (Rectangle r : chests) {
            shape.rect(r.x, r.y, r.width, r.height);
            shape.line(r.x, r.y, r.x + r.width, r.y + r.height);
            shape.line(r.x + r.width, r.y, r.x, r.y + r.height);
        }
        Gdx.gl.glLineWidth(1f);
        shape.end();
    }

    private void drawLavaRects(ShapeRenderer shape, MapManager mapManager) {
        if (mapManager == null) return;
        ArrayList<Rectangle> lavas = mapManager.getLavaRects();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COLOR_LAVA.r, COLOR_LAVA.g, COLOR_LAVA.b, 0.3f);
        for (Rectangle r : lavas) shape.rect(r.x, r.y, r.width, r.height);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shape.setColor(COLOR_LAVA);
        for (Rectangle r : lavas) {
            shape.rect(r.x, r.y, r.width, r.height);
            shape.line(r.x, r.y, r.x + r.width, r.y + r.height);
            shape.line(r.x + r.width, r.y, r.x, r.y + r.height);
        }
        Gdx.gl.glLineWidth(1f);
        shape.end();
    }

    private void drawSpawnMarkers(ShapeRenderer shape, MapManager mapManager) {
        if (mapManager == null) return;

        Vector2 ps = mapManager.getPlayerSpawn();
        if (ps.x != 0 || ps.y != 0) {
            float size = 6f;
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(COLOR_PLAYER_SPAWN);
            shape.triangle(ps.x, ps.y - size, ps.x - size, ps.y, ps.x, ps.y + size);
            shape.triangle(ps.x, ps.y - size, ps.x + size, ps.y, ps.x, ps.y + size);
            shape.end();

            shape.begin(ShapeRenderer.ShapeType.Line);
            Gdx.gl.glLineWidth(2f);
            shape.setColor(COLOR_PLAYER_SPAWN);
            shape.circle(ps.x, ps.y, size + 3f, 24);
            Gdx.gl.glLineWidth(1f);
            shape.end();
        }

        for (Vector2 spawn : mapManager.getEnemySpawns()) {
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

    private void drawMapBorder(ShapeRenderer shape, MapManager mapManager) {
        if (mapManager == null) return;
        float w = mapManager.getMapWidth(), h = mapManager.getMapHeight();

        shape.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(3f);
        shape.setColor(COLOR_BOUNDARY);
        shape.rect(0, 0, w, h);

        float markSize = 12f;
        shape.line(0, 0, markSize, 0); shape.line(0, 0, 0, markSize);
        shape.line(w, 0, w - markSize, 0); shape.line(w, 0, w, markSize);
        shape.line(0, h, markSize, h); shape.line(0, h, 0, h - markSize);
        shape.line(w, h, w - markSize, h); shape.line(w, h, w, h - markSize);

        Gdx.gl.glLineWidth(1f);
        shape.end();
    }

    private void drawEntityHitboxes(ShapeRenderer shape, GameWorld world) {
        Player player = world.getPlayer();

        shape.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);

        shape.setColor(COLOR_PLAYER_HITBOX);
        Rectangle pb = player.getBounds();
        shape.rect(pb.x, pb.y, pb.width, pb.height);

        if (player.canDealSwordDamage()) {
            shape.setColor(COLOR_SWORD_HITBOX);
            Rectangle sb = player.getSwordHitbox();
            shape.rect(sb.x, sb.y, sb.width, sb.height);
        }

        shape.setColor(COLOR_ARROW_HITBOX);
        for (Arrow a : player.getArrows()) {
            Rectangle ab = a.getBounds();
            shape.rect(ab.x, ab.y, ab.width, ab.height);
        }

        shape.setColor(COLOR_ENEMY_HITBOX);
        for (Enemy e : world.getEnemies()) {
            Rectangle eb = e.getBounds();
            shape.rect(eb.x, eb.y, eb.width, eb.height);

            if (e.getAttackTimer() > 0f) {
                shape.setColor(1f, 0.2f, 0.2f, 0.6f);
                float cx = eb.x + eb.width / 2f, cy = eb.y + eb.height / 2f;
                shape.circle(cx, cy, Enemy.ATTACK_RANGE, 24);
                shape.setColor(COLOR_ENEMY_HITBOX);
            }
        }

        Gdx.gl.glLineWidth(1f);
        shape.end();
    }

    private void drawInfoPanel(ShapeRenderer shape, SpriteBatch batch, BitmapFont font,
                               OrthographicCamera camera, GameWorld world, MapManager mapManager) {

        float uiScale = camera.viewportWidth / 400f;

        float panelW = 165f * uiScale;
        float panelH = 135f * uiScale;
        float panelX = camera.position.x + camera.viewportWidth / 2f - panelW - 5f * uiScale;
        float panelY = camera.position.y + camera.viewportHeight / 2f - panelH - 5f * uiScale;

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COLOR_PANEL_BG);
        shape.rect(panelX, panelY, panelW, panelH);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(COLOR_PANEL_BORDER);
        shape.rect(panelX, panelY, panelW, panelH);
        shape.end();

        batch.begin();
        font.getData().setScale(0.35f * uiScale);

        float lineH = 11f * uiScale;
        float tx = panelX + 8f * uiScale;
        float ty = panelY + panelH - 8f * uiScale;
        float colOffset = 80f * uiScale;

        font.setColor(COLOR_ACTIVE_TAB);
        font.draw(batch, "DEBUG INFO", tx, ty);
        ty -= lineH + 4f * uiScale;

        Player p = world.getPlayer();
        int alive = 0;
        for (Enemy e : world.getEnemies()) if (e.isAlive()) alive++;

        drawInfoLine(batch, font, "FPS:", String.valueOf(Gdx.graphics.getFramesPerSecond()), tx, ty, colOffset, COLOR_LABEL, COLOR_TEXT); ty -= lineH;
        drawInfoLine(batch, font, "Pos:", String.format("%.0f, %.0f", p.getPosition().x, p.getPosition().y), tx, ty, colOffset, COLOR_LABEL, COLOR_TEXT); ty -= lineH;

        Color hpColor = p.getHealthRatio() > 0.5f ? COLOR_ACTIVE_TAB : new Color(1f, 0.3f, 0.3f, 1f);
        drawInfoLine(batch, font, "HP:", String.format("%.0f / %.0f", p.getHealth(), p.getMaxHealth()), tx, ty, colOffset, COLOR_LABEL, hpColor); ty -= lineH;

        drawInfoLine(batch, font, "STA:", String.format("%.0f / %.0f", p.getStamina(), p.getMaxStamina()), tx, ty, colOffset, COLOR_LABEL, COLOR_TEXT); ty -= lineH;
        drawInfoLine(batch, font, "Enemies:", alive + " / " + world.getEnemies().size(), tx, ty, colOffset, COLOR_LABEL, COLOR_TEXT); ty -= lineH;

        if (mapManager != null) {
            drawInfoLine(batch, font, "Map Size:", String.format("%.0fx%.0f", mapManager.getMapWidth(), mapManager.getMapHeight()), tx, ty, colOffset, COLOR_LABEL, COLOR_TEXT); ty -= lineH;
            drawInfoLine(batch, font, "Walls:", String.valueOf(mapManager.getCollisionRects().size()), tx, ty, colOffset, COLOR_LABEL, COLOR_TEXT); ty -= lineH;
            drawInfoLine(batch, font, "Chests:", String.valueOf(mapManager.getChestRects().size()), tx, ty, colOffset, COLOR_LABEL, COLOR_CHEST); ty -= lineH;
        }

        drawInfoLine(batch, font, "Arrows:", String.valueOf(p.getArrows().size()), tx, ty, colOffset, COLOR_LABEL, COLOR_ARROW_HITBOX); ty -= lineH;
        drawInfoLine(batch, font, "Kills:", String.valueOf(p.getEnemiesKilled()), tx, ty, colOffset, COLOR_LABEL, COLOR_TEXT);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void drawInfoLine(SpriteBatch batch, BitmapFont font, String label, String value, float x, float y, float colOffset, Color labelCol, Color valCol) {
        font.setColor(labelCol);
        font.draw(batch, label, x, y);
        font.setColor(valCol);
        font.draw(batch, value, x + colOffset, y);
    }

    private void drawTabBar(ShapeRenderer shape, SpriteBatch batch, BitmapFont font, OrthographicCamera camera) {
        float uiScale = camera.viewportWidth / 400f;
        font.getData().setScale(0.35f * uiScale);

        // Added the [B] Boss cheat text to the bottom tab bar!
        String[] tabs = { "[1] Hit", "[2] Bnd", "[3] Col", "[4] Inf", "[8] TP", "[9] Spwn", "[0] Skip", "[B] Boss" };
        boolean[] states = { showHitboxes, showBoundaries, showCollisions, showInfo, tpMode, spawnMode, false, false };

        float tabPadding = 24f * uiScale;

        float totalTabW = 0;
        for (String tab : tabs) {
            layout.setText(font, tab);
            totalTabW += layout.width + tabPadding;
        }

        float barW = totalTabW + 4f * uiScale;
        float barH = 14f * uiScale;
        float barX = camera.position.x - camera.viewportWidth / 2f + 5f * uiScale;
        float barY = camera.position.y + camera.viewportHeight / 2f - barH - 5f * uiScale;

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COLOR_PANEL_BG);
        shape.rect(barX, barY, barW, barH);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(COLOR_PANEL_BORDER);
        shape.rect(barX, barY, barW, barH);
        shape.end();

        batch.begin();
        float tx = barX + 6f * uiScale;
        float ty = barY + barH - 3f * uiScale;

        for (int i = 0; i < tabs.length; i++) {
            font.setColor(states[i] ? COLOR_ACTIVE_TAB : COLOR_INACTIVE_TAB);
            layout.setText(font, tabs[i]);
            font.draw(batch, layout, tx, ty);

            tx += layout.width + tabPadding;
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }
}
