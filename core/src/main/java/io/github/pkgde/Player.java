package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Player {

    private final Animation<TextureRegion> walkAnimation;
    private final TextureRegion[] walkFrames;
    private TextureRegion currentFrame;

    private final Texture[] textures; // store textures to dispose later

    private float stateTime;
    private boolean isMoving;

    private final Vector2 position;

    public Rectangle bounds;

    private final int WIDTH = 64;
    private final int HEIGHT = 64;

    public Player() {
        position = new Vector2(100, 100);
        bounds = new Rectangle(position.x, position.y, WIDTH, HEIGHT);

        int frameCount = 23;

        textures = new Texture[frameCount];
        walkFrames = new TextureRegion[frameCount];

        for (int i = 0; i < frameCount; i++) {
            String fileName = String.format(
                "Movments/Walking/0_Forest_Ranger_Walking_%03d.png", i
            );

            textures[i] = new Texture(fileName);
            walkFrames[i] = new TextureRegion(textures[i]);
        }

        walkAnimation = new Animation<>(0.08f, walkFrames); // smoother animation
        currentFrame = walkFrames[0];
        stateTime = 0f;
    }

    public void update(float delta) {
        handleMovement(delta);

        if (isMoving) {
            stateTime += delta;
            currentFrame = walkAnimation.getKeyFrame(stateTime, true);
        } else {
            currentFrame = walkFrames[0]; // idle
        }

        bounds.setPosition(position.x, position.y);
    }

    private void handleMovement(float delta) {
        float newX = position.x;
        float newY = position.y;

        isMoving = false;

        float speed = 100;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            newY += speed * delta;
            isMoving = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            newY -= speed * delta;
            isMoving = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            newX -= speed * delta;
            isMoving = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            newX += speed * delta;
            isMoving = true;
        }

        newX = MathUtils.clamp(newX, 0, 800 - WIDTH);
        newY = MathUtils.clamp(newY, 0, 600 - HEIGHT);

        position.set(newX, newY);
    }

    public void render(SpriteBatch batch) {
        batch.draw(currentFrame, position.x, position.y, WIDTH, HEIGHT);
    }

    public void dispose() {
        for (Texture tex : textures) {
            tex.dispose();
        }
    }
}
