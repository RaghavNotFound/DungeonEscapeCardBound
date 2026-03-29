package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class GameRenderer {

    private GameWorld world;

    private SpriteBatch batch;
    private ShapeRenderer shape;
    private BitmapFont font;

    private OrthographicCamera camera;
    private MapManager mapManager;

    public GameRenderer(GameWorld world, OrthographicCamera camera, MapManager mapManager) {
        this.world = world;
        this.camera = camera;
        this.mapManager = mapManager;

        batch = new SpriteBatch();
        shape = new ShapeRenderer();
        font = new BitmapFont();
    }

    public void render(float offsetX, float offsetY) {

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // APPLY SHAKE
        camera.position.add(offsetX, offsetY, 0);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        shape.setProjectionMatrix(camera.combined);

        // MAP
        mapManager.render(camera);

        // ENTITIES
        batch.begin();
        world.getPlayer().render(batch);
        world.getEnemy().render(batch);
        batch.end();

        // DEBUG
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(1, 0, 0, 1);

        for (Rectangle rect : mapManager.getCollisionRects()) {
            shape.rect(rect.x, rect.y, rect.width, rect.height);
        }

        shape.end();

        // RESET CAMERA
        camera.position.sub(offsetX, offsetY, 0);
        camera.update();
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
