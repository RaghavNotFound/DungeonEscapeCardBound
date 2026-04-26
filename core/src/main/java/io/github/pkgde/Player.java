package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;
import java.util.ArrayList;

public class Player {

    private final Animation<TextureRegion> walkAnimation, runAnimation, idleAnimation;
    private final Animation<TextureRegion> idleBlinkingAnimation, hurtAnimation;
    private final Animation<TextureRegion> swordAnimation, deathAnimation;
    private final Texture[] walkingTextures, runTextures, idleTextures, idleBlinkingTextures;
    private final Texture[] hurtTextures, swordTextures, deathTextures;
    private final Texture swordTexture;

    private TextureRegion currentFrame;
    private float stateTime, idleLoopTime;

    private final Vector2 position;
    public Rectangle bounds;
    private final Rectangle swordHitbox = new Rectangle();
    private ArrayList<Rectangle> boundaries;
    private ArrayList<Polygon> collisionPolygons;
    private float worldMinX = 0f, worldMinY = 0f, worldMaxX = Float.MAX_VALUE, worldMaxY = Float.MAX_VALUE;

    // Reusable temp objects — avoids per-frame GC pressure
    private final Rectangle tmpCollisionRect = new Rectangle();
    private final Polygon tmpCollisionPoly = new Polygon(new float[8]);
    private final Vector2 tmpKnockback = new Vector2();

    public static final float ENTITY_SCALE = 0.16f;

    private final float WIDTH = 128f * ENTITY_SCALE;
    private final float HEIGHT = 128f * ENTITY_SCALE;
    private final float HITBOX_WIDTH = 55f * ENTITY_SCALE;
    private final float HITBOX_HEIGHT = 80f * ENTITY_SCALE;
    private final float HITBOX_OFFSET_X = (128f - 55f) / 2f * ENTITY_SCALE;
    private final float HITBOX_OFFSET_Y = 19f * ENTITY_SCALE;

    private boolean isRunning;
    private boolean facingRight = true;
    private Vector2 knockbackVelocity = new Vector2();
    private static final float KNOCKBACK_FRICTION = 600f;
    private boolean sinkingInLava = false;
    private float sinkOffset = 0f;
    private float lavaRotation = 0f;
    private float targetLavaRotation = 0f;
    private Vector2 lastVelocity = new Vector2();
    private Vector2 lavaVelocity = new Vector2();

    // --- LAVA TICK DAMAGE ---
    private static final float LAVA_DAMAGE_PER_TICK = 8f;   // HP removed each tick
    private static final float LAVA_TICK_INTERVAL = 0.25f;   // seconds between ticks
    private float lavaDamageTimer = 0f;                       // accumulator for tick timing
    private boolean inLava = false;                           // whether player is currently in lava

    // --- JUMP MECHANIC ---
    private static final float JUMP_DURATION = 0.45f;      // total airtime in seconds
    private static final float JUMP_MAX_HEIGHT = 18f;       // peak visual height (pixels)
    private static final float JUMP_STAMINA_COST = 40f;
    private static final float JUMP_COOLDOWN = 0.3f;
    private static final float JUMP_FIXED_DISTANCE = 18f;   // total horizontal distance in pixels
    private boolean isJumping = false;
    private float jumpTimer = 0f;
    private float jumpCooldownTimer = 0f;
    private float jumpHeight = 0f;  // current visual offset (parabolic arc)
    private Vector2 jumpDirection = new Vector2(); // locked movement direction at jump start
    private float jumpDistanceTravelled = 0f;      // how far we've moved during this jump

    private static final float DASH_DURATION = 0.22f, DASH_SPEED_MULT = 3.8f;
    private static final float DASH_STAMINA_COST = 30f, DASH_COOLDOWN = 0.6f;
    private float dashTimer, dashCooldownTimer;
    private Vector2 dashDirection = new Vector2();
    private boolean isDashing;

    private float stamina = 100f, maxStamina = 100f;
    private boolean runLocked;
    private static final float STAMINA_DRAIN_RATE = 40f;
    private static final float STAMINA_IDLE_REGEN_RATE = STAMINA_DRAIN_RATE * 0.8f;
    private static final float STAMINA_WALK_REGEN_RATE = STAMINA_DRAIN_RATE * 0.4f;
    private static final float RUN_UNLOCK_THRESHOLD_RATIO = 0.5f;

