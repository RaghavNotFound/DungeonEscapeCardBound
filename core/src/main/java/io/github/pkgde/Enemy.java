package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;

/**
 * Represents a dungeon enemy with state-based AI, predictive tracking,
 * obstacle avoidance, and telegraphed attacks.
 */
public class Enemy {

    private final Vector2 position;
    private final Rectangle bounds;

    private enum Facing { FRONT, BACK, LEFT, RIGHT }
    private enum State { IDLE, CHASE }

    // Animations
    private Animation<TextureRegion> frontIdleAnim, frontWalkAnim, frontRunAnim, frontHurtAnim, frontAttackAnim;
    private Animation<TextureRegion> backIdleAnim, backWalkAnim, backRunAnim, backHurtAnim, backAttackAnim;
    private Animation<TextureRegion> leftIdleAnim, leftWalkAnim, leftRunAnim, leftHurtAnim, leftAttackAnim;
    private Animation<TextureRegion> rightIdleAnim, rightWalkAnim, rightRunAnim, rightHurtAnim, rightAttackAnim;
    private Animation<TextureRegion> deathAnim;

    private Facing facing = Facing.FRONT;
    private State state = State.IDLE;
    private TextureRegion currentFrame;

    private float stateTime, hurtStateTime, attackStateTime, deathStateTime;

    // Movement & AI Stats
    private static final float BASE_SPEED = 95f;
    private float speedMultiplier = 1f;
    private static final float LEASH_RANGE = 600f;
    private static final float LEASH_RANGE_SQ = LEASH_RANGE * LEASH_RANGE;

    // Expanded from File 2 for better cornering
    private static final float[] AVOIDANCE_ANGLES = { 30f, -30f, 60f, -60f, 90f, -90f, 120f, -120f, 150f, -150f };

    private float baseRange = 200f;
    private float alertRange = baseRange * 2f;
    private float fovAngle = 90f;
    private float forcedAggroTimer = 0f;
    private Vector2 forward = new Vector2(1, 0);

    public static final float ENTITY_SCALE = 0.16f;

    // Dimensions & Hitbox
    private final float WIDTH = 128f * ENTITY_SCALE, HEIGHT = 128f * ENTITY_SCALE;
    private final float HITBOX_WIDTH = 30f * ENTITY_SCALE, HITBOX_HEIGHT = 40f * ENTITY_SCALE;
    private final float HITBOX_OFFSET_X = 49f * ENTITY_SCALE, HITBOX_OFFSET_Y = 40f * ENTITY_SCALE;

    // Combat Stats
    public static final float ATTACK_RANGE = 76f * ENTITY_SCALE;
    private static final float MAX_HEALTH = 100f, ATTACK_DAMAGE = 14f;
    private static final float ATTACK_COOLDOWN = 1.2f, HURT_TIME = 0.24f;

    private float health = MAX_HEALTH, hurtTimer, attackTimer, attackCooldownTimer;
    private boolean attackDamageConsumed, disposed;

    // Boss properties
    private boolean isBoss = false;

    // Collision
    private ArrayList<Rectangle> boundaries;
    private ArrayList<Polygon> collisionPolygons;
    private float worldMinX = 0f, worldMinY = 0f, worldMaxX = Float.MAX_VALUE, worldMaxY = Float.MAX_VALUE;

    // Wandering
    private final Vector2 randomDir = new Vector2();
    private float moveTimer = 0f;
    private boolean isMoving = false;

    // Steering override (From File 2) - persists across frames to avoid oscillation
    private final Vector2 overrideDir = new Vector2();
    private float overrideTimer = 0f;
    private static final float OVERRIDE_DURATION = 0.6f; // commit to detour for this long

    // Physics
    private final Vector2 knockbackVelocity = new Vector2(0, 0);
    private static final float KNOCKBACK_FRICTION = 600f;

    private final ShapeRenderer shape = new ShapeRenderer();

