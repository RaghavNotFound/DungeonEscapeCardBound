package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import com.badlogic.gdx.utils.Array;
import java.util.ArrayList;
import java.util.List;
/**
 * Represents a dungeon enemy with state-based AI, predictive tracking,
 * obstacle avoidance, and telegraphed attacks.
 */
public class Enemy {

    private final Vector2 position;
    private final Rectangle bounds;

    private enum State { IDLE, CHASE }

    // Animations — single direction (Bringer-of-Death sprite set)
    private Animation<TextureRegion> idleAnim, walkAnim, attackAnim, hurtAnim;
    private Animation<TextureRegion> deathAnim;

    private boolean facingRight = false;
    private State state = State.IDLE;
    private TextureRegion currentFrame;

    private float stateTime, hurtStateTime, attackStateTime, deathStateTime;

    // Movement & AI Stats
    private static final float BASE_SPEED = 65f;
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
    private final float HITBOX_WIDTH = 50f * ENTITY_SCALE, HITBOX_HEIGHT = 60f * ENTITY_SCALE;
    private final float HITBOX_OFFSET_X = 39f * ENTITY_SCALE, HITBOX_OFFSET_Y = 30f * ENTITY_SCALE;

    // Combat Stats
    public static final float ATTACK_RANGE = 76f * ENTITY_SCALE;
    private static final float MAX_HEALTH = 100f, ATTACK_DAMAGE = 14f;
    private static final float ATTACK_COOLDOWN = 1.2f, HURT_TIME = 0.24f;

    private float health = MAX_HEALTH, hurtTimer, attackTimer, attackCooldownTimer;
    private float animatedHealth = MAX_HEALTH;
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

    // A* Pathfinding
    private List<Vector2> currentPath = new ArrayList<>();
    private float pathUpdateTimer = 0f;
    private AStar pathfinder;
    private static final float PATH_UPDATE_INTERVAL = 0.8f;
    private static final float NODE_REACHED_TOLERANCE = 6f;
    /** Minimum distance the player must move before we recalculate the path. */
    private static final float PATH_RECALC_PLAYER_MOVE_THRESHOLD = 32f;
    private static final float PATH_RECALC_THRESHOLD_SQ = PATH_RECALC_PLAYER_MOVE_THRESHOLD * PATH_RECALC_PLAYER_MOVE_THRESHOLD;
    /** Cached last target position used for A* — prevents oscillation when player is behind walls. */
    private final Vector2 lastPathTarget = new Vector2(Float.NaN, Float.NaN);

    private final ShapeRenderer shape = new ShapeRenderer();

    public static void queueAssets(com.badlogic.gdx.assets.AssetManager manager) {
        // Bringer-of-Death individual sprite frames
        for (int i = 1; i <= 8; i++)  manager.load("Enemy_Sprite/Individual Sprite/Idle/Bringer-of-Death_Idle_" + i + ".png", Texture.class);
        for (int i = 1; i <= 8; i++)  manager.load("Enemy_Sprite/Individual Sprite/Walk/Bringer-of-Death_Walk_" + i + ".png", Texture.class);
        for (int i = 1; i <= 10; i++) manager.load("Enemy_Sprite/Individual Sprite/Attack/Bringer-of-Death_Attack_" + i + ".png", Texture.class);
        for (int i = 1; i <= 3; i++)  manager.load("Enemy_Sprite/Individual Sprite/Hurt/Bringer-of-Death_Hurt_" + i + ".png", Texture.class);
        for (int i = 1; i <= 10; i++) manager.load("Enemy_Sprite/Individual Sprite/Death/Bringer-of-Death_Death_" + i + ".png", Texture.class);

        // boss_sprite individual frames
        String bBase = "boss_sprite/sprites/";
        for (int i = 1; i <= 4; i++) manager.load(bBase + "idle" + i + ".png", Texture.class);
        for (int i = 1; i <= 6; i++) manager.load(bBase + "walk" + i + ".png", Texture.class);
        for (int i = 1; i <= 6; i++) manager.load(bBase + "punch" + i + ".png", Texture.class);
        for (int i = 1; i <= 2; i++) manager.load(bBase + "hurt" + i + ".png", Texture.class);
        for (int i = 1; i <= 2; i++) manager.load(bBase + "fall" + i + ".png", Texture.class);
    }

    public Enemy() {
        position = new Vector2(400, 300);
        bounds = new Rectangle(position.x + HITBOX_OFFSET_X, position.y + HITBOX_OFFSET_Y, HITBOX_WIDTH, HITBOX_HEIGHT);
        speedMultiplier = MathUtils.random(0.9f, 1.1f);

        loadAllAnimations();
        currentFrame = idleAnim.getKeyFrame(0f, true);
        pickNewRandomAction();
    }