    private static final float MAX_HEALTH = 100f, DAMAGE_INVULNERABILITY = 0.45f, HURT_ANIM_TIME = 0.28f;
    private float health = MAX_HEALTH, animatedHealth = MAX_HEALTH, damageInvulnTimer, hurtTimer, hurtStateTime;

    private static final float SWORD_DAMAGE = 35f, SWORD_COOLDOWN = 0.55f;
    private float swordAttackTimer, swordAttackStateTime, swordCooldownTimer, deathStateTime;
    private boolean swordDamageConsumed;

    private final ArrayList<Arrow> arrows = new ArrayList<>();
    private static final float SHOOT_COOLDOWN = 1.0f;
    private float shootTimer = 0f;

    private int torchCount = 0;
    private int cardsCount = 0;

    private final ArrayList<String> inventoryOrder = new ArrayList<>();

    private int enemiesKilled = 0;
    private float timeSurvived = 0f;

    public static void queueAssets(com.badlogic.gdx.assets.AssetManager manager) {
        int walkCount = 23, runCount = 12, idleCount = 18, blinkCount = 18;
        int hurtCount = 12, swordCount = 12, deathCount = 15;

        for (int i = 0; i < walkCount; i++) manager.load("Movements/Player/walking/walking_" + (i + 1) + ".png", Texture.class);
        for (int i = 0; i < runCount; i++) manager.load("Movements/Player/running/running_" + (i + 1) + ".png", Texture.class);
        for (int i = 0; i < idleCount; i++) manager.load("Movements/Player/idle/idle_" + (i + 1) + ".png", Texture.class);
        for (int i = 0; i < blinkCount; i++) manager.load("Movements/Player/idleBlinking/idleBlinking_" + (i + 1) + ".png", Texture.class);
        for (int i = 0; i < hurtCount; i++) manager.load("Movements/Player/hurt/hurt_" + (i + 1) + ".png", Texture.class);
        for (int i = 0; i < swordCount; i++) manager.load("Movements/Player/kicking/kicking_" + (i + 1) + ".png", Texture.class);
        for (int i = 0; i < deathCount; i++) manager.load("Movements/Player/dying/dying_" + (i + 1) + ".png", Texture.class);

        manager.load("Vectors/Sword.png", Texture.class);

        // FIXED: ADDED ARROW QUEUE TO PREVENT CRASH!
        manager.load("Vectors/Arrow.png", Texture.class);
    }

    public Player() {
        position = new Vector2(200, 200);
        bounds = new Rectangle(position.x + HITBOX_OFFSET_X, position.y + HITBOX_OFFSET_Y, HITBOX_WIDTH, HITBOX_HEIGHT);

        int walkCount = 23, runCount = 12, idleCount = 18, blinkCount = 18;
        int hurtCount = 12, swordCount = 12, deathCount = 15;

        walkingTextures = new Texture[walkCount];
        runTextures = new Texture[runCount];
        idleTextures = new Texture[idleCount];
        idleBlinkingTextures = new Texture[blinkCount];
        hurtTextures = new Texture[hurtCount];
        swordTextures = new Texture[swordCount];
        deathTextures = new Texture[deathCount];

        TextureRegion[] walkFrames = new TextureRegion[walkCount];
        TextureRegion[] runFrames = new TextureRegion[runCount];
        TextureRegion[] idleFrames = new TextureRegion[idleCount];
        TextureRegion[] blinkFrames = new TextureRegion[blinkCount];

        for (int i = 0; i < walkCount; i++) {
            walkingTextures[i] = Main.assets.get("Movements/Player/walking/walking_" + (i + 1) + ".png", Texture.class);
            walkFrames[i] = new TextureRegion(walkingTextures[i]);
        }
        for (int i = 0; i < runCount; i++) {
            runTextures[i] = Main.assets.get("Movements/Player/running/running_" + (i + 1) + ".png", Texture.class);
            runFrames[i] = new TextureRegion(runTextures[i]);
        }
        for (int i = 0; i < idleCount; i++) {
            idleTextures[i] = Main.assets.get("Movements/Player/idle/idle_" + (i + 1) + ".png", Texture.class);
            idleFrames[i] = new TextureRegion(idleTextures[i]);
        }
        for (int i = 0; i < blinkCount; i++) {
            idleBlinkingTextures[i] = Main.assets.get("Movements/Player/idleBlinking/idleBlinking_" + (i + 1) + ".png", Texture.class);
            blinkFrames[i] = new TextureRegion(idleBlinkingTextures[i]);
        }

        TextureRegion[] hFrames = new TextureRegion[hurtCount];
        for (int i = 0; i < hurtCount; i++) {
            hurtTextures[i] = Main.assets.get("Movements/Player/hurt/hurt_" + (i + 1) + ".png", Texture.class);
            hFrames[i] = new TextureRegion(hurtTextures[i]);
        }
        TextureRegion[] sFrames = new TextureRegion[swordCount];
        for (int i = 0; i < swordCount; i++) {
            swordTextures[i] = Main.assets.get("Movements/Player/kicking/kicking_" + (i + 1) + ".png", Texture.class);
            sFrames[i] = new TextureRegion(swordTextures[i]);
        }
        TextureRegion[] dFrames = new TextureRegion[deathCount];
        for (int i = 0; i < deathCount; i++) {
            deathTextures[i] = Main.assets.get("Movements/Player/dying/dying_" + (i + 1) + ".png", Texture.class);
            dFrames[i] = new TextureRegion(deathTextures[i]);
        }

        walkAnimation = new Animation<>(0.025f, walkFrames);
        runAnimation = new Animation<>(0.08f, runFrames);
        idleAnimation = new Animation<>(0.08f, idleFrames);
        idleBlinkingAnimation = new Animation<>(0.08f, blinkFrames);
        hurtAnimation = new Animation<>(0.05f, hFrames);
        swordAnimation = new Animation<>(0.045f, sFrames);
        deathAnimation = new Animation<>(0.07f, dFrames);

        swordTexture = Main.assets.get("Vectors/Sword.png", Texture.class);
        swordTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        currentFrame = idleFrames[0];
    }

