package io.github.pkgde;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;
import java.util.ArrayList;

public class Enemy {

    private final Vector2 position;
    private final Rectangle bounds;

    private final Animation<TextureRegion> walkAnim;
    private TextureRegion currentFrame;

    private final ArrayList<Texture> textures = new ArrayList<>();

    private float stateTime = 0f;

    private enum State { IDLE, CHASE }
    private State state = State.IDLE;

    private float speed = 120f;

    private float baseRange = 200f;
    private float alertRange = baseRange * 2f;

    private float fovAngle = 90f;

    private Vector2 forward = new Vector2(1, 0);

    private final float WIDTH = 128;
    private final float HEIGHT = 128;

    private Vector2 randomDir = new Vector2();
    private float moveTimer = 0f;
    private float moveDuration = 0f;
    private boolean isMoving = false;

    private int hp = 3;

    public Enemy() {
        position = new Vector2(400, 300);
        bounds = new Rectangle(position.x, position.y, WIDTH, HEIGHT);

        walkAnim = load("Movements/Enemy/Front/Walking/Front - Walking_", 10);
        currentFrame = walkAnim.getKeyFrame(0);

        pickNewRandomAction();
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

        Vector2 playerPos = player.getPos();

        Vector2 toPlayer = new Vector2(playerPos).sub(position);
        float distance = toPlayer.len();

        boolean inRange;
        boolean inCone = false;

        if (state == State.CHASE) {
            inRange = distance <= alertRange;
        } else {
            inRange = distance <= baseRange;
        }

        if (inRange) {
            toPlayer.nor();
            float dot = forward.dot(toPlayer);
            float threshold = MathUtils.cosDeg(fovAngle / 2f);
            inCone = dot >= threshold;
        }

        if (inRange && inCone) {
            state = State.CHASE;
        }
        else if (state == State.CHASE && distance <= alertRange) {
            state = State.CHASE;
        }
        else {
            state = State.IDLE;
        }

        switch (state) {

            case IDLE:

                moveTimer -= delta;

                if (moveTimer <= 0) {
                    pickNewRandomAction();
                }

                if (isMoving) {
                    position.mulAdd(randomDir, speed * 0.5f * delta);
                    forward.set(randomDir);
                }

                break;

            case CHASE:

                Vector2 direction = new Vector2(playerPos)
                    .sub(position)
                    .nor();

                position.mulAdd(direction, speed * delta);
                forward.set(direction);

                break;
        }

        position.x = MathUtils.clamp(position.x, 0, 1280 - WIDTH);
        position.y = MathUtils.clamp(position.y, 120f, 720 - HEIGHT);

        bounds.setPosition(position.x, position.y);

        currentFrame = walkAnim.getKeyFrame(stateTime, true);
    }

    private void pickNewRandomAction()
    {

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
    public void render(SpriteBatch batch)
    {
        if (currentFrame==null) return;

        batch.draw(currentFrame,position.x,position.y,WIDTH,HEIGHT);
    }
    public Rectangle getBounds()
    {
        return bounds;
    }

    public void takeDamage(int dmg)
    {
        hp -= dmg;
    }

    public boolean isDead() {
        return hp <= 0;
    }

    public void dispose() {
        for (Texture t : textures)
        {
            t.dispose();
        }
    }
}
