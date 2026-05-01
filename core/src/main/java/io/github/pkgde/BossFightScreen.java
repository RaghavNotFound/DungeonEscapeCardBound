package io.github.pkgde;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.*;
import com.badlogic.gdx.scenes.scene2d.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BossFightScreen implements Screen {

    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final SpriteBatch batch;
    private final ShapeRenderer shape;
    private final BitmapFont font;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    private final Player player;
    private final String bossName;
    private final boolean hasExit;
    private final Screen previousScreen;
    private Texture background;

    private final GameOverOverlay gameOverOverlay;
    private final DialogueOverlay dialogueOverlay;
    private PauseOverlay pauseOverlay;
    private SettingsOverlay settingsOverlay;

    public enum State { GAME, PAUSE, SETTINGS }
    private State state = State.GAME;

    // Animations
    private Animation<TextureRegion> playerIdle, playerHurt;
    private Animation<TextureRegion> bossIdle, bossHurt, bossAttack;
    private float stateTime = 0f;
    private float playerHurtTimer = 0f;
    private float bossHurtTimer = 0f;
    private float bossAttackTimer = 0f;

    // Stats
    private float playerMaxHealth;
    private float playerHealth;
    private int playerBlock = 0;
    private int playerEnergy = 3;
    private int playerMaxEnergy = 3;

    private float bossMaxHealth = 300f;
    private float bossHealth = 300f;

    // Card System
    public enum CardType { ATTACK, DEFEND, HEAL, BOSS_ATTACK, BOSS_ULTIMATE }

    public class Card {
        String name;
        Texture texture;
        int cost;
        int value;
        CardType type;
        Rectangle bounds = new Rectangle();

        public Card(String name, String texturePath, int cost, int value, CardType type) {
            this.name = name;
            try {
                this.texture = new Texture(Gdx.files.internal(texturePath));
                this.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            } catch (Exception e) {
                Gdx.app.error("Cards", "Could not load card texture: " + texturePath);
            }
            this.cost = cost;
            this.value = value;
            this.type = type;
        }
    }

    private List<Card> drawPile = new ArrayList<>();
    private List<Card> hand = new ArrayList<>();
    private List<Card> discardPile = new ArrayList<>();

    private List<Card> bossDeck = new ArrayList<>();
    private Card activeBossCard = null;

    private enum TurnState { DIALOGUE, PLAYER_TURN, BOSS_THINKING, BOSS_FLASHING_CARD, VICTORY, GAME_OVER, ANSWERING_QUESTION }
    private TurnState turnState = TurnState.DIALOGUE;

    private Stage quizStage;
    private io.github.pkgde.quiz.QuizUIStyles quizUIStyles;
    private io.github.pkgde.quiz.CardQuizHandler cardQuizHandler;
    private Card pendingCard = null;
    private io.github.pkgde.quiz.QuestionPresenter questionPresenter;

    private float bossThinkTimer = 0f;
    private float bossFlashTimer = 0f;

    private String combatLog = "Boss Fight Started!";
    private final Vector3 touch = new Vector3();

    public BossFightScreen(Player player, String bossName, boolean hasExit, Screen previousScreen) {
        this.player = player;
        this.bossName = bossName;
        this.hasExit = hasExit;
        this.previousScreen = previousScreen;
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
        pauseOverlay = new PauseOverlay();
        settingsOverlay = new SettingsOverlay();
        dialogueOverlay = new DialogueOverlay();

        quizStage = new Stage(viewport, batch);
        quizUIStyles = new io.github.pkgde.quiz.QuizUIStyles(font);
        cardQuizHandler = new io.github.pkgde.quiz.CardQuizHandler(quizUIStyles);

        questionPresenter = new io.github.pkgde.quiz.QuestionPresenter() {
            @Override
            public void showQuestion(String text) {
                // Dialogue box placeholder
                combatLog = "Question: " + text;
            }
        };

        loadAnimations();
        loadCards();
        buildIntroDialogue();

        try {
            background = new Texture(Gdx.files.internal("BossScreen/BossScreen.png"));
            background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {}
    }

    private void loadCards() {
        // Player Deck Build (Load textures dynamically to avoid LoadingScreen crashes)
        for(int i=0; i<4; i++) drawPile.add(new Card("Strike", "Player_Cards/strike.png", 1, 15, CardType.ATTACK));
        for(int i=0; i<3; i++) drawPile.add(new Card("Defend", "Player_Cards/defend.png", 1, 10, CardType.DEFEND));
        drawPile.add(new Card("Heavy Strike", "Player_Cards/strike+.png", 2, 35, CardType.ATTACK));
        drawPile.add(new Card("Heal", "Player_Cards/heal.png", 1, 20, CardType.HEAL));
        Collections.shuffle(drawPile);

        // Boss Deck Build
        bossDeck.add(new Card("Physical Attack", "Boss_Cards/Boss_phy.png", 0, 15, CardType.BOSS_ATTACK));
        bossDeck.add(new Card("Life Drain", "Boss_Cards/Boss_drain.png", 0, 12, CardType.BOSS_ATTACK)); // Will code custom drain logic
        bossDeck.add(new Card("Ultimate", "Boss_Cards/Boss_Ulti.png", 0, 45, CardType.BOSS_ULTIMATE));
    }

    private void startPlayerTurn() {
        turnState = TurnState.PLAYER_TURN;
        playerEnergy = playerMaxEnergy;
        playerBlock = 0; // Block resets every turn like Slay the Spire

        // Discard old hand
        discardPile.addAll(hand);
        hand.clear();

        // Draw 3 cards
        for (int i = 0; i < 3; i++) {
            if (drawPile.isEmpty()) {
                drawPile.addAll(discardPile);
                discardPile.clear();
                Collections.shuffle(drawPile);
            }
            if (!drawPile.isEmpty()) {
                hand.add(drawPile.remove(0));
            }
        }
        combatLog = "Your turn! Play your cards.";
    }

    @Override
    public void render(float delta) {
        Screen currentScreen = ((Game) Gdx.app.getApplicationListener()).getScreen();

        settingsOverlay.update(delta);
        handleStateInput();

        if (((Game) Gdx.app.getApplicationListener()).getScreen() != currentScreen) return;

        if (state == State.GAME) {
            stateTime += delta;
            if (turnState == TurnState.DIALOGUE) {
                dialogueOverlay.update(delta);
            } else {
                handleTurnLogic(delta);
            }
        }

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shape.setProjectionMatrix(camera.combined);

        // Background
        batch.begin();
        if (background != null) batch.draw(background, 0, 0, GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT);
        batch.end();

        // Bottom UI Panel
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.05f, 0.05f, 0.08f, 0.9f);
        shape.rect(0, 0, GameWorld.WORLD_WIDTH, 180);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.4f, 0.4f, 0.5f, 0.8f);
        shape.rect(0, 0, GameWorld.WORLD_WIDTH, 180);
        shape.end();

        // Health Bars
        drawHealthBars(shape);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Characters
        batch.begin();
        TextureRegion pFrame = (playerHurtTimer > 0) ? playerHurt.getKeyFrame(playerHurt.getAnimationDuration() - playerHurtTimer, false) : playerIdle.getKeyFrame(stateTime, true);
        batch.draw(pFrame, 180, 180, 380, 380);

        TextureRegion bFrame;
        if (bossHurtTimer > 0) bFrame = bossHurt.getKeyFrame(bossHurt.getAnimationDuration() - bossHurtTimer, false);
        else if (bossAttackTimer > 0) bFrame = bossAttack.getKeyFrame(bossAttack.getAnimationDuration() - bossAttackTimer, false);
        else bFrame = bossIdle.getKeyFrame(stateTime, true);

        // Flip boss to face left
        if (!bFrame.isFlipX()) bFrame.flip(true, false);
        batch.draw(bFrame, 720, 180, 380, 380);

        // Draw Cards in Hand
        if (turnState == TurnState.PLAYER_TURN) {
            float cardW = 120;
            float cardH = 160;
            float startX = (GameWorld.WORLD_WIDTH - (hand.size() * cardW + (hand.size()-1)*20)) / 2f;

            for (int i = 0; i < hand.size(); i++) {
                Card c = hand.get(i);
                float cx = startX + i * (cardW + 20);
                float cy = 10;
                c.bounds.set(cx, cy, cardW, cardH);

                if (c.texture != null) {
                    batch.draw(c.texture, cx, cy, cardW, cardH);
                }

                // Dim card if not enough energy
                if (playerEnergy < c.cost) {
                    batch.end();
                    Gdx.gl.glEnable(GL20.GL_BLEND);
                    shape.begin(ShapeRenderer.ShapeType.Filled);
                    shape.setColor(0f,0f,0f, 0.6f);
                    shape.rect(cx, cy, cardW, cardH);
                    shape.end();
                    Gdx.gl.glDisable(GL20.GL_BLEND);
                    batch.begin();
                }
            }

            font.getData().setScale(1.2f);
            drawTextWithShadow(batch, font, "Press SPACE to End Turn", GameWorld.WORLD_WIDTH - 250, 50, Color.YELLOW);
        }

        // Draw Boss Card Flash
        if (turnState == TurnState.BOSS_FLASHING_CARD && activeBossCard != null && activeBossCard.texture != null) {
            float flashW = 240;
            float flashH = 320;
            batch.draw(activeBossCard.texture, (GameWorld.WORLD_WIDTH - flashW)/2f, (GameWorld.WORLD_HEIGHT - flashH)/2f, flashW, flashH);
        }

        // Text & Stats
        font.getData().setScale(2.8f);
        drawTextWithShadow(batch, font, bossName, (GameWorld.WORLD_WIDTH - getGlyphWidth(font, bossName))/2f, GameWorld.WORLD_HEIGHT - 20, new Color(1f, 0.3f, 0.3f, 1f));

        font.getData().setScale(1.2f);
        String pStats = "HP: " + (int)playerHealth + "  |  Block: " + playerBlock + "  |  Energy: " + playerEnergy + "/" + playerMaxEnergy;
        drawTextWithShadow(batch, font, pStats, 50, 700, Color.WHITE);

        String bStats = "HP: " + (int)bossHealth + "/" + (int)bossMaxHealth;
        drawTextWithShadow(batch, font, bStats, GameWorld.WORLD_WIDTH - 200, 700, Color.WHITE);

        drawTextWithShadow(batch, font, combatLog, (GameWorld.WORLD_WIDTH - getGlyphWidth(font, combatLog))/2f, 210, Color.CYAN);

        if (turnState == TurnState.VICTORY) {
            font.getData().setScale(1.5f);
            drawTextWithShadow(batch, font, "Press ENTER to Continue", (GameWorld.WORLD_WIDTH - getGlyphWidth(font, "Press ENTER to Continue"))/2f, 100, Color.YELLOW);
        }

        batch.end();

        // Dialogue overlay (drawn on top of everything except pause/settings)
        if (turnState == TurnState.DIALOGUE && dialogueOverlay.isActive()) {
            dialogueOverlay.render(batch, font, viewport);
        }

        if (turnState == TurnState.GAME_OVER) gameOverOverlay.render(shape, batch, font, viewport);

        if (state == State.GAME) {
            quizStage.act(delta);
            quizStage.draw();
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        if (state == State.PAUSE || (state == State.SETTINGS && settingsOverlay.getTransitionProgress() < 1f)) {
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(0, 0, 0, 0.7f);
            shape.rect(0, 0, GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT);
            shape.end();

            float alpha = 1f;
            if (state == State.SETTINGS) alpha = 1f - settingsOverlay.getTransitionProgress();
            pauseOverlay.render(shape, batch, font, viewport, alpha);
        }
        settingsOverlay.render(shape, batch, font, viewport);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawHealthBars(ShapeRenderer shape) {
        float pBarW = 300, bBarW = 300, barH = 20;

        // Player Bar
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.2f, 0.2f, 0.2f, 1f);
        shape.rect(50, 660, pBarW, barH);
        shape.setColor(0.2f, 0.8f, 0.2f, 1f);
        shape.rect(50, 660, (playerHealth / playerMaxHealth) * pBarW, barH);

        // Player Block (Blue overlay)
        if (playerBlock > 0) {
            shape.setColor(0.2f, 0.6f, 1f, 1f);
            shape.rect(50, 660, Math.min(1f, (float)playerBlock / playerMaxHealth) * pBarW, barH/2f);
        }

        // Boss Bar
        shape.setColor(0.2f, 0.2f, 0.2f, 1f);
        shape.rect(GameWorld.WORLD_WIDTH - bBarW - 50, 660, bBarW, barH);
        shape.setColor(0.8f, 0.2f, 0.2f, 1f);
        shape.rect(GameWorld.WORLD_WIDTH - bBarW - 50, 660, (bossHealth / bossMaxHealth) * bBarW, barH);
        shape.end();
    }

    private void drawTextWithShadow(SpriteBatch batch, BitmapFont font, String text, float x, float y, Color mainColor) {
        font.setColor(0f, 0f, 0f, 0.75f);
        glyphLayout.setText(font, text);
        font.draw(batch, glyphLayout, x + 2, y - 2);
        font.setColor(mainColor);
        glyphLayout.setText(font, text);
        font.draw(batch, glyphLayout, x, y);
        font.setColor(Color.WHITE);
    }

    private float getGlyphWidth(BitmapFont font, String text) {
        glyphLayout.setText(font, text);
        return glyphLayout.width;
    }

    private void handleTurnLogic(float delta) {
        if (turnState == TurnState.ANSWERING_QUESTION) {
            return; // Freeze game logic during quiz
        }

        if (playerHurtTimer > 0) playerHurtTimer -= delta;
        if (bossHurtTimer > 0) bossHurtTimer -= delta;
        if (bossAttackTimer > 0) bossAttackTimer -= delta;

        if (bossHealth <= 0 && turnState != TurnState.VICTORY && turnState != TurnState.GAME_OVER) {
            combatLog = "You defeated " + bossName + "!";
            turnState = TurnState.VICTORY;
            cardQuizHandler.cancelQuiz(quizStage);
        } else if (playerHealth <= 0 && turnState != TurnState.GAME_OVER && turnState != TurnState.VICTORY) {
            combatLog = "You Died!";
            turnState = TurnState.GAME_OVER;
            cardQuizHandler.cancelQuiz(quizStage);
        }

        if (turnState == TurnState.BOSS_THINKING) {
            bossThinkTimer -= delta;
            if (bossThinkTimer <= 0) {
                // Pick a card
                double roll = Math.random();
                if (roll < 0.15) activeBossCard = bossDeck.get(2); // 15% Ultimate
                else if (roll < 0.50) activeBossCard = bossDeck.get(1); // 35% Drain
                else activeBossCard = bossDeck.get(0); // 50% Standard Attack

                combatLog = bossName + " is preparing " + activeBossCard.name + "!";
                turnState = TurnState.BOSS_FLASHING_CARD;
                bossFlashTimer = 1.5f;
            }
        } else if (turnState == TurnState.BOSS_FLASHING_CARD) {
            bossFlashTimer -= delta;
            if (bossFlashTimer <= 0) {
                executeBossCard();
            }
        }
    }

    private void executeBossCard() {
        int damage = activeBossCard.value;

        if (activeBossCard.name.equals("Life Drain")) {
            bossHealth = Math.min(bossMaxHealth, bossHealth + (damage/2));
        }

        if (playerBlock >= damage) {
            playerBlock -= damage;
            combatLog = "Blocked! " + bossName + " used " + activeBossCard.name;
        } else {
            damage -= playerBlock;
            playerBlock = 0;
            playerHealth -= damage;
            if (playerHealth < 0) playerHealth = 0;
            playerHurtTimer = playerHurt.getAnimationDuration();
            bossAttackTimer = bossAttack.getAnimationDuration();
            combatLog = bossName + " used " + activeBossCard.name + " for " + damage + " dmg!";
        }

        activeBossCard = null;
        if (playerHealth > 0) startPlayerTurn();
    }

    private void handleStateInput() {
        if (state == State.GAME) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && turnState != TurnState.GAME_OVER && turnState != TurnState.VICTORY) {
                state = State.PAUSE;
            } else {
                handleGameInput();
            }
        } else if (state == State.PAUSE) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                state = State.GAME;
            } else {
                PauseOverlay.Action action = pauseOverlay.handleInput(viewport);
                switch (action) {
                    case RESUME -> state = State.GAME;
                    case SAVE -> {
                        combatLog = "Cannot save during boss fight!";
                    }
                    case RESTART -> {
                        ((Main) Gdx.app.getApplicationListener()).setScreen(new LoadingScreen(null));
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
        }
    }

    private void handleGameInput() {
        if (turnState == TurnState.GAME_OVER) {
            GameOverOverlay.Action action = gameOverOverlay.handleInput(viewport);
            if (action == GameOverOverlay.Action.RETRY) {
                ((Main) Gdx.app.getApplicationListener()).setScreen(new LoadingScreen(null));
                this.dispose();
            } else if (action == GameOverOverlay.Action.MAIN_MENU) {
                ((Main) Gdx.app.getApplicationListener()).setScreen(new HomeScreen());
                this.dispose();
            }
            return;
        }

        if (turnState == TurnState.VICTORY && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (hasExit) {
                ((Main) Gdx.app.getApplicationListener()).setScreen(previousScreen);
            } else {
                ((Main) Gdx.app.getApplicationListener()).setScreen(new VictoryScreen(
                    player.getEnemiesKilled(),
                    player.getTorchCount(),
                    player.getTimeSurvived()
                ));
                if (previousScreen != null) previousScreen.dispose();
            }
            this.dispose();
            return;
        }

        if (turnState == TurnState.PLAYER_TURN) {
            if (Gdx.input.justTouched()) {
                touch.set(Gdx.input.getX(), Gdx.input.getY(), 0);
                viewport.unproject(touch);

                for (int i = 0; i < hand.size(); i++) {
                    Card c = hand.get(i);
                    if (c.bounds.contains(touch.x, touch.y)) {
                        if (playerEnergy >= c.cost) {
                            pendingCard = c;
                            turnState = TurnState.ANSWERING_QUESTION;

                            cardQuizHandler.startQuizForCard(pendingCard, questionPresenter, quizStage, isCorrect -> {
                                playerEnergy -= pendingCard.cost;
                                hand.remove(pendingCard);
                                discardPile.add(pendingCard);

                                if (isCorrect) {
                                    playPlayerCard(pendingCard);
                                } else {
                                    combatLog = pendingCard.name + " failed! Incorrect answer.";
                                }

                                pendingCard = null;
                                turnState = TurnState.PLAYER_TURN;
                            });
                        } else {
                            combatLog = "Not enough energy for " + c.name + "!";
                        }
                        break;
                    }
                }
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                turnState = TurnState.BOSS_THINKING;
                bossThinkTimer = 1.0f;
                combatLog = "Player ended turn. Boss is acting...";
            }
        }
    }

    private void playPlayerCard(Card c) {
        if (c.type == CardType.ATTACK) {
            bossHealth -= c.value;
            if (bossHealth < 0) bossHealth = 0;
            bossHurtTimer = bossHurt.getAnimationDuration();
            combatLog = "Played " + c.name + "! Dealt " + c.value + " dmg.";
        } else if (c.type == CardType.DEFEND) {
            playerBlock += c.value;
            combatLog = "Played " + c.name + "! Gained " + c.value + " block.";
        } else if (c.type == CardType.HEAL) {
            playerHealth = Math.min(playerMaxHealth, playerHealth + c.value);
            combatLog = "Played " + c.name + "! Healed for " + c.value + ".";
        }
    }

    private void loadAnimations() {
        // Player animations from Player_sprite/Adventurer/Individual Sprites/
        String pBase = "Player_sprite/Adventurer/Individual Sprites/adventurer-";
        Array<TextureRegion> pIdle = new Array<>();
        for (int i = 0; i < 4; i++) pIdle.add(new TextureRegion(Main.assets.get(pBase + "idle-0" + i + ".png", Texture.class)));
        playerIdle = new Animation<>(0.12f, pIdle, Animation.PlayMode.LOOP);

        Array<TextureRegion> pHurt = new Array<>();
        for (int i = 0; i < 3; i++) pHurt.add(new TextureRegion(Main.assets.get(pBase + "hurt-0" + i + ".png", Texture.class)));
        playerHurt = new Animation<>(0.08f, pHurt, Animation.PlayMode.NORMAL);

        // Boss animations from boss_sprite/
        String bBase = "boss_sprite/sprites/";
        Array<TextureRegion> bIdle = new Array<>();
        for (int i = 1; i <= 4; i++) bIdle.add(new TextureRegion(Main.assets.get(bBase + "idle" + i + ".png", Texture.class)));
        bossIdle = new Animation<>(0.15f, bIdle, Animation.PlayMode.LOOP);

        Array<TextureRegion> bHurt = new Array<>();
        for (int i = 1; i <= 2; i++) bHurt.add(new TextureRegion(Main.assets.get(bBase + "hurt" + i + ".png", Texture.class)));
        bossHurt = new Animation<>(0.10f, bHurt, Animation.PlayMode.NORMAL);

        Array<TextureRegion> bAttack = new Array<>();
        for (int i = 1; i <= 6; i++) bAttack.add(new TextureRegion(Main.assets.get(bBase + "punch" + i + ".png", Texture.class)));
        bossAttack = new Animation<>(0.08f, bAttack, Animation.PlayMode.NORMAL);
    }

    @Override public void resize(int w, int h) { viewport.update(w, h, true); }
    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    /** Build the intro dialogue sequence between Boss and Player. */
    private void buildIntroDialogue() {
        Color bossColor = new Color(1f, 0.3f, 0.3f, 1f);
        Color playerColor = new Color(0.3f, 0.9f, 1f, 1f);

        List<DialogueOverlay.DialogueNode> nodes = new ArrayList<>();

        // Node 0
        nodes.add(new DialogueOverlay.DialogueNode(
            bossName,
            "So... you've finally made it this far. I must admit, I'm impressed.",
            bossColor
        ));

        // Node 1
        nodes.add(new DialogueOverlay.DialogueNode(
            bossName,
            "But this is where your little adventure ends, mortal.",
            bossColor
        ));

        // Node 2 — player choice
        nodes.add(new DialogueOverlay.DialogueNode(
            "You",
            "...",
            playerColor,
            Arrays.asList(
                new DialogueOverlay.Choice("I will end you, demon!", 3),
                new DialogueOverlay.Choice("(Stay silent and draw your weapon)", 5)
            )
        ));

        // Node 3 — brave path
        nodes.add(new DialogueOverlay.DialogueNode(
            bossName,
            "Ha! Bold words for someone standing in MY domain.",
            bossColor
        ));

        // Node 4 — brave path continued
        nodes.add(new DialogueOverlay.DialogueNode(
            bossName,
            "Very well... Let's see if your blade is as sharp as your tongue!",
            bossColor
        ));
        // -> falls through to node 5 naturally since indices are sequential

        // Node 5 — silent path / shared ending
        nodes.add(new DialogueOverlay.DialogueNode(
            bossName,
            "No words? ...Fine. Actions speak louder anyway.",
            bossColor
        ));

        // Node 6
        nodes.add(new DialogueOverlay.DialogueNode(
            bossName,
            "PREPARE YOURSELF!",
            bossColor
        ));

        dialogueOverlay.start(nodes, () -> {
            // Dialogue finished — transition to the card battle
            startPlayerTurn();
        });
    }

    @Override public void dispose() {
        if (cardQuizHandler != null && quizStage != null) {
            cardQuizHandler.cancelQuiz(quizStage);
        }
        if (quizStage != null) quizStage.dispose();
        if (quizUIStyles != null) quizUIStyles.dispose();

        batch.dispose(); shape.dispose(); font.dispose();
        if (background != null) background.dispose();
        dialogueOverlay.dispose();
        for(Card c : drawPile) if(c.texture != null) c.texture.dispose();
        for(Card c : discardPile) if(c.texture != null) c.texture.dispose();
        for(Card c : hand) if(c.texture != null) c.texture.dispose();
        for(Card c : bossDeck) if(c.texture != null) c.texture.dispose();
    }
}
