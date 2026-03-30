package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class GameRenderer
{
    private final GameWorld world;

    private final SpriteBatch batch;
    private final ShapeRenderer shape;
    private final BitmapFont font;

    private final OrthographicCamera camera;
    private final MapManager mapManager = new MapManager();

    public GameRenderer(GameWorld world, OrthographicCamera camera)
    {
        this.world = world;
        this.camera = camera;

        batch = new SpriteBatch();
        shape = new ShapeRenderer();
        font = new BitmapFont();
        mapManager.load("Maps/safeRoom.tmx");
    }

    // ===== MAIN RENDER (USED BY GAME + BLUR) =====
    public void render()
    {
        render(0, 0);
    }

    // ===== OFFSET RENDER (SHAKE SUPPORT) =====
    public void render(float offsetX, float offsetY)
    {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.position.add(offsetX, offsetY, 0);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        shape.setProjectionMatrix(camera.combined);

        // MAP
        mapManager.render(camera);

        // ===== DEPTH LAYERING =====
        batch.begin();

        if (world.getPlayer().getPos().y > world.getEnemy().getBounds().y)
        {
            world.getEnemy().render(batch);
        }

        world.getPlayer().render(batch);

        if (world.getPlayer().getPos().y <= world.getEnemy().getBounds().y)
        {
            world.getEnemy().render(batch);
        }

        batch.end();

        // ===== COLLISION DEBUG =====
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.RED);

        for (Rectangle r : mapManager.getCollisionRects())
        {
            shape.rect(r.x, r.y, r.width, r.height);
        }

        shape.end();

        // ===== UI =====
        drawUI();

        camera.position.sub(offsetX, offsetY, 0);
        camera.update();
    }

    // ===== UI SYSTEM =====
    private void drawUI()
    {
        Gdx.gl.glEnable(GL20.GL_BLEND);

        shape.begin(ShapeRenderer.ShapeType.Filled);

        float stamina = world.getPlayer().getStamina();
        float maxStamina = world.getPlayer().getMaxStamina();
        float cooldown = world.getPlayer().getShootCooldownPercent();

        float barWidth = 220f;
        float barHeight = 20f;

        float x = camera.position.x - camera.viewportWidth / 2 + 30;
        float y = camera.position.y + camera.viewportHeight / 2 - 40;

        // STAMINA
        drawRoundedBar(
            shape,
            x, y,
            barWidth, barHeight,
            stamina / maxStamina,
            new Color(0.2f,0.2f,0.2f,1),
            new Color(0,1,0,1)
        );

        // COOLDOWN
        drawRoundedBar(
            shape,
            x, y - 30,
            barWidth, barHeight,
            cooldown,
            new Color(0.2f,0.2f,0.2f,1),
            new Color(0,0.4f,1,1)
        );

        shape.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ===== ROUNDED BAR =====
    private void drawRoundedBar(ShapeRenderer shape, float x,float y, float width,float height, float percent, Color bgColor, Color fillColor)
    {
        float radius = height / 2f;

        // BACKGROUND
        shape.setColor(bgColor);
        shape.rect(x + radius, y, width - 2*radius, height);
        shape.circle(x + radius, y + radius, radius);
        shape.circle(x + width - radius, y + radius, radius);

        if (percent <= 0f) return;

        shape.setColor(fillColor);

        float fill = width * percent;

        if (fill < radius)
        {
            shape.circle(x + radius, y + radius, fill);
            return;
        }

        float rectWidth = fill - radius;

        shape.rect(x + radius, y, rectWidth, height);
        shape.circle(x + radius, y + radius, radius);

        if (percent >= 0.99f)
        {
            shape.circle(x + width - radius, y + radius, radius);
        }
    }

    public SpriteBatch getBatch() { return batch; }
    public ShapeRenderer getShape() { return shape; }
    public BitmapFont getFont() { return font; }

    public void dispose()
    {
        batch.dispose();
        shape.dispose();
        font.dispose();
    }
}
