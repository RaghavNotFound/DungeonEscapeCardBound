package io.github.pkgde;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.*;

public class BossFightScreen implements Screen {

    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final SpriteBatch batch;
    private final ShapeRenderer shape;
    private final BitmapFont font;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    private final Player player;
    private final String bossName;
    private Texture background;

    private final GameOverOverlay gameOverOverlay;

    private Animation<TextureRegion> playerIdle, playerHurt;
    private Animation<TextureRegion> bossIdle, bossHurt;
    private float stateTime = 0f;
    private float playerHurtTimer = 0f;
    private float bossHurtTimer = 0f;

    private float playerMaxHealth;
    private float playerHealth;
    private int playerEnergy = 3;
    private int playerMaxEnergy = 3;

    private float bossMaxHealth = 300f;
    private float bossHealth = 300f;
    private int bossEnergy = 3;
    private int bossMaxEnergy = 3;

    private enum TurnState { PLAYER_TURN, BOSS_THINKING, VICTORY, GAME_OVER }
    private TurnState turnState = TurnState.PLAYER_TURN;
    private float bossThinkTimer = 0f;

    private String combatLog = "Boss Fight Started!";

    public BossFightScreen(Player player, String bossName) {
        this.player = player;
        this.bossName = bossName;
        this.playerMaxHealth = player.getMaxHealth();
        this.playerHealth = player.getHealth();

        camera = new OrthographicCamera();
        viewport = new FitViewport(GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT, camera);
        viewport.apply();
        camera.position.set(GameWorld.WORLD_WIDTH / 2f, GameWorld.WORLD_HEIGHT / 2f, 0);

        batch = new SpriteBatch();
        shape = new ShapeRenderer();
        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        gameOverOverlay = new GameOverOverlay();

        loadAnimations();

        // FIXED: Removed "assets/" so it works perfectly in compiled JARs too!
        try {
            background = new Texture(Gdx.files.internal("BossScreen/BossScreen.png"));
            background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            Gdx.app.log("BossFightScreen", "Background image not found. Using solid color fallback.");
        }
    }

    private void loadAnimations() {
        Array<TextureRegion> pIdle = new Array<>();
        for(int i = 1; i <= 18; i++) pIdle.add(new TextureRegion(Main.assets.get("Movements/Player/idle/idle_" + i + ".png", Texture.class)));
        playerIdle = new Animation<>(0.08f, pIdle, Animation.PlayMode.LOOP);

        Array<TextureRegion> pHurt = new Array<>();
        for(int i = 1; i <= 12; i++) pHurt.add(new TextureRegion(Main.assets.get("Movements/Player/hurt/hurt_" + i + ".png", Texture.class)));
        playerHurt = new Animation<>(0.05f, pHurt, Animation.PlayMode.NORMAL);

        Array<TextureRegion> bIdle = new Array<>();
        for(int i = 0; ; i++) {
            String path = "Movements/Enemy/Left/Idle/Left - Idle_" + String.format("%03d", i) + ".png";
            if (!Main.assets.isLoaded(path)) break;
            bIdle.add(new TextureRegion(Main.assets.get(path, Texture.class)));
        }
        bossIdle = new Animation<>(0.09f, bIdle, Animation.PlayMode.LOOP);

        Array<TextureRegion> bHurt = new Array<>();
        for(int i = 0; ; i++) {
            String path = "Movements/Enemy/Left/Hurt/Left - Hurt_" + String.format("%03d", i) + ".png";
            if (!Main.assets.isLoaded(path)) break;
            bHurt.add(new TextureRegion(Main.assets.get(path, Texture.class)));
        }
        bossHurt = new Animation<>(0.05f, bHurt, Animation.PlayMode.NORMAL);
    }

    @Override
    public void render(float delta) {
        stateTime += delta;

        handleTurnLogic(delta);
        handleInput();

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shape.setProjectionMatrix(camera.combined);

        batch.begin();
        if (background != null) {
            batch.draw(background, 0, 0, GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT);
        }
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.05f, 0.05f, 0.05f, 0.85f);
        shape.rect(0, 0, GameWorld.WORLD_WIDTH, 220);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        TextureRegion pFrame;
        if (playerHurtTimer > 0) {
            pFrame = playerHurt.getKeyFrame(playerHurt.getAnimationDuration() - playerHurtTimer, false);
        } else {
            pFrame = playerIdle.getKeyFrame(stateTime, true);
        }
        batch.draw(pFrame, 150, 250, 384, 384);

        TextureRegion bFrame;
        if (bossHurtTimer > 0) {
            bFrame = bossHurt.getKeyFrame(bossHurt.getAnimationDuration() - bossHurtTimer, false);
        } else {
            bFrame = bossIdle.getKeyFrame(stateTime, true);
        }
        batch.draw(bFrame, 750, 250, 384, 384);

