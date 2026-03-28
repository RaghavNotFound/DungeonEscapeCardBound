package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ExplorationScreen implements Screen {

    private SpriteBatch batch;
    private Texture background;
    private Player player;
    private ShapeRenderer shape;
    private BitmapFont font;

    private OrthographicCamera camera;
    private Viewport viewport;

    private enum MenuState { NONE, PAUSE, SETTINGS }
    private MenuState menuState = MenuState.NONE;

    private FrameBuffer fbo1, fbo2;
    private ShaderProgram blurH, blurV;
    private Texture pausedBackground;

    private boolean needsBlurRefresh = false;

    private SettingsOverlay settingsOverlay;

    private int pauseSelected = 0;

    public ExplorationScreen() {

        batch = new SpriteBatch();
        background = new Texture("background.png");
        player = new Player();
        shape = new ShapeRenderer();
        font = new BitmapFont();

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);

        viewport.apply(true);
        camera.position.set(
            viewport.getWorldWidth() / 2f,
            viewport.getWorldHeight() / 2f,
            0
        );
        camera.update();

        createFBOs(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        blurH = BlurShader.createShader(true);
        blurV = BlurShader.createShader(false);

        settingsOverlay = new SettingsOverlay();
    }

    private void createFBOs(int w, int h) {
        fbo1 = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        fbo2 = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
    }

    private void captureAndBlur(float w, float h) {

        fbo1.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(background, 0, 0, w, h);
        player.render(batch);
        batch.end();

        fbo1.end();

        for (int i = 0; i < 3; i++) {

            batch.setShader(blurH);
            batch.begin();
            blurH.setUniformf("blur", 3f / w);

            fbo2.begin();
            batch.draw(fbo1.getColorBufferTexture(),
                0, 0, w, h,
                0, 1, 1, -1);
            batch.end();
            fbo2.end();

            batch.setShader(blurV);
            batch.begin();
            blurV.setUniformf("blur", 3f / h);

            fbo1.begin();
            batch.draw(fbo2.getColorBufferTexture(),
                0, 0, w, h,
                0, 1, 1, -1);
            batch.end();
            fbo1.end();
        }

        batch.setShader(null);
        pausedBackground = fbo1.getColorBufferTexture();
    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();

        batch.setProjectionMatrix(camera.combined);
        shape.setProjectionMatrix(camera.combined);

        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();

        if (needsBlurRefresh && menuState != MenuState.NONE) {
            captureAndBlur(w, h);
            needsBlurRefresh = false;
        }

        // ESC
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {

            if (menuState == MenuState.NONE) {
                menuState = MenuState.PAUSE;
                pauseSelected = 0;
                captureAndBlur(w, h);
            }
            else if (menuState == MenuState.SETTINGS) {
                menuState = MenuState.PAUSE;
                settingsOverlay.hide();
            }
            else {
                menuState = MenuState.NONE;
            }
        }

        // ===== UI DIMENSIONS =====
        float btnW = w * 0.22f;
        float btnH = h * 0.08f;
        float gap = h * 0.03f;

        float pauseX = w / 2f - btnW / 2f;

        float totalHeight = (btnH * 3) + (gap * 2);
        float startY = h / 2f + totalHeight / 2f;

        float y1 = startY - btnH;
        float y2 = y1 - btnH - gap;
        float y3 = y2 - btnH - gap;

        // ===== INPUT =====
        if (menuState == MenuState.SETTINGS) {
            settingsOverlay.handleInput(viewport);

            if (!settingsOverlay.isActive()) {
                menuState = MenuState.PAUSE;
            }
        }
        else if (menuState == MenuState.PAUSE) {

            // KEYBOARD
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP))
                pauseSelected = (pauseSelected + 2) % 3;

            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
                pauseSelected = (pauseSelected + 1) % 3;

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                applyPauseSelection();
            }

            // MOUSE
            if (Gdx.input.justTouched()) {

                var touch = viewport.unproject(
                    new com.badlogic.gdx.math.Vector3(
                        Gdx.input.getX(), Gdx.input.getY(), 0));

                float x = touch.x;
                float y = touch.y;

                if (x >= pauseX && x <= pauseX + btnW &&
                    y >= y1 && y <= y1 + btnH) {
                    pauseSelected = 0;
                }
                else if (x >= pauseX && x <= pauseX + btnW &&
                    y >= y2 && y <= y2 + btnH) {
                    pauseSelected = 1;
                }
                else if (x >= pauseX && x <= pauseX + btnW &&
                    y >= y3 && y <= y3 + btnH) {
                    pauseSelected = 2;
                }

                applyPauseSelection();
            }
        }

        // UPDATE
        if (menuState == MenuState.NONE) {
            player.update(delta);
        }

        // DRAW GAME / BLUR
        if (menuState == MenuState.NONE) {
            batch.begin();
            batch.draw(background, 0, 0, w, h);
            player.render(batch);
            batch.end();
        } else {
            batch.begin();
            batch.draw(pausedBackground,
                0, 0, w, h,
                0, 1, 1, -1);
            batch.end();
        }

        // ===== DRAW PAUSE =====
        if (menuState == MenuState.PAUSE) {

            shape.begin(ShapeRenderer.ShapeType.Line);

            shape.setColor(pauseSelected == 0 ? 1 : 0.5f, 1, 1, 1);
            shape.rect(pauseX, y1, btnW, btnH);

            shape.setColor(pauseSelected == 1 ? 1 : 0.5f, 1, 1, 1);
            shape.rect(pauseX, y2, btnW, btnH);

            shape.setColor(pauseSelected == 2 ? 1 : 0.5f, 1, 1, 1);
            shape.rect(pauseX, y3, btnW, btnH);

            shape.end();

            batch.begin();

            float scale = w / 800f;
            font.getData().setScale(scale * 1.2f);

            font.draw(batch, "GAME PAUSED", w / 2f - 90 * scale, startY + btnH);

            font.draw(batch, "CONTINUE", pauseX + btnW * 0.25f, y1 + btnH * 0.65f);
            font.draw(batch, "SETTINGS", pauseX + btnW * 0.25f, y2 + btnH * 0.65f);
            font.draw(batch, "MAIN MENU", pauseX + btnW * 0.20f, y3 + btnH * 0.65f);

            batch.end();
        }

        // ===== SETTINGS =====
        if (menuState == MenuState.SETTINGS) {
            settingsOverlay.render(shape, batch, font, viewport);
        }
    }

    private void applyPauseSelection() {

        if (pauseSelected == 0) {
            menuState = MenuState.NONE;
        }
        else if (pauseSelected == 1) {
            menuState = MenuState.SETTINGS;
            settingsOverlay.show();
        }
        else if (pauseSelected == 2) {
            ((Main) Gdx.app.getApplicationListener())
                .setScreen(new HomeScreen());
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);

        if (fbo1 != null) fbo1.dispose();
        if (fbo2 != null) fbo2.dispose();

        createFBOs(width, height);
        needsBlurRefresh = true;
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
        fbo1.dispose();
        fbo2.dispose();
        blurH.dispose();
        blurV.dispose();
    }
}
