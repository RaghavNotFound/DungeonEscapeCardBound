package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class HomeScreen implements Screen {

    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;

    private OrthographicCamera camera;
    private Viewport viewport;

    private final float WORLD_WIDTH = 800;
    private final float WORLD_HEIGHT = 600;

    private float width = 250, height = 70;

    private float playX = 275, playY = 320;
    private float settingsX = 275, settingsY = 220;
    private float exitX = 275, exitY = 120;

    // 🔥 NEW: shared overlay
    private SettingsOverlay settings;

    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();

        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);

        viewport.apply(true);
        camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);
        camera.update();

        settings = new SettingsOverlay();
    }

    @Override
    public void render(float delta) {

        viewport.apply();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapeRenderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        // ===== SETTINGS ACTIVE =====
        if (settings.isActive()) {
            settings.handleInput(viewport);
        } else {

            // ===== INPUT =====
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                ((Main) Gdx.app.getApplicationListener())
                    .setScreen(new DungeonScreen());
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                settings.show();
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                Gdx.app.exit();
            }

            if (Gdx.input.justTouched()) {

                var touch = viewport.unproject(
                    new com.badlogic.gdx.math.Vector3(
                        Gdx.input.getX(), Gdx.input.getY(), 0));

                float x = touch.x;
                float y = touch.y;

                if (x >= playX && x <= playX + width &&
                    y >= playY && y <= playY + height) {

                    ((Main) Gdx.app.getApplicationListener())
                        .setScreen(new DungeonScreen());
                }

                if (x >= settingsX && x <= settingsX + width &&
                    y >= settingsY && y <= settingsY + height) {

                    settings.show();
                }

                if (x >= exitX && x <= exitX + width &&
                    y >= exitY && y <= exitY + height) {

                    Gdx.app.exit();
                }
            }
        }

        // ===== DRAW MENU =====
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.rect(playX, playY, width, height);
        shapeRenderer.rect(settingsX, settingsY, width, height);
        shapeRenderer.rect(exitX, exitY, width, height);

        shapeRenderer.end();

        batch.begin();

        font.draw(batch, "PLAY (ENTER)", playX + 50, playY + 45);
        font.draw(batch, "SETTINGS (S)", settingsX + 40, settingsY + 45);
        font.draw(batch, "EXIT (ESC)", exitX + 50, exitY + 45);

        batch.end();

        // ===== DRAW SETTINGS ON TOP =====
        settings.render(shapeRenderer, batch, font);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
    }
}
