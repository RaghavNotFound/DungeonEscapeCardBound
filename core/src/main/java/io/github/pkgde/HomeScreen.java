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
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.Vector3;

public class HomeScreen implements Screen {

    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;

    private Texture background;

    private OrthographicCamera camera;
    private Viewport viewport;

    private SettingsOverlay settings;

    private int selected = 0;

    @Override
    public void show() {

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();

        background = new Texture("HomeScreen/HomeScreen.jpg");

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);

        viewport.apply(true);
        camera.position.set(
            viewport.getWorldWidth() / 2f,
            viewport.getWorldHeight() / 2f,
            0
        );
        camera.update();

        settings = new SettingsOverlay();
    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();

        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        float worldW = viewport.getWorldWidth();
        float worldH = viewport.getWorldHeight();

        float btnWidth = worldW * 0.20f;
        float btnHeight = worldH * 0.08f;
        float gap = worldH * 0.03f;

        float btnX = worldW * 0.70f;
        float playY = worldH * 0.55f;
        float settingsY = playY - btnHeight - gap;
        float exitY = settingsY - btnHeight - gap;

        // ===== INPUT =====
        if (settings.isActive()) {

            settings.handleInput(viewport);

        } else {

            // ===== KEYBOARD NAV =====
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                selected = (selected + 2) % 3;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                selected = (selected + 1) % 3;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                applySelection();
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                Gdx.app.exit();
            }

            // ===== MOUSE (SYNCED) =====
            if (Gdx.input.justTouched()) {

                Vector3 touch = viewport.unproject(
                    new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

                float x = touch.x;
                float y = touch.y;

                if (x >= btnX && x <= btnX + btnWidth &&
                    y >= playY && y <= playY + btnHeight) {
                    selected = 0;
                }
                else if (x >= btnX && x <= btnX + btnWidth &&
                    y >= settingsY && y <= settingsY + btnHeight) {
                    selected = 1;
                }
                else if (x >= btnX && x <= btnX + btnWidth &&
                    y >= exitY && y <= exitY + btnHeight) {
                    selected = 2;
                }

                // 🔥 SAME ACTION SYSTEM
                applySelection();
            }
        }

        // ===== DRAW =====
        batch.begin();

        batch.draw(background, 0, 0, worldW, worldH);

        float scale = worldW / 800f;

        font.getData().setScale(scale * 2f);
        font.draw(batch, "DUNGEON ESCAPE", worldW * 0.05f, worldH * 0.92f);

        font.getData().setScale(scale * 1.2f);
        font.draw(batch, "PLAY", btnX + btnWidth * 0.35f, playY + btnHeight * 0.65f);
        font.draw(batch, "SETTINGS", btnX + btnWidth * 0.20f, settingsY + btnHeight * 0.65f);
        font.draw(batch, "EXIT", btnX + btnWidth * 0.35f, exitY + btnHeight * 0.65f);

        batch.end();

        // ===== BUTTON BOXES =====
        if (!settings.isActive()) {

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

            shapeRenderer.setColor(selected == 0 ? 1 : 0.5f, 1, 1, 1);
            shapeRenderer.rect(btnX, playY, btnWidth, btnHeight);

            shapeRenderer.setColor(selected == 1 ? 1 : 0.5f, 1, 1, 1);
            shapeRenderer.rect(btnX, settingsY, btnWidth, btnHeight);

            shapeRenderer.setColor(selected == 2 ? 1 : 0.5f, 1, 1, 1);
            shapeRenderer.rect(btnX, exitY, btnWidth, btnHeight);

            shapeRenderer.end();
        }

        // ===== SETTINGS =====
        settings.render(shapeRenderer, batch, font, viewport);
    }

    // ✅ CENTRAL ACTION SYSTEM
    private void applySelection() {

        if (selected == 0) {
            ((Main) Gdx.app.getApplicationListener())
                .setScreen(new ExplorationScreen());
        }
        else if (selected == 1) {
            settings.show();
        }
        else if (selected == 2) {
            Gdx.app.exit();
        }
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
