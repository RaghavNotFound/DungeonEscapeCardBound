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

        // Render enemies behind the player
        for (Enemy enemy : world.getEnemies()) {
            if (world.getPlayer().getPos().y > enemy.getBounds().y) {
                enemy.render(batch);
            }
        }

        world.getPlayer().render(batch);

        // Render enemies in front of the player
        for (Enemy enemy : world.getEnemies()) {
            if (world.getPlayer().getPos().y <= enemy.getBounds().y) {
                enemy.render(batch);
            }
        }

        batch.end();

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
        drawEnemyHealthBars();
        drawCollisionDebug();
        drawUI();

        camera.position.sub(offsetX, offsetY, 0);
        camera.update();
    }

    private void drawTorches(SpriteBatch batch) {
        if (mapManager == null || torchFrames.length == 0) {
            return;
        }

        int frameIndex = ((int) (torchAnimTime / 0.1f)) % torchFrames.length;
        Texture frame = torchFrames[frameIndex];

        for (Rectangle torch : mapManager.getTorchRects()) {
            batch.draw(frame, torch.x, torch.y, torch.width, torch.height);
        }
    }

    private void drawCollisionDebug() {
        shape.begin(ShapeRenderer.ShapeType.Line);

        // Draw map boundaries
        shape.setColor(Color.RED);
        if (mapManager != null) {
            for (Rectangle r : mapManager.getCollisionRects()) {
                shape.rect(r.x, r.y, r.width, r.height);
            }
            if (mapManager.getCollisionPolygons() != null) {
                for (Polygon p : mapManager.getCollisionPolygons()) {
                    shape.polygon(p.getTransformedVertices());
                }
            }
        }

        // Draw Player boundaries
        shape.setColor(Color.GREEN);
        Rectangle pBounds = world.getPlayer().getBounds();
        shape.rect(pBounds.x, pBounds.y, pBounds.width, pBounds.height);

        // Draw Player Sword Hitbox if active
        if (world.getPlayer().canDealSwordDamage()) {
            shape.setColor(Color.YELLOW);
            Rectangle sBounds = world.getPlayer().getSwordHitbox();
            shape.rect(sBounds.x, sBounds.y, sBounds.width, sBounds.height);
        }

        // Draw Enemy boundaries
        shape.setColor(Color.MAGENTA);
        for (Enemy enemy : world.getEnemies()) {
            Rectangle eBounds = enemy.getBounds();
            shape.rect(eBounds.x, eBounds.y, eBounds.width, eBounds.height);
        }

        // Draw Arrows
        shape.setColor(Color.CYAN);
        for (Arrow arrow : world.getPlayer().getArrows()) {
            Rectangle aBounds = arrow.getBounds();
            shape.rect(aBounds.x, aBounds.y, aBounds.width, aBounds.height);
        }

        shape.end();
    }

    private void drawChestBorders() {
        if (mapManager == null) {
            return;
        }

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.GOLD);

        for (Rectangle r : mapManager.getChestRects()) {
            shape.rect(r.x, r.y, r.width, r.height);
        }

        shape.end();
    }

    private void drawEnemyHealthBars() {
        // This health bar was redundant. The primary, color-changing health bar
        // is now rendered directly within the Enemy.render() method.
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
                // Inner portal glow
                float pulse = 0.8f + 0.2f * MathUtils.sin(time * 4f);
                shape.setColor(0.1f, 0.9f, 0.3f, 0.4f * pulse);
                shape.rect(gate.x + pillarW, gate.y, gate.width - pillarW * 2, archHeight - 15f);

                // Portal sparkles
                for (int i = 0; i < 8; i++) {
                    float spX = gate.x + pillarW + MathUtils.random(0, gate.width - pillarW * 2);
                    float spY = gate.y + MathUtils.random(0, archHeight - 15f);
                    float alpha = 0.3f + 0.7f * MathUtils.sin(time * 5f + spX);
                    shape.setColor(0.4f, 1f, 0.6f, alpha);
                    shape.circle(spX, spY, 2f);
                }
            } else {
                // Red locked energy barrier
                float pulse = 0.6f + 0.2f * MathUtils.sin(time * 3f);
                shape.setColor(0.8f, 0.1f, 0.1f, 0.3f * pulse);
                shape.rect(gate.x + pillarW, gate.y, gate.width - pillarW * 2, archHeight - 15f);

                // Lock icon (simple cross in the middle of the doorway)
                shape.setColor(0.6f, 0.15f, 0.15f, 0.9f);
                shape.rect(cx - 3f, gate.y + archHeight / 2f - 10f, 6f, 20f);
                shape.rect(cx - 10f, gate.y + archHeight / 2f - 3f, 20f, 6f);
            }

            // Pillars and archway (dark stone color)
            shape.setColor(0.2f, 0.2f, 0.25f, 1f);
            // Left pillar
            shape.rect(gate.x, gate.y, pillarW, archHeight);
            // Right pillar
            shape.rect(gate.x + gate.width - pillarW, gate.y, pillarW, archHeight);
            // Top lintel
            shape.rect(gate.x - 5f, gate.y + archHeight - 15f, gate.width + 10f, 15f);

            shape.end();

            // Border outline for 3D depth
            shape.begin(ShapeRenderer.ShapeType.Line);
            if (unlocked) {
                float borderPulse = 0.7f + 0.3f * MathUtils.sin(time * 4f);
                shape.setColor(0.2f, 1f, 0.4f, borderPulse);
            } else {
                shape.setColor(0.1f, 0.1f, 0.15f, 1f);
            }
            shape.rect(gate.x, gate.y, pillarW, archHeight);
            shape.rect(gate.x + gate.width - pillarW, gate.y, pillarW, archHeight);
            shape.rect(gate.x - 5f, gate.y + archHeight - 15f, gate.width + 10f, 15f);
            shape.end();

            // "EXIT" text when unlocked and player is nearby
            if (unlocked) {
                Rectangle pBounds = world.getPlayer().getBounds();
                float dx = (pBounds.x + pBounds.width / 2f) - cx;
                float dy = (pBounds.y + pBounds.height / 2f) - cy;
                if (dx * dx + dy * dy < 150f * 150f) {
                    batch.begin();
                    font.getData().setScale(1.3f);
                    float bobY = MathUtils.sin(time * 4f) * 3f;
                    font.setColor(0.2f, 1f, 0.4f, 0.9f);
                    font.draw(batch, "EXIT", cx - 18f, gate.y + 130f + bobY);
                    font.getData().setScale(1f);
                    font.setColor(Color.WHITE);
                    batch.end();
                }
            }
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawInteractables() {
        if (world.getInteractables() == null) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        for (Interactable interactable : world.getInteractables()) {
            boolean inRange = interactable.isPlayerInRange(world.getPlayer());
            interactable.render(batch, shape, font, inRange);
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawUI() {
        Gdx.gl.glEnable(GL20.GL_BLEND);

        float health = world.getPlayer().getHealth();
        float maxHealth = world.getPlayer().getMaxHealth();
        float stamina = world.getPlayer().getStamina();
        float maxStamina = world.getPlayer().getMaxStamina();
        float cooldown = world.getPlayer().getShootCooldownPercent();

        float x = camera.position.x - camera.viewportWidth / 2f + 30f;
        float y = camera.position.y + camera.viewportHeight / 2f - 40f;

        shape.begin(ShapeRenderer.ShapeType.Filled);

        drawRoundedBar(
            shape, x, y, 220f, 20f,
            health / maxHealth,
            new Color(0.2f, 0.2f, 0.2f, 1f),
            new Color(1f, 0.25f, 0.25f, 1f)
        );

        drawRoundedBar(
            shape, x, y - 30f, 220f, 20f,
            stamina / maxStamina,
            new Color(0.2f, 0.2f, 0.2f, 1f),
            new Color(0.15f, 0.95f, 0.35f, 1f)
        );

        drawRoundedBar(
            shape, x, y - 60f, 220f, 20f,
            cooldown,
            new Color(0.2f, 0.2f, 0.2f, 1f),
            new Color(0.25f, 0.65f, 1f, 1f)
        );

        shape.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawRoundedBar(
        ShapeRenderer shape,
        float x,
        float y,
        float width,
        float height,
        float percent,
        Color bgColor,
        Color fillColor
    ) {
        float clampedPercent = MathUtils.clamp(percent, 0f, 1f);
        float radius = height / 2f;

        shape.setColor(bgColor);
        shape.rect(x, y, width - radius, height);
        shape.circle(x + width - radius, y + radius, radius);

        if (clampedPercent <= 0f) {
            return;
        }

        shape.setColor(fillColor);
        float fillWidth = width * clampedPercent;

        if (fillWidth <= width - radius) {
            shape.rect(x, y, fillWidth, height);
        } else {
            shape.rect(x, y, width - radius, height);
            shape.circle(x + width - radius, y + radius, radius);
        }
    }

    public void renderInventoryOverlay() {
        float panelW = Math.min(420f, camera.viewportWidth * 0.6f);
        float panelH = Math.min(280f, camera.viewportHeight * 0.55f);
        float x = camera.position.x - panelW * 0.5f;
        float y = camera.position.y - panelH * 0.5f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.08f, 0.08f, 0.1f, 0.9f);
        shape.rect(x, y, panelW, panelH);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.95f, 0.85f, 0.35f, 1f);
        shape.rect(x, y, panelW, panelH);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Inventory", x + 18f, y + panelH - 18f);
        font.draw(batch, "Torches: " + world.getPlayer().getTorchCount(), x + 18f, y + panelH - 58f);
        font.draw(batch, "Press E or ESC to close", x + 18f, y + 30f);
        batch.end();
    }

    public SpriteBatch getBatch() { return batch; }
    public ShapeRenderer getShape() { return shape; }
    public BitmapFont getFont() { return font; }

    public void dispose() {
        batch.dispose();
        shape.dispose();
        font.dispose();
        for (Texture torchFrame : torchFrames) {
            torchFrame.dispose();
        }
    }
}
