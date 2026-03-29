package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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
    }

    public void render(float offsetX, float offsetY) {

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.position.add(offsetX, offsetY, 0);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        shape.setProjectionMatrix(camera.combined);

        mapManager.render(camera);

        batch.begin();
        world.getPlayer().render(batch);
        world.getEnemy().render(batch);
        batch.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.RED);

        for (Rectangle r : mapManager.getCollisionRects()) {
            shape.rect(r.x, r.y, r.width, r.height);
        }

        shape.end();

        drawUI();

        camera.position.sub(offsetX, offsetY, 0);
        camera.update();
    }

    private void drawUI() {

        shape.begin(ShapeRenderer.ShapeType.Filled);

        float stamina = world.getPlayer().getStamina();
        float max = world.getPlayer().getMaxStamina();

        float x = camera.position.x - camera.viewportWidth / 2 + 20;
        float y = camera.position.y + camera.viewportHeight / 2 - 30;

        shape.setColor(Color.DARK_GRAY);
        shape.rect(x, y, 200, 20);

        shape.setColor(Color.GREEN);
        shape.rect(x, y, 200 * (stamina / max), 20);

        shape.end();
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