    public float getStamina() { return stamina; }
    public float getMaxStamina() { return maxStamina; }
    public float getShootCooldownPercent() { return MathUtils.clamp(1f - (shootTimer / SHOOT_COOLDOWN), 0f, 1f); }
    public float getHealth() { return health; }
    public float getAnimatedHealth() { return animatedHealth; }
    public float getMaxHealth() { return MAX_HEALTH; }
    public float getHealthRatio() { return MathUtils.clamp(health / MAX_HEALTH, 0f, 1f); }
    public float getAnimatedHealthRatio() { return MathUtils.clamp(animatedHealth / MAX_HEALTH, 0f, 1f); }
    public void addHealth(float amount) { health = Math.min(health + amount, MAX_HEALTH); }
    public float getSwordDamage() { return SWORD_DAMAGE; }
    public Vector2 getPosition() { return position; }
    public Rectangle getBounds() { return bounds; }
    public ArrayList<Arrow> getArrows() { return arrows; }
    public int getTorchCount() { return torchCount; }
    public boolean hasTorch() { return torchCount > 0; }
    public int getCardsCount() { return cardsCount; }
    public int getEnemiesKilled() { return enemiesKilled; }
    public float getTimeSurvived() { return timeSurvived; }
    public ArrayList<String> getInventoryOrder() { return inventoryOrder; }

    public void addTorch() {
        torchCount++;
        if (!inventoryOrder.contains("Torch")) inventoryOrder.add("Torch");
    }
    public void addCard() {
        cardsCount++;
        if (!inventoryOrder.contains("Card")) inventoryOrder.add("Card");
    }
    public void removeTorch() {
        if (torchCount > 0) {
            torchCount--;
            if (torchCount == 0) inventoryOrder.remove("Torch");
        }
    }

    public void addStamina(float amount) { stamina = Math.min(stamina + amount, maxStamina); }
    public void incrementEnemiesKilled() { enemiesKilled++; }

    public void setBoundaries(ArrayList<Rectangle> b) { this.boundaries = b; }
    public void setCollisionPolygons(ArrayList<Polygon> p) { this.collisionPolygons = p; }
    public void setWorldBounds(float minX, float minY, float maxX, float maxY) {
        this.worldMinX = minX; this.worldMinY = minY;
        this.worldMaxX = maxX; this.worldMaxY = maxY;
    }

    public void setPosition(float x, float y) { position.set(x, y); updateBoundsPosition(); }
    public void setHealth(float h) { health = h; animatedHealth = h; }
    public void setStamina(float s) { stamina = s; }

