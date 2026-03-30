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
    private float idleLoopTime;
    private boolean isRunning;
    private boolean facingRight = true;

    private final Vector2 position;
    public Rectangle bounds;

    private ArrayList<Rectangle> boundaries;
    private float worldMinX = 0f;
    private float worldMinY = 0f;
    private float worldMaxX = Float.MAX_VALUE;
    private float worldMaxY = Float.MAX_VALUE;

    private final int WIDTH = 128;
    private final int HEIGHT = 128;

    // ===== ARROWS =====
    private final ArrayList<Arrow> arrows = new ArrayList<>();

    private static final float SHOOT_COOLDOWN = 1.0f;
    private float shootTimer = 0f;

    // ===== STAMINA =====
    private static final float STAMINA_DRAIN_RATE = 40f;
    private static final float STAMINA_IDLE_REGEN_RATE = STAMINA_DRAIN_RATE * 0.8f;
    private static final float STAMINA_WALK_REGEN_RATE = STAMINA_DRAIN_RATE * 0.4f;
    private static final float RUN_UNLOCK_THRESHOLD_RATIO = 0.5f;

    private float stamina = 100f;
    private float maxStamina = 100f;
    private boolean runLocked;

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
    public float getShootCooldownPercent() { return MathUtils.clamp(shootTimer / SHOOT_COOLDOWN, 0f, 1f); }

    public Vector2 getPosition() { return position; }
    public Vector2 getPos() { return position; }
    public Rectangle getBounds() { return bounds; }
    public ArrayList<Arrow> getArrows() { return arrows; }

    public void setBoundaries(ArrayList<Rectangle> boundaries) {
        this.boundaries = boundaries;
    }

    public void setWorldBounds(float minX, float minY, float maxX, float maxY) {
        this.worldMinX = minX;
        this.worldMinY = minY;
        this.worldMaxX = maxX;
        this.worldMaxY = maxY;
    }

    // ===== UPDATE =====
    public void update(float delta, OrthographicCamera camera) {

        updateRunLockState();
        boolean moved = handleMovement(delta);
        stateTime += delta;

        // ===== ANIMATION =====
        if (moved) {
            idleLoopTime = 0f;
            currentFrame = isRunning
                ? runAnimation.getKeyFrame(stateTime, true)
                : walkAnimation.getKeyFrame(stateTime, true);
        } else {
            idleLoopTime += delta;
            currentFrame = getIdleLoopFrame(idleLoopTime);
        }

        // ===== STAMINA =====
        boolean runningNow = isRunning && moved && stamina > 0;

        if (runningNow) {
            stamina -= STAMINA_DRAIN_RATE * delta;
        } else if (moved) {
            stamina += STAMINA_WALK_REGEN_RATE * delta;
        } else {
            stamina += STAMINA_IDLE_REGEN_RATE * delta;
        }

        stamina = Math.max(0, Math.min(maxStamina, stamina));
        updateRunLockState();

        bounds.setPosition(position.x, position.y);

        // ===== SHOOT =====
        if (shootTimer > 0) shootTimer -= delta;

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && shootTimer <= 0f) {
            shootArrow(camera);
            shootTimer = SHOOT_COOLDOWN;
        }

        // ===== UPDATE ARROWS =====
        for (int i = arrows.size() - 1; i >= 0; i--) {
            Arrow arrow = arrows.get(i);
            arrow.update(delta);

            if (arrow.isCollided(worldMaxX, worldMaxY, boundaries == null ? new ArrayList<>() : boundaries)) {
                arrows.remove(i);
            }
        }
    }

    private void updateRunLockState() {
        if (stamina <= 0f) {
            runLocked = true;
            return;
        }

        if (runLocked && stamina >= maxStamina * RUN_UNLOCK_THRESHOLD_RATIO) {
            runLocked = false;
        }
    }

    // ===== IDLE LOOP (IDLE -> BLINK -> REPEAT) =====
    private TextureRegion getIdleLoopFrame(float time) {
        float idleDuration = idleAnimation.getAnimationDuration();
        float blinkDuration = idleBlinkingAnimation.getAnimationDuration();
        float fullCycle = idleDuration + blinkDuration;

        float cycleTime = time % fullCycle;
        if (cycleTime < idleDuration) {
            return idleAnimation.getKeyFrame(cycleTime, false);
        }

        return idleBlinkingAnimation.getKeyFrame(cycleTime - idleDuration, false);
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

        boolean runKeyPressed = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);

        float baseSpeed = 100f;
        boolean canRun = runKeyPressed && !runLocked;
        isRunning = canRun;

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

        newX = MathUtils.clamp(newX, worldMinX, worldMaxX - WIDTH);
        newY = MathUtils.clamp(newY, worldMinY, worldMaxY - HEIGHT);

        Rectangle xBounds = new Rectangle(newX, position.y, WIDTH, HEIGHT);
        if (boundaries != null) {
            for (Rectangle wall : boundaries) {
                if (xBounds.overlaps(wall)) return false;
            }
        }
        position.x = newX;

        Rectangle yBounds = new Rectangle(position.x, newY, WIDTH, HEIGHT);
        if (boundaries != null) {
            for (Rectangle wall : boundaries) {
                if (yBounds.overlaps(wall)) return false;
            }
        }
        position.y = newY;

        return oldX != position.x || oldY != position.y;
    }

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