    public static void queueAssets(com.badlogic.gdx.assets.AssetManager manager) {
        String[] prefixes = {
            "Movements/Enemy/Front/Idle/Front - Idle_", "Movements/Enemy/Front/Walking/Front - Walking_", "Movements/Enemy/Front/Running/Front - Running_", "Movements/Enemy/Front/Hurt/Front - Hurt_", "Movements/Enemy/Front/Attacking/Front - Attacking_",
            "Movements/Enemy/Back/Idle/Back - Idle_", "Movements/Enemy/Back/Walking/Back - Walking_", "Movements/Enemy/Back/Running/Back - Running_", "Movements/Enemy/Back/Hurt/Back - Hurt_", "Movements/Enemy/Back/Attacking/Back - Attacking_",
            "Movements/Enemy/Left/Idle/Left - Idle_", "Movements/Enemy/Left/Walking/Left - Walking_", "Movements/Enemy/Left/Running/Left - Running_", "Movements/Enemy/Left/Hurt/Left - Hurt_", "Movements/Enemy/Left/Attacking/Left - Attacking_",
            "Movements/Enemy/Right/Idle/Right - Idle_", "Movements/Enemy/Right/Walking/Right - Walking_", "Movements/Enemy/Right/Running/Right - Running_", "Movements/Enemy/Right/Hurt/Right - Hurt_", "Movements/Enemy/Right/Attacking/Right - Attacking_",
            "Movements/Enemy/Dying/Dying_"
        };
        for (String p : prefixes) {
            for (int i = 0; ; i++) {
                String path = p + String.format("%03d", i) + ".png";
                if (!Gdx.files.internal(path).exists()) break;
                manager.load(path, Texture.class);
            }
        }
    }

    public Enemy() {
        position = new Vector2(400, 300);
        bounds = new Rectangle(position.x + HITBOX_OFFSET_X, position.y + HITBOX_OFFSET_Y, HITBOX_WIDTH, HITBOX_HEIGHT);
        speedMultiplier = MathUtils.random(0.9f, 1.1f);

        loadAllAnimations();
        currentFrame = frontIdleAnim.getKeyFrame(0f, true);
        pickNewRandomAction();
    }

    private void loadAllAnimations() {
        frontIdleAnim   = load("Movements/Enemy/Front/Idle/Front - Idle_", 0.09f);
        frontWalkAnim   = load("Movements/Enemy/Front/Walking/Front - Walking_", 0.08f);
        frontRunAnim    = load("Movements/Enemy/Front/Running/Front - Running_", 0.07f);
        frontHurtAnim   = load("Movements/Enemy/Front/Hurt/Front - Hurt_", 0.05f);
        frontAttackAnim = load("Movements/Enemy/Front/Attacking/Front - Attacking_", 0.05f);
        backIdleAnim    = load("Movements/Enemy/Back/Idle/Back - Idle_", 0.09f);
        backWalkAnim    = load("Movements/Enemy/Back/Walking/Back - Walking_", 0.08f);
        backRunAnim     = load("Movements/Enemy/Back/Running/Back - Running_", 0.07f);
        backHurtAnim    = load("Movements/Enemy/Back/Hurt/Back - Hurt_", 0.05f);
        backAttackAnim  = load("Movements/Enemy/Back/Attacking/Back - Attacking_", 0.05f);
        leftIdleAnim    = load("Movements/Enemy/Left/Idle/Left - Idle_", 0.09f);
        leftWalkAnim    = load("Movements/Enemy/Left/Walking/Left - Walking_", 0.08f);
        leftRunAnim     = load("Movements/Enemy/Left/Running/Left - Running_", 0.07f);
        leftHurtAnim    = load("Movements/Enemy/Left/Hurt/Left - Hurt_", 0.05f);
        leftAttackAnim  = load("Movements/Enemy/Left/Attacking/Left - Attacking_", 0.05f);
        rightIdleAnim   = load("Movements/Enemy/Right/Idle/Right - Idle_", 0.09f);
        rightWalkAnim   = load("Movements/Enemy/Right/Walking/Right - Walking_", 0.08f);
        rightRunAnim    = load("Movements/Enemy/Right/Running/Right - Running_", 0.07f);
        rightHurtAnim   = load("Movements/Enemy/Right/Hurt/Right - Hurt_", 0.05f);
        rightAttackAnim = load("Movements/Enemy/Right/Attacking/Right - Attacking_", 0.05f);
        deathAnim       = load("Movements/Enemy/Dying/Dying_", 0.08f);
    }

