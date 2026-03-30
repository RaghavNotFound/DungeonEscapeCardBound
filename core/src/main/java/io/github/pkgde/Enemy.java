package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;
import java.util.ArrayList;

public class Enemy {

    private final Vector2 position;
    private final Rectangle bounds;

    public boolean canDealDamage()
    {
        return attackTimer > 0f && !attackDamageConsumed;
    }

    public float getDamage() {
        return ATTACK_DAMAGE;
    }

    public void consumeAttackDamage() {
        attackDamageConsumed = true;
    }

    public float getHealthRatio() {
        return MathUtils.clamp(health / MAX_HEALTH, 0f, 1f);
    }

    public boolean isAlive() {
        return health > 0f;
    }

    public void setPosition(float x, float y) {
        position.set(x, y);
    }

    public void setWorldBounds(float minX, float minY, float maxX, float maxY) {
        worldMinX = minX;
        worldMinY = minY;
        worldMaxX = maxX;
        worldMaxY = maxY;
    }

    public void setBoundaries(ArrayList<Rectangle> boundaries) {
        this.boundaries = boundaries;
    }

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

    private Vector2 randomDir = new Vector2();
    private float moveTimer = 0f;
    private float moveDuration = 0f;
    private boolean isMoving = false;

    private int hp = 3;

    public Enemy() {
        position = new Vector2(400, 300);
        bounds = new Rectangle(position.x, position.y, WIDTH, HEIGHT);

        frontIdleAnim = load("Movements/Enemy/Front/Idle/Front - Idle_", 0.09f);
        frontWalkAnim = load("Movements/Enemy/Front/Walking/Front - Walking_", 0.08f);
        frontRunAnim = load("Movements/Enemy/Front/Running/Front - Running_", 0.07f);
        frontHurtAnim = load("Movements/Enemy/Front/Hurt/Front - Hurt_", 0.05f);
        frontAttackAnim = load("Movements/Enemy/Front/Attacking/Front - Attacking_", 0.05f);

        backIdleAnim = load("Movements/Enemy/Back/Idle/Back - Idle_", 0.09f);
        backWalkAnim = load("Movements/Enemy/Back/Walking/Back - Walking_", 0.08f);
        backRunAnim = load("Movements/Enemy/Back/Running/Back - Running_", 0.07f);
        backHurtAnim = load("Movements/Enemy/Back/Hurt/Back - Hurt_", 0.05f);
        backAttackAnim = load("Movements/Enemy/Back/Attacking/Back - Attacking_", 0.05f);

        leftIdleAnim = load("Movements/Enemy/Left/Idle/Left - Idle_", 0.09f);
        leftWalkAnim = load("Movements/Enemy/Left/Walking/Left - Walking_", 0.08f);
        leftRunAnim = load("Movements/Enemy/Left/Running/Left - Running_", 0.07f);
        leftHurtAnim = load("Movements/Enemy/Left/Hurt/Left - Hurt_", 0.05f);
        leftAttackAnim = load("Movements/Enemy/Left/Attacking/Left - Attacking_", 0.05f);

        rightIdleAnim = load("Movements/Enemy/Right/Idle/Right - Idle_", 0.09f);
        rightWalkAnim = load("Movements/Enemy/Right/Walking/Right - Walking_", 0.08f);
        rightRunAnim = load("Movements/Enemy/Right/Running/Right - Running_", 0.07f);
        rightHurtAnim = load("Movements/Enemy/Right/Hurt/Right - Hurt_", 0.05f);
        rightAttackAnim = load("Movements/Enemy/Right/Attacking/Right - Attacking_", 0.05f);

        deathAnim = load("Movements/Enemy/Dying/Dying_", 0.08f);

        currentFrame = frontIdleAnim.getKeyFrame(0f, true);

        pickNewRandomAction();
    }

    private Animation<TextureRegion> load(String pathPrefix, float frameDuration) {
        ArrayList<TextureRegion> frames = new ArrayList<>();

        for (int i = 0; ; i++) {
            String path = pathPrefix + String.format("%03d", i) + ".png";
            if (!Gdx.files.internal(path).exists()) break;

            Texture tex = new Texture(path);
            textures.add(tex);
            frames.add(new TextureRegion(tex));
        }

        if (frames.isEmpty()) {
            throw new IllegalStateException("Missing enemy animation frames for prefix: " + pathPrefix);
        }

        return new Animation<>(frameDuration, frames.toArray(new TextureRegion[0]));
    }

