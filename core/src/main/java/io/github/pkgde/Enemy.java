package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;
import java.util.ArrayList;

public class Enemy
{
    private final Vector2 position;
    private final Rectangle bounds;

    private enum Facing { FRONT, BACK, LEFT, RIGHT }

    private Animation<TextureRegion> frontIdleAnim;
    private Animation<TextureRegion> frontWalkAnim;
    private Animation<TextureRegion> frontRunAnim;
    private Animation<TextureRegion> frontHurtAnim;
    private Animation<TextureRegion> frontAttackAnim;

    private Animation<TextureRegion> backIdleAnim;
    private Animation<TextureRegion> backWalkAnim;
    private Animation<TextureRegion> backRunAnim;
    private Animation<TextureRegion> backHurtAnim;
    private Animation<TextureRegion> backAttackAnim;

    private Animation<TextureRegion> leftIdleAnim;
    private Animation<TextureRegion> leftWalkAnim;
    private Animation<TextureRegion> leftRunAnim;
    private Animation<TextureRegion> leftHurtAnim;
    private Animation<TextureRegion> leftAttackAnim;

    private Animation<TextureRegion> rightIdleAnim;
    private Animation<TextureRegion> rightWalkAnim;
    private Animation<TextureRegion> rightRunAnim;
    private Animation<TextureRegion> rightHurtAnim;
    private Animation<TextureRegion> rightAttackAnim;
    private Animation<TextureRegion> deathAnim;

    private Facing facing = Facing.FRONT;
    private TextureRegion currentFrame;

    private final ArrayList<Texture> textures = new ArrayList<>();

    private float stateTime = 0f;
    private float hurtStateTime = 0f;
    private float attackStateTime = 0f;
    private float deathStateTime = 0f;

    private enum State { IDLE, CHASE }
    private State state = State.IDLE;

    private static final float SPEED = 95f;

    private float baseRange = 200f;
    private float alertRange = baseRange * 2f;

    private float fovAngle = 90f;

    private Vector2 forward = new Vector2(1, 0);

    private final float WIDTH = 128;
    private final float HEIGHT = 128;

    private static final float HITBOX_WIDTH = 30f;
    private static final float HITBOX_HEIGHT = 40f;
    private static final float HITBOX_OFFSET_X = 49f;
    private static final float HITBOX_OFFSET_Y = 40f;

    private static final float MAX_HEALTH = 100f;
    private static final float ATTACK_DAMAGE = 14f;
    private static final float ATTACK_RANGE = 76f;
    private static final float ATTACK_COOLDOWN = 1.2f;
    private static final float HURT_TIME = 0.24f;

    private float health = MAX_HEALTH;
    private float hurtTimer;
    private float attackTimer;
    private float attackCooldownTimer;
    private boolean attackDamageConsumed;
    private boolean disposed;

    private ArrayList<Rectangle> boundaries;
    private float worldMinX = 0f;
    private float worldMinY = 0f;
    private float worldMaxX = Float.MAX_VALUE;
    private float worldMaxY = Float.MAX_VALUE;

    private ArrayList<Polygon> collisionPolygons;

    private Vector2 randomDir = new Vector2();
    private float moveTimer = 0f;
    private float moveDuration = 0f;
    private boolean isMoving = false;

