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

public class DungeonScreen implements Screen {

    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;

    // ===== CAMERA =====
    private OrthographicCamera camera;
    private Viewport viewport;

    private final float WORLD_WIDTH = 800;
    private final float WORLD_HEIGHT = 600;

    // ===== PLAYER =====
    private float playerX = 100;
    private float playerY = 100;
    private float speed = 300;
    private float size = 40;

    // ===== PAUSE =====
    private boolean isPaused = false;

    // Pause buttons
    private float width = 250, height = 70;
    private float continueX = 275, continueY = 300;
    private float settingsX = 275, settingsY = 200;
    private float menuX = 275, menuY = 100;

    // ===== SETTINGS OVERLAY =====
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

        // ===== ESC LOGIC (FIXED PRIORITY) =====
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {

            if (settings.isActive()) {
                settings.hide();   // Close settings first
            }
            else {
                isPaused = !isPaused; // Then toggle pause
            }
        }

        // ===== SETTINGS INPUT =====
        if (settings.isActive()) {
            settings.handleInput(viewport);
        }

        // ===== GAME LOGIC =====
        if (!isPaused && !settings.isActive()) {

            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))
                playerY += speed * delta;

            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))
                playerY -= speed * delta;

            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))
                playerX -= speed * delta;

            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT))
                playerX += speed * delta;
        }

        // ===== DRAW PLAYER =====
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.rect(playerX, playerY, size, size);
        shapeRenderer.end();

        // ===== PAUSE MENU INPUT =====
        if (isPaused && !settings.isActive() && Gdx.input.justTouched()) {

            var touch = viewport.unproject(
                new com.badlogic.gdx.math.Vector3(
                    Gdx.input.getX(), Gdx.input.getY(), 0));

            float x = touch.x;
            float y = touch.y;

            // Continue
            if (x >= continueX && x <= continueX + width &&
                y >= continueY && y <= continueY + height) {

                isPaused = false;
            }

            // Settings
            if (x >= settingsX && x <= settingsX + width &&
                y >= settingsY && y <= settingsY + height) {

                settings.show();
            }

            // Return to HomeScreen (NOT exit)
            if (x >= menuX && x <= menuX + width &&
                y >= menuY && y <= menuY + height) {

                ((Main) Gdx.app.getApplicationListener())
                    .setScreen(new HomeScreen());
            }
        }

        // ===== DRAW PAUSE OVERLAY =====
        if (isPaused) {

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            shapeRenderer.setColor(0, 0, 0, 0.7f);
            shapeRenderer.rect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);

            shapeRenderer.setColor(1, 1, 1, 1);

            shapeRenderer.rect(continueX, continueY, width, height);
            shapeRenderer.rect(settingsX, settingsY, width, height);
            shapeRenderer.rect(menuX, menuY, width, height);

            shapeRenderer.end();

            batch.begin();

            font.draw(batch, "GAME PAUSED", 310, 450);
            font.draw(batch, "CONTINUE", continueX + 60, continueY + 45);
            font.draw(batch, "SETTINGS", settingsX + 65, settingsY + 45);
            font.draw(batch, "MAIN MENU", menuX + 55, menuY + 45);

            batch.end();
        }

        // ===== SETTINGS ON TOP =====
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
