package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
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

    private Texture background;

    private OrthographicCamera camera;
    private Viewport viewport;

    private final float WORLD_WIDTH = 800;
    private final float WORLD_HEIGHT = 600;

    // Button size
    private float width = 220, height = 60;

    // RIGHT SIDE BUTTONS (based on your sketch)
    private float btnX = 550;
    private float playY = 320;
    private float settingsY = 240;
    private float exitY = 160;

    private SettingsOverlay settings;

    @Override
    public void show() {

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();

        // 🔥 LOAD YOUR IMAGE
        background = new Texture("assets/HomeScreen/HomeScreen.jpg");

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

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // ===== INPUT =====
        if (settings.isActive()) {
            settings.handleInput(viewport);
        } else {

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                ((Main) Gdx.app.getApplicationListener())
                    .setScreen(new ExplorationScreen());
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

                if (x >= btnX && x <= btnX + width &&
                    y >= playY && y <= playY + height) {

                    ((Main) Gdx.app.getApplicationListener())
                        .setScreen(new ExplorationScreen());
                }

                if (x >= btnX && x <= btnX + width &&
                    y >= settingsY && y <= settingsY + height) {

                    settings.show();
                }

                if (x >= btnX && x <= btnX + width &&
                    y >= exitY && y <= exitY + height) {

                    Gdx.app.exit();
                }
            }
        }

        // ===== DRAW BACKGROUND =====
        batch.begin();
        batch.draw(background, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);

        // ===== TITLE (TOP LEFT) =====
        font.getData().setScale(2f);
        font.draw(batch, "DUNGEON ESCAPE", 40, 560);

        // ===== BUTTON TEXT =====
        font.getData().setScale(1.2f);
        font.draw(batch, "PLAY", btnX + 70, playY + 40);
        font.draw(batch, "SETTINGS", btnX + 40, settingsY + 40);
        font.draw(batch, "EXIT", btnX + 70, exitY + 40);

        batch.end();

        // ===== BUTTON BOXES =====
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        shapeRenderer.rect(btnX, playY, width, height);
        shapeRenderer.rect(btnX, settingsY, width, height);
        shapeRenderer.rect(btnX, exitY, width, height);

        shapeRenderer.end();

        // ===== SETTINGS =====
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
        background.dispose();
    }
}
