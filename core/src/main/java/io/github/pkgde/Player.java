package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Player {

    // Animations
    private final Animation<TextureRegion> walkAnimation;
    private final Animation<TextureRegion> runAnimation;
    private final Animation<TextureRegion> idleAnimation;
    private final Animation<TextureRegion> idleBlinkingAnimation;

    // Frames
    private final TextureRegion[] walkFrames;
    private final TextureRegion[] runFrames;
    private final TextureRegion[] idleFrames;
    private final TextureRegion[] idleBlinkingFrames;

    // Textures
    private final Texture[] walkingTextures;
    private final Texture[] runTextures;
    private final Texture[] idleTextures;
    private final Texture[] idleBlinkingTextures;

    private TextureRegion currentFrame;

    private float stateTime;
    private boolean isRunning;
    private boolean facingRight = true;
    private boolean playBlink = false;
    private boolean anyKeyPressed;

    private final Vector2 position;
    public Rectangle bounds;

    private final int WIDTH = 128;
    private final int HEIGHT = 128;

    // 🔥 Stamina
    private float stamina = 100f;

    public Player() {

        position = new Vector2(100, 100);
        bounds = new Rectangle(position.x, position.y, WIDTH, HEIGHT);

        int walkingFrameCount = 23;
        int runFrameCount = 12;
        int idleFrameCount = 18;
        int idleBlinkingFrameCount = 18;

        walkingTextures = new Texture[walkingFrameCount];
        runTextures = new Texture[runFrameCount];
        idleTextures = new Texture[idleFrameCount];
        idleBlinkingTextures = new Texture[idleBlinkingFrameCount];

        walkFrames = new TextureRegion[walkingFrameCount];
        runFrames = new TextureRegion[runFrameCount];
        idleFrames = new TextureRegion[idleFrameCount];
        idleBlinkingFrames = new TextureRegion[idleBlinkingFrameCount];

        // WALK
        for (int i = 0; i < walkingFrameCount; i++) {
            walkingTextures[i] = new Texture("Movments/walking/walking_" + (i + 1) + ".png");
            walkFrames[i] = new TextureRegion(walkingTextures[i]);
        }

        // RUN
        for (int i = 0; i < runFrameCount; i++) {
            runTextures[i] = new Texture("Movments/running/running_" + (i + 1) + ".png");
            runFrames[i] = new TextureRegion(runTextures[i]);
        }

        // IDLE
        for (int i = 0; i < idleFrameCount; i++) {
            idleTextures[i] = new Texture("Movments/idle/idle_" + (i + 1) + ".png");
            idleFrames[i] = new TextureRegion(idleTextures[i]);
        }

        // IDLE BLINK
        for (int i = 0; i < idleBlinkingFrameCount; i++) {
            idleBlinkingTextures[i] = new Texture("Movments/idleBlinking/idleBlinking_" + (i + 1) + ".png");
            idleBlinkingFrames[i] = new TextureRegion(idleBlinkingTextures[i]);
        }

        // Animations (your values)
        walkAnimation = new Animation<>(0.025f, walkFrames);
        runAnimation = new Animation<>(0.08f, runFrames);

        idleAnimation = new Animation<>(0.08f, idleFrames);
        idleAnimation.setPlayMode(Animation.PlayMode.NORMAL);

        idleBlinkingAnimation = new Animation<>(0.08f, idleBlinkingFrames);
        idleBlinkingAnimation.setPlayMode(Animation.PlayMode.NORMAL);

        currentFrame = idleFrames[0];
        stateTime = 0f;
    }

    public void update(float delta) {

        boolean isMoving = handleMovement(delta);

        float maxStamina = 100f;
        if (isRunning && anyKeyPressed) {
            stamina -= 25f * delta;

            if (stamina <= 0) {
                stamina = 0;
                isRunning = false;
            }

        } else if (anyKeyPressed) {
            stamina += 15f * delta;

            if (stamina > maxStamina) {
                stamina = maxStamina;
            }

        } else {
            stamina += 25f * delta;

            if (stamina > maxStamina) {
                stamina = maxStamina;
            }
        }

        stateTime += delta;

        // Animation
        if (isMoving) {

            if (isRunning) {
                currentFrame = runAnimation.getKeyFrame(stateTime, true);
            } else {
                currentFrame = walkAnimation.getKeyFrame(stateTime, true);
            }

            playBlink = false;

        } else {

            if (!playBlink) {
                currentFrame = idleAnimation.getKeyFrame(stateTime, false);

                if (idleAnimation.isAnimationFinished(stateTime)) {
                    stateTime = 0;
                    playBlink = true;
                }

            } else {
                currentFrame = idleBlinkingAnimation.getKeyFrame(stateTime, false);

                if (idleBlinkingAnimation.isAnimationFinished(stateTime)) {
                    stateTime = 0;
                    playBlink = false;
                }
            }
        }

        bounds.setPosition(position.x, position.y);
    }

    private boolean handleMovement(float delta) {

        float newX = position.x;
        float newY = position.y;

        anyKeyPressed =
            Gdx.input.isKeyPressed(Input.Keys.W) ||
                Gdx.input.isKeyPressed(Input.Keys.A) ||
                Gdx.input.isKeyPressed(Input.Keys.S) ||
                Gdx.input.isKeyPressed(Input.Keys.D) ||
                Gdx.input.isKeyPressed(Input.Keys.UP) ||
                Gdx.input.isKeyPressed(Input.Keys.DOWN) ||
                Gdx.input.isKeyPressed(Input.Keys.LEFT) ||
                Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        boolean shiftPressed = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);

        isRunning = shiftPressed && stamina > 0;

        float speed = 100f;
        float currentSpeed = isRunning ? speed * 1.5f : speed;

        boolean moved = false;

        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            newY += currentSpeed * delta;
            moved = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            newY -= currentSpeed * delta;
            moved = true;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            newX -= currentSpeed * delta;
            moved = true;

            if (facingRight) {
                flipFrames();
                facingRight = false;
            }
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            newX += currentSpeed * delta;
            moved = true;

            if (!facingRight) {
                flipFrames();
                facingRight = true;
            }
        }

        newX = MathUtils.clamp(newX, 0, 800 - WIDTH);
        newY = MathUtils.clamp(newY, 0, 600 - HEIGHT);

        position.set(newX, newY);

        return moved;
    }

    private void flipFrames() {
        for (TextureRegion f : walkFrames) f.flip(true, false);
        for (TextureRegion f : runFrames) f.flip(true, false);
        for (TextureRegion f : idleFrames) f.flip(true, false);
        for (TextureRegion f : idleBlinkingFrames) f.flip(true, false);
    }

    public void render(SpriteBatch batch) {
        batch.draw(currentFrame, position.x, position.y, WIDTH, HEIGHT);
    }
    // 🔥 UI Getters
//    public float getStamina() {
//        return stamina;
//    }
//
//    public float getMaxStamina() {
//        return maxStamina;
//    }

    public void dispose() {
        for (Texture t : walkingTextures) t.dispose();
        for (Texture t : runTextures) t.dispose();
        for (Texture t : idleTextures) t.dispose();
        for (Texture t : idleBlinkingTextures) t.dispose();
    }

}
