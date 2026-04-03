package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;
import java.util.ArrayList;

/**
 * Represents the playable character in DungeonEscapeCardbound.
 * Handles movement, stamina-based dashing/running, sword and bow combat,
 * and state-based animations.
 */
public class Player {

    // ===== ANIMATIONS =====
    private final Animation<TextureRegion> walkAnimation;
    private final Animation<TextureRegion> runAnimation;
    private final Animation<TextureRegion> idleAnimation;
    private final Animation<TextureRegion> idleBlinkingAnimation;
    private final Animation<TextureRegion> hurtAnimation;
    private final Animation<TextureRegion> swordAnimation;
    private final Animation<TextureRegion> deathAnimation;

    private final Texture[] walkingTextures, runTextures, idleTextures, idleBlinkingTextures;
    private final Texture[] hurtTextures, swordTextures, deathTextures;
    private final Texture swordTexture;

    private TextureRegion currentFrame;
    private float stateTime;
    private float idleLoopTime;

    // ===== PHYSICS & COLLISION =====
    private final Vector2 position;
    public Rectangle bounds;
    private final Rectangle swordHitbox = new Rectangle();
    private ArrayList<Rectangle> boundaries;
    private ArrayList<Polygon> collisionPolygons;
    private float worldMinX = 0f, worldMinY = 0f, worldMaxX = Float.MAX_VALUE, worldMaxY = Float.MAX_VALUE;

    private final int WIDTH = 128;
    private final int HEIGHT = 128;
    private static final float HITBOX_WIDTH = 55f;
    private static final float HITBOX_HEIGHT = 80f;
    private static final float HITBOX_OFFSET_X = 36f;
    private static final float HITBOX_OFFSET_Y = 19f;

    // ===== MOVEMENT STATE =====
    private boolean isRunning;
    private boolean facingRight = true;
    private Vector2 knockbackVelocity = new Vector2();
    private static final float KNOCKBACK_FRICTION = 600f;

    // ===== DASH =====
    private static final float DASH_DURATION = 0.22f;
    private static final float DASH_SPEED_MULT = 3.8f;
    private static final float DASH_STAMINA_COST = 30f;
    private static final float DASH_COOLDOWN = 0.6f;
    private float dashTimer, dashCooldownTimer;
    private Vector2 dashDirection = new Vector2();
    private boolean isDashing;

    // ===== STAMINA =====
    private float stamina = 100f;
    private float maxStamina = 100f;
    private boolean runLocked;
    private static final float STAMINA_DRAIN_RATE = 40f;
    private static final float STAMINA_IDLE_REGEN_RATE = STAMINA_DRAIN_RATE * 0.8f;
    private static final float STAMINA_WALK_REGEN_RATE = STAMINA_DRAIN_RATE * 0.4f;
    private static final float RUN_UNLOCK_THRESHOLD_RATIO = 0.5f;

    // ===== HEALTH & COMBAT =====
    private static final float MAX_HEALTH = 100f;
    private static final float DAMAGE_INVULNERABILITY = 0.45f;
    private static final float HURT_ANIM_TIME = 0.28f;
    private float health = MAX_HEALTH;
    private float damageInvulnTimer, hurtTimer, hurtStateTime;

    private static final float SWORD_DAMAGE = 35f;
    private static final float SWORD_COOLDOWN = 0.55f;
    private float swordAttackTimer, swordAttackStateTime, swordCooldownTimer, deathStateTime;
    private boolean swordDamageConsumed;

    // ===== INVENTORY & ARROWS =====
    private final ArrayList<Arrow> arrows = new ArrayList<>();
    private static final float SHOOT_COOLDOWN = 1.0f;
    private float shootTimer = 0f;
    private int torchCount = 0;
    private int cardsCount = 0;

    // ===== STATS =====
    private int enemiesKilled = 0;
    private float timeSurvived = 0f;