    private void loadAllAnimations() {
        if (isBoss) {
            // New Boss character from assets/boss_sprite/
            String bBase = "boss_sprite/sprites/";
            Array<TextureRegion> bIdle = new Array<>();
            for (int i = 1; i <= 4; i++) bIdle.add(new TextureRegion(Main.assets.get(bBase + "idle" + i + ".png", Texture.class)));
            idleAnim = new Animation<>(0.15f, bIdle, Animation.PlayMode.LOOP);

            Array<TextureRegion> bWalk = new Array<>();
            for (int i = 1; i <= 6; i++) bWalk.add(new TextureRegion(Main.assets.get(bBase + "walk" + i + ".png", Texture.class)));
            walkAnim = new Animation<>(0.12f, bWalk, Animation.PlayMode.LOOP);

            Array<TextureRegion> bAttack = new Array<>();
            for (int i = 1; i <= 6; i++) bAttack.add(new TextureRegion(Main.assets.get(bBase + "punch" + i + ".png", Texture.class)));
            attackAnim = new Animation<>(0.08f, bAttack, Animation.PlayMode.NORMAL);

            Array<TextureRegion> bHurt = new Array<>();
            for (int i = 1; i <= 2; i++) bHurt.add(new TextureRegion(Main.assets.get(bBase + "hurt" + i + ".png", Texture.class)));
            hurtAnim = new Animation<>(0.10f, bHurt, Animation.PlayMode.NORMAL);

            Array<TextureRegion> bDeath = new Array<>();
            for (int i = 1; i <= 2; i++) bDeath.add(new TextureRegion(Main.assets.get(bBase + "fall" + i + ".png", Texture.class)));
            deathAnim = new Animation<>(0.20f, bDeath, Animation.PlayMode.NORMAL);
        } else {
            // Regular enemies use Bringer-of-Death
            idleAnim   = loadNamed("Enemy_Sprite/Individual Sprite/Idle/Bringer-of-Death_Idle_",   8,  0.10f);
            walkAnim   = loadNamed("Enemy_Sprite/Individual Sprite/Walk/Bringer-of-Death_Walk_",   8,  0.09f);
            attackAnim = loadNamed("Enemy_Sprite/Individual Sprite/Attack/Bringer-of-Death_Attack_", 10, 0.08f);
            hurtAnim   = loadNamed("Enemy_Sprite/Individual Sprite/Hurt/Bringer-of-Death_Hurt_",     3,  0.07f);
            deathAnim  = loadNamed("Enemy_Sprite/Individual Sprite/Death/Bringer-of-Death_Death_",   10, 0.12f);
        }
    }

    /** Load a numbered animation from the AssetManager (path_N.png, 1-based). */
    private Animation<TextureRegion> loadNamed(String pathPrefix, int count, float frameDuration) {
        TextureRegion[] frames = new TextureRegion[count];
        for (int i = 0; i < count; i++) {
            String path = pathPrefix + (i + 1) + ".png";
            Texture tex = Main.assets.get(path, Texture.class);
            frames[i] = new TextureRegion(tex);
        }
        return new Animation<>(frameDuration, frames);
    }

