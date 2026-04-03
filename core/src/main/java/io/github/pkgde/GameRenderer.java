package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;

public class GameRenderer {

    private final GameWorld world;
    private final SpriteBatch batch;
    private final ShapeRenderer shape;
    private final BitmapFont font;
    private final Texture[] torchFrames;
    private float torchAnimTime;

    private final OrthographicCamera camera;
    private final MapManager mapManager;

    public GameRenderer(GameWorld world, OrthographicCamera camera, MapManager mapManager) {
        this.world = world;
        this.camera = camera;
        this.mapManager = mapManager;

        batch = new SpriteBatch();
        shape = new ShapeRenderer();
        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        torchFrames = new Texture[7];
        for (int i = 0; i < torchFrames.length; i++) {
            torchFrames[i] = new Texture("Objects/torch/torch_" + (i + 1) + ".png");
            torchFrames[i].setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
    }

    public void render() {
        render(0f, 0f);
    }

    public void render(float offsetX, float offsetY) {
        torchAnimTime += Gdx.graphics.getDeltaTime();

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.position.add(offsetX, offsetY, 0);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        shape.setProjectionMatrix(camera.combined);

        if (mapManager != null) {
            mapManager.render(camera);
        }

        batch.begin();
        drawTorches(batch);

        // Z-Sorting: Render enemies behind the player
        for (Enemy enemy : world.getEnemies()) {
            if (world.getPlayer().getBounds().y > enemy.getBounds().y) {
                enemy.render(batch);
            }
        }

        world.getPlayer().render(batch);

        // Z-Sorting: Render enemies in front of the player
        for (Enemy enemy : world.getEnemies()) {
            if (world.getPlayer().getBounds().y <= enemy.getBounds().y) {
                enemy.render(batch);
            }
        }
        batch.end();

        // Render Loot Drops
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        if (world.getLootDrops() != null) {
            for (LootDrop drop : world.getLootDrops()) {
                drop.render(batch, shape);
            }
        }
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        drawExitGate();
        drawInteractables();
        drawEnemyUI();
        drawCollisionDebug();
        drawChestBorders();
        drawUI();

        camera.position.sub(offsetX, offsetY, 0);
        camera.update();
    }

    private void drawTorches(SpriteBatch batch) {
        if (mapManager == null || torchFrames.length == 0) return;

        int frameIndex = ((int) (torchAnimTime / 0.1f)) % torchFrames.length;
        Texture frame = torchFrames[frameIndex];

        for (Rectangle torch : mapManager.getTorchRects()) {
            batch.draw(frame, torch.x, torch.y, torch.width, torch.height);
        }
    }

    private void drawCollisionDebug() {
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.RED);
        if (mapManager != null) {
            for (Rectangle r : mapManager.getCollisionRects()) shape.rect(r.x, r.y, r.width, r.height);
            for (Polygon p : mapManager.getCollisionPolygons()) shape.polygon(p.getTransformedVertices());
        }

        shape.setColor(Color.GREEN);
        Rectangle pBounds = world.getPlayer().getBounds();
        shape.rect(pBounds.x, pBounds.y, pBounds.width, pBounds.height);

        if (world.getPlayer().canDealSwordDamage()) {
            shape.setColor(Color.YELLOW);
            Rectangle sBounds = world.getPlayer().getSwordHitbox();
            shape.rect(sBounds.x, sBounds.y, sBounds.width, sBounds.height);
        }

        shape.setColor(Color.MAGENTA);
        for (Enemy enemy : world.getEnemies()) {
            Rectangle eBounds = enemy.getBounds();
            shape.rect(eBounds.x, eBounds.y, eBounds.width, eBounds.height);
        }
        shape.end();
    }