    public Player() {
        position = new Vector2(200, 200);
        bounds = new Rectangle(position.x + HITBOX_OFFSET_X, position.y + HITBOX_OFFSET_Y, HITBOX_WIDTH, HITBOX_HEIGHT);

        // Frame counts
        int walkCount = 23, runCount = 12, idleCount = 18, blinkCount = 18;
        int hurtCount = 12, swordCount = 12, deathCount = 15;

        // Init Texture Arrays
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

        // Resource Loading (Helper pattern could be used, but keeping explicit per project style)
        for (int i = 0; i < walkCount; i++) {
            walkingTextures[i] = new Texture("Movements/Player/walking/walking_" + (i + 1) + ".png");
            walkFrames[i] = new TextureRegion(walkingTextures[i]);
        }
        for (int i = 0; i < runCount; i++) {
            runTextures[i] = new Texture("Movements/Player/running/running_" + (i + 1) + ".png");
            runFrames[i] = new TextureRegion(runTextures[i]);
        }
        for (int i = 0; i < idleCount; i++) {
            idleTextures[i] = new Texture("Movements/Player/idle/idle_" + (i + 1) + ".png");
            idleFrames[i] = new TextureRegion(idleTextures[i]);
        }
        for (int i = 0; i < blinkCount; i++) {
            idleBlinkingTextures[i] = new Texture("Movements/Player/idleBlinking/idleBlinking_" + (i + 1) + ".png");
            blinkFrames[i] = new TextureRegion(idleBlinkingTextures[i]);
        }

        TextureRegion[] hFrames = new TextureRegion[hurtCount];
        for (int i = 0; i < hurtCount; i++) {
            hurtTextures[i] = new Texture("Movements/Player/hurt/hurt_" + (i + 1) + ".png");
            hFrames[i] = new TextureRegion(hurtTextures[i]);
        }
        TextureRegion[] sFrames = new TextureRegion[swordCount];
        for (int i = 0; i < swordCount; i++) {
            swordTextures[i] = new Texture("Movements/Player/kicking/kicking_" + (i + 1) + ".png");
            sFrames[i] = new TextureRegion(swordTextures[i]);
        }
        TextureRegion[] dFrames = new TextureRegion[deathCount];
        for (int i = 0; i < deathCount; i++) {
            deathTextures[i] = new Texture("Movements/Player/dying/dying_" + (i + 1) + ".png");
            dFrames[i] = new TextureRegion(deathTextures[i]);
        }

        walkAnimation = new Animation<>(0.025f, walkFrames);
        runAnimation = new Animation<>(0.08f, runFrames);
        idleAnimation = new Animation<>(0.08f, idleFrames);
        idleBlinkingAnimation = new Animation<>(0.08f, blinkFrames);
        hurtAnimation = new Animation<>(0.05f, hFrames);
        swordAnimation = new Animation<>(0.045f, sFrames);
        deathAnimation = new Animation<>(0.07f, dFrames);

        swordTexture = new Texture("Vectors/Sword.png");
        swordTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        currentFrame = idleFrames[0];
    }

    // ===== GETTERS & SETTERS =====
    public float getStamina() { return stamina; }
    public float getMaxStamina() { return maxStamina; }
    public float getShootCooldownPercent() { return MathUtils.clamp(1f - (shootTimer / SHOOT_COOLDOWN), 0f, 1f); }
    public float getHealth() { return health; }
    public float getMaxHealth() { return MAX_HEALTH; }
    public float getHealthRatio() { return MathUtils.clamp(health / MAX_HEALTH, 0f, 1f); }
    public void addHealth(float amount) { health = Math.min(health + amount, MAX_HEALTH); }
    public float getSwordDamage() { return SWORD_DAMAGE; }
    public Vector2 getPosition() { return position; }
    public Rectangle getBounds() { return bounds; }
    public ArrayList<Arrow> getArrows() { return arrows; }
    public int getTorchCount() { return torchCount; }
    public int getCardsCount() { return cardsCount; }
    public int getEnemiesKilled() { return enemiesKilled; }
    public float getTimeSurvived() { return timeSurvived; }

    public void addTorch() { torchCount++; }
    public void addCard() { cardsCount++; }
    public void addStamina(float amount) { stamina = Math.min(stamina + amount, maxStamina); }
    public void incrementEnemiesKilled() { enemiesKilled++; }

    public void setBoundaries(ArrayList<Rectangle> b) { this.boundaries = b; }
    public void setCollisionPolygons(ArrayList<Polygon> p) { this.collisionPolygons = p; }
    public void setWorldBounds(float minX, float minY, float maxX, float maxY) {
        this.worldMinX = minX; this.worldMinY = minY;
        this.worldMaxX = maxX; this.worldMaxY = maxY;
    }

