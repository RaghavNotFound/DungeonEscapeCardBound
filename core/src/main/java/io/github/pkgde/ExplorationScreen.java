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
import com.badlogic.gdx.math.Vector3;

public class ExplorationScreen implements Screen {

    private SpriteBatch batch;
    private Texture background;
    private Player player;
    private ShapeRenderer shape;
    private BitmapFont font;
    private Enemy enemy;

    private OrthographicCamera camera;
    private Viewport viewport;

    private enum MenuState { NONE, PAUSE, SETTINGS }
    private MenuState menuState = MenuState.NONE;

    private FrameBuffer fbo1, fbo2;
    private ShaderProgram blurH, blurV;
    private Texture pausedBackground;

    private boolean needsBlurRefresh = false;

    public ExplorationScreen() {

        batch = new SpriteBatch();
        background = new Texture("background.png");
        player = new Player();
        enemy = new Enemy();
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
        enemy.render(batch); // ✅ ADDED
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {

            if (menuState == MenuState.NONE) {
                menuState = MenuState.PAUSE;
                captureAndBlur(w, h);
            }
            else if (menuState == MenuState.SETTINGS) {
                menuState = MenuState.PAUSE;
            }
            else {
                menuState = MenuState.NONE;
            }
        }

        // ✅ UPDATED GAME LOGIC
        if (menuState == MenuState.NONE) {
            player.update(delta);
            enemy.update(delta, player);

            // 🔥 COMBAT TRIGGER
            if (enemy.getBounds().overlaps(player.bounds)) {
                System.out.println("COMBAT TRIGGERED");
            }
        }

        if (menuState == MenuState.NONE) {
            batch.begin();
            batch.draw(background, 0, 0, w, h);
            player.render(batch);
            enemy.render(batch);
            batch.end();
        } else {
            batch.begin();
            batch.draw(pausedBackground,
                0, 0, w, h,
                0, 1, 1, -1);
            batch.end();
        }

        float btnW = w * 0.22f;
        float btnH = h * 0.08f;
        float gap = h * 0.03f;

        float pauseX = w * 0.25f - btnW / 2f;
        float settingsX = w * 0.75f - btnW / 2f;

        float baseY = h * 0.55f;

        float y1 = baseY;
        float y2 = y1 - btnH - gap;
        float y3 = y2 - btnH - gap;

        float sy1 = baseY;
        float sy2 = sy1 - btnH - gap;
        float sy3 = sy2 - btnH - gap;
        float sy4 = sy3 - btnH - gap;

        if (Gdx.input.justTouched()) {

            Vector3 touch = viewport.unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

            float x = touch.x;
            float y = touch.y;

            if (menuState == MenuState.SETTINGS) {

                if (x >= settingsX && x <= settingsX + btnW &&
                    y >= sy1 && y <= sy1 + btnH) {
                    Gdx.graphics.setWindowedMode(800, 600);
                    needsBlurRefresh = true;
                    return;
                }

                if (x >= settingsX && x <= settingsX + btnW &&
                    y >= sy2 && y <= sy2 + btnH) {
                    Gdx.graphics.setWindowedMode(1280, 720);
                    needsBlurRefresh = true;
                    return;
                }

                if (x >= settingsX && x <= settingsX + btnW &&
                    y >= sy3 && y <= sy3 + btnH) {
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                    needsBlurRefresh = true;
                    return;
                }

                if (x >= settingsX && x <= settingsX + btnW &&
                    y >= sy4 && y <= sy4 + btnH) {
                    menuState = MenuState.PAUSE;
                    return;
                }
            }

            if (menuState == MenuState.PAUSE) {

                if (x >= pauseX && x <= pauseX + btnW &&
                    y >= y1 && y <= y1 + btnH) {
                    menuState = MenuState.NONE;
                    return;
                }

                if (x >= pauseX && x <= pauseX + btnW &&
                    y >= y2 && y <= y2 + btnH) {
                    menuState = MenuState.SETTINGS;
                    return;
                }

                if (x >= pauseX && x <= pauseX + btnW &&
                    y >= y3 && y <= y3 + btnH) {
                    ((Main) Gdx.app.getApplicationListener())
                        .setScreen(new HomeScreen());
                    return;
                }
            }
        }

        if (menuState == MenuState.PAUSE || menuState == MenuState.SETTINGS) {

            shape.begin(ShapeRenderer.ShapeType.Line);
            shape.rect(pauseX, y1, btnW, btnH);
            shape.rect(pauseX, y2, btnW, btnH);
            shape.rect(pauseX, y3, btnW, btnH);
            shape.end();

            batch.begin();

            float scale = w / 800f;
            font.getData().setScale(scale * 1.2f);

            font.draw(batch, "GAME PAUSED", w * 0.42f, h * 0.75f);
            font.draw(batch, "CONTINUE", pauseX + btnW * 0.25f, y1 + btnH * 0.65f);
            font.draw(batch, "SETTINGS", pauseX + btnW * 0.25f, y2 + btnH * 0.65f);
            font.draw(batch, "MAIN MENU", pauseX + btnW * 0.2f, y3 + btnH * 0.65f);

            batch.end();
        }

        if (menuState == MenuState.SETTINGS) {

            shape.begin(ShapeRenderer.ShapeType.Line);
            shape.rect(settingsX, sy1, btnW, btnH);
            shape.rect(settingsX, sy2, btnW, btnH);
            shape.rect(settingsX, sy3, btnW, btnH);
            shape.rect(settingsX, sy4, btnW, btnH);
            shape.end();

            batch.begin();

            font.draw(batch, "800x600", settingsX + btnW * 0.3f, sy1 + btnH * 0.65f);
            font.draw(batch, "1280x720", settingsX + btnW * 0.25f, sy2 + btnH * 0.65f);
            font.draw(batch, "FULLSCREEN", settingsX + btnW * 0.2f, sy3 + btnH * 0.65f);
            font.draw(batch, "BACK (ESC)", settingsX + btnW * 0.25f, sy4 + btnH * 0.65f);

            batch.end();
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
        enemy.dispose(); // ✅ ADDED
        shape.dispose();
        font.dispose();
        fbo1.dispose();
        fbo2.dispose();
        blurH.dispose();
        blurV.dispose();
    }
}