    private void drawEnemyUI() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);

        for (Enemy enemy : world.getEnemies()) {
            if (!enemy.isAlive()) continue;

            // --- Health Bar ---
            Rectangle eBounds = enemy.getBounds();
            float ratio = MathUtils.clamp(enemy.getHealthRatio(), 0f, 1f);
            float barWidth = eBounds.width * 1.5f;
            float barHeight = 6f;
            float x = eBounds.x + (eBounds.width - barWidth) * 0.5f;
            float y = eBounds.y + eBounds.height + 12f;

            // Background
            shape.setColor(0f, 0f, 0f, 0.8f);
            shape.rect(x - 1f, y - 1f, barWidth + 2f, barHeight + 2f);

            // Health Fill
            if (ratio > 0.6f) shape.setColor(0.2f, 0.9f, 0.2f, 1f);
            else if (ratio > 0.3f) shape.setColor(0.95f, 0.85f, 0.2f, 1f);
            else shape.setColor(0.9f, 0.2f, 0.2f, 1f);

            shape.rect(x, y, barWidth * ratio, barHeight);

            // --- Attack Telegraph ---
            if (enemy.getAttackTimer() > 0f) {
                float progress = 1f - (enemy.getAttackTimer() / enemy.getAttackAnimDuration());
                float cx = eBounds.x + eBounds.width / 2f, cy = eBounds.y + eBounds.height / 2f;
                shape.setColor(1f, 0.1f, 0.1f, 0.15f);
                shape.circle(cx, cy, Enemy.ATTACK_RANGE);
                shape.setColor(1f, 0f, 0f, 0.4f * progress);
                shape.circle(cx, cy, Enemy.ATTACK_RANGE * progress);
            }
        }
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawExitGate() {
        if (world.getMapManager() == null) return;
        java.util.ArrayList<Rectangle> gates = world.getMapManager().getExitGateRects();
        if (gates.isEmpty()) return;

        boolean unlocked = world.isExitGateUnlocked();
        float time = (float) (System.nanoTime() / 1_000_000_000.0);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        for (Rectangle gate : gates) {
            float cx = gate.x + gate.width / 2f;
            float cy = gate.y + gate.height / 2f;
            float archHeight = 100f;
            float pillarW = 15f;

            shape.begin(ShapeRenderer.ShapeType.Filled);
            if (unlocked) {
                float pulse = 0.8f + 0.2f * MathUtils.sin(time * 4f);
                shape.setColor(0.1f, 0.9f, 0.3f, 0.4f * pulse); // Portal energy
                shape.rect(gate.x + pillarW, gate.y, gate.width - pillarW * 2, archHeight - 15f);
                shape.setColor(0.2f, 1f, 0.4f, 0.15f + 0.1f * MathUtils.sin(time * 6f));
                shape.circle(cx, cy, gate.width * 0.4f); // Core
            } else {
                shape.setColor(0.8f, 0.1f, 0.1f, 0.3f); // Red Barrier
                shape.rect(gate.x + pillarW, gate.y, gate.width - pillarW * 2, archHeight - 15f);
                shape.setColor(0.6f, 0.15f, 0.15f, 0.9f); // Lock Icon
                shape.rect(cx - 3f, cy - 10f, 6f, 20f);
                shape.rect(cx - 10f, cy - 3f, 20f, 6f);
            }
            shape.setColor(0.2f, 0.2f, 0.25f, 1f); // Stone Structure
            shape.rect(gate.x, gate.y, pillarW, archHeight);
            shape.rect(gate.x + gate.width - pillarW, gate.y, pillarW, archHeight);
            shape.rect(gate.x - 5f, gate.y + archHeight - 15f, gate.width + 10f, 15f);
            shape.end();

            if (unlocked) {
                batch.begin();
                font.setColor(0.2f, 1f, 0.4f, 1f);
                font.draw(batch, "EXIT", cx - 18f, gate.y + archHeight + 20f);
                batch.end();
            }
        }
    }

    private void drawInteractables() {
        if (world.getInteractables() == null) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        for (Interactable interactable : world.getInteractables()) {
            interactable.render(batch, shape, font, interactable.isPlayerInRange(world.getPlayer()));
        }
    }

    private void drawUI() {
        float x = camera.position.x - camera.viewportWidth / 2f + 30f;
        float y = camera.position.y + camera.viewportHeight / 2f - 40f;

        shape.begin(ShapeRenderer.ShapeType.Filled);

        // Use new rounded status bars
        drawRoundedStatusBar(shape, x, y, 220f, 20f, world.getPlayer().getHealthRatio(), new Color(0.2f, 0.2f, 0.2f, 1f), new Color(1f, 0.25f, 0.25f, 1f));
        drawRoundedStatusBar(shape, x, y - 30f, 220f, 20f, world.getPlayer().getStamina() / world.getPlayer().getMaxStamina(), new Color(0.2f, 0.2f, 0.2f, 1f), new Color(0.15f, 0.95f, 0.35f, 1f));
        drawRoundedStatusBar(shape, x, y - 60f, 220f, 20f, world.getPlayer().getShootCooldownPercent(), new Color(0.2f, 0.2f, 0.2f, 1f), new Color(0.25f, 0.65f, 1f, 1f));

        shape.end();
    }

    private void drawRoundedStatusBar(ShapeRenderer shape, float x, float y, float width, float height, float percent, Color bgColor, Color fillColor) {
        float clamped = MathUtils.clamp(percent, 0f, 1f);
        float radius = height / 2f;

        // 1. Draw background
        shape.setColor(bgColor);
        drawFullyRoundedRect(shape, x, y, width, height);

        // 2. Draw fill
        if (clamped > 0f) {
            shape.setColor(fillColor);
            float fillW = width * clamped;
            float clipX = x + fillW;
            
            if (clamped >= 1.0f) { 
                drawFullyRoundedRect(shape, x, y, width, height);
            } else {
                // Left Circle
                drawLeftClippedCircle(shape, x + radius, y + radius, radius, clipX);
                
                // Middle Rectangle
                float rectX = x + radius;
                if (clipX > rectX) {
                    float rectDrawW = Math.min(width - 2 * radius, clipX - rectX);
                    if (rectDrawW > 0) {
                        shape.rect(rectX, y, rectDrawW, height);
                    }
                }
                
                // Right Circle
                float cx2 = x + width - radius;
                if (clipX > cx2 - radius) {
                    drawLeftClippedCircle(shape, cx2, y + radius, radius, clipX);
                }
            }
        }
    }

    private void drawLeftClippedCircle(ShapeRenderer shape, float cx, float cy, float r, float clipX) {
        if (clipX >= cx + r) {
            shape.circle(cx, cy, r);
            return;
        }
        if (clipX <= cx - r) return;

        float dx = (clipX - cx) / r;
        dx = MathUtils.clamp(dx, -1f, 1f);
        float intersectAngle = (float) Math.acos(dx) * MathUtils.radiansToDegrees;
        
        float startAngle = intersectAngle;
        float endAngle = 360f - intersectAngle;
        
        int segments = 32;
        float step = (endAngle - startAngle) / segments;

        for (int i = 0; i < segments; i++) {
            float theta1 = (startAngle + i * step) * MathUtils.degreesToRadians;
            float theta2 = (startAngle + (i + 1) * step) * MathUtils.degreesToRadians;

            float p1x = cx + r * MathUtils.cos(theta1);
            float p1y = cy + r * MathUtils.sin(theta1);
            float p2x = cx + r * MathUtils.cos(theta2);
            float p2y = cy + r * MathUtils.sin(theta2);

            shape.triangle(clipX, cy, p1x, p1y, p2x, p2y);
        }
    }

    private void drawFullyRoundedRect(ShapeRenderer shape, float x, float y, float width, float height) {
        float radius = height / 2f;
        shape.rect(x + radius, y, width - height, height);
        shape.circle(x + radius, y + radius, radius);
        shape.circle(x + width - radius, y + radius, radius);
    }

    private void drawChestBorders() {
        if (mapManager == null) return;
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.GOLD);
        for (Rectangle r : mapManager.getChestRects()) shape.rect(r.x, r.y, r.width, r.height);
        shape.end();
    }

    public void dispose() {
        batch.dispose();
        shape.dispose();
        font.dispose();
        for (Texture t : torchFrames) t.dispose();
    }

    public SpriteBatch getBatch() { return batch; }
    public ShapeRenderer getShape() { return shape; }
    public BitmapFont getFont() { return font; }
}