    // Base logic: Uses Main.assets to manage memory
    private Animation<TextureRegion> load(String pathPrefix, float frameDuration) {
        ArrayList<TextureRegion> frames = new ArrayList<>();
        for (int i = 0; ; i++) {
            String path = pathPrefix + String.format("%03d", i) + ".png";
            if (!Main.assets.isLoaded(path)) break;
            Texture tex = Main.assets.get(path, Texture.class);
            frames.add(new TextureRegion(tex));
        }
        if (frames.isEmpty()) throw new IllegalStateException("Missing enemy animation frames: " + pathPrefix);
        return new Animation<>(frameDuration, frames.toArray(new TextureRegion[0]));
    }

    public void update(float delta, Player player) {
        if (disposed) return;

        // Process Knockback
        if (knockbackVelocity.len2() > 0) {
            float currentSpeed = knockbackVelocity.len();
            currentSpeed -= KNOCKBACK_FRICTION * delta;
            if (currentSpeed <= 0) {
                knockbackVelocity.setZero();
            } else {
                knockbackVelocity.setLength(currentSpeed);
                moveBy(knockbackVelocity.x * delta, knockbackVelocity.y * delta);
            }
        }

        if (!isAlive()) {
            deathStateTime += delta;
            currentFrame = deathAnim.getKeyFrame(deathStateTime, false);
            return;
        }

        stateTime += delta;
        float prevX = position.x, prevY = position.y;

        if (hurtTimer > 0f) { hurtTimer -= delta; hurtStateTime += delta; }
        if (attackCooldownTimer > 0f) attackCooldownTimer -= delta;
        if (attackTimer > 0f) {
            attackTimer -= delta;
            attackStateTime += delta;
            if (attackTimer <= 0f) attackDamageConsumed = false;
        }

        Vector2 playerPos = player.getPosition();
        Vector2 toPlayer = new Vector2(playerPos).sub(position);
        float distance = toPlayer.len();

        updateAIState(distance, toPlayer, delta);

        boolean isAttacking = attackTimer > 0f;
        float speed = BASE_SPEED * speedMultiplier;

        if (state == State.IDLE) {
            moveTimer -= delta;
            if (moveTimer <= 0) pickNewRandomAction();
            if (isMoving && !isAttacking && hurtTimer <= 0f) {
                // Integrated from File 2: Repick action if stuck on a wall
                float oldX = position.x, oldY = position.y;
                moveBy(randomDir.x * speed * 0.5f * delta, randomDir.y * speed * 0.5f * delta);
                float movedDist = Vector2.dst(oldX, oldY, position.x, position.y);
                if (movedDist < 0.01f) {
                    pickNewRandomAction();
                }
                forward.set(randomDir);
            }
        } else if (state == State.CHASE) {
            if (!isAttacking && hurtTimer <= 0f) {
                if (distance <= ATTACK_RANGE && attackCooldownTimer <= 0f && !isBoss) {
                    startAttack();
                } else {
                    handleChaseMovement(playerPos, speed, delta, distance, toPlayer);
                }
            }
        }

        position.x = MathUtils.clamp(position.x, worldMinX, worldMaxX - WIDTH);
        position.y = MathUtils.clamp(position.y, worldMinY, worldMaxY - HEIGHT);

        boolean moved = !MathUtils.isEqual(prevX, position.x, 0.0001f) || !MathUtils.isEqual(prevY, position.y, 0.0001f);
        updateFacing();
        bounds.setPosition(position.x + HITBOX_OFFSET_X, position.y + HITBOX_OFFSET_Y);

        updateAnimation(moved);
    }

