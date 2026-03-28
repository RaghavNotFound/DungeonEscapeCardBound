package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;
import java.util.ArrayList;

public class Player {

    private final Animation<TextureRegion> walkAnimation;
    private final Animation<TextureRegion> runAnimation;
    private final Animation<TextureRegion> idleAnimation;
    private final Animation<TextureRegion> idleBlinkingAnimation;

    private final TextureRegion[] walkFrames;
    private final TextureRegion[] runFrames;
    private final TextureRegion[] idleFrames;
    private final TextureRegion[] idleBlinkingFrames;

    private final Texture[] walkingTextures;
    private final Texture[] runTextures;
    private final Texture[] idleTextures;
    private final Texture[] idleBlinkingTextures;

    private TextureRegion currentFrame;

    private float stateTime;
    private boolean isRunning;
    private boolean facingRight = true;
    private boolean playBlink = false;

    private final Vector2 position;
    public Rectangle bounds;

    private final int WIDTH = 128;
    private final int HEIGHT = 128;

    // 🔥 ARROW SYSTEM
    private ArrayList<Arrow> arrows = new ArrayList<>();

    // 🔥 COOLDOWN
    private float shootCooldown = 1.0f;
    private float shootTimer = 0f;

    public Player() {

        position = new Vector2(200, 200);
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
            walkingTextures[i] = new Texture("Movements/Player/walking/walking_" + (i + 1) + ".png");
            walkFrames[i] = new TextureRegion(walkingTextures[i]);
        }

        // RUN
        for (int i = 0; i < runFrameCount; i++) {
            runTextures[i] = new Texture("Movements/Player/running/running_" + (i + 1) + ".png");
            runFrames[i] = new TextureRegion(runTextures[i]);
        }

        // IDLE
        for (int i = 0; i < idleFrameCount; i++) {
            idleTextures[i] = new Texture("Movements/Player/idle/idle_" + (i + 1) + ".png");
            idleFrames[i] = new TextureRegion(idleTextures[i]);
        }

        // IDLE BLINK
        for (int i = 0; i < idleBlinkingFrameCount; i++) {
            idleBlinkingTextures[i] = new Texture("Movements/Player/idleBlinking/idleBlinking_" + (i + 1) + ".png");
            idleBlinkingFrames[i] = new TextureRegion(idleBlinkingTextures[i]);
        }

        walkAnimation = new Animation<>(0.025f, walkFrames);
        runAnimation = new Animation<>(0.08f, runFrames);

        idleAnimation = new Animation<>(0.08f, idleFrames);
        idleAnimation.setPlayMode(Animation.PlayMode.NORMAL);

        idleBlinkingAnimation = new Animation<>(0.08f, idleBlinkingFrames);
        idleBlinkingAnimation.setPlayMode(Animation.PlayMode.NORMAL);

        currentFrame = idleFrames[0];
        stateTime = 0f;
    }

    public void update(float delta, OrthographicCamera camera) {

        boolean moved = handleMovement(delta);

        stateTime += delta;

        // ===== ANIMATION =====
        if (moved) {

            currentFrame = isRunning
                ? runAnimation.getKeyFrame(stateTime, true)
                : walkAnimation.getKeyFrame(stateTime, true);

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

        // ===== COOLDOWN =====
        if (shootTimer > 0) shootTimer -= delta;

        // ===== SHOOT =====
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (shootTimer <= 0f) {
                shootArrow(camera);
                shootTimer = shootCooldown;
            }
        }

        // ===== UPDATE ARROWS =====
        for (int i = arrows.size() - 1; i >= 0; i--) {
            Arrow arrow = arrows.get(i);
            arrow.update(delta);

            if (arrow.isCollided(
                camera.viewportWidth,
                camera.viewportHeight,
                new ArrayList<>()
            )) {
                arrows.remove(i);
            }
        }
    }

    private void shootArrow(OrthographicCamera camera) {

        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse);

        Vector2 direction = new Vector2(
            mouse.x - (position.x + WIDTH / 2f),
            mouse.y - (position.y + HEIGHT / 2f)
        ).nor();

        arrows.add(new Arrow(
            position.x + WIDTH / 2f,
            position.y + HEIGHT / 2f,
            direction
        ));
    }

    private boolean handleMovement(float delta) {

        float oldX = position.x;
        float oldY = position.y;

        float newX = position.x;
        float newY = position.y;

        boolean up = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean down = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean left = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        if (up && down) { up = false; down = false; }
        if (left && right) { left = false; right = false; }

        isRunning = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        float speed = 100;
        float currentSpeed = isRunning ? speed * 1.5f : speed;

        if (up) newY += currentSpeed * delta;
        if (down) newY -= currentSpeed * delta;

        if (left) {
            newX -= currentSpeed * delta;
            if (facingRight) { flipFrames(); facingRight = false; }
        }

        if (right) {
            newX += currentSpeed * delta;
            if (!facingRight) { flipFrames(); facingRight = true; }
        }

        // ===== X AXIS COLLISION =====
        Rectangle xBounds = new Rectangle(newX, position.y, WIDTH, HEIGHT);

        boolean collideX = false;
        for (Rectangle wall : GameWorld.getBoundaries()) {
            if (xBounds.overlaps(wall)) {
                collideX = true;
                break;
            }
        }

        if (!collideX) {
            position.x = newX;
        }

        // ===== Y AXIS COLLISION =====
        Rectangle yBounds = new Rectangle(position.x, newY, WIDTH, HEIGHT);

        boolean collideY = false;
        for (Rectangle wall : GameWorld.getBoundaries()) {
            if (yBounds.overlaps(wall)) {
                collideY = true;
                break;
            }
        }

        if (!collideY) {
            position.y = newY;
        }

        return (oldX != position.x || oldY != position.y);
    }

    private void flipFrames() {
        for (TextureRegion f : walkFrames) f.flip(true, false);
        for (TextureRegion f : runFrames) f.flip(true, false);
        for (TextureRegion f : idleFrames) f.flip(true, false);
        for (TextureRegion f : idleBlinkingFrames) f.flip(true, false);
    }

    public void render(SpriteBatch batch) {
        batch.draw(currentFrame, position.x, position.y, WIDTH, HEIGHT);

        for (Arrow arrow : arrows) {
            arrow.render(batch);
        }
    }

    public void dispose() {
        for (Texture t : walkingTextures) t.dispose();
        for (Texture t : runTextures) t.dispose();
        for (Texture t : idleTextures) t.dispose();
        for (Texture t : idleBlinkingTextures) t.dispose();
    }

    public Vector2 getPosition() {
        return new Vector2(bounds.x, bounds.y);
    }
}
