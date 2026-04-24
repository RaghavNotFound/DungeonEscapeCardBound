package io.github.pkgde;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;

public class Arrow {

    private Rectangle bounds;
    private Vector2 position;
    private Vector2 direction;

    private static final float SPEED = 120f;
    private static final float SIZE = 8f;

    // CACHED: Prevents crashing the UI via constantly hitting the global asset manager
    private static Texture arrowTex;

    public Arrow(float x, float y, Vector2 direction) {
        this.position = new Vector2(x, y);
        this.direction = new Vector2(direction).nor();
        this.bounds = new Rectangle(x - SIZE / 2f, y - SIZE / 2f, SIZE, SIZE);
    }

    public void update(float delta) {
        position.mulAdd(direction, SPEED * delta);
        bounds.setPosition(position.x - SIZE / 2f, position.y - SIZE / 2f);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void render(SpriteBatch batch) {
        if (arrowTex == null) {
            arrowTex = Main.assets.get("Vectors/Arrow.png", Texture.class);
        }

        float angle = direction.angleDeg();

        batch.draw(
            arrowTex,
            position.x - SIZE / 2f,
            position.y - SIZE / 2f,
            SIZE / 2f,
            SIZE / 2f,
            SIZE,
            SIZE,
            1f,
            1f,
            angle,
            0,
            0,
            arrowTex.getWidth(),
            arrowTex.getHeight(),
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
            if (bounds.overlaps(rect)) return true;
        }
        return false;
    }
}