    private void updateAIState(float distance, Vector2 toPlayer, float delta) {
        if (isBoss) {
            state = State.CHASE;
            return;
        }

        boolean inRange = (state == State.CHASE) ? distance <= alertRange : distance <= baseRange;
        boolean inCone = false;
        if (inRange) {
            float dot = forward.dot(new Vector2(toPlayer).nor());
            inCone = dot >= MathUtils.cosDeg(fovAngle / 2f);
        }

        if (forcedAggroTimer > 0f) {
            forcedAggroTimer -= delta;
            state = State.CHASE;
        } else {
            if (inRange && inCone) state = State.CHASE;
            else if (state == State.CHASE && distance > LEASH_RANGE) state = State.IDLE;
        }
    }

    // Integrated from File 2: Advanced Steering Override
    private void handleChaseMovement(Vector2 playerPos, float speed, float delta, float dist, Vector2 toP) {
        Vector2 direction = new Vector2(playerPos).sub(position).nor();

        // Tick down override timer
        if (overrideTimer > 0f) overrideTimer -= delta;

        // If we have an active steering override, follow it
        if (overrideTimer > 0f) {
            // Check if direct path to player is NOW clear (combined check)
            if (canMoveToCombined(position.x + direction.x * speed * delta,
                position.y + direction.y * speed * delta)) {
                overrideTimer = 0f; // direct path open, cancel detour
            } else {
                // Continue committed detour
                float omx = overrideDir.x * speed * delta, omy = overrideDir.y * speed * delta;
                if (tryMoveCombined(omx, omy)) {
                    forward.set(overrideDir);
                    return;
                }
                // Override direction blocked too — fall through to find new one
                overrideTimer = 0f;
            }
        }

        // 1. Try direct path to player (COMBINED — both axes at once)
        forward.set(direction);
        float mx = direction.x * speed * delta, my = direction.y * speed * delta;
        if (tryMoveCombined(mx, my)) {
            return; // direct diagonal path works
        }

        // 2. Direct path blocked — find avoidance angle and COMMIT
        for (float angle : AVOIDANCE_ANGLES) {
            Vector2 altDir = new Vector2(direction).rotateDeg(angle);
            float ax = altDir.x * speed * delta, ay = altDir.y * speed * delta;
            if (tryMoveCombined(ax, ay)) {
                forward.set(altDir);
                overrideDir.set(altDir);
                overrideTimer = OVERRIDE_DURATION;
                return;
            }
        }

        // 3. No avoidance angle worked — try axis sliding as last resort
        if (canMoveTo(position.x + mx, position.y)) {
            position.x += mx;
        } else if (canMoveTo(position.x, position.y + my)) {
            position.y += my;
        } else {
            // 4. Completely stuck — try perpendicular
            Vector2 perp = new Vector2(-direction.y, direction.x);
            float pmx = perp.x * speed * delta, pmy = perp.y * speed * delta;
            if (tryMoveCombined(pmx, pmy)) {
                forward.set(perp);
                overrideDir.set(perp);
                overrideTimer = OVERRIDE_DURATION;
            } else if (tryMoveCombined(-pmx, -pmy)) {
                perp.scl(-1f);
                forward.set(perp);
                overrideDir.set(perp);
                overrideTimer = OVERRIDE_DURATION;
            }
        }
    }

    /** Move in the combined direction. Returns true only if the FULL diagonal move succeeds. */
    private boolean tryMoveCombined(float dx, float dy) {
        if (canMoveToCombined(position.x + dx, position.y + dy)) {
            position.x += dx;
            position.y += dy;
            return true;
        }
        return false;
    }