    public Enemy()
    {
        position = new Vector2(400, 300);
        bounds = new Rectangle(position.x + HITBOX_OFFSET_X, position.y + HITBOX_OFFSET_Y, HITBOX_WIDTH, HITBOX_HEIGHT);

        frontIdleAnim   = load("Movements/Enemy/Front/Idle/Front - Idle_", 0.09f);
        frontWalkAnim   = load("Movements/Enemy/Front/Walking/Front - Walking_", 0.08f);
        frontRunAnim    = load("Movements/Enemy/Front/Running/Front - Running_", 0.07f);
        frontHurtAnim   = load("Movements/Enemy/Front/Hurt/Front - Hurt_", 0.05f);
        frontAttackAnim = load("Movements/Enemy/Front/Attacking/Front - Attacking_", 0.05f);

        backIdleAnim   = load("Movements/Enemy/Back/Idle/Back - Idle_", 0.09f);
        backWalkAnim   = load("Movements/Enemy/Back/Walking/Back - Walking_", 0.08f);
        backRunAnim    = load("Movements/Enemy/Back/Running/Back - Running_", 0.07f);
        backHurtAnim   = load("Movements/Enemy/Back/Hurt/Back - Hurt_", 0.05f);
        backAttackAnim = load("Movements/Enemy/Back/Attacking/Back - Attacking_", 0.05f);

        leftIdleAnim   = load("Movements/Enemy/Left/Idle/Left - Idle_", 0.09f);
        leftWalkAnim   = load("Movements/Enemy/Left/Walking/Left - Walking_", 0.08f);
        leftRunAnim    = load("Movements/Enemy/Left/Running/Left - Running_", 0.07f);
        leftHurtAnim   = load("Movements/Enemy/Left/Hurt/Left - Hurt_", 0.05f);
        leftAttackAnim = load("Movements/Enemy/Left/Attacking/Left - Attacking_", 0.05f);

        rightIdleAnim   = load("Movements/Enemy/Right/Idle/Right - Idle_", 0.09f);
        rightWalkAnim   = load("Movements/Enemy/Right/Walking/Right - Walking_", 0.08f);
        rightRunAnim    = load("Movements/Enemy/Right/Running/Right - Running_", 0.07f);
        rightHurtAnim   = load("Movements/Enemy/Right/Hurt/Right - Hurt_", 0.05f);
        rightAttackAnim = load("Movements/Enemy/Right/Attacking/Right - Attacking_", 0.05f);
        deathAnim       = load("Movements/Enemy/Dying/Dying_", 0.08f);

        currentFrame = frontIdleAnim.getKeyFrame(0f, true);

        pickNewRandomAction();
    }

    public void setBoundaries(ArrayList<Rectangle> boundaries)
    {
        this.boundaries = boundaries;
    }

    public void setCollisionPolygons(ArrayList<Polygon> polygons)
    {
        this.collisionPolygons = polygons;
    }

    public void setWorldBounds(float minX, float minY, float maxX, float maxY)
    {
        this.worldMinX = minX;
        this.worldMinY = minY;
        this.worldMaxX = maxX;
        this.worldMaxY = maxY;
    }

    public void setPosition(float x, float y)
    {
        position.set(x, y);
        bounds.setPosition(x + HITBOX_OFFSET_X, y + HITBOX_OFFSET_Y);
    }

    private Animation<TextureRegion> load(String pathPrefix, float frameDuration)
    {
        ArrayList<TextureRegion> frames = new ArrayList<>();

        for (int i = 0; ; i++)
        {
            String path = pathPrefix + String.format("%03d", i) + ".png";
            if (!Gdx.files.internal(path).exists()) break;

            Texture tex = new Texture(path);
            textures.add(tex);
            frames.add(new TextureRegion(tex));
        }

        if (frames.isEmpty())
        {
            throw new IllegalStateException("Missing enemy animation frames for prefix: " + pathPrefix);
        }

        return new Animation<>(frameDuration, frames.toArray(new TextureRegion[0]));
    }