    public void setTorchCount(int t) {
        torchCount = t;
        if (t > 0 && !inventoryOrder.contains("Torch")) inventoryOrder.add("Torch");
    }
    public void setCardsCount(int c) {
        cardsCount = c;
        if (c > 0 && !inventoryOrder.contains("Card")) inventoryOrder.add("Card");
    }

    public void setEnemiesKilled(int k) { enemiesKilled = k; }
    public void setTimeSurvived(float t) { timeSurvived = t; }

    public void triggerLavaDeath() {
        if (!isAlive()) return;
        health = 0;
        sinkingInLava = true;
        deathStateTime = 0f;
        hurtTimer = 0f;
        lavaVelocity.set(lastVelocity).scl(0.4f);
        if (Math.abs(lavaVelocity.x) > Math.abs(lavaVelocity.y)) {
            targetLavaRotation = lavaVelocity.x > 0 ? -90f : 90f;
        } else {
            targetLavaRotation = 0f;
        }
        lavaRotation = 0f;
    }

    /**
     * Apply tick damage while standing in lava.
     * Called every frame from GameWorld while the player overlaps lava.
     * When health hits 0, automatically triggers the lava sinking death.
     */
    public void applyLavaDamage(float delta) {
        if (!isAlive()) return;
        lavaDamageTimer += delta;
        while (lavaDamageTimer >= LAVA_TICK_INTERVAL) {
            lavaDamageTimer -= LAVA_TICK_INTERVAL;
            health = Math.max(0f, health - LAVA_DAMAGE_PER_TICK);
            // Brief hurt flash on each tick (but don't override an existing longer hurt)
            if (hurtTimer <= 0f) {
                hurtTimer = 0.15f;
                hurtStateTime = 0f;
            }
            if (!isAlive()) {
                triggerLavaDeath();
                return;
            }
        }
    }

    /** Reset lava tick timer when the player leaves lava. */
    public void resetLavaDamage() {
        lavaDamageTimer = 0f;
        inLava = false;
    }

    public void setInLava(boolean inLava) { this.inLava = inLava; }
    public boolean isInLava() { return inLava; }