    /** Check if moving to (x,y) simultaneously is collision-free. */
    private boolean canMoveToCombined(float x, float y) {
        return canMoveTo(x, y);
    }

    private void startAttack() {
        attackTimer = getAttackAnimation().getAnimationDuration();
        attackStateTime = 0f;
        attackDamageConsumed = false;
        attackCooldownTimer = ATTACK_COOLDOWN;
    }

    public void takeDamage(float damage) {
        if (damage <= 0f || !isAlive() || disposed) return;
        if (isBoss) return; // Bosses are invincible in the overworld
        health = Math.max(0f, health - damage);
        if (!isAlive()) {
            deathStateTime = 0f; attackTimer = 0f; hurtTimer = 0f;
            currentFrame = deathAnim.getKeyFrame(0f, false);
            return;
        }
        hurtTimer = HURT_TIME;
        hurtStateTime = 0f;
    }

    public void applyKnockback(Vector2 forceDir, float forceAmt) {
        if (!isAlive() || disposed) return;
        if (isBoss) return; // Boss doesn't get knocked back
        knockbackVelocity.add(new Vector2(forceDir).nor().scl(forceAmt));
    }

    public boolean canDealDamage() {
        if (attackTimer <= 0f || attackDamageConsumed || !isAlive()) return false;
        float progress = 1f - (attackTimer / getAttackAnimation().getAnimationDuration());
        return progress >= 0.35f && progress <= 0.6f;
    }

    public boolean isPlayerInAttackRadius(Player p) {
        float cx = bounds.x + bounds.width / 2f, cy = bounds.y + bounds.height / 2f;
        float closestX = MathUtils.clamp(cx, p.getBounds().x, p.getBounds().x + p.getBounds().width);
        float closestY = MathUtils.clamp(cy, p.getBounds().y, p.getBounds().y + p.getBounds().height);
        return Vector2.dst2(cx, cy, closestX, closestY) <= (ATTACK_RANGE * ATTACK_RANGE);
    }

    // ===== FIXED COLLISION MOVEMENT =====
    private void moveBy(float dx, float dy) {
        if (canMoveTo(position.x + dx, position.y)) position.x += dx;
        if (canMoveTo(position.x, position.y + dy)) position.y += dy;
    }

    private boolean smartMoveBy(float dx, float dy) {
        boolean mx = canMoveTo(position.x + dx, position.y);
        boolean my = canMoveTo(position.x, position.y + dy);
        if (mx) position.x += dx;
        if (my) position.y += dy;
        return mx || my;
    }

    private boolean canMoveTo(float x, float y) {
        Rectangle next = new Rectangle(x + HITBOX_OFFSET_X, y + HITBOX_OFFSET_Y, HITBOX_WIDTH, HITBOX_HEIGHT);
        if (boundaries != null) {
            for (Rectangle wall : boundaries) if (next.overlaps(wall)) return false;
        }
        if (collisionPolygons != null) {
            Polygon p = new Polygon(new float[]{
                next.x, next.y,
                next.x + next.width, next.y,
                next.x + next.width, next.y + next.height,
                next.x, next.y + next.height
            });
            for (Polygon poly : collisionPolygons) if (Intersector.overlapConvexPolygons(p, poly)) return false;
        }
        return true;
    }
    // =====================================

    private void updateFacing() {
        if (Math.abs(forward.x) > Math.abs(forward.y)) facing = forward.x >= 0f ? Facing.RIGHT : Facing.LEFT;
        else facing = forward.y >= 0f ? Facing.BACK : Facing.FRONT;
    }

    private void updateAnimation(boolean moved) {
        if (hurtTimer > 0f) currentFrame = getHurtAnimation().getKeyFrame(hurtStateTime, false);
        else if (attackTimer > 0f) currentFrame = getAttackAnimation().getKeyFrame(attackStateTime, false);
        else currentFrame = pickAnimation(state, moved).getKeyFrame(stateTime, true);
    }

