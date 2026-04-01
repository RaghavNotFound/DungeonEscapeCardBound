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

    private static final String SAFE_ROOM_MAP = "Maps/safeRoom.tmx";

    private final OrthographicCamera camera;
    private final Viewport viewport;

    private final MapManager mapManager;
    private final GameWorld world;
    private final GameRenderer renderer;
    private final InputHandler input;

    private final PauseOverlay pauseOverlay;
    private final SettingsOverlay settingsOverlay;
    private final InventoryOverlay inventoryOverlay;
    private final GameOverOverlay gameOverOverlay;

    public enum State { GAME, INVENTORY, PAUSE, SETTINGS, GAME_OVER, VICTORY }
    private State state = State.GAME;

    private float shakeTime = 0f;
    private final float shakeDuration = 0.25f;
    private boolean shakeTriggered = false;

    private FrameBuffer fbo;
    private final ShaderProgram blurShader;
    private final SpriteBatch blurBatch;

    public ExplorationScreen() {
        camera = new OrthographicCamera();

        viewport = new FitViewport(1280, 720, camera);
        viewport.apply(true);

        camera.position.set(640, 360, 0);
        camera.update();

        mapManager = new MapManager();
        mapManager.load(SAFE_ROOM_MAP);

        world = new GameWorld(mapManager);
        renderer = new GameRenderer(world, camera, mapManager);
        input = new InputHandler(viewport);

        pauseOverlay = new PauseOverlay();
        settingsOverlay = new SettingsOverlay();
        inventoryOverlay = new InventoryOverlay();
        gameOverOverlay = new GameOverOverlay();

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
        settingsOverlay.update(delta);

        // ===== INPUT HANDLING =====
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
            inventoryOverlay.handleInput();
            if (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                state = State.GAME;
            }
        } else if (state == State.PAUSE) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                state = State.GAME;
            } else {
                PauseOverlay.Action action = pauseOverlay.handleInput(viewport);

                switch (action) {
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
            if (settingsOverlay.getTransitionProgress() >= 1f) {
                settingsOverlay.handleInput(viewport);
            }

            if (!settingsOverlay.isOverlayVisible() && settingsOverlay.getTransitionProgress() <= 0f) {
                state = State.PAUSE;
            }
        } else if (state == State.GAME_OVER) {
            GameOverOverlay.Action action = gameOverOverlay.handleInput(viewport);
            switch (action) {
                case RETRY:
                    ((Main) Gdx.app.getApplicationListener()).setScreen(new ExplorationScreen());
                    return;
                case MAIN_MENU:
                    ((Main) Gdx.app.getApplicationListener()).setScreen(new HomeScreen());
                    return;
                case NONE:
                    break;
            }
        } else if (state == State.VICTORY) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                ((Main) Gdx.app.getApplicationListener()).setScreen(new HomeScreen());
                return;
            }
        }

        // ===== UPDATE =====
        if (state == State.GAME) {
            float gameDelta = delta;

            boolean playerDead = !world.getPlayer().isAlive();
            boolean formatVictory = !world.getEnemies().isEmpty();
            boolean allAnimsFinished = true;

            for (Enemy e : world.getEnemies()) {
                if (e.isAlive()) formatVictory = false;
                if (!e.isDeathAnimationFinished()) allAnimsFinished = false;
            }

            // Apply slow-motion effect during death or victory sequences
            if (playerDead) {
                gameDelta *= 0.3f;
                if (world.getPlayer().isDeathAnimationFinished()) {
                    state = State.GAME_OVER;
                }
            } else if (formatVictory) {
                gameDelta *= 0.3f;
                if (allAnimsFinished) {
                    state = State.VICTORY;
                }
            }

            world.update(gameDelta, camera);

            if (world.isPlayerNearEnemy() && !playerDead && !formatVictory) {
                if (!shakeTriggered) {
                    shakeTime = shakeDuration;
                    shakeTriggered = true;
                }
            } else {
                shakeTriggered = false;
            }
        }

        float offsetX = 0f, offsetY = 0f;

        if (shakeTime > 0) {
            shakeTime -= delta;
            offsetX = MathUtils.random(-10f, 10f);
            offsetY = MathUtils.random(-10f, 10f);
        }

        // ===== RENDER =====
        boolean isOverlayActive = (state != State.GAME);

        if (isOverlayActive) {
            fbo.begin();
            renderer.render(offsetX, offsetY);
            fbo.end();

            viewport.apply();

            Texture tex = fbo.getColorBufferTexture();

            float blurAmount = 0f;
            if (state == State.PAUSE || state == State.INVENTORY || state == State.GAME_OVER || state == State.VICTORY) {
                blurAmount = 0.002f;
            }
            if (state == State.SETTINGS) {
                blurAmount = 0.002f * settingsOverlay.getTransitionProgress();
            }

            blurBatch.setProjectionMatrix(camera.combined);
            blurBatch.begin();
            blurShader.setUniformf("blur", blurAmount);
            blurBatch.draw(
                tex,
                camera.position.x - camera.viewportWidth / 2f,
                camera.position.y - camera.viewportHeight / 2f,
                camera.viewportWidth,
                camera.viewportHeight,
                0, 0,
                tex.getWidth(),
                tex.getHeight(),
                false, true
            );
            blurBatch.end();
        } else {
            renderer.render(offsetX, offsetY);
        }

        if (isOverlayActive) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            SpriteBatch batch = renderer.getBatch();
            ShapeRenderer shape = renderer.getShape();
            BitmapFont font = renderer.getFont();

            batch.setProjectionMatrix(camera.combined);
            shape.setProjectionMatrix(camera.combined);

            if (state == State.PAUSE || state == State.SETTINGS) {
                float settingsProgress = settingsOverlay.getTransitionProgress();
                float pauseAlpha = 1f - settingsProgress;
                pauseOverlay.render(shape, batch, font, viewport, pauseAlpha);
            }

            settingsOverlay.render(shape, batch, font, viewport);

            if (state == State.INVENTORY) {
                inventoryOverlay.render(shape, batch, font, viewport, world.getPlayer());
            }

            if (state == State.GAME_OVER) {
                gameOverOverlay.render(shape, batch, font, viewport);
            }

            if (state == State.VICTORY) {
                shape.begin(ShapeRenderer.ShapeType.Filled);
                shape.setColor(0.8f, 0.6f, 0.1f, 0.6f);
                shape.rect(
                    camera.position.x - camera.viewportWidth / 2f,
                    camera.position.y - camera.viewportHeight / 2f,
                    camera.viewportWidth,
                    camera.viewportHeight
                );
                shape.end();

                batch.begin();
                font.getData().setScale(3f);
                font.setColor(Color.WHITE);

                String text = "VICTORY ACHIEVED";
                float textW = 320f;
                font.draw(batch, text, camera.position.x - textW / 2f, camera.position.y + 70f);

                font.getData().setScale(1.5f);
                float subtitleW = 280f;
                font.draw(batch, "Press ESCAPE to return to Menu", camera.position.x - subtitleW / 2f, camera.position.y - 10f);

                font.getData().setScale(1f);
                batch.end();
            }

            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    @Override
    public void resize(int w, int h) {
        viewport.update(w, h, true);

        if (fbo != null) fbo.dispose();

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        fbo.getColorBufferTexture().setFilter(
            Texture.TextureFilter.Linear,
            Texture.TextureFilter.Linear
        );
    }

    @Override
    public void dispose() {
        renderer.dispose();
        world.dispose();
        mapManager.dispose();
        fbo.dispose();
        blurBatch.dispose();
        blurShader.dispose();
        inventoryOverlay.dispose();
        if (gameOverOverlay != null) {
            // Unsure if gameOverOverlay requires disposing in your structure,
            // but assuming it follows standard UI component conventions
            // gameOverOverlay.dispose();
        }
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