    // ===== UPDATE =====
    public void update(float delta, OrthographicCamera camera) {
        timeSurvived += delta;

        if (!isAlive()) {
            deathStateTime += delta;
            currentFrame = deathAnimation.getKeyFrame(deathStateTime, false);
            updateBoundsPosition();
            return;
        }

        // Timers
        if (damageInvulnTimer > 0f) damageInvulnTimer -= delta;
        if (hurtTimer > 0f) { hurtTimer -= delta; hurtStateTime += delta; }
        if (swordCooldownTimer > 0f) swordCooldownTimer -= delta;
        if (dashCooldownTimer > 0f) dashCooldownTimer -= delta;
        if (shootTimer > 0) shootTimer -= delta;

        // Dash logic
        if (dashTimer > 0f) {
            dashTimer -= delta;
            if (dashTimer <= 0f) isDashing = false;
            else damageInvulnTimer = Math.max(damageInvulnTimer, 0.1f);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && stamina >= DASH_STAMINA_COST && !isDashing && dashCooldownTimer <= 0f && hurtTimer <= 0f) {
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

        // Sword Input
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

        updateRunLockState();
        boolean moved = handleMovement(delta);
        stateTime += delta;

        // Animation State
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

        // Stamina Regen/Drain
        boolean runningNow = isRunning && moved && stamina > 0;
        if (!isDashing) {
            if (runningNow) stamina -= STAMINA_DRAIN_RATE * delta;
            else if (moved) stamina += STAMINA_WALK_REGEN_RATE * delta;
            else stamina += STAMINA_IDLE_REGEN_RATE * delta;
        }
        stamina = MathUtils.clamp(stamina, 0, maxStamina);
        updateRunLockState();
        updateBoundsPosition();

        // Shooting
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && shootTimer <= 0f) {
            shootArrow(camera);
            shootTimer = SHOOT_COOLDOWN;
        }

        // Arrow Updates
        for (int i = arrows.size() - 1; i >= 0; i--) {
            Arrow a = arrows.get(i);
            a.update(delta);
            if (a.isCollided(worldMaxX, worldMaxY, boundaries == null ? new ArrayList<>() : boundaries, collisionPolygons == null ? new ArrayList<>() : collisionPolygons)) {
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
        knockbackVelocity.add(new Vector2(forceDir).nor().scl(forceAmt));
    }

    public boolean isAlive() { return health > 0f; }
    public boolean isDeathAnimationFinished() { return !isAlive() && deathAnimation.isAnimationFinished(deathStateTime); }

    public boolean canDealSwordDamage() {
        if (swordAttackTimer <= 0f || swordDamageConsumed) return false;
        float progress = 1f - (swordAttackTimer / swordAnimation.getAnimationDuration());
        return progress >= 0.28f && progress <= 0.62f;
    }

    public void consumeSwordDamage() { swordDamageConsumed = true; }

    public Rectangle getSwordHitbox() {
        float hitW = 78f, hitH = 60f;
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

        if (isDashing) {
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

        if (!collides(new Rectangle(newX + HITBOX_OFFSET_X, position.y + HITBOX_OFFSET_Y, HITBOX_WIDTH, HITBOX_HEIGHT))) position.x = newX;
        if (!collides(new Rectangle(position.x + HITBOX_OFFSET_X, newY + HITBOX_OFFSET_Y, HITBOX_WIDTH, HITBOX_HEIGHT))) position.y = newY;

        return !MathUtils.isEqual(oldX, position.x, 0.001f) || !MathUtils.isEqual(oldY, position.y, 0.001f);
    }

    private boolean collides(Rectangle next) {
        if (boundaries != null) for (Rectangle r : boundaries) if (next.overlaps(r)) return true;
        if (collisionPolygons != null) {
            Polygon p = new Polygon(new float[]{next.x, next.y, next.x + next.width, next.y, next.x + next.width, next.y + next.height, next.x, next.y + next.height});
            for (Polygon poly : collisionPolygons) if (Intersector.overlapConvexPolygons(p, poly)) return true;
        }
        return false;
    }

    private void shootArrow(OrthographicCamera cam) {
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        cam.unproject(mouse);
        Vector2 dir = new Vector2(mouse.x - (position.x + WIDTH / 2f), mouse.y - (position.y + HEIGHT / 2f)).nor();
        arrows.add(new Arrow(position.x + WIDTH / 2f, position.y + HEIGHT / 2f, dir));
    }

    private void updateBoundsPosition() { bounds.setPosition(position.x + HITBOX_OFFSET_X, position.y + HITBOX_OFFSET_Y); }

    public void render(SpriteBatch batch) {
        float dW = facingRight ? WIDTH : -WIDTH;
        float dX = facingRight ? position.x : position.x + WIDTH;
        batch.draw(currentFrame, dX, position.y, dW, HEIGHT);

        if (swordAttackTimer > 0f) {
            float prog = 1f - (swordAttackTimer / swordAnimation.getAnimationDuration());
            float ox = 14f, oy = 8f, swW = 70f, swH = 22f;
            float ax = facingRight ? position.x + WIDTH * 0.68f : position.x + WIDTH * 0.32f;
            float ay = position.y + HEIGHT * 0.56f;
            float start = facingRight ? -80f : 260f, end = facingRight ? 40f : 140f;
            float angle = MathUtils.lerp(start, end, MathUtils.clamp(prog, 0f, 1f));

            batch.draw(swordTexture, ax - ox, ay - oy, ox, oy, swW, swH, facingRight ? 1f : -1f, 1f, angle, 0, 0, swordTexture.getWidth(), swordTexture.getHeight(), false, false);
        }
        for (Arrow a : arrows) a.render(batch);
    }

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