    private Animation<TextureRegion> pickAnimation(State s, boolean m) {
        if (!m) return switch (facing) { case BACK -> backIdleAnim; case LEFT -> leftIdleAnim; case RIGHT -> rightIdleAnim; default -> frontIdleAnim; };
        if (s == State.CHASE) return switch (facing) { case BACK -> backRunAnim; case LEFT -> leftRunAnim; case RIGHT -> rightRunAnim; default -> frontRunAnim; };
        return switch (facing) { case BACK -> backWalkAnim; case LEFT -> leftWalkAnim; case RIGHT -> rightWalkAnim; default -> frontWalkAnim; };
    }

    private Animation<TextureRegion> getHurtAnimation() { return switch (facing) { case BACK -> backHurtAnim; case LEFT -> leftHurtAnim; case RIGHT -> rightHurtAnim; default -> frontHurtAnim; }; }
    private Animation<TextureRegion> getAttackAnimation() { return switch (facing) { case BACK -> backAttackAnim; case LEFT -> leftAttackAnim; case RIGHT -> rightAttackAnim; default -> frontAttackAnim; }; }

    public void render(SpriteBatch batch) {
        if (disposed) return;
        if (hurtTimer > 0f) batch.setColor(1f, 0.5f, 0.5f, 1f);
        // Boss has red tint
        if (isBoss) batch.setColor(1f, 0.5f, 0.5f, 1f);

        // Make boss bigger maybe? (Optional)
        float w = isBoss ? WIDTH * 1.5f : WIDTH;
        float h = isBoss ? HEIGHT * 1.5f : HEIGHT;

        batch.draw(currentFrame, position.x, position.y, w, h);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void pickNewRandomAction() {
        isMoving = MathUtils.randomBoolean(0.7f);
        moveTimer = MathUtils.random(1f, 3f);
        if (isMoving) randomDir.set(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f)).nor();
    }

    // ===== GETTERS & SETTERS (Preserved from File 1) =====
    public Rectangle getBounds() { return bounds; }
    public void setBoundaries(ArrayList<Rectangle> b) { this.boundaries = b; }
    public void setCollisionPolygons(ArrayList<Polygon> p) { this.collisionPolygons = p; }
    public void setWorldBounds(float minX, float minY, float maxX, float maxY) { this.worldMinX = minX; this.worldMinY = minY; this.worldMaxX = maxX; this.worldMaxY = maxY; }
    public void setPosition(float x, float y) { position.set(x, y); bounds.setPosition(x + HITBOX_OFFSET_X, y + HITBOX_OFFSET_Y); }
    public Vector2 getPosition() { return position; }
    public float getHealth() { return health; }
    public void setHealth(float h) { health = h; }
    public boolean isAlive() { return health > 0f; }
    public float getHealthRatio() { return MathUtils.clamp(health / MAX_HEALTH, 0f, 1f); }
    public float getDamage() { return ATTACK_DAMAGE; }
    public void consumeAttackDamage() { attackDamageConsumed = true; }
    public void forceChase(float duration) { if (isAlive()) { forcedAggroTimer = Math.max(forcedAggroTimer, duration); state = State.CHASE; } }
    public boolean isChasing() { return state == State.CHASE; }
    public boolean isDeathAnimationFinished() { return !isAlive() && deathStateTime >= deathAnim.getAnimationDuration(); }
    public float getAttackTimer() { return attackTimer; }
    public float getAttackAnimDuration() { return getAttackAnimation().getAnimationDuration(); }
    public void dispose() { if (!disposed) { shape.dispose(); disposed = true; } }

    public boolean isBoss() { return isBoss; }
    public void setBoss(boolean boss) {
        this.isBoss = boss;
        if (boss) {
            this.health = Float.MAX_VALUE;
        }
    }
}