    public void update(float delta, Player player) {
        // Health animation logic
        if (animatedHealth > health) {
            animatedHealth -= 40f * delta;
            if (animatedHealth < health) animatedHealth = health;
        } else if (animatedHealth < health) {
            animatedHealth = health;
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
            // Clear A* path when not chasing
            if (currentPath != null && !currentPath.isEmpty()) {
                currentPath.clear();
            }
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
        // --- A* Pathfinding Logic ---
        if (pathfinder != null) {
            pathUpdateTimer -= delta;

            // Compute hitbox centers for accurate pathfinding
            float enemyCenterX = position.x + HITBOX_OFFSET_X + HITBOX_WIDTH / 2f;
            float enemyCenterY = position.y + HITBOX_OFFSET_Y + HITBOX_HEIGHT / 2f;
            float playerCenterX = playerPos.x;
            float playerCenterY = playerPos.y;

            // Decide whether to recalculate the path:
            // - Timer has expired
            // - AND (no existing path OR the player has moved significantly from where we last targeted)
            boolean needsRecalc = false;
            if (pathUpdateTimer <= 0f) {
                if (currentPath == null || currentPath.isEmpty()) {
                    needsRecalc = true;
                } else {
                    float dxTarget = playerCenterX - lastPathTarget.x;
                    float dyTarget = playerCenterY - lastPathTarget.y;
                    if (Float.isNaN(lastPathTarget.x) || (dxTarget * dxTarget + dyTarget * dyTarget) > PATH_RECALC_THRESHOLD_SQ) {
                        needsRecalc = true;
                    }
                }
            }

            if (needsRecalc) {
                List<Vector2> newPath = pathfinder.findPath(
                    new Vector2(enemyCenterX, enemyCenterY),
                    new Vector2(playerCenterX, playerCenterY)
                );
                if (newPath != null && !newPath.isEmpty()) {
                    currentPath = newPath;
                    lastPathTarget.set(playerCenterX, playerCenterY);
                }
                // Even if path is empty (unreachable), don't spam recalc — wait for timer
                pathUpdateTimer = PATH_UPDATE_INTERVAL;
            }

            // If we have a valid path, follow the waypoints
            if (currentPath != null && !currentPath.isEmpty()) {
                // Convert the cell center waypoint to position-space
                Vector2 cellCenter = currentPath.get(0);
                float targetPosX = cellCenter.x - HITBOX_OFFSET_X - HITBOX_WIDTH / 2f;
                float targetPosY = cellCenter.y - HITBOX_OFFSET_Y - HITBOX_HEIGHT / 2f;

                float dxNode = targetPosX - position.x;
                float dyNode = targetPosY - position.y;
                float distToNode = (float) Math.sqrt(dxNode * dxNode + dyNode * dyNode);

                // Advance past reached waypoints
                while (distToNode <= NODE_REACHED_TOLERANCE && currentPath.size() > 1) {
                    currentPath.remove(0);
                    cellCenter = currentPath.get(0);
                    targetPosX = cellCenter.x - HITBOX_OFFSET_X - HITBOX_WIDTH / 2f;
                    targetPosY = cellCenter.y - HITBOX_OFFSET_Y - HITBOX_HEIGHT / 2f;
                    dxNode = targetPosX - position.x;
                    dyNode = targetPosY - position.y;
                    distToNode = (float) Math.sqrt(dxNode * dxNode + dyNode * dyNode);
                }

                // If we've reached the last waypoint, clear path
                if (distToNode <= NODE_REACHED_TOLERANCE && currentPath.size() == 1) {
                    currentPath.clear();
                    return;
                }

                if (!currentPath.isEmpty()) {
                    // Move toward the current waypoint in a straight line
                    float invDist = 1f / distToNode;
                    float dirX = dxNode * invDist;
                    float dirY = dyNode * invDist;
                    forward.set(dirX, dirY);

                    float moveAmount = speed * delta;

                    // Don't overshoot the waypoint — clamp movement distance
                    if (moveAmount > distToNode) {
                        moveAmount = distToNode;
                    }

                    float mx = dirX * moveAmount;
                    float my = dirY * moveAmount;

                    // Try combined diagonal first
                    if (tryMoveCombined(mx, my)) {
                        return;
                    }
                    // Axis-slide fallback: try each axis independently
                    if (canMoveTo(position.x + mx, position.y)) {
                        position.x += mx;
                        return;
                    }
                    if (canMoveTo(position.x, position.y + my)) {
                        position.y += my;
                        return;
                    }
                    // Totally blocked — clear path and recalculate next tick
                    currentPath.clear();
                    pathUpdateTimer = 0f;
                }
            }
            // If A* is active but path is empty (unreachable/same cell), fall through to direct chase
        }

        // --- Fallback Direct Chasing (when no A* pathfinder or path is empty) ---
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
        facingRight = forward.x >= 0f;
    }

    private void updateAnimation(boolean moved) {
        if (hurtTimer > 0f)   currentFrame = hurtAnim.getKeyFrame(hurtStateTime, false);
        else if (attackTimer > 0f) currentFrame = attackAnim.getKeyFrame(attackStateTime, false);
        else if (!isAlive())       currentFrame = deathAnim.getKeyFrame(deathStateTime, false);
        else if (moved)            currentFrame = walkAnim.getKeyFrame(stateTime, true);
        else                       currentFrame = idleAnim.getKeyFrame(stateTime, true);
    }

    private Animation<TextureRegion> getAttackAnimation() { return attackAnim; }

    public void render(SpriteBatch batch) {
        if (disposed) return;
        if (hurtTimer > 0f) batch.setColor(1f, 0.5f, 0.5f, 1f);
        if (isBoss) batch.setColor(1f, 0.5f, 0.5f, 1f);

        float w = isBoss ? WIDTH * 1.5f : WIDTH;
        float h = isBoss ? HEIGHT * 1.5f : HEIGHT;

        // Flip sprite horizontally based on facing direction
        float dW = facingRight ? w : -w;
        float dX = facingRight ? position.x : position.x + w;
        batch.draw(currentFrame, dX, position.y, dW, h);
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
    public void setPathfinder(AStar pathfinder) { this.pathfinder = pathfinder; }
    public void setWorldBounds(float minX, float minY, float maxX, float maxY) { this.worldMinX = minX; this.worldMinY = minY; this.worldMaxX = maxX; this.worldMaxY = maxY; }
    public void setPosition(float x, float y) { position.set(x, y); bounds.setPosition(x + HITBOX_OFFSET_X, y + HITBOX_OFFSET_Y); }
    public Vector2 getPosition() { return position; }
    public float getHealth() { return health; }
    public void setHealth(float h) { health = h; animatedHealth = h; }
    public boolean isAlive() { return health > 0f; }
    public float getHealthRatio() { return MathUtils.clamp(health / MAX_HEALTH, 0f, 1f); }
    public float getAnimatedHealthRatio() { return MathUtils.clamp(animatedHealth / MAX_HEALTH, 0f, 1f); }
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
        loadAllAnimations(); // Refresh animations to use boss-specific sprites if applicable
        if (boss) {
            this.health = Float.MAX_VALUE; // World boss is immortal, fight happens on BossFightScreen
        }
    }
}