    public void update(float delta, Player player)
    {
        if (disposed) return;

        if (!isAlive())
        {
            deathStateTime += delta;
            currentFrame = deathAnim.getKeyFrame(deathStateTime, false);
            return;
        }

        stateTime += delta;
        float prevX = position.x;
        float prevY = position.y;

        if (hurtTimer > 0f)
        {
            hurtTimer -= delta;
            hurtStateTime += delta;
        }

        if (attackCooldownTimer > 0f) attackCooldownTimer -= delta;

        if (attackTimer > 0f)
        {
            attackTimer -= delta;
            attackStateTime += delta;
            if (attackTimer <= 0f) attackDamageConsumed = false;
        }

        Vector2 playerPos = player.getPosition();
        Vector2 toPlayer = new Vector2(playerPos).sub(position);
        float distance = toPlayer.len();

        boolean inRange = state == State.CHASE ? distance <= alertRange : distance <= baseRange;
        boolean inCone = false;

        if (inRange)
        {
            toPlayer.nor();
            float dot = forward.dot(toPlayer);
            float threshold = MathUtils.cosDeg(fovAngle / 2f);
            inCone = dot >= threshold;
        }

        if (inRange && inCone)
        {
            state = State.CHASE;
        }
        else if (state == State.CHASE && distance <= alertRange)
        {
            state = State.CHASE;
        }
        else
        {
            state = State.IDLE;
        }

        boolean isAttacking = attackTimer > 0f;

        switch (state)
        {
            case IDLE:
                moveTimer -= delta;
                if (moveTimer <= 0) pickNewRandomAction();

                if (isMoving && !isAttacking)
                {
                    moveBy(randomDir.x * SPEED * 0.5f * delta, randomDir.y * SPEED * 0.5f * delta);
                    forward.set(randomDir);
                }
                break;

            case CHASE:
                if (!isAttacking)
                {
                    Vector2 direction = new Vector2(playerPos).sub(position).nor();
                    forward.set(direction);

                    if (distance <= ATTACK_RANGE && attackCooldownTimer <= 0f)
                    {
                        attackTimer = getAttackAnimation().getAnimationDuration();
                        attackStateTime = 0f;
                        attackDamageConsumed = false;
                        attackCooldownTimer = ATTACK_COOLDOWN;
                    }
                    else
                    {
                        moveBy(direction.x * SPEED * delta, direction.y * SPEED * delta);
                    }
                }
                break;
        }

        position.x = MathUtils.clamp(position.x, worldMinX, worldMaxX - WIDTH);
        position.y = MathUtils.clamp(position.y, worldMinY, worldMaxY - HEIGHT);

        boolean movedThisFrame = !MathUtils.isEqual(prevX, position.x, 0.0001f)
            || !MathUtils.isEqual(prevY, position.y, 0.0001f);

        updateFacing();
        bounds.setPosition(position.x + HITBOX_OFFSET_X, position.y + HITBOX_OFFSET_Y);

        if (hurtTimer > 0f)
        {
            currentFrame = getHurtAnimation().getKeyFrame(hurtStateTime, false);
        }
        else if (attackTimer > 0f)
        {
            currentFrame = getAttackAnimation().getKeyFrame(attackStateTime, false);
        }
        else
        {
            currentFrame = pickAnimation(state, movedThisFrame).getKeyFrame(stateTime, true);
        }
    }

    public void takeDamage(float damage)
    {
        if (damage <= 0f || !isAlive() || disposed) return;

        health = Math.max(0f, health - damage);

        if (!isAlive())
        {
            deathStateTime = 0f;
            attackTimer = 0f;
            attackCooldownTimer = 0f;
            attackDamageConsumed = true;
            hurtTimer = 0f;
            currentFrame = deathAnim.getKeyFrame(0f, false);
            return;
        }

        hurtTimer = HURT_TIME;
        hurtStateTime = 0f;
    }

    public boolean canDealDamage()
    {
        if (attackTimer <= 0f || attackDamageConsumed || !isAlive()) return false;

        float duration = getAttackAnimation().getAnimationDuration();
        float progress = 1f - (attackTimer / duration);
        return progress >= 0.35f && progress <= 0.6f;
    }

    public void consumeAttackDamage()
    {
        attackDamageConsumed = true;
    }

    public float getDamage()
    {
        return ATTACK_DAMAGE;
    }

    public float getHealth()
    {
        return health;
    }

    public float getMaxHealth()
    {
        return MAX_HEALTH;
    }

