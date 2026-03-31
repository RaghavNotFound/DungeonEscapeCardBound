package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import io.github.pkgde.Enemy;

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

        for (Enemy enemy : world.getEnemies()) {
            if (world.getPlayer().getPos().y > enemy.getBounds().y) {
                enemy.render(batch);
            }
        }

        world.getPlayer().render(batch);

        for (Enemy enemy : world.getEnemies()) {
            if (world.getPlayer().getPos().y <= enemy.getBounds().y) {
                enemy.render(batch);
            }
        }

        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (LootDrop drop : world.getLootDrops()) {
            drop.render(batch, shape);
        }
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

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
                for (com.badlogic.gdx.math.Polygon p : mapManager.getCollisionPolygons()) {
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
            shape,
            x,
            y,
            220f,
            20f,
            health / maxHealth,
            new Color(0.2f, 0.2f, 0.2f, 1f),
            new Color(1f, 0.25f, 0.25f, 1f)
        );


        drawRoundedBar(
            shape,
            x,
            y - 30f,
            220f,
            20f,
            stamina / maxStamina,
            new Color(0.2f, 0.2f, 0.2f, 1f),
            new Color(0.15f, 0.95f, 0.35f, 1f)
        );

        drawRoundedBar(
            shape,
            x,
            y - 60f,
            220f,
            20f,
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

    public SpriteBatch getBatch() { return batch; }
    public ShapeRenderer getShape() { return shape; }
    public BitmapFont getFont() { return font; }

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

    public void dispose() {
        batch.dispose();
        shape.dispose();
        font.dispose();
        for (Texture torchFrame : torchFrames) {
            torchFrame.dispose();
        }
    }
}
