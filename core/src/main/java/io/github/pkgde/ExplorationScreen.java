package io.github.pkgde;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.viewport.*;
import com.badlogic.gdx.math.MathUtils;

/**
 * The main gameplay screen. Manages the game state, camera effects,
 * and coordinate synchronization between the world and UI overlays.
 */
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

    // Juice Effects
    private float shakeTime = 0f;
    private final float shakeDuration = 0.25f;
    private boolean shakeTriggered = false;

    // Post-Processing
    private FrameBuffer fbo;
    private final ShaderProgram blurShader;
    private final SpriteBatch blurBatch;

    public ExplorationScreen() {
        // 1. Initialize Map and World logic
        mapManager = new MapManager();
        mapManager.load(SAFE_ROOM_MAP);

        float mapW = mapManager.getMapWidth();
        float mapH = mapManager.getMapHeight();

        // 2. Setup Camera and Viewport based on Map Dimensions
        camera = new OrthographicCamera();
        camera.setToOrtho(false, mapW, mapH);
        camera.position.set(mapW / 2f, mapH / 2f, 0);

        viewport = new FitViewport(mapW, mapH, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        // 3. Initialize Systems
        world = new GameWorld(mapManager);
        renderer = new GameRenderer(world, camera, mapManager);
        input = new InputHandler(viewport);

        // 4. Initialize UI Overlays
        pauseOverlay = new PauseOverlay();
        settingsOverlay = new SettingsOverlay();
        inventoryOverlay = new InventoryOverlay();
        gameOverOverlay = new GameOverOverlay();

        // 5. Setup Blur Buffer
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
        fbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        blurShader = BlurShader.createShader(true);
        blurBatch = new SpriteBatch();
        blurBatch.setShader(blurShader);
    }

    @Override
    public void render(float delta) {
        viewport.apply();
        settingsOverlay.update(delta);

        handleStateInput();
        updateGameLogic(delta);
        draw(delta);
    }

    private void handleStateInput() {
        if (state == State.GAME) {
            InputHandler.Action action = input.handle();
            switch (action) {
                case TOGGLE_PAUSE -> state = State.PAUSE;
                case TOGGLE_INVENTORY -> state = State.INVENTORY;
                case OPEN_SETTINGS -> { state = State.SETTINGS; settingsOverlay.show(); }
                case EXIT_TO_MENU -> ((Main) Gdx.app.getApplicationListener()).setScreen(new HomeScreen());
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
                    case RESUME -> state = State.GAME;
                    case SETTINGS -> { state = State.SETTINGS; settingsOverlay.show(); }
                    case EXIT -> ((Main) Gdx.app.getApplicationListener()).setScreen(new HomeScreen());
                }
            }
        } else if (state == State.SETTINGS) {
            if (settingsOverlay.getTransitionProgress() >= 1f) settingsOverlay.handleInput(viewport);
            if (!settingsOverlay.isOverlayVisible() && settingsOverlay.getTransitionProgress() <= 0f) state = State.PAUSE;
        } else if (state == State.GAME_OVER) {
            GameOverOverlay.Action action = gameOverOverlay.handleInput(viewport);
            switch (action) {
                case RETRY -> ((Main) Gdx.app.getApplicationListener()).setScreen(new ExplorationScreen());
                case MAIN_MENU -> ((Main) Gdx.app.getApplicationListener()).setScreen(new HomeScreen());
            }
        }
    }

    private void updateGameLogic(float delta) {
        if (state != State.GAME) return;

        float gameDelta = delta;
        boolean playerIsDead = !world.getPlayer().isAlive();

        if (playerIsDead) {
            gameDelta *= 0.3f; // Slow motion on death
            if (world.getPlayer().isDeathAnimationFinished()) state = State.GAME_OVER;
        } else if (world.isExitGateReached()) {
            Main main = (Main) Gdx.app.getApplicationListener();
            main.setScreen(new VictoryScreen(world.getPlayer().getEnemiesKilled(), world.getPlayer().getTorchCount(), world.getPlayer().getTimeSurvived()));
            return;
        }

        world.update(gameDelta, camera);

        // Camera Shake logic
        if (world.isPlayerNearEnemy() && !playerIsDead) {
            if (!shakeTriggered) {
                shakeTime = shakeDuration;
                shakeTriggered = true;
            }
        } else {
            shakeTriggered = false;
        }
    }

    private void draw(float delta) {
        float offsetX = 0, offsetY = 0;
        if (shakeTime > 0) {
            shakeTime -= delta;
            offsetX = MathUtils.random(-10f, 10f);
            offsetY = MathUtils.random(-10f, 10f);
        }

        boolean isOverlayActive = (state != State.GAME);

        if (isOverlayActive) {
            // Render world to FBO for blurring
            fbo.begin();
            renderer.render(offsetX, offsetY);
            fbo.end();

            viewport.apply();
            Texture tex = fbo.getColorBufferTexture();
            float blurAmount = (state == State.SETTINGS) ? 0.002f * settingsOverlay.getTransitionProgress() : 0.002f;

            blurBatch.setProjectionMatrix(camera.combined);
            blurBatch.begin();
            blurShader.setUniformf("blur", blurAmount);
            blurBatch.draw(tex, camera.position.x - camera.viewportWidth / 2f, camera.position.y - camera.viewportHeight / 2f,
                camera.viewportWidth, camera.viewportHeight, 0, 0, tex.getWidth(), tex.getHeight(), false, true);
            blurBatch.end();

            renderer.renderLighting();

            // Darken background
            Gdx.gl.glEnable(GL20.GL_BLEND);
            ShapeRenderer shape = renderer.getShape();
            shape.setProjectionMatrix(camera.combined);
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(0, 0, 0, (state == State.GAME_OVER) ? 0.65f : 0.5f);
            if (state == State.GAME_OVER) shape.setColor(0.5f, 0, 0, 0.65f); // Red tint for Game Over
            shape.rect(camera.position.x - camera.viewportWidth / 2f, camera.position.y - camera.viewportHeight / 2f, camera.viewportWidth, camera.viewportHeight);
            shape.end();

            // Render Overlays
            renderUIOverlays();
        } else {
            renderer.render(offsetX, offsetY);
            renderer.renderLighting();
        }
    }

    private void renderUIOverlays() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        SpriteBatch batch = renderer.getBatch();
        ShapeRenderer shape = renderer.getShape();
        BitmapFont font = renderer.getFont();

        batch.setProjectionMatrix(camera.combined);
        shape.setProjectionMatrix(camera.combined);

        if (state == State.PAUSE || state == State.SETTINGS) {
            pauseOverlay.render(shape, batch, font, viewport, 1f - settingsOverlay.getTransitionProgress());
        }
        settingsOverlay.render(shape, batch, font, viewport);
        if (state == State.INVENTORY) inventoryOverlay.render(shape, batch, font, viewport, world.getPlayer());
        if (state == State.GAME_OVER) gameOverOverlay.render(shape, batch, font, viewport);

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void resize(int w, int h) {
        viewport.update(w, h, true);
        if (fbo != null) fbo.dispose();
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        fbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        if (world != null && world.getLightingManager() != null) {
            world.getLightingManager().resize(w, h);
        }
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
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
