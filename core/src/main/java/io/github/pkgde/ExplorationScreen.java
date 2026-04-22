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

    private static final String SAFE_ROOM_MAP = "Maps/tutorial.ldtk";
    private static final String AUTO_SAVE_SLOT_NAME = "auto_save";
    private final float AUTO_SAVE_INTERVAL = 10f; // Auto-save every 10 seconds

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
    private final SaveLoadOverlay saveLoadOverlay;
    private final DebugOverlay debugOverlay;

    public enum State { GAME, INVENTORY, PAUSE, SETTINGS, GAME_OVER, SAVE_LOAD }
    private State state = State.GAME;

    // Juice Effects
    private float shakeTime = 0f;
    private final float shakeDuration = 0.25f;
    private boolean shakeTriggered = false;

    // Post-Processing
    private FrameBuffer fbo;
    private final ShaderProgram blurShader;
    private final SpriteBatch blurBatch;

    public ExplorationScreen(String saveFileToLoad) {
        this(saveFileToLoad, SAFE_ROOM_MAP, 0);
    }

    public ExplorationScreen(String saveFileToLoad, String mapPath, int levelIndex) {
        mapManager = new MapManager();
        mapManager.load(mapPath, levelIndex);
        float mapW = mapManager.getMapWidth();
        float mapH = mapManager.getMapHeight();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, mapW, mapH);
        camera.position.set(mapW / 2f, mapH / 2f, 0);
        viewport = new FitViewport(mapW, mapH, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        world = new GameWorld(mapManager);
        renderer = new GameRenderer(world, camera, mapManager);
        input = new InputHandler(viewport);

        pauseOverlay = new PauseOverlay();
        settingsOverlay = new SettingsOverlay();
        inventoryOverlay = new InventoryOverlay();
        gameOverOverlay = new GameOverOverlay();
        saveLoadOverlay = new SaveLoadOverlay();
        debugOverlay = new DebugOverlay();

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
        fbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        blurShader = BlurShader.createShader(true);
        blurBatch = new SpriteBatch();
        blurBatch.setShader(blurShader);

        if (saveFileToLoad != null) {
            SaveManager.loadGame(world, saveFileToLoad);
            // After loading stats from a checkpoint/save when constructing a map,
            // ensure the player spawns exactly at the start of THIS specific new map.
            world.getPlayer().setPosition(mapManager.getPlayerSpawn().x, mapManager.getPlayerSpawn().y);
        } else {
            // Ensure bounds update properly manually anyway
            world.getPlayer().setPosition(mapManager.getPlayerSpawn().x, mapManager.getPlayerSpawn().y);
        }
    }

    @Override
    public void render(float delta) {
        viewport.apply();
        settingsOverlay.update(delta);

        handleStateInput();
        updateGameLogic(delta);
        draw(delta);

        if (world.isLevelComplete() && state == State.GAME) {
            // Move to next level
            int nextLevelIndex = mapManager.getCurrentLevelIndex() + 1;
            SaveManager.saveGame(world, "checkpoint");

            if (mapManager.getCurrentMapPath().equals("Maps/tutorial.ldtk")) {
                if (nextLevelIndex < 4) {
                    ((Main) Gdx.app.getApplicationListener()).setScreen(new ExplorationScreen("checkpoint", "Maps/tutorial.ldtk", nextLevelIndex));
                } else {
                    // Load safe room
                    ((Main) Gdx.app.getApplicationListener()).setScreen(new ExplorationScreen(null, "Maps/safeRoom.ldtk", 0));
                }
            } else if (mapManager.getCurrentMapPath().equals("Maps/safeRoom.ldtk")) {
                // Done playing map
            }
        }

        // Check for Boss Trigger interaction
        if (state == State.GAME) {
            for (Interactable i : world.getInteractables()) {
                if (i.getType() == Interactable.Type.BOSS_TRIGGER && i.isInteracted()) {
                    // You can change the boss name right here!
                    ((Main) Gdx.app.getApplicationListener()).setScreen(new BossFightScreen(world.getPlayer(), "THE LICH KING"));
                    return;
                }
            }
        }
    }

    private void handleStateInput() {
        if (debugOverlay.handleInput()) {
            return;
        }
        debugOverlay.handleCheats(camera, world, mapManager);

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
                    case SAVE -> {
                        state = State.SAVE_LOAD;
                        saveLoadOverlay.show(SaveLoadOverlay.Mode.SAVE);
                    }
                    case RESTART -> ((Main) Gdx.app.getApplicationListener()).setScreen(new ExplorationScreen(null));
                    case SETTINGS -> { state = State.SETTINGS; settingsOverlay.show(); }
                    case EXIT -> ((Main) Gdx.app.getApplicationListener()).setScreen(new HomeScreen());
                }
            }
        } else if (state == State.SETTINGS) {
            if (settingsOverlay.getTransitionProgress() >= 1f) settingsOverlay.handleInput(viewport);
            if (!settingsOverlay.isOverlayVisible() && settingsOverlay.getTransitionProgress() <= 0f) state = State.PAUSE;
        } else if (state == State.SAVE_LOAD) {
            SaveLoadOverlay.Result res = saveLoadOverlay.handleInput(viewport);
            if (res != null) {
                if (res.action == SaveLoadOverlay.ResultAction.LOAD) {
                    ((Main) Gdx.app.getApplicationListener()).setScreen(new LoadingScreen(res.saveName));
                } else if (res.action == SaveLoadOverlay.ResultAction.SAVE) {
                    SaveManager.saveGame(world, res.saveName);
                    state = State.GAME; // Auto resume on successful save
                }
            } else if (!saveLoadOverlay.isVisible()) {
                state = State.PAUSE;
            }
        } else if (state == State.GAME_OVER) {
            GameOverOverlay.Action action = gameOverOverlay.handleInput(viewport);
            switch (action) {
                case RETRY -> ((Main) Gdx.app.getApplicationListener()).setScreen(new ExplorationScreen(null));
                case MAIN_MENU -> ((Main) Gdx.app.getApplicationListener()).setScreen(new HomeScreen());
            }
        }
    }

    private void updateGameLogic(float delta) {
        if (state != State.GAME) {
            // Need to update lighting even if paused so the FBO is ready for the blur
            world.getLightingManager().updateLightFbo(camera, renderer.getShape());
            return;
        }

        float gameDelta = delta;
        boolean playerIsDead = !world.getPlayer().isAlive();

        if (playerIsDead) {
            gameDelta *= 0.3f; // Slow motion on death
            if (world.getPlayer().isDeathAnimationFinished()) state = State.GAME_OVER;
        }

        world.update(gameDelta, camera, renderer.getShape());

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
            renderer.render(offsetX, offsetY); // Render the game first

            // Darken background (now drawn into FBO to be blurred)
            Gdx.gl.glEnable(GL20.GL_BLEND);
            ShapeRenderer shape = renderer.getShape();
            shape.setProjectionMatrix(camera.combined);
            shape.begin(ShapeRenderer.ShapeType.Filled);
            if (state == State.GAME_OVER) {
                shape.setColor(0.5f, 0, 0, 0.65f); // Red tint for Game Over
            } else {
                shape.setColor(0, 0, 0, 0.5f); // General darkening for overlays
            }
            shape.rect(camera.position.x - camera.viewportWidth / 2f, camera.position.y - camera.viewportHeight / 2f, camera.viewportWidth, camera.viewportHeight);
            shape.end();
            Gdx.gl.glDisable(GL20.GL_BLEND); // Disable blend after drawing shape

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

            // Render Overlays
            renderUIOverlays();
        } else {
            // Not paused, just render normally
            world.getLightingManager().updateLightFbo(camera, renderer.getShape());
            renderer.render(offsetX, offsetY);
        }

        // Render debug overlay last, on top of everything, without needing alpha blend overrides
        if (state == State.GAME) {
            debugOverlay.render(renderer.getShape(), renderer.getBatch(), renderer.getFont(), camera, world, mapManager);
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

        if (state == State.PAUSE || (state == State.SETTINGS && settingsOverlay.getTransitionProgress() < 1f)) {
            float alpha = 1f;
            if (state == State.SETTINGS) alpha = 1f - settingsOverlay.getTransitionProgress();
            pauseOverlay.render(shape, batch, font, viewport, alpha);
        }
        settingsOverlay.render(shape, batch, font, viewport);
        if (state == State.SAVE_LOAD) saveLoadOverlay.render(shape, batch, font, viewport);
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
