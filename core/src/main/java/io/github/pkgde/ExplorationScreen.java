package io.github.pkgde;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.viewport.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import io.github.pkgde.quiz.*;

public class ExplorationScreen implements Screen {

    private static final String AUTO_SAVE_SLOT_NAME = "auto_save";
    private final float AUTO_SAVE_INTERVAL = 10f;

    private OrthographicCamera camera;
    private Viewport viewport;
    private Viewport uiViewport;

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

    public enum State { GAME, INVENTORY, PAUSE, SETTINGS, GAME_OVER, SAVE_LOAD, ANSWERING_QUESTION }
    private State state = State.GAME;

    private Stage gameStage;
    private Stage quizStage;
    private Stage pauseStage;
    private QuizUIStyles quizUIStyles;
    private QuizController quizController;
    private QuestionProvider questionProvider;

    private float shakeTime = 0f;
    private final float shakeDuration = 0.25f;
    private boolean shakeTriggered = false;
    private DoorEntity lastTriggeredDoor = null;
    private State previousState = null;

    private DialogueOverlay dialogueOverlay;

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
        dialogueOverlay = new DialogueOverlay();

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
        fbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        blurShader = BlurShader.createShader(true);
        blurBatch = new SpriteBatch();
        blurBatch.setShader(blurShader);

        // Create a standard 720p viewport purely for UI consistency
        uiViewport = new FitViewport(1280, 720);

        quizUIStyles = new QuizUIStyles(renderer.getFont());
        questionProvider = new HardcodedQuestionProvider();
        quizController = new QuizController(quizUIStyles, questionProvider);

        gameStage = new Stage(uiViewport, renderer.getBatch());
        quizStage = new Stage(uiViewport, renderer.getBatch());
        pauseStage = new Stage(uiViewport, renderer.getBatch());

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
            if (nextPath.equals("Maps/final_map.ldtk") && mapManager.getCurrentLevelIndex() >= 6) {
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
                nextPath = "Maps/final_map.ldtk";
                nextLevelIndex = 0;
            }

