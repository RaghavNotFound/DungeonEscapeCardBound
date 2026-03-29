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

    private ArrayList<Rectangle> boundaries;

    private final int WIDTH = 128;
    private final int HEIGHT = 128;

    // ===== ARROWS =====
    private final ArrayList<Arrow> arrows = new ArrayList<>();

    // ===== SHOOT COOLDOWN =====
    private float shootCooldown = 1.0f;
    private float shootTimer = 0f;

    // ===== STAMINA =====
    private float stamina = 100f;
    private float maxStamina = 100f;

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
        idleBlinkingAnimation = new Animation<>(0.08f, idleBlinkingFrames);

        currentFrame = idleFrames[0];
    }

    // ===== GETTERS =====
    public float getStamina() { return stamina; }
    public float getMaxStamina() { return maxStamina; }

    public Vector2 getPosition() { return position; }
    public Vector2 getPos() { return position; }
    public Rectangle getBounds() { return bounds; }
    public ArrayList<Arrow> getArrows() { return arrows; }

    public void setBoundaries(ArrayList<Rectangle> boundaries) {
        this.boundaries = boundaries;
    }

    // ===== UPDATE =====
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
            currentFrame = idleAnimation.getKeyFrame(stateTime, true);
        }

        // ===== STAMINA =====
        boolean runningNow = isRunning && moved && stamina > 0;

        if (runningNow) stamina -= 40f * delta;
        else stamina += 25f * delta;

        stamina = Math.max(0, Math.min(maxStamina, stamina));

        bounds.setPosition(position.x, position.y);

        // ===== SHOOT =====
        if (shootTimer > 0) shootTimer -= delta;

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && shootTimer <= 0f) {
            shootArrow(camera);
            shootTimer = shootCooldown;
        }

        // ===== UPDATE ARROWS =====
        for (int i = arrows.size() - 1; i >= 0; i--) {
            Arrow arrow = arrows.get(i);
            arrow.update(delta);

            if (arrow.isCollided(camera.viewportWidth, camera.viewportHeight, new ArrayList<>())) {
                arrows.remove(i);
            }
        }
    }

    // ===== MOVEMENT =====
    private boolean handleMovement(float delta) {

        float oldX = position.x;
        float oldY = position.y;

        float newX = position.x;
        float newY = position.y;

        boolean up = Gdx.input.isKeyPressed(Input.Keys.W);
        boolean down = Gdx.input.isKeyPressed(Input.Keys.S);
        boolean left = Gdx.input.isKeyPressed(Input.Keys.A);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D);

        isRunning = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);

        float baseSpeed = 100f;
        boolean canRun = isRunning && stamina > 0;

        float speed = canRun ? baseSpeed * 1.5f : baseSpeed;

        if (up) newY += speed * delta;
        if (down) newY -= speed * delta;

        if (left) {
            newX -= speed * delta;
            facingRight = false;
        }

        if (right) {
            newX += speed * delta;
            facingRight = true;
        }

        Rectangle xBounds = new Rectangle(newX, position.y, WIDTH, HEIGHT);
        for (Rectangle wall : boundaries) {
            if (xBounds.overlaps(wall)) return false;
        }
        position.x = newX;

        Rectangle yBounds = new Rectangle(position.x, newY, WIDTH, HEIGHT);
        for (Rectangle wall : boundaries) {
            if (yBounds.overlaps(wall)) return false;
        }
        position.y = newY;

        return oldX != position.x || oldY != position.y;
    }

    // ===== SHOOT =====
    private void shootArrow(OrthographicCamera camera) {

        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse);

        Vector2 dir = new Vector2(
            mouse.x - (position.x + WIDTH / 2f),
            mouse.y - (position.y + HEIGHT / 2f)
        ).nor();

        arrows.add(new Arrow(
            position.x + WIDTH / 2f,
            position.y + HEIGHT / 2f,
            dir
        ));
    }

    // ===== RENDER (FIXED FLIP) =====
    public void render(SpriteBatch batch) {

        float drawWidth = facingRight ? WIDTH : -WIDTH;
        float drawX = facingRight ? position.x : position.x + WIDTH;

        batch.draw(currentFrame, drawX, position.y, drawWidth, HEIGHT);

        for (Arrow arrow : arrows) {
            arrow.render(batch);
        }
    }

    // ===== CLEANUP =====
    public void dispose() {
        for (Texture t : walkingTextures) t.dispose();
        for (Texture t : runTextures) t.dispose();
        for (Texture t : idleTextures) t.dispose();
        for (Texture t : idleBlinkingTextures) t.dispose();
    }
}
