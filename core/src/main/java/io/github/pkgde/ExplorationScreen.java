package io.github.pkgde;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.viewport.*;
import com.badlogic.gdx.math.MathUtils;

public class ExplorationScreen implements Screen
{
    private static final String SAFE_ROOM_MAP = "Maps/safeRoom.tmx";

    private final OrthographicCamera camera;
    private final Viewport viewport;

    private final MapManager mapManager;
    private final GameWorld world;
    private final GameRenderer renderer;
    private final InputHandler input;

    private final PauseOverlay pauseOverlay;
    private final SettingsOverlay settingsOverlay;

    public enum State { GAME, INVENTORY, PAUSE, SETTINGS }
    private State state = State.GAME;

    private float shakeTime = 0f;
    private final float shakeDuration = 0.25f;
    private boolean shakeTriggered = false;

    private FrameBuffer fbo;
    private final ShaderProgram blurShader;
    private final SpriteBatch blurBatch;

    public ExplorationScreen() {
        mapManager = new MapManager();
        mapManager.load(SAFE_ROOM_MAP);

        float w = mapManager.getMapWidth();
        float h = mapManager.getMapHeight();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, w, h);
        camera.position.set(w / 2, h / 2, 0);

        viewport = new FitViewport(w, h, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        world = new GameWorld(mapManager);
        renderer = new GameRenderer(world, camera, mapManager);
        input = new InputHandler(viewport);

        pauseOverlay = new PauseOverlay();
        settingsOverlay = new SettingsOverlay();

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

        viewport.apply();

        if (state == State.GAME) {
            InputHandler.Action action = input.handle();

            switch (action) {
                case TOGGLE_PAUSE:
                    state = State.PAUSE;
                    break;
                case TOGGLE_INVENTORY:
                    state = State.INVENTORY;
                    break;
                case OPEN_SETTINGS:
                    state = State.SETTINGS;
                    settingsOverlay.show();
                    break;
                case EXIT_TO_MENU:
                    ((Main) Gdx.app.getApplicationListener()).setScreen(new HomeScreen());
                    return;
                case NONE:
                    break;
            }
        } else if (state == State.INVENTORY) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                state = State.GAME;
            }
        } else if (state == State.PAUSE) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                state = State.GAME;
            } else {
                PauseOverlay.Action pauseAction = pauseOverlay.handleInput(viewport);

                switch (pauseAction) {
                    case RESUME:
                        state = State.GAME;
                        break;
                    case SETTINGS:
                        state = State.SETTINGS;
                        settingsOverlay.show();
                        break;
                    case EXIT:
                        ((Main) Gdx.app.getApplicationListener()).setScreen(new HomeScreen());
                        return;
                    case NONE:
                        break;
                }
            }
        } else if (state == State.SETTINGS) {
            settingsOverlay.handleInput(viewport);
            if (!settingsOverlay.isActive()) {
                state = State.PAUSE;
            }
        }

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

        float offsetX = 0, offsetY = 0;

        if (shakeTime > 0) {
            shakeTime -= delta;
            offsetX = MathUtils.random(-10f, 10f);
            offsetY = MathUtils.random(-10f, 10f);
        }

        if (state == State.PAUSE || state == State.SETTINGS || state == State.INVENTORY) {
            fbo.begin();
            renderer.render(offsetX, offsetY);
            fbo.end();

            // Restore viewport on backbuffer before drawing blur + overlays.
            viewport.apply();

            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            Texture tex = fbo.getColorBufferTexture();

            blurBatch.setProjectionMatrix(camera.combined);
            blurBatch.begin();
            blurShader.setUniformf("blur", 0.002f);
            blurBatch.draw(
                tex,
                camera.position.x - camera.viewportWidth / 2f,
                camera.position.y - camera.viewportHeight / 2f,
                camera.viewportWidth,
                camera.viewportHeight,
                0,
                0,
                tex.getWidth(),
                tex.getHeight(),
                false,
                true
            );
            blurBatch.end();

            Gdx.gl.glEnable(GL20.GL_BLEND);
            ShapeRenderer shape = renderer.getShape();
            shape.setProjectionMatrix(camera.combined);
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(0, 0, 0, 0.5f);
            shape.rect(
                camera.position.x - camera.viewportWidth / 2f,
                camera.position.y - camera.viewportHeight / 2f,
                camera.viewportWidth,
                camera.viewportHeight
            );
            shape.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        } else {
            renderer.render(offsetX, offsetY);
        }

        SpriteBatch batch = renderer.getBatch();
        ShapeRenderer shape = renderer.getShape();
        BitmapFont font = renderer.getFont();

        if (state == State.PAUSE) {
            pauseOverlay.render(shape, batch, font, viewport);
        }

        if (state == State.SETTINGS) {
            settingsOverlay.render(shape, batch, font, viewport);
        }

        if (state == State.INVENTORY) {
            renderer.renderInventoryOverlay();
        }
    }

    @Override public void resize(int w, int h) {
        viewport.update(w, h, true);

        if (fbo != null) {
            fbo.dispose();
        }
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        fbo.getColorBufferTexture().setFilter(
            Texture.TextureFilter.Linear,
            Texture.TextureFilter.Linear
        );
    }

    @Override public void dispose() {
        renderer.dispose();
        world.dispose();
        mapManager.dispose();
        fbo.dispose();
        blurBatch.dispose();
        blurShader.dispose();
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

}