    public void update(float delta, OrthographicCamera camera) {
        timeSurvived += delta;

        if (animatedHealth > health) {
            animatedHealth -= 25f * delta;
            if (animatedHealth < health) {
                animatedHealth = health;
            }
        } else if (animatedHealth < health) {
            animatedHealth = health;
        }

        if (!isAlive()) {
            if (sinkingInLava) {
                sinkOffset += 30f * delta;
                position.add(lavaVelocity.x * delta, lavaVelocity.y * delta);
                lavaVelocity.scl(0.95f);
                lavaRotation = MathUtils.lerp(lavaRotation, targetLavaRotation, 2.5f * delta);
            }
            deathStateTime += delta;
            currentFrame = deathAnimation.getKeyFrame(deathStateTime, false);
            updateBoundsPosition();
            return;
        }

        if (damageInvulnTimer > 0f) damageInvulnTimer -= delta;
        if (hurtTimer > 0f) { hurtTimer -= delta; hurtStateTime += delta; }
        if (swordCooldownTimer > 0f) swordCooldownTimer -= delta;
        if (dashCooldownTimer > 0f) dashCooldownTimer -= delta;
        if (jumpCooldownTimer > 0f) jumpCooldownTimer -= delta;
        if (shootTimer > 0) shootTimer -= delta;

        if (dashTimer > 0f) {
            dashTimer -= delta;
            if (dashTimer <= 0f) isDashing = false;
            else damageInvulnTimer = Math.max(damageInvulnTimer, 0.1f);
        }

        // --- JUMP: SPACE triggers a jump arc ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && !isJumping && jumpCooldownTimer <= 0f
            && stamina >= JUMP_STAMINA_COST && hurtTimer <= 0f && !isDashing) {
            isJumping = true;
            jumpTimer = 0f;
            jumpDistanceTravelled = 0f;
            stamina -= JUMP_STAMINA_COST;
            jumpCooldownTimer = JUMP_COOLDOWN;
            // Lock the movement direction at the moment of jump
            float dx = 0, dy = 0;
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) dy = 1;
            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) dy = -1;
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dx = -1;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx = 1;
            if (dx == 0 && dy == 0) dx = facingRight ? 1 : -1; // default to facing direction
            jumpDirection.set(dx, dy).nor();
        }

        // Update jump arc (parabola: h = 4*H*t*(1-t) where t goes 0→1)
        if (isJumping) {
            jumpTimer += delta;
            float t = jumpTimer / JUMP_DURATION;
            if (t >= 1f) {
                isJumping = false;
                jumpTimer = 0f;
                jumpHeight = 0f;
            } else {
                jumpHeight = 4f * JUMP_MAX_HEIGHT * t * (1f - t);
            }
        }

        // --- DASH: Q triggers a dash ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q) && stamina >= DASH_STAMINA_COST && !isDashing && dashCooldownTimer <= 0f && hurtTimer <= 0f && !isJumping) {
            stamina -= DASH_STAMINA_COST;
            dashTimer = DASH_DURATION;
            dashCooldownTimer = DASH_COOLDOWN;
            isDashing = true;
            float dx = 0, dy = 0;
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) dy = 1;
            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) dy = -1;
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dx = -1;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx = 1;
            if (dx == 0 && dy == 0) dx = facingRight ? 1 : -1;
            dashDirection.set(dx, dy).nor();
        }

        if (swordAttackTimer > 0f) {
            swordAttackTimer -= delta;
            swordAttackStateTime += delta;
            if (swordAttackTimer <= 0f) swordDamageConsumed = false;
        }
        if (isAlive() && (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.F)) && swordCooldownTimer <= 0f && swordAttackTimer <= 0f) {
            swordAttackTimer = swordAnimation.getAnimationDuration();
            swordAttackStateTime = 0f;
            swordCooldownTimer = SWORD_COOLDOWN;
            swordDamageConsumed = false;
        }

        float oldX = position.x, oldY = position.y;
        updateRunLockState();
        boolean moved = handleMovement(delta);
        if (isAlive() && delta > 0f) {
            lastVelocity.set(position.x - oldX, position.y - oldY).scl(1f / delta);
        }
        stateTime += delta;

        if (hurtTimer > 0f) {
            currentFrame = hurtAnimation.getKeyFrame(hurtStateTime, false);
        } else if (swordAttackTimer > 0f) {
            currentFrame = swordAnimation.getKeyFrame(swordAttackStateTime, false);
        } else if (moved) {
            idleLoopTime = 0f;
            currentFrame = isRunning ? runAnimation.getKeyFrame(stateTime, true) : walkAnimation.getKeyFrame(stateTime, true);
        } else {
            idleLoopTime += delta;
            currentFrame = getIdleLoopFrame(idleLoopTime);
        }

        boolean runningNow = isRunning && moved && stamina > 0;
        if (!isDashing) {
            if (runningNow) stamina -= STAMINA_DRAIN_RATE * delta;
            else if (moved) stamina += STAMINA_WALK_REGEN_RATE * delta;
            else stamina += STAMINA_IDLE_REGEN_RATE * delta;
        }
        stamina = MathUtils.clamp(stamina, 0, maxStamina);
        updateRunLockState();
        updateBoundsPosition();

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && shootTimer <= 0f) {
            shootArrow(camera);
            shootTimer = SHOOT_COOLDOWN;
        }

        for (int i = arrows.size() - 1; i >= 0; i--) {
            Arrow a = arrows.get(i);
            a.update(delta);
            if (a.isCollided(worldMaxX, worldMaxY, boundaries == null ? new ArrayList<Rectangle>() : boundaries)) {
                arrows.remove(i);
            }
        }
    }

    public void takeDamage(float damage) {
        if (damage <= 0f || !isAlive() || damageInvulnTimer > 0f) return;
        health = Math.max(0f, health - damage);
        if (!isAlive()) { hurtTimer = 0f; deathStateTime = 0f; return; }
        damageInvulnTimer = DAMAGE_INVULNERABILITY;
        hurtTimer = HURT_ANIM_TIME;
        hurtStateTime = 0f;
    }

    public void applyKnockback(Vector2 forceDir, float forceAmt) {
        if (!isAlive()) return;
        knockbackVelocity.add(tmpKnockback.set(forceDir).nor().scl(forceAmt));
    }

    public boolean isAlive() { return health > 0f; }
    public boolean isDeathAnimationFinished() { return !isAlive() && deathAnimation.isAnimationFinished(deathStateTime); }
    public boolean isAirborne() { return isJumping && jumpHeight > JUMP_MAX_HEIGHT * 0.15f; }
    public float getJumpHeight() { return jumpHeight; }

    public boolean canDealSwordDamage() {
        if (swordAttackTimer <= 0f || swordDamageConsumed) return false;
        float progress = 1f - (swordAttackTimer / swordAnimation.getAnimationDuration());
        return progress >= 0.28f && progress <= 0.62f;
    }

    public void consumeSwordDamage() { swordDamageConsumed = true; }

    public Rectangle getSwordHitbox() {
        float hitW = 78f * ENTITY_SCALE, hitH = 60f * ENTITY_SCALE;
        float hitX = facingRight ? position.x + WIDTH * 0.62f : position.x - hitW + WIDTH * 0.38f;
        float hitY = position.y + HEIGHT * 0.24f;
        swordHitbox.set(hitX, hitY, hitW, hitH);
        return swordHitbox;
    }

    private void updateRunLockState() {
        if (stamina <= 0f) runLocked = true;
        else if (runLocked && stamina >= maxStamina * RUN_UNLOCK_THRESHOLD_RATIO) runLocked = false;
    }

    private TextureRegion getIdleLoopFrame(float time) {
        float iDur = idleAnimation.getAnimationDuration();
        float bDur = idleBlinkingAnimation.getAnimationDuration();
        float cycleTime = time % (iDur + bDur);
        return (cycleTime < iDur) ? idleAnimation.getKeyFrame(cycleTime, false) : idleBlinkingAnimation.getKeyFrame(cycleTime - iDur, false);
    }

    private boolean handleMovement(float delta) {
        float oldX = position.x, oldY = position.y;
        float newX = position.x, newY = position.y;

        if (knockbackVelocity.len2() > 0) {
            float speed = knockbackVelocity.len() - KNOCKBACK_FRICTION * delta;
            if (speed <= 0) knockbackVelocity.setZero();
            else {
                knockbackVelocity.setLength(speed);
                newX += knockbackVelocity.x * delta;
                newY += knockbackVelocity.y * delta;
            }
        }

        if (isJumping) {
            // During a jump, use the locked direction at a fixed speed so total
            // displacement across the full jump equals JUMP_FIXED_DISTANCE pixels.
            float jumpSpeed = JUMP_FIXED_DISTANCE / JUMP_DURATION;
            float step = jumpSpeed * delta;
            float remaining = JUMP_FIXED_DISTANCE - jumpDistanceTravelled;
            if (step > remaining) step = remaining;
            newX += jumpDirection.x * step;
            newY += jumpDirection.y * step;
            jumpDistanceTravelled += step;
            if (jumpDirection.x != 0) facingRight = jumpDirection.x > 0;
            isRunning = false;
        } else if (isDashing) {
            newX += dashDirection.x * 100f * DASH_SPEED_MULT * delta;
            newY += dashDirection.y * 100f * DASH_SPEED_MULT * delta;
            facingRight = (dashDirection.x != 0) ? (dashDirection.x > 0) : facingRight;
            isRunning = true;
        } else if (hurtTimer <= 0f) {
            boolean up = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
            boolean down = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
            boolean left = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
            boolean right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
            isRunning = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && !runLocked;
            float speed = isRunning ? 150f : 100f;

            if (up) newY += speed * delta;
            if (down) newY -= speed * delta;
            if (left) { newX -= speed * delta; facingRight = false; }
            if (right) { newX += speed * delta; facingRight = true; }
        } else {
            isRunning = false;
        }

        newX = MathUtils.clamp(newX, worldMinX, worldMaxX - WIDTH);
        newY = MathUtils.clamp(newY, worldMinY, worldMaxY - HEIGHT);

        // Always check wall collisions (even while jumping — only lava is skippable)
        float hx = newX + HITBOX_OFFSET_X;
        tmpCollisionRect.set(hx, position.y + HITBOX_OFFSET_Y, HITBOX_WIDTH, HITBOX_HEIGHT);
        if (!collides(tmpCollisionRect)) position.x = newX;

        hx = position.x + HITBOX_OFFSET_X;
        tmpCollisionRect.set(hx, newY + HITBOX_OFFSET_Y, HITBOX_WIDTH, HITBOX_HEIGHT);
        if (!collides(tmpCollisionRect)) position.y = newY;

        return !MathUtils.isEqual(oldX, position.x, 0.001f) || !MathUtils.isEqual(oldY, position.y, 0.001f);
    }

    private boolean collides(Rectangle next) {
        if (boundaries != null) for (Rectangle r : boundaries) if (next.overlaps(r)) return true;
        if (collisionPolygons != null) {
            float[] v = tmpCollisionPoly.getVertices();
            v[0] = next.x;                    v[1] = next.y;
            v[2] = next.x + next.width;        v[3] = next.y;
            v[4] = next.x + next.width;        v[5] = next.y + next.height;
            v[6] = next.x;                     v[7] = next.y + next.height;
            tmpCollisionPoly.setVertices(v);
            for (Polygon poly : collisionPolygons) if (Intersector.overlapConvexPolygons(tmpCollisionPoly, poly)) return true;
        }
        return false;
    }

    private void shootArrow(OrthographicCamera cam) {
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        cam.unproject(mouse);
        Vector2 dir = new Vector2(mouse.x - (position.x + WIDTH / 2f), mouse.y - (position.y + HEIGHT / 2f)).nor();
        arrows.add(new Arrow(position.x + WIDTH / 2f, position.y + HEIGHT / 2f, dir));
    }

    private void updateBoundsPosition() {
        bounds.setPosition(position.x + HITBOX_OFFSET_X, position.y + HITBOX_OFFSET_Y);
    }

    public void render(SpriteBatch batch) {
        float dW = facingRight ? WIDTH : -WIDTH;
        float dX = facingRight ? position.x : position.x + WIDTH;

        // --- Draw jump shadow on the ground ---
        if (isJumping && jumpHeight > 0.5f) {
            float shadowScale = 1f - (jumpHeight / JUMP_MAX_HEIGHT) * 0.4f; // shadow shrinks at peak
            float shadowW = HITBOX_WIDTH * shadowScale;
            float shadowH = 3f;
            float shadowX = position.x + HITBOX_OFFSET_X + (HITBOX_WIDTH - shadowW) * 0.5f;
            float shadowY = position.y + HITBOX_OFFSET_Y - 1f;
            // We draw the shadow using the batch's color tint (semi-transparent black oval)
            Color prev = batch.getColor().cpy();
            float shadowAlpha = 0.35f * shadowScale;
            batch.setColor(0f, 0f, 0f, shadowAlpha);
            // Use the current frame as a hacky 1px slice for the shadow shape
            batch.draw(currentFrame, shadowX, shadowY, shadowW, shadowH);
            batch.setColor(prev);
        }

        if (sinkingInLava) {
            float renderedHeight = Math.max(0, HEIGHT - sinkOffset);
            TextureRegion cropped = new TextureRegion(currentFrame);
            float cropRatio = renderedHeight / HEIGHT;

            cropped.setRegionHeight((int)(cropped.getRegionHeight() * cropRatio));

            float originX = facingRight ? WIDTH / 2f : -WIDTH / 2f;
            float originY = renderedHeight / 2f;

            batch.draw(cropped, dX, position.y + sinkOffset, originX, originY, dW, renderedHeight, 1f, 1f, lavaRotation);
        } else {
            // Offset sprite upward by jumpHeight when airborne
            batch.draw(currentFrame, dX, position.y + jumpHeight, dW, HEIGHT);
        }

        if (swordAttackTimer > 0f) {
            float prog = 1f - (swordAttackTimer / swordAnimation.getAnimationDuration());
            float ox = 14f * ENTITY_SCALE, oy = 8f * ENTITY_SCALE, swW = 70f * ENTITY_SCALE, swH = 22f * ENTITY_SCALE;
            float ax = facingRight ? position.x + WIDTH * 0.68f : position.x + WIDTH * 0.32f;
            float ay = position.y + HEIGHT * 0.56f;
            float start = facingRight ? -80f : 260f, end = facingRight ? 40f : 140f;
            float angle = MathUtils.lerp(start, end, MathUtils.clamp(prog, 0f, 1f));

            batch.draw(swordTexture, ax - ox, ay - oy, ox, oy, swW, swH, facingRight ? 1f : -1f, 1f, angle, 0, 0, swordTexture.getWidth(), swordTexture.getHeight(), false, false);
        }
        for (Arrow a : arrows) a.render(batch);
    }

    public void dispose() { }
}