            SaveManager.saveLevelTransition(world, "checkpoint", nextPath, nextLevelIndex);
            ((Main) Gdx.app.getApplicationListener()).setScreen(new LoadingScreen("checkpoint"));
            this.dispose();
            return;
        }

        // --- CHECK FOR DOOR QUIZ ---
        if (state == State.GAME) {
            boolean overlappingAnyDoor = false;
            for (com.badlogic.gdx.math.Rectangle exit : mapManager.getExitGateRects()) {
                if (world.getPlayer().getBounds().overlaps(exit)) {
                    overlappingAnyDoor = true;
                    DoorEntity door = world.getDoorAt(exit);
                    if (door != null && door.isLocked()) {
                        // Re-entry guard
                        if (door == lastTriggeredDoor) continue;
                        if (quizController.isActive()) continue;

                        // NEW LOGIC: Check if any enemies are still alive
                        boolean enemiesAlive = false;
                        for (Enemy e : world.getEnemies()) {
                            if (e.isAlive()) {
                                enemiesAlive = true;
                                break;
                            }
                        }

                        // If enemies are alive, deny entry and warn the player
                        if (enemiesAlive) {
                            world.getPlayer().getPosition().add(
                                world.getPlayer().getBounds().x > exit.x ? 5f : -5f,
                                world.getPlayer().getBounds().y > exit.y ? 5f : -5f
                            );
                            lastTriggeredDoor = door;

                            java.util.List<DialogueOverlay.DialogueNode> nodes = new java.util.ArrayList<>();
                            nodes.add(new DialogueOverlay.DialogueNode("SYSTEM", "The door is sealed. You must defeat all enemies first!", Color.RED));
                            dialogueOverlay.setTop(true);
                            // Ensure the lock is false so they can dismiss the warning
                            dialogueOverlay.setInputLocked(false); 
                            dialogueOverlay.start(nodes, null);
                            break; // Stop processing the door
                        }

                        // If enemies are dead, proceed to the quiz as normal!
                        world.getPlayer().getPosition().add(
                            world.getPlayer().getBounds().x > exit.x ? 2f : -2f,
                            world.getPlayer().getBounds().y > exit.y ? 2f : -2f
                        );

                        state = State.ANSWERING_QUESTION;
                        world.setQuizBlocked(true);
                        lastTriggeredDoor = door;

                        QuizContext ctx = new QuizContext(QuizContext.Source.DOOR, true);

                        QuestionPresenter presenter = text -> {
                            java.util.List<DialogueOverlay.DialogueNode> nodes = new java.util.ArrayList<>();
                            nodes.add(new DialogueOverlay.DialogueNode("SYSTEM", text, Color.YELLOW));
                            dialogueOverlay.setTop(true);
                            dialogueOverlay.setInputLocked(true); 
                            dialogueOverlay.start(nodes, null);
                        };

                        quizController.startQuiz(ctx, presenter, quizStage, isCorrect -> {
                            dialogueOverlay.setInputLocked(false);
                            dialogueOverlay.hide();

                            world.setQuizBlocked(false);
                            dialogueOverlay.setTop(false); 
                            
                            if (isCorrect) {
                                door.unlock();
                            } else {
                                world.getPlayer().setHealth(0f); // Kill player on wrong answer
                            }
                            
                            state = State.GAME;
                        });
                        break;
                    }
                }
            }
            if (!overlappingAnyDoor) {
                lastTriggeredDoor = null; 
            }
        }

        if (world.isBossFightTriggered()) {
            world.setBossFightTriggered(false);
            world.getEnemies().removeIf(io.github.pkgde.Enemy::isBoss);
            boolean hasExit = !mapManager.getExitGateRects().isEmpty();
            ((Main) Gdx.app.getApplicationListener()).setScreen(new BossFightScreen(world.getPlayer(), "THE DEMONIC MONK", hasExit, this));
            // We do NOT dispose ExplorationScreen here so we can return to it if hasExit is true
            return;
        }
    }

    private void handleStateInput() {
        if (debugOverlay.handleInput()) return;
        debugOverlay.handleCheats(camera, world, mapManager);

        // --- Input Routing ---
        if (state == State.PAUSE || state == State.SETTINGS || state == State.SAVE_LOAD) {
            Gdx.input.setInputProcessor(pauseStage); 
        } else if (state == State.ANSWERING_QUESTION) {
            Gdx.input.setInputProcessor(quizStage);
        } else {
            Gdx.input.setInputProcessor(null); 
        }

        if (state == State.GAME || state == State.ANSWERING_QUESTION) {
            InputHandler.Action action = input.handle();
            switch (action) {
                case TOGGLE_PAUSE -> {
                    previousState = state;
                    state = State.PAUSE;
                }
                case TOGGLE_INVENTORY -> {
                    if (state == State.GAME) state = State.INVENTORY;
                }
                case OPEN_SETTINGS -> {
                    previousState = state;
                    state = State.SETTINGS;
                    if (previousState == State.ANSWERING_QUESTION) {
                        Gdx.input.setInputProcessor(null);
                    }
                    settingsOverlay.show();
                }
                case EXIT_TO_MENU -> {
                    ((Main) Gdx.app.getApplicationListener()).setScreen(new HomeScreen());
                    this.dispose();
                }
            }
            
            if (state == State.ANSWERING_QUESTION) {
                // Quiz-specific input locking is handled by Gdx.input.setInputProcessor(quizStage)
                // but we still check for ESC cancel if needed
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                    quizController.cancelQuiz(quizStage);
                    world.setQuizBlocked(false);
                    lastTriggeredDoor = null;
                    state = State.GAME;
                }
            }
        } else if (state == State.INVENTORY) {
            inventoryOverlay.handleInput();
            if (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                state = State.GAME;
            }
        } else if (state == State.PAUSE) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                state = (previousState != null) ? previousState : State.GAME;
                previousState = null;
            } else {
                PauseOverlay.Action action = pauseOverlay.handleInput(uiViewport);
                switch (action) {
                    case RESUME -> {
                        state = (previousState != null) ? previousState : State.GAME;
                        previousState = null;
                    }
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
            if (settingsOverlay.getTransitionProgress() >= 1f) settingsOverlay.handleInput(uiViewport);
            if (!settingsOverlay.isOverlayVisible() && settingsOverlay.getTransitionProgress() <= 0f) state = State.PAUSE;
        } else if (state == State.SAVE_LOAD) {
            SaveLoadOverlay.Result res = saveLoadOverlay.handleInput(uiViewport);
            if (res != null) {
                if (res.action == SaveLoadOverlay.ResultAction.LOAD) {
                    ((Main) Gdx.app.getApplicationListener()).setScreen(new LoadingScreen(res.saveName));
                    this.dispose();
                } else if (res.action == SaveLoadOverlay.ResultAction.SAVE) {
                    SaveManager.saveGame(world, res.saveName);
                    state = (previousState != null) ? previousState : State.GAME;
                    previousState = null;
                }
            } else if (!saveLoadOverlay.isVisible()) {
                state = State.PAUSE;
            }
        } else if (state == State.GAME_OVER) {
            GameOverOverlay.Action action = gameOverOverlay.handleInput(uiViewport);
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
        if (state == State.ANSWERING_QUESTION) {
            quizStage.act(delta);
            quizController.update(quizStage);
            if (dialogueOverlay.isActive()) dialogueOverlay.update(delta);
        }

        if (state != State.GAME && state != State.ANSWERING_QUESTION) {
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

        uiViewport.apply();
        batch.setProjectionMatrix(uiViewport.getCamera().combined);
        shape.setProjectionMatrix(uiViewport.getCamera().combined);

        // 1. Game Stage (HUD)
        gameStage.draw();

        if (state == State.ANSWERING_QUESTION || (previousState == State.ANSWERING_QUESTION && (state == State.PAUSE || state == State.SETTINGS))) {
            if (dialogueOverlay.isActive()) dialogueOverlay.render(batch, font, uiViewport);
            quizStage.draw();
        }

        if (state == State.PAUSE || (state == State.SETTINGS && settingsOverlay.getTransitionProgress() < 1f)) {
            float alpha = 1f;
            if (state == State.SETTINGS) alpha = 1f - settingsOverlay.getTransitionProgress();
            pauseOverlay.render(shape, batch, font, uiViewport, alpha);
        }
        settingsOverlay.render(shape, batch, font, uiViewport);
        if (state == State.SAVE_LOAD) saveLoadOverlay.render(shape, batch, font, uiViewport);
        if (state == State.INVENTORY) inventoryOverlay.render(shape, batch, font, uiViewport, world.getPlayer());
        if (state == State.GAME_OVER) gameOverOverlay.render(shape, batch, font, uiViewport);

        // 4. Pause Stage
        pauseStage.draw();

        Gdx.gl.glDisable(GL20.GL_BLEND);


    }

    @Override
    public void resize(int w, int h) {
        viewport.update(w, h, true);
        
        // FIX: Ensure the UI viewport is also updated when the screen resizes
        if (uiViewport != null) {
            uiViewport.update(w, h, true);
        }
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
        if (inventoryOverlay != null) inventoryOverlay.dispose();
        if (dialogueOverlay != null) dialogueOverlay.dispose();
        if (quizController != null && quizStage != null) {
            quizController.cancelQuiz(quizStage);
        }
        if (quizStage != null) quizStage.dispose();
        if (gameStage != null) gameStage.dispose();
        if (pauseStage != null) pauseStage.dispose();
        if (quizUIStyles != null) quizUIStyles.dispose();
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
