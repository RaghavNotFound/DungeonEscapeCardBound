package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList; // ✅ ADDED

public class Arrow {

    private static final Texture texture = new Texture("Vectors/Arrow.png");

    private Rectangle bounds;

    private Vector2 position;
    private Vector2 direction;

    private float speed = 400f;

    private float width = 32;
    private float height = 32;

    public Arrow(float x, float y, Vector2 direction) {
        this.position = new Vector2(x, y);
        this.direction = new Vector2(direction).nor();
        this.bounds = new Rectangle(x, y, width, height);
    }

    public void update(float delta) {
        position.x += direction.x * speed * delta;
        position.y += direction.y * speed * delta;

        bounds.setPosition(position.x - width / 2f, position.y - height / 2f);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void render(SpriteBatch batch) {

        float angle = direction.angleDeg();

        batch.draw(
            texture,
            position.x - width / 2f,
            position.y - height / 2f,
            width / 2f,
            height / 2f,
            width,
            height,
            1f,
            1f,
            angle,
            0,
            0,
            texture.getWidth(),
            texture.getHeight(),
            false,
            false
        );
    }

    public boolean isCollided(float worldWidth, float worldHeight, ArrayList<Rectangle> obstacles) {

        if (bounds.x < 0 || bounds.x + bounds.width > worldWidth ||
            bounds.y < 0 || bounds.y + bounds.height > worldHeight) {
            return true;
        }

        for (Rectangle rect : obstacles) {
            if (bounds.overlaps(rect)) {
                return true;
            }
        }

        return false;
    }

    public static void disposeTexture() {
        texture.dispose();
    }
}
