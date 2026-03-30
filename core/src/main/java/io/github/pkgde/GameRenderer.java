package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

public class GameRenderer {

    private final GameWorld world;
    private final SpriteBatch batch;
    private final ShapeRenderer shape;
    private final BitmapFont font;

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
    }

    public void render() {
        render(0f, 0f);
    }

    public void render(float offsetX, float offsetY) {
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

        if (world.getPlayer().getPos().y > world.getEnemy().getBounds().y) {
            world.getEnemy().render(batch);
        }

        world.getPlayer().render(batch);

        if (world.getPlayer().getPos().y <= world.getEnemy().getBounds().y) {
            world.getEnemy().render(batch);
        }

        batch.end();

        drawCollisionDebug();
        drawUI();

        camera.position.sub(offsetX, offsetY, 0);
        camera.update();
    }

    private void drawCollisionDebug() {
        if (mapManager == null) {
            return;
        }

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.RED);
        for (Rectangle r : mapManager.getCollisionRects()) {
            shape.rect(r.x, r.y, r.width, r.height);
        }
        shape.end();
    }

    private void drawUI() {
        Gdx.gl.glEnable(GL20.GL_BLEND);

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
            stamina / maxStamina,
            new Color(0.2f, 0.2f, 0.2f, 1f),
            new Color(0.15f, 0.95f, 0.35f, 1f)
        );

        drawRoundedBar(
            shape,
            x,
            y - 30f,
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

    public void dispose() {
        batch.dispose();
        shape.dispose();
        font.dispose();
    }
}
