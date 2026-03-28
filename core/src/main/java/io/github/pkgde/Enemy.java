package io.github.pkgde;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;

public class Enemy {

    private Vector2 position;
    private Vector2 forward;
    private Rectangle bounds;

    private float patrolSpeed = 70f;
    private float chaseSpeed = 130f;

    private float range = 250f;
    private float fovAngle = 90f;

    private float patrolLeft;
    private float patrolRight;
    private float patrolTop;
    private float patrolBottom;
    private boolean movingRight = true;
    private boolean movingUp = true;

    private float stateTime = 0f;

    private final int WIDTH = 128;
    private final int HEIGHT = 128;

    private enum State { WALK, RUN }
    private enum Direction { FRONT, BACK, LEFT, RIGHT }

    private State state = State.WALK;
    private Direction direction = Direction.FRONT;

    private Animation<TextureRegion> walkFront, walkBack, walkLeft, walkRight;
    private Animation<TextureRegion> runFront, runBack, runLeft, runRight;

    private TextureRegion currentFrame;

    public Enemy() {

        position = new Vector2(400, 300);
        forward = new Vector2(1, 0);

        bounds = new Rectangle(position.x, position.y, WIDTH, HEIGHT);

        patrolLeft = position.x - 350;
        patrolRight = position.x + 350;
        patrolTop = 480f;
        patrolBottom = 130f;

        loadAnimations();
    }

    private Animation<TextureRegion> load(String folder, String prefix, int count) {

        TextureRegion[] frames = new TextureRegion[count];

        for (int i = 0; i < count; i++) {

            String fileName = folder + "/" + prefix + "_" + String.format("%03d", i) + ".png";

            frames[i] = new TextureRegion(new Texture(fileName));
        }

        return new Animation<>(0.1f, frames);
    }

    private void loadAnimations() {

        walkFront = load("Movements/Enemy/Front/Walking", "Front - Walking", 10);
        walkBack  = load("Movements/Enemy/Back/Walking", "Back - Walking", 10);
        walkLeft  = load("Movements/Enemy/Left/Walking", "Left - Walking", 10);
        walkRight = load("Movements/Enemy/Right/Walking", "Right - Walking", 10);

        runFront = load("Movements/Enemy/Front/Running", "Front - Running", 10);
        runBack  = load("Movements/Enemy/Back/Running", "Back - Running", 10);
        runLeft  = load("Movements/Enemy/Left/Running", "Left - Running", 10);
        runRight = load("Movements/Enemy/Right/Running", "Right - Running", 10);

        currentFrame = walkFront.getKeyFrame(0);
    }

    private void updateDirectionFromMovement() {

        if (Math.abs(forward.x) > Math.abs(forward.y)) {
            direction = forward.x > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            // FIX: In libGDX, Y+ is UP (camera faces down toward player's back → BACK animation)
            //      Y- is DOWN (enemy faces toward camera → FRONT animation)
            direction = forward.y > 0 ? Direction.BACK : Direction.FRONT;
        }
    }

    public void update(float delta, Player player) {

        stateTime += delta;

        if (canSeePlayer(player)) {
            state = State.RUN;
            chase(player, delta);
        } else {
            state = State.WALK;
            patrol(delta);
        }

        float minY = 120f;
        float maxY = 800f;

        position.y = MathUtils.clamp(position.y, minY, maxY - HEIGHT);

        if (state == State.RUN) {
            updateDirection(player);
        } else {
            updateDirectionFromMovement();
        }

        updateAnimation();

        bounds.setPosition(position.x, position.y);
    }

    private void patrol(float delta) {

        float dx = 0;
        float dy = 0;

        // HORIZONTAL
        if (movingRight) {
            dx = patrolSpeed * delta;
            position.x += dx;
            if (position.x > patrolRight) movingRight = false;
        } else {
            dx = -patrolSpeed * delta;
            position.x += dx;
            if (position.x < patrolLeft) movingRight = true;
        }

        // VERTICAL
        if (movingUp) {
            dy = patrolSpeed * delta;
            position.y += dy;
            if (position.y > patrolTop) movingUp = false;
        } else {
            dy = -patrolSpeed * delta;
            position.y += dy;
            if (position.y < patrolBottom) movingUp = true;
        }

        // 🔥 FIXED FORWARD (NO OVERRIDE)
        // FIX: Only update forward when there is actual movement (dx or dy non-zero),
        //      preventing a zero vector from corrupting the last valid direction.
        if (dx != 0 || dy != 0) {
            forward.set(dx, dy).nor();
        }
    }

    private void chase(Player player, float delta) {

        Vector2 target = new Vector2(player.bounds.x, player.bounds.y);
        Vector2 dir = target.sub(position).nor();

        position.add(dir.scl(chaseSpeed * delta));
        forward.set(dir);
    }

    private void updateDirection(Player player) {

        Vector2 dir = new Vector2(player.bounds.x, player.bounds.y)
            .sub(position)
            .nor();

        if (Math.abs(dir.x) > Math.abs(dir.y)) {
            direction = dir.x > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            // FIX: Mirror the same corrected axis logic used in updateDirectionFromMovement()
            direction = dir.y > 0 ? Direction.BACK : Direction.FRONT;
        }
    }

    private void updateAnimation() {

        switch (state) {

            case WALK:
                currentFrame = getWalk().getKeyFrame(stateTime, true);
                break;

            case RUN:
                currentFrame = getRun().getKeyFrame(stateTime, true);
                break;
        }
    }

    private Animation<TextureRegion> getWalk() {
        switch (direction) {
            case FRONT: return walkFront;
            case BACK: return walkBack;
            case LEFT: return walkLeft;
            case RIGHT: return walkRight;
        }
        return walkFront;
    }

    private Animation<TextureRegion> getRun() {
        switch (direction) {
            case FRONT: return runFront;
            case BACK: return runBack;
            case LEFT: return runLeft;
            case RIGHT: return runRight;
        }
        return runFront;
    }

    private boolean canSeePlayer(Player player) {

        Vector2 toPlayer = new Vector2(player.bounds.x, player.bounds.y)
            .sub(position);

        if (toPlayer.len2() > range * range) return false;

        toPlayer.nor();

        float dot = forward.dot(toPlayer);
        float threshold = MathUtils.cosDeg(fovAngle / 2);

        return dot >= threshold;
    }

    public void render(SpriteBatch batch) {
        batch.draw(currentFrame, position.x, position.y, WIDTH, HEIGHT);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void dispose() {
        // For now empty
    }
}
