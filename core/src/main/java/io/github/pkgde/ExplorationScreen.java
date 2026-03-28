package io.github.pkgde;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.viewport.*;
import com.badlogic.gdx.math.MathUtils;

public class ExplorationScreen implements Screen {

    private OrthographicCamera camera;
    private Viewport viewport;

    private GameWorld world;
    private GameRenderer renderer;
    private InputHandler input;

    private PauseOverlay pauseOverlay;
    private SettingsOverlay settingsOverlay;

    public enum State { GAME, PAUSE, SETTINGS }
    private State state = State.GAME;

    // 💥 SHAKE
    private float shakeTime = 0f;
    private float shakeDuration = 0.25f;
    private float shakeIntensity = 10f;
    private boolean shakeTriggered = false;

    // 🌫️ BLUR
    private FrameBuffer fbo;
    private ShaderProgram blurShader;
    private SpriteBatch blurBatch;

    public ExplorationScreen() {

        camera = new OrthographicCamera();

        viewport = new FitViewport(1280, 720, camera);
        viewport.apply(true);

        camera.position.set(viewport.getWorldWidth()/2f, viewport.getWorldHeight()/2f, 0);
        camera.update();

        world = new GameWorld();
        renderer = new GameRenderer(world, camera);
        input = new InputHandler(viewport);

        pauseOverlay = new PauseOverlay();
        settingsOverlay = new SettingsOverlay();

        // 🔥 DYNAMIC FBO (QUALITY FIX)
        fbo = new FrameBuffer(
            Pixmap.Format.RGBA8888,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            false
        );

        fbo.getColorBufferTexture().setFilter(
            Texture.TextureFilter.Linear,
            Texture.TextureFilter.Linear
        );

        blurShader = BlurShader.createShader(true);
        blurBatch = new SpriteBatch();
        blurBatch.setShader(blurShader);
    }

    @Override
    public void render(float delta) {

        // ===== INPUT =====
        InputHandler.Action action = input.handle();

        switch (action) {
            case TOGGLE_PAUSE:
                if (state == State.GAME) state = State.PAUSE;
                else if (state == State.PAUSE) state = State.GAME;
                else if (state == State.SETTINGS) {
                    state = State.PAUSE;
                    settingsOverlay.hide();
                }
                break;

            case OPEN_SETTINGS:
                if (state == State.PAUSE) {
                    state = State.SETTINGS;
                    settingsOverlay.show();
                }
                break;

            case EXIT_TO_MENU:
                ((Main) Gdx.app.getApplicationListener())
                    .setScreen(new HomeScreen());
                break;
        }

        // ===== UPDATE =====
        if (state == State.GAME) {
            world.update(delta, camera);

            if (world.isPlayerNearEnemy()) {
                if (!shakeTriggered) {
                    shakeTime = shakeDuration;
                    shakeTriggered = true;
                }
            } else {
                shakeTriggered = false;
            }
        }

        // ===== PAUSE / SETTINGS INPUT =====
        // 🔥 else-if ensures only ONE block runs per frame — prevents Enter
        // from bleeding into the newly entered state on the same frame
        if (state == State.PAUSE) {
            PauseOverlay.Action pauseAction = pauseOverlay.handleInput();

            switch (pauseAction) {
                case RESUME: state = State.GAME; break;
                case SETTINGS:
                    state = State.SETTINGS;
                    settingsOverlay.show();
                    break;
                case EXIT:
                    ((Main) Gdx.app.getApplicationListener())
                        .setScreen(new HomeScreen());
                    break;
            }

        } else if (state == State.SETTINGS) {
            settingsOverlay.handleInput(viewport);

            if (!settingsOverlay.isActive()) {
                state = State.PAUSE;
            }
        }

        // ===== SHAKE =====
        float offsetX = 0, offsetY = 0;

        if (shakeTime > 0) {
            shakeTime -= delta;
            offsetX = MathUtils.random(-shakeIntensity, shakeIntensity);
            offsetY = MathUtils.random(-shakeIntensity, shakeIntensity);
        }

        camera.position.set(
            viewport.getWorldWidth()/2f + offsetX,
            viewport.getWorldHeight()/2f + offsetY,
            0
        );
        camera.update();

        // ===== RENDER =====
        if (state == State.PAUSE || state == State.SETTINGS) {

            fbo.begin();
            renderer.render();
            fbo.end();

            Texture tex = fbo.getColorBufferTexture();

            blurBatch.setProjectionMatrix(camera.combined);

            blurBatch.begin();
            blurShader.setUniformf("blur", 0.002f);

            // 🔥 FIXED DRAW (NO ARTIFACTS)
            blurBatch.draw(
                tex,
                camera.position.x - camera.viewportWidth/2f,
                camera.position.y - camera.viewportHeight/2f,
                camera.viewportWidth,
                camera.viewportHeight,
                0, 0,
                tex.getWidth(),
                tex.getHeight(),
                false, true
            );

            blurBatch.end();

            // 🌑 SOFTER OVERLAY
            Gdx.gl.glEnable(GL20.GL_BLEND);

            ShapeRenderer shape = renderer.getShape();
            shape.setProjectionMatrix(camera.combined);

            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(0, 0, 0, 0.5f);
            shape.rect(
                camera.position.x - camera.viewportWidth/2f,
                camera.position.y - camera.viewportHeight/2f,
                camera.viewportWidth,
                camera.viewportHeight
            );
            shape.end();

            Gdx.gl.glDisable(GL20.GL_BLEND);

        } else {
            renderer.render();
        }

        // ===== UI =====
        SpriteBatch batch = renderer.getBatch();
        ShapeRenderer shape = renderer.getShape();
        BitmapFont font = renderer.getFont();

        if (state == State.PAUSE) {
            pauseOverlay.render(shape, batch, font, viewport);
        }

        if (state == State.SETTINGS) {
            settingsOverlay.render(shape, batch, font, viewport);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);

        camera.position.set(
            viewport.getWorldWidth()/2f,
            viewport.getWorldHeight()/2f,
            0
        );
        camera.update();
    }

    @Override
    public void dispose() {
        renderer.dispose();
        world.dispose();
        fbo.dispose();
        blurBatch.dispose();
        blurShader.dispose();
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
