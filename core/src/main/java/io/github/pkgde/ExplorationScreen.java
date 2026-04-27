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

    private static final String AUTO_SAVE_SLOT_NAME = "auto_save";
    private final float AUTO_SAVE_INTERVAL = 10f;

    private OrthographicCamera camera;
    private Viewport viewport;

    private MapManager mapManager;
    private GameWorld world;
    private GameRenderer renderer;
    private InputHandler input;

    private PauseOverlay pauseOverlay;
    private SettingsOverlay settingsOverlay;
    private InventoryOverlay inventoryOverlay;
    private GameOverOverlay gameOverOverlay;
    private SaveLoadOverlay saveLoadOverlay;
    private DebugOverlay debugOverlay;

    public enum State { GAME, INVENTORY, PAUSE, SETTINGS, GAME_OVER, SAVE_LOAD }
    private State state = State.GAME;

    private float shakeTime = 0f;
    private final float shakeDuration = 0.25f;
    private boolean shakeTriggered = false;

    private FrameBuffer fbo;
    private ShaderProgram blurShader;
    private SpriteBatch blurBatch;

    // FIXED: Reads the save file first to figure out exactly what map and level to load!
    public ExplorationScreen(String saveFileToLoad) {
        String mapPath = "Maps/tutorial.ldtk";
        int levelIndex = 0;

        if (saveFileToLoad != null) {
            SaveState s = SaveManager.peekSave(saveFileToLoad);
            if (s != null) {
                if (Gdx.files.internal(s.currentMapPath).exists()) {
                    mapPath = s.currentMapPath;
                    levelIndex = s.currentLevelIndex;
                } else {
                    System.err.println("Warning: Saved map '" + s.currentMapPath + "' not found. Falling back to default.");
                    mapPath = "Maps/tutorial.ldtk";
                    levelIndex = 0;
                    saveFileToLoad = null; // Prevent loading invalid entity states
                }
            }
        }
        init(saveFileToLoad, mapPath, levelIndex);
    }

    private void init(String saveFileToLoad, String mapPath, int levelIndex) {
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
        } else {
            world.getPlayer().setPosition(mapManager.getPlayerSpawn().x, mapManager.getPlayerSpawn().y);
        }
    }

    @Override
    public void render(float delta) {
        viewport.apply();
        settingsOverlay.update(delta);

        Screen currentScreen = ((Game) Gdx.app.getApplicationListener()).getScreen();

        handleStateInput();
        if (((Game) Gdx.app.getApplicationListener()).getScreen() != currentScreen) return;

        updateGameLogic(delta);
        if (((Game) Gdx.app.getApplicationListener()).getScreen() != currentScreen) return;

        draw(delta);

        // FIXED: Transitioning through LoadingScreen prevents the "Blue Spinner Freeze" crash!
        if (world.isLevelComplete() && state == State.GAME) {
            int nextLevelIndex = mapManager.getCurrentLevelIndex() + 1;
            String nextPath = mapManager.getCurrentMapPath();

            // Victory: completed the last level of map.ldtk (index 6)
            if (nextPath.equals("Maps/map.ldtk") && mapManager.getCurrentLevelIndex() >= 6) {
                ((Main) Gdx.app.getApplicationListener()).setScreen(new VictoryScreen(
                    world.getPlayer().getEnemiesKilled(),
                    world.getPlayer().getTorchCount(),
                    world.getPlayer().getTimeSurvived()
                ));
                this.dispose();
                return;
            }

            // Transition from tutorial to map after all 6 tutorial levels (indices 0-5)
            if (nextPath.equals("Maps/tutorial.ldtk") && nextLevelIndex >= 6) {
                nextPath = "Maps/map.ldtk";
                nextLevelIndex = 0;
            }

            SaveManager.saveLevelTransition(world, "checkpoint", nextPath, nextLevelIndex);
            ((Main) Gdx.app.getApplicationListener()).setScreen(new LoadingScreen("checkpoint"));
            this.dispose();
            return;
        }

        if (world.isBossFightTriggered()) {
            ((Main) Gdx.app.getApplicationListener()).setScreen(new BossFightScreen(world.getPlayer(), "THE DEMONIC MONK"));
            this.dispose();
            return;
        }
    }

    private void handleStateInput() {
        if (debugOverlay.handleInput()) return;
        debugOverlay.handleCheats(camera, world, mapManager);

        if (state == State.GAME) {
            InputHandler.Action action = input.handle();
            switch (action) {
                case TOGGLE_PAUSE -> state = State.PAUSE;
                case TOGGLE_INVENTORY -> state = State.INVENTORY;
                case OPEN_SETTINGS -> { state = State.SETTINGS; settingsOverlay.show(); }
                case EXIT_TO_MENU -> {
                    ((Main) Gdx.app.getApplicationListener()).setScreen(new HomeScreen());
                    this.dispose();
                }
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
                    case SAVE -> { state = State.SAVE_LOAD; saveLoadOverlay.show(SaveLoadOverlay.Mode.SAVE); }
                    case RESTART -> {
                        ((Main) Gdx.app.getApplicationListener()).setScreen(new LoadingScreen(null)); // Changed to LoadingScreen
                        this.dispose();
                    }
                    case SETTINGS -> { state = State.SETTINGS; settingsOverlay.show(); }
                    case EXIT -> {
                        ((Main) Gdx.app.getApplicationListener()).setScreen(new HomeScreen());
                        this.dispose();
                    }
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
                    this.dispose();
                } else if (res.action == SaveLoadOverlay.ResultAction.SAVE) {
                    SaveManager.saveGame(world, res.saveName);
                    state = State.GAME;
                }
            } else if (!saveLoadOverlay.isVisible()) {
                state = State.PAUSE;
            }
        } else if (state == State.GAME_OVER) {
            GameOverOverlay.Action action = gameOverOverlay.handleInput(viewport);
            switch (action) {
                case RETRY -> {
                    ((Main) Gdx.app.getApplicationListener()).setScreen(new LoadingScreen(null)); // Changed to LoadingScreen
                    this.dispose();
                }
                case MAIN_MENU -> {
                    ((Main) Gdx.app.getApplicationListener()).setScreen(new HomeScreen());
                    this.dispose();
                }
            }
        }
    }

    private void updateGameLogic(float delta) {
        if (state != State.GAME) {
            world.getLightingManager().updateLightFbo(camera, renderer.getShape());
            return;
        }

        float gameDelta = delta;
        boolean playerIsDead = !world.getPlayer().isAlive();

        if (playerIsDead) {
            gameDelta *= 0.3f;
            if (world.getPlayer().isDeathAnimationFinished()) state = State.GAME_OVER;
        }

        world.update(gameDelta, camera, renderer.getShape());

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
            fbo.begin();
            renderer.render(offsetX, offsetY);

            Gdx.gl.glEnable(GL20.GL_BLEND);
            ShapeRenderer shape = renderer.getShape();
            shape.setProjectionMatrix(camera.combined);
            shape.begin(ShapeRenderer.ShapeType.Filled);
            if (state == State.GAME_OVER) shape.setColor(0.5f, 0, 0, 0.65f);
            else shape.setColor(0, 0, 0, 0.5f);

            shape.rect(camera.position.x - camera.viewportWidth / 2f, camera.position.y - camera.viewportHeight / 2f, camera.viewportWidth, camera.viewportHeight);
            shape.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
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

            renderUIOverlays();
        } else {
            world.getLightingManager().updateLightFbo(camera, renderer.getShape());
            renderer.render(offsetX, offsetY);
        }

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
        if (w == 0 || h == 0) return; // Prevent crash when minimized
        if (fbo != null) fbo.dispose();
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        fbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        if (world != null && world.getLightingManager() != null) {
            world.getLightingManager().resize(w, h);
        }
    }

    @Override
    public void dispose() {
        if(renderer != null) renderer.dispose();
        if(world != null) world.dispose();
        if(mapManager != null) mapManager.dispose();
        if(fbo != null) fbo.dispose();
        if(blurBatch != null) blurBatch.dispose();
        if(blurShader != null) blurShader.dispose();
        if(inventoryOverlay != null) inventoryOverlay.dispose();
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