    public void update(float delta, Player player) {
        if (disposed) return;

        stateTime += delta;
        bounds.setPosition(position.x, position.y);

        if (!isAlive()) {
            deathStateTime += delta;
            currentFrame = deathAnim.getKeyFrame(deathStateTime, false);
            return;
        }

        if (attackCooldownTimer > 0f) attackCooldownTimer -= delta;

        if (attackTimer > 0f) {
            attackTimer -= delta;
            attackStateTime += delta;
            currentFrame = getFacingAnim("attack").getKeyFrame(attackStateTime, false);
            return;
        }

        if (hurtTimer > 0f) {
            hurtTimer -= delta;
            hurtStateTime += delta;
            currentFrame = getFacingAnim("hurt").getKeyFrame(hurtStateTime, false);
            return;
        }

        float myX = position.x + WIDTH / 2f;
        float myY = position.y + HEIGHT / 2f;

        Rectangle pb = player.getBounds();
        float px = pb.x + pb.width / 2f;
        float py = pb.y + pb.height / 2f;

        float dx = px - myX;
        float dy = py - myY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        updateFacing(dx, dy);

        if (state == State.IDLE && dist < alertRange) state = State.CHASE;
        if (state == State.CHASE && dist > alertRange * 1.5f) state = State.IDLE;

        if (state == State.CHASE) {
            if (dist <= ATTACK_RANGE && attackCooldownTimer <= 0f) {
                attackTimer = getFacingAnim("attack").getAnimationDuration();
                attackStateTime = 0f;
                attackCooldownTimer = ATTACK_COOLDOWN;
                attackDamageConsumed = false;
                currentFrame = getFacingAnim("attack").getKeyFrame(0f, false);
                return;
            }

            if (dist > 1f) {
                float nx = position.x + (dx / dist) * SPEED * delta;
                float ny = position.y + (dy / dist) * SPEED * delta;
                position.x = MathUtils.clamp(nx, worldMinX, worldMaxX - WIDTH);
                position.y = MathUtils.clamp(ny, worldMinY, worldMaxY - HEIGHT);
            }

            String anim = dist < 150f ? "run" : "walk";
            currentFrame = getFacingAnim(anim).getKeyFrame(stateTime, true);
        } else {
            moveTimer -= delta;
            if (moveTimer <= 0f) pickNewRandomAction();

            if (isMoving) {
                float nx = position.x + randomDir.x * SPEED * 0.4f * delta;
                float ny = position.y + randomDir.y * SPEED * 0.4f * delta;
                position.x = MathUtils.clamp(nx, worldMinX, worldMaxX - WIDTH);
                position.y = MathUtils.clamp(ny, worldMinY, worldMaxY - HEIGHT);
                currentFrame = getFacingAnim("walk").getKeyFrame(stateTime, true);
            } else {
                currentFrame = getFacingAnim("idle").getKeyFrame(stateTime, true);
            }
        }
    }

    private void updateFacing(float dx, float dy) {
        if (Math.abs(dx) >= Math.abs(dy)) {
            facing = dx < 0 ? Facing.LEFT : Facing.RIGHT;
        } else {
            facing = dy < 0 ? Facing.FRONT : Facing.BACK;
        }
    }

    private Animation<TextureRegion> getFacingAnim(String type) {
        switch (type) {
            case "walk":
                switch (facing) {
                    case BACK:  return backWalkAnim;
                    case LEFT:  return leftWalkAnim;
                    case RIGHT: return rightWalkAnim;
                    default:    return frontWalkAnim;
                }
            case "run":
                switch (facing) {
                    case BACK:  return backRunAnim;
                    case LEFT:  return leftRunAnim;
                    case RIGHT: return rightRunAnim;
                    default:    return frontRunAnim;
                }
            case "attack":
                switch (facing) {
                    case BACK:  return backAttackAnim;
                    case LEFT:  return leftAttackAnim;
                    case RIGHT: return rightAttackAnim;
                    default:    return frontAttackAnim;
                }
            case "hurt":
                switch (facing) {
                    case BACK:  return backHurtAnim;
                    case LEFT:  return leftHurtAnim;
                    case RIGHT: return rightHurtAnim;
                    default:    return frontHurtAnim;
                }
            default:
                switch (facing) {
                    case BACK:  return backIdleAnim;
                    case LEFT:  return leftIdleAnim;
                    case RIGHT: return rightIdleAnim;
                    default:    return frontIdleAnim;
                }
        }
    }

    private void pickNewRandomAction() {
        isMoving = MathUtils.randomBoolean(0.7f);
        moveDuration = MathUtils.random(1f, 3f);
        moveTimer = moveDuration;

        if (isMoving) {
            randomDir.set(
                MathUtils.random(-1f, 1f),
                MathUtils.random(-1f, 1f)
            ).nor();
        }
    }

    public void render(SpriteBatch batch) {
        if (disposed) return;
        if (currentFrame == null) return;

        batch.draw(currentFrame, position.x, position.y, WIDTH, HEIGHT);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void takeDamage(int dmg)
    {
        if (disposed || !isAlive()) return;
        health = Math.max(0f, health - dmg);
        if (isAlive()) {
            hurtTimer = HURT_TIME;
            hurtStateTime = 0f;
        } else {
            deathStateTime = 0f;
        }
    }

    public boolean isDead() {
        return !isAlive();
    }

    public void dispose() {
        if (disposed) return;

        for (Texture t : textures) {
            t.dispose();
        }
        disposed = true;
    }
}
