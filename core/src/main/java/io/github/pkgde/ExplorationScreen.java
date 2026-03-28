package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ExplorationScreen implements Screen {

    private SpriteBatch batch;
    private Texture background;
    private Player player;
    private ShapeRenderer shape;
    private BitmapFont font;

    // ===== CAMERA =====
    private OrthographicCamera camera;
    private Viewport viewport;

    private final float WORLD_WIDTH = 800;
    private final float WORLD_HEIGHT = 600;

    // ===== PAUSE =====
    private boolean isPaused = false;

    // Pause buttons
    private float width = 250, height = 70;
    private float continueX = 275, continueY = 300;
    private float settingsX = 275, settingsY = 200;
    private float menuX = 275, menuY = 100;

    // ===== SETTINGS OVERLAY =====
    private SettingsOverlay settings;

    public ExplorationScreen() {
        batch = new SpriteBatch();
        background = new Texture("background.png");
        player = new Player();
        shape = new ShapeRenderer();
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

        ScreenUtils.clear(0, 0, 0, 1);

        batch.setProjectionMatrix(camera.combined);
        shape.setProjectionMatrix(camera.combined);

        // ===== ESC LOGIC =====
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {

            if (settings.isActive()) {
                settings.hide();
            } else {
                isPaused = !isPaused;
            }
        }

        // ===== SETTINGS INPUT =====
        if (settings.isActive()) {
            settings.handleInput(viewport);
        }

        // ===== GAME UPDATE =====
        if (!isPaused && !settings.isActive()) {
            player.update(delta);
        }

        // ===== DRAW GAME =====
        batch.begin();
        batch.draw(background, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        player.render(batch);
        batch.end();

        // ===== DEBUG (optional) =====
        shape.begin(ShapeRenderer.ShapeType.Line);
//        shape.rect(player.bounds.x, player.bounds.y, player.bounds.width, player.bounds.height);
        shape.end();

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

            // Main Menu
            if (x >= menuX && x <= menuX + width &&
                y >= menuY && y <= menuY + height) {

                ((Main) Gdx.app.getApplicationListener())
                    .setScreen(new HomeScreen());
            }
        }

        // ===== DRAW PAUSE OVERLAY =====
        if (isPaused) {

            shape.begin(ShapeRenderer.ShapeType.Filled);

            shape.setColor(0, 0, 0, 0.7f);
            shape.rect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);

            shape.setColor(1, 1, 1, 1);

            shape.rect(continueX, continueY, width, height);
            shape.rect(settingsX, settingsY, width, height);
            shape.rect(menuX, menuY, width, height);

            shape.end();

            batch.begin();

            font.draw(batch, "GAME PAUSED", 310, 450);
            font.draw(batch, "CONTINUE", continueX + 60, continueY + 45);
            font.draw(batch, "SETTINGS", settingsX + 65, settingsY + 45);
            font.draw(batch, "MAIN MENU", menuX + 55, menuY + 45);

            batch.end();
        }

        // ===== SETTINGS OVERLAY =====
        settings.render(shape, batch, font);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        background.dispose();
        player.dispose();
        shape.dispose();
        font.dispose();
    }
}