        font.getData().setScale(3.5f);
        glyphLayout.setText(font, bossName);
        float titleX = (GameWorld.WORLD_WIDTH - glyphLayout.width) / 2f;
        float titleY = GameWorld.WORLD_HEIGHT - 40f;

        font.setColor(0f, 0f, 0f, 0.8f);
        font.draw(batch, glyphLayout, titleX + 4f, titleY - 4f);
        font.setColor(new Color(1f, 0.3f, 0.3f, 1f));
        font.draw(batch, glyphLayout, titleX, titleY);

        font.getData().setScale(1.5f);
        font.setColor(Color.WHITE);

        font.draw(batch, "PLAYER", 200, 180);
        font.draw(batch, "HP: " + (int)playerHealth + " / " + (int)playerMaxHealth, 200, 140);
        font.draw(batch, "Energy: " + playerEnergy + " / " + playerMaxEnergy, 200, 110);

        font.draw(batch, bossName, 900, 180);
        font.draw(batch, "HP: " + (int)bossHealth + " / " + (int)bossMaxHealth, 900, 140);
        font.draw(batch, "Energy: " + bossEnergy + " / " + bossMaxEnergy, 900, 110);

        glyphLayout.setText(font, combatLog);
        font.draw(batch, glyphLayout, (GameWorld.WORLD_WIDTH - glyphLayout.width) / 2f, 180);

        if (turnState == TurnState.PLAYER_TURN) {
            font.draw(batch, "Press 1 to Attack (1 Energy)", 500, 100);
            font.draw(batch, "Press SPACE to End Turn", 500, 60);
        } else if (turnState == TurnState.VICTORY) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "Press ENTER to Continue", 500, 100);
            font.setColor(Color.WHITE);
        }

        batch.end();

        if (turnState == TurnState.GAME_OVER) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(0.5f, 0, 0, 0.65f);
            shape.rect(0, 0, GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT);
            shape.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            gameOverOverlay.render(shape, batch, font, viewport);
        }
    }

    private void handleTurnLogic(float delta) {
        if (playerHurtTimer > 0) playerHurtTimer -= delta;
        if (bossHurtTimer > 0) bossHurtTimer -= delta;

        if (bossHealth <= 0 && turnState != TurnState.VICTORY && turnState != TurnState.GAME_OVER) {
            combatLog = "You defeated " + bossName + "!";
            turnState = TurnState.VICTORY;
        } else if (playerHealth <= 0 && turnState != TurnState.GAME_OVER && turnState != TurnState.VICTORY) {
            combatLog = "You Died!";
            turnState = TurnState.GAME_OVER;
        }

        if (turnState == TurnState.BOSS_THINKING) {
            bossThinkTimer -= delta;
            if (bossThinkTimer <= 0) {
                executeBossTurn();
            }
        }
    }

    private void handleInput() {
        if (turnState == TurnState.GAME_OVER) {
            GameOverOverlay.Action action = gameOverOverlay.handleInput(viewport);
            if (action == GameOverOverlay.Action.RETRY) {
                ((Main) Gdx.app.getApplicationListener()).setScreen(new ExplorationScreen(null));
                this.dispose();
            } else if (action == GameOverOverlay.Action.MAIN_MENU) {
                ((Main) Gdx.app.getApplicationListener()).setScreen(new HomeScreen());
                this.dispose();
            }
            return;
        }

        if (turnState == TurnState.VICTORY) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                ((Main) Gdx.app.getApplicationListener()).setScreen(new ExplorationScreen("checkpoint"));
                this.dispose();
            }
            return;
        }

        if (turnState == TurnState.PLAYER_TURN) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                if (playerEnergy >= 1) {
                    playerEnergy -= 1;
                    bossHealth -= 20;
                    bossHurtTimer = bossHurt.getAnimationDuration();
                    combatLog = "Player used Attack! Boss takes 20 dmg.";
                } else {
                    combatLog = "Not enough energy!";
                }
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                turnState = TurnState.BOSS_THINKING;
                bossThinkTimer = 1.5f;
                bossEnergy = bossMaxEnergy;
                combatLog = "Player ended turn. Boss is thinking...";
            }
        }
    }

    private void executeBossTurn() {
        bossEnergy -= 2;
        playerHealth -= 15;
        playerHurtTimer = playerHurt.getAnimationDuration();
        combatLog = bossName + " used Smash! Player takes 15 dmg.";

        if (playerHealth > 0) {
            turnState = TurnState.PLAYER_TURN;
            playerEnergy = playerMaxEnergy;
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        shape.dispose();
        font.dispose();
        if (background != null) {
            background.dispose();
        }
    }
}