    public float getHealthRatio()
    {
        return MathUtils.clamp(health / MAX_HEALTH, 0f, 1f);
    }

    public boolean isAlive()
    {
        return health > 0f;
    }

    public boolean isDead()
    {
        return !isAlive();
    }

    public boolean isDeathAnimationFinished()
    {
        return !isAlive() && deathStateTime >= deathAnim.getAnimationDuration();
    }

    private void updateFacing()
    {
        float ax = Math.abs(forward.x);
        float ay = Math.abs(forward.y);

        if (ax > ay)
        {
            facing = forward.x >= 0f ? Facing.RIGHT : Facing.LEFT;
        }
        else
        {
            facing = forward.y >= 0f ? Facing.BACK : Facing.FRONT;
        }
    }

    private Animation<TextureRegion> pickAnimation(State state, boolean moved)
    {
        if (!moved)
        {
            switch (facing)
            {
                case BACK:  return backIdleAnim;
                case LEFT:  return leftIdleAnim;
                case RIGHT: return rightIdleAnim;
                default:    return frontIdleAnim;
            }
        }

        if (state == State.CHASE)
        {
            switch (facing)
            {
                case BACK:  return backRunAnim;
                case LEFT:  return leftRunAnim;
                case RIGHT: return rightRunAnim;
                default:    return frontRunAnim;
            }
        }

        switch (facing)
        {
            case BACK:  return backWalkAnim;
            case LEFT:  return leftWalkAnim;
            case RIGHT: return rightWalkAnim;
            default:    return frontWalkAnim;
        }
    }

    private Animation<TextureRegion> getHurtAnimation()
    {
        switch (facing)
        {
            case BACK:  return backHurtAnim;
            case LEFT:  return leftHurtAnim;
            case RIGHT: return rightHurtAnim;
            default:    return frontHurtAnim;
        }
    }

    private Animation<TextureRegion> getAttackAnimation()
    {
        switch (facing)
        {
            case BACK:  return backAttackAnim;
            case LEFT:  return leftAttackAnim;
            case RIGHT: return rightAttackAnim;
            default:    return frontAttackAnim;
        }
    }

    private void moveBy(float dx, float dy)
    {
        if (canMoveTo(position.x + dx, position.y)) position.x += dx;
        if (canMoveTo(position.x, position.y + dy)) position.y += dy;
    }

    private boolean canMoveTo(float x, float y)
    {
        Rectangle next = new Rectangle(x + HITBOX_OFFSET_X, y + HITBOX_OFFSET_Y, HITBOX_WIDTH, HITBOX_HEIGHT);

        if (boundaries != null)
        {
            for (Rectangle wall : boundaries)
            {
                if (next.overlaps(wall)) return false;
            }
        }

        if (collisionPolygons != null)
        {
            Polygon rectPoly = new Polygon(new float[] {
                next.x, next.y,
                next.x + next.width, next.y,
                next.x + next.width, next.y + next.height,
                next.x, next.y + next.height
            });
            for (Polygon poly : collisionPolygons)
            {
                if (Intersector.overlapConvexPolygons(rectPoly, poly)) return false;
            }
        }

        return true;
    }

    private void pickNewRandomAction()
    {
        isMoving = MathUtils.randomBoolean(0.7f);
        moveDuration = MathUtils.random(1f, 3f);
        moveTimer = moveDuration;

        if (isMoving)
        {
            randomDir.set(
                MathUtils.random(-1f, 1f),
                MathUtils.random(-1f, 1f)
            ).nor();
        }
    }

    public void render(SpriteBatch batch)
    {
        if (disposed) return;
        batch.draw(currentFrame, position.x, position.y, WIDTH, HEIGHT);
    }

    public Rectangle getBounds()
    {
        return bounds;
    }

    public void dispose()
    {
        if (disposed) return;

        for (Texture t : textures) t.dispose();
        disposed = true;
    }
}
