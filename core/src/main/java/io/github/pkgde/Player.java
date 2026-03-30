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
    private final Animation<TextureRegion> hurtAnimation;
    private final Animation<TextureRegion> swordAnimation;
    private final Animation<TextureRegion> deathAnimation;

    private final TextureRegion[] walkFrames;
    private final TextureRegion[] runFrames;
    private final TextureRegion[] idleFrames;
    private final TextureRegion[] idleBlinkingFrames;

    private final Texture[] walkingTextures;
    private final Texture[] runTextures;
    private final Texture[] idleTextures;
    private final Texture[] idleBlinkingTextures;
    private final Texture[] hurtTextures;
    private final Texture[] swordTextures;
    private final Texture[] deathTextures;

    private final Texture swordTexture;

    private TextureRegion currentFrame;

    private float stateTime;
    private float idleLoopTime;
    private boolean isRunning;
    private boolean facingRight = true;
    private boolean swordDamageConsumed;

    private final Vector2 position;
    public Rectangle bounds;

    private ArrayList<Rectangle> boundaries;
    private float worldMinX = 0f;
    private float worldMinY = 0f;
    private float worldMaxX = Float.MAX_VALUE;
    private float worldMaxY = Float.MAX_VALUE;

    private final int WIDTH = 128;
    private final int HEIGHT = 128;
    private static final float HITBOX_WIDTH = 55f;
    private static final float HITBOX_HEIGHT = 80f;
    private static final float HITBOX_OFFSET_X = 36f;
    private static final float HITBOX_OFFSET_Y = 19f;

    // ===== ARROWS =====
    private final ArrayList<Arrow> arrows = new ArrayList<>();

    private static final float SHOOT_COOLDOWN = 1.0f;
    private float shootTimer = 0f;
    private int torchCount = 0;

    // ===== STAMINA =====
    private static final float STAMINA_DRAIN_RATE = 40f;
    private static final float STAMINA_IDLE_REGEN_RATE = STAMINA_DRAIN_RATE * 0.8f;
    private static final float STAMINA_WALK_REGEN_RATE = STAMINA_DRAIN_RATE * 0.4f;
    private static final float RUN_UNLOCK_THRESHOLD_RATIO = 0.5f;

    private float stamina = 100f;
    private float maxStamina = 100f;
    private boolean runLocked;

    // ===== HEALTH / DAMAGE =====
    private static final float MAX_HEALTH = 100f;
    private static final float DAMAGE_INVULNERABILITY = 0.45f;
    private static final float HURT_ANIM_TIME = 0.28f;
    private float health = MAX_HEALTH;
    private float damageInvulnTimer;
    private float hurtTimer;
    private float hurtStateTime;

    // ===== SWORD =====
    private static final float SWORD_DAMAGE = 35f;
    private static final float SWORD_COOLDOWN = 0.55f;
    private float swordAttackTimer;
    private float swordAttackStateTime;
    private float swordCooldownTimer;
    private float deathStateTime;

    public Player() {

        position = new Vector2(200, 200);
        bounds = new Rectangle(position.x + HITBOX_OFFSET_X, position.y + HITBOX_OFFSET_Y, HITBOX_WIDTH, HITBOX_HEIGHT);

        int walkingFrameCount = 23;
        int runFrameCount = 12;
        int idleFrameCount = 18;
        int idleBlinkingFrameCount = 18;
        int hurtFrameCount = 12;
        int swordFrameCount = 12;
        int deathFrameCount = 15;

        walkingTextures = new Texture[walkingFrameCount];
        runTextures = new Texture[runFrameCount];
        idleTextures = new Texture[idleFrameCount];
        idleBlinkingTextures = new Texture[idleBlinkingFrameCount];
        hurtTextures = new Texture[hurtFrameCount];
        swordTextures = new Texture[swordFrameCount];
        deathTextures = new Texture[deathFrameCount];

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

        TextureRegion[] hurtFrames = new TextureRegion[hurtFrameCount];
        for (int i = 0; i < hurtFrameCount; i++) {
            hurtTextures[i] = new Texture("Movements/Player/hurt/hurt_" + (i + 1) + ".png");
            hurtFrames[i] = new TextureRegion(hurtTextures[i]);
        }

        TextureRegion[] swordFrames = new TextureRegion[swordFrameCount];
        for (int i = 0; i < swordFrameCount; i++) {
            swordTextures[i] = new Texture("Movements/Player/kicking/kicking_" + (i + 1) + ".png");
            swordFrames[i] = new TextureRegion(swordTextures[i]);
        }

        TextureRegion[] deathFrames = new TextureRegion[deathFrameCount];
        for (int i = 0; i < deathFrameCount; i++) {
            deathTextures[i] = new Texture("Movements/Player/dying/dying_" + (i + 1) + ".png");
            deathFrames[i] = new TextureRegion(deathTextures[i]);
        }

        walkAnimation = new Animation<>(0.025f, walkFrames);
        runAnimation = new Animation<>(0.08f, runFrames);

        idleAnimation = new Animation<>(0.08f, idleFrames);
        idleBlinkingAnimation = new Animation<>(0.08f, idleBlinkingFrames);
        hurtAnimation = new Animation<>(0.05f, hurtFrames);
        swordAnimation = new Animation<>(0.045f, swordFrames);
        deathAnimation = new Animation<>(0.07f, deathFrames);

        swordTexture = new Texture("Vectors/Sword.png");
        swordTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        currentFrame = idleFrames[0];
    }

    // ===== GETTERS =====
    public float getStamina() { return stamina; }
    public float getMaxStamina() { return maxStamina; }
    public float getShootCooldownPercent() { return MathUtils.clamp(1f - (shootTimer / SHOOT_COOLDOWN), 0f, 1f); }
    public float getHealth() { return health; }
    public float getMaxHealth() { return MAX_HEALTH; }
    public float getHealthRatio() { return MathUtils.clamp(health / MAX_HEALTH, 0f, 1f); }
    public float getSwordDamage() { return SWORD_DAMAGE; }

    public Vector2 getPosition() { return position; }
    public Vector2 getPos() { return position; }
    public Rectangle getBounds() { return bounds; }
    public ArrayList<Arrow> getArrows() { return arrows; }
    public int getTorchCount() { return torchCount; }

    public void addTorch() {
        torchCount++;
    }

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

        if (!isAlive()) {
            deathStateTime += delta;
            currentFrame = deathAnimation.getKeyFrame(deathStateTime, false);
            updateBoundsPosition();
            return;
        }

        if (damageInvulnTimer > 0f) {
            damageInvulnTimer -= delta;
        }

        if (hurtTimer > 0f) {
            hurtTimer -= delta;
            hurtStateTime += delta;
        }

        if (swordCooldownTimer > 0f) {
            swordCooldownTimer -= delta;
        }

        if (swordAttackTimer > 0f) {
            swordAttackTimer -= delta;
            swordAttackStateTime += delta;

            if (swordAttackTimer <= 0f) {
                swordDamageConsumed = false;
            }
        }

        if (isAlive() && (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)
            || Gdx.input.isKeyJustPressed(Input.Keys.F))
            && swordCooldownTimer <= 0f && swordAttackTimer <= 0f) {

            swordAttackTimer = swordAnimation.getAnimationDuration();
            swordAttackStateTime = 0f;
            swordCooldownTimer = SWORD_COOLDOWN;
            swordDamageConsumed = false;
        }

        updateRunLockState();
        boolean moved = handleMovement(delta);
        stateTime += delta;

        // ===== ANIMATION =====
        if (hurtTimer > 0f) {
            currentFrame = hurtAnimation.getKeyFrame(hurtStateTime, false);
        } else if (swordAttackTimer > 0f) {
            currentFrame = swordAnimation.getKeyFrame(swordAttackStateTime, false);
        } else if (moved) {
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

        updateBoundsPosition();

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

    public void takeDamage(float damage) {
        if (damage <= 0f || !isAlive() || damageInvulnTimer > 0f) {
            return;
        }

        health = Math.max(0f, health - damage);

        if (!isAlive()) {
            hurtTimer = 0f;
            deathStateTime = 0f;
            return;
        }

        damageInvulnTimer = DAMAGE_INVULNERABILITY;
        hurtTimer = HURT_ANIM_TIME;
        hurtStateTime = 0f;
    }

    public boolean isAlive() {
        return health > 0f;
    }

    public boolean canDealSwordDamage() {
        if (swordAttackTimer <= 0f || swordDamageConsumed) {
            return false;
        }

        float duration = swordAnimation.getAnimationDuration();
        float progress = 1f - (swordAttackTimer / duration);
        return progress >= 0.28f && progress <= 0.62f;
    }

    public void consumeSwordDamage() {
        swordDamageConsumed = true;
    }

    public Rectangle getSwordHitbox() {
        float hitW = 78f;
        float hitH = 60f;
        float hitX = facingRight ? position.x + WIDTH * 0.62f : position.x - hitW + WIDTH * 0.38f;
        float hitY = position.y + HEIGHT * 0.24f;
        return new Rectangle(hitX, hitY, hitW, hitH);
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

        boolean xBlocked = false;
        Rectangle xBounds = new Rectangle(newX + HITBOX_OFFSET_X, position.y + HITBOX_OFFSET_Y, HITBOX_WIDTH, HITBOX_HEIGHT);
        if (boundaries != null) {
            for (Rectangle wall : boundaries) {
                if (xBounds.overlaps(wall)) { xBlocked = true; break; }
            }
        }
        if (!xBlocked) position.x = newX;

        boolean yBlocked = false;
        Rectangle yBounds = new Rectangle(position.x + HITBOX_OFFSET_X, newY + HITBOX_OFFSET_Y, HITBOX_WIDTH, HITBOX_HEIGHT);
        if (boundaries != null) {
            for (Rectangle wall : boundaries) {
                if (yBounds.overlaps(wall)) { yBlocked = true; break; }
            }
        }
        if (!yBlocked) position.y = newY;

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

    private void updateBoundsPosition() {
        bounds.setPosition(position.x + HITBOX_OFFSET_X, position.y + HITBOX_OFFSET_Y);
    }

    public void render(SpriteBatch batch) {

        float drawWidth = facingRight ? WIDTH : -WIDTH;
        float drawX = facingRight ? position.x : position.x + WIDTH;

        batch.draw(currentFrame, drawX, position.y, drawWidth, HEIGHT);

        if (swordAttackTimer > 0f) {
            float duration = swordAnimation.getAnimationDuration();
            float progress = 1f - (swordAttackTimer / duration);

            float originX = 14f;
            float originY = 8f;
            float swordW = 70f;
            float swordH = 22f;

            float anchorX = facingRight ? position.x + WIDTH * 0.68f : position.x + WIDTH * 0.32f;
            float anchorY = position.y + HEIGHT * 0.56f;

            float startAngle = facingRight ? -80f : 260f;
            float endAngle = facingRight ? 40f : 140f;
            float angle = MathUtils.lerp(startAngle, endAngle, MathUtils.clamp(progress, 0f, 1f));

            batch.draw(
                swordTexture,
                anchorX - originX,
                anchorY - originY,
                originX,
                originY,
                swordW,
                swordH,
                facingRight ? 1f : -1f,
                1f,
                angle,
                0,
                0,
                swordTexture.getWidth(),
                swordTexture.getHeight(),
                false,
                false
            );
        }

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
        for (Texture t : hurtTextures) t.dispose();
        for (Texture t : swordTextures) t.dispose();
        for (Texture t : deathTextures) t.dispose();
        swordTexture.dispose();
    }
}
