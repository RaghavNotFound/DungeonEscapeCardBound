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
    private boolean isMoving;
    private boolean isRunning;
    private boolean facingRight;
    private boolean playBlink = false;

    private final Vector2 position;
    public Rectangle bounds;

    private final int WIDTH = 128;
    private final int HEIGHT = 128;

    private float speed = 80;

    public Player() {
        position = new Vector2(100, 100);
        bounds = new Rectangle(position.x, position.y, WIDTH, HEIGHT);

        facingRight = true;

        int walkingFrameCount = 23;
        int runFrameCount = 12;
        int idleFrameCount = 18;
        int idleBlinkingFrameCount = 18;

        // Allocate arrays
        walkingTextures = new Texture[walkingFrameCount];
        runTextures = new Texture[runFrameCount];
        idleTextures = new Texture[idleFrameCount];
        idleBlinkingTextures = new Texture[idleBlinkingFrameCount];

        walkFrames = new TextureRegion[walkingFrameCount];
        runFrames = new TextureRegion[runFrameCount];
        idleFrames = new TextureRegion[idleFrameCount];
        idleBlinkingFrames = new TextureRegion[idleBlinkingFrameCount];

        // Load WALK
        for (int i = 0; i < walkingFrameCount; i++) {
            walkingTextures[i] = new Texture("Movments/walking/walking_" + (i+1) + ".png");
            walkFrames[i] = new TextureRegion(walkingTextures[i]);
        }

        // Load RUN
        for (int i = 0; i < runFrameCount; i++) {
            runTextures[i] = new Texture("Movments/running/running_" + (i+1) + ".png");
            runFrames[i] = new TextureRegion(runTextures[i]);
        }

        // Load IDLE
        for (int i = 0; i < idleFrameCount; i++) {
            idleTextures[i] = new Texture("Movments/idle/idle_" + (i+1) + ".png");
            idleFrames[i] = new TextureRegion(idleTextures[i]);
        }

        // Load IDLE BLINK
        for (int i = 0; i < idleBlinkingFrameCount; i++) {
            idleBlinkingTextures[i] = new Texture("Movments/idleBlinking/idleBlinking_" + (i+1) + ".png");
            idleBlinkingFrames[i] = new TextureRegion(idleBlinkingTextures[i]);
        }

        // Animations
        walkAnimation = new Animation<>(0.08f, walkFrames);
        runAnimation = new Animation<>(0.05f, runFrames);

        idleAnimation = new Animation<>(0.08f, idleFrames);
        idleAnimation.setPlayMode(Animation.PlayMode.NORMAL);

        idleBlinkingAnimation = new Animation<>(0.08f, idleBlinkingFrames);
        idleBlinkingAnimation.setPlayMode(Animation.PlayMode.NORMAL);

        currentFrame = idleFrames[0];
        stateTime = 0f;
    }

    public void update(float delta) {
        handleMovement(delta);

        stateTime += delta;

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

    private void handleMovement(float delta) {
        float newX = position.x;
        float newY = position.y;

        isMoving = false;

        isRunning = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        float currentSpeed = isRunning ? speed * 2 : speed;

        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            newY += currentSpeed * delta;
            isMoving = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            newY -= currentSpeed * delta;
            isMoving = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            newX -= currentSpeed * delta;
            isMoving = true;

            if (facingRight) {
                flipFrames();
                facingRight = false;
            }
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            newX += currentSpeed * delta;
            isMoving = true;

            if (!facingRight) {
                flipFrames();
                facingRight = true;
            }
        }

        newX = MathUtils.clamp(newX, 0, 800 - WIDTH);
        newY = MathUtils.clamp(newY, 0, 600 - HEIGHT);

        position.set(newX, newY);
    }

    private void flipFrames() {
        for (TextureRegion frame : walkFrames) frame.flip(true, false);
        for (TextureRegion frame : runFrames) frame.flip(true, false);
        for (TextureRegion frame : idleFrames) frame.flip(true, false);
        for (TextureRegion frame : idleBlinkingFrames) frame.flip(true, false);
    }

    public void render(SpriteBatch batch) {
        batch.draw(currentFrame, position.x, position.y, WIDTH, HEIGHT);
    }

    public void dispose() {
        for (Texture tex : walkingTextures) tex.dispose();
        for (Texture tex : runTextures) tex.dispose();
        for (Texture tex : idleTextures) tex.dispose();
        for (Texture tex : idleBlinkingTextures) tex.dispose();
    }
}
