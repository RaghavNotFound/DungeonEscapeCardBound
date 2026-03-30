package io.github.pkgde;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;
import java.util.ArrayList;

public class Enemy {

    private Vector2 position;
    private Rectangle bounds;

    private Animation<TextureRegion> walkAnim;
    private TextureRegion currentFrame;

    private ArrayList<Texture> textures = new ArrayList<>();

    private float stateTime = 0f;

    // ===== AI STATES =====
    private enum State { IDLE, CHASE }
    private State state = State.IDLE;

    // ===== SETTINGS =====
    private float speed = 120f;

    private float baseRange = 200f;
    private float alertRange = baseRange * 2f;

    private float fovAngle = 90f;

    private Vector2 forward = new Vector2(1, 0);

    private final float WIDTH = 128;
    private final float HEIGHT = 128;

    private ArrayList<Rectangle> boundaries;
    private float worldMinX = 0f;
    private float worldMinY = 0f;
    private float worldMaxX = Float.MAX_VALUE;
    private float worldMaxY = Float.MAX_VALUE;

    // 🔥 RANDOM MOVEMENT
    private Vector2 randomDir = new Vector2();
    private float moveTimer = 0f;
    private float moveDuration = 0f;
    private boolean isMoving = false;

    public Enemy() {
        position = new Vector2(400, 300);
        bounds = new Rectangle(position.x, position.y, WIDTH, HEIGHT);

        walkAnim = load("Movements/Enemy/Front/Walking/Front - Walking_", 10);
        currentFrame = walkAnim.getKeyFrame(0);

        pickNewRandomAction();
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

    public void setPosition(float x, float y) {
        position.set(x, y);
        bounds.setPosition(x, y);
    }

    private Animation<TextureRegion> load(String path, int count) {
        TextureRegion[] frames = new TextureRegion[count];

        for (int i = 0; i < count; i++) {
            Texture tex = new Texture(path + String.format("%03d", i) + ".png");
            textures.add(tex);
            frames[i] = new TextureRegion(tex);
        }

        return new Animation<>(0.1f, frames);
    }

    public void update(float delta, Player player) {

        stateTime += delta;

        Vector2 playerPos = player.getPosition();

        Vector2 toPlayer = new Vector2(playerPos).sub(position);
        float distance = toPlayer.len();

        boolean inRange;
        boolean inCone = false;

        // ===== RANGE =====
        if (state == State.CHASE) {
            inRange = distance <= alertRange;
        } else {
            inRange = distance <= baseRange;
        }

        // ===== VISION =====
        if (inRange) {
            toPlayer.nor();
            float dot = forward.dot(toPlayer);
            float threshold = MathUtils.cosDeg(fovAngle / 2f);
            inCone = dot >= threshold;
        }

        // ===== STATE =====
        if (inRange && inCone) {
            state = State.CHASE;
        }
        else if (state == State.CHASE && distance <= alertRange) {
            state = State.CHASE;
        }
        else {
            state = State.IDLE;
        }

        // ===== BEHAVIOR =====
        switch (state) {

            case IDLE:

                moveTimer -= delta;

                if (moveTimer <= 0) {
                    pickNewRandomAction();
                }

                if (isMoving) {
                    moveBy(randomDir.x * speed * 0.5f * delta, randomDir.y * speed * 0.5f * delta);
                    forward.set(randomDir);
                }

                break;

            case CHASE:

                Vector2 direction = new Vector2(playerPos)
                    .sub(position)
                    .nor();

                moveBy(direction.x * speed * delta, direction.y * speed * delta);
                forward.set(direction);

                break;
        }

        // Keep enemy inside map bounds.
        position.x = MathUtils.clamp(position.x, worldMinX, worldMaxX - WIDTH);
        position.y = MathUtils.clamp(position.y, worldMinY, worldMaxY - HEIGHT);

        bounds.setPosition(position.x, position.y);

        currentFrame = walkAnim.getKeyFrame(stateTime, true);
    }

    private void moveBy(float dx, float dy) {
        float newX = position.x + dx;
        float newY = position.y + dy;

        if (canMoveTo(newX, position.y)) {
            position.x = newX;
        }

        if (canMoveTo(position.x, newY)) {
            position.y = newY;
        }
    }

    private boolean canMoveTo(float x, float y) {
        Rectangle next = new Rectangle(x, y, WIDTH, HEIGHT);

        if (boundaries != null) {
            for (Rectangle wall : boundaries) {
                if (next.overlaps(wall)) {
                    return false;
                }
            }
        }

        return true;
    }

    // 🔥 RANDOM ACTION PICKER
    private void pickNewRandomAction() {

        isMoving = MathUtils.randomBoolean(0.7f); // 70% move, 30% idle

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
        batch.draw(currentFrame, position.x, position.y, WIDTH, HEIGHT);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void dispose() {
        for (Texture t : textures) {
            t.dispose();
        }
    }
}
