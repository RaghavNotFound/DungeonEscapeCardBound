package io.github.pkgde;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Intersector;
import java.util.ArrayList;

public class Arrow {

    private static Texture texture;

    private static Texture getTexture() {
        if (texture == null) {
            texture = new Texture("Vectors/Arrow.png");
        }
        return texture;
    }

    private Rectangle bounds;
    private Vector2 position;
    private Vector2 direction;

    private static final float SPEED = 400f;
    private static final float SIZE = 32f;

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
        Texture tex = getTexture();
        float angle = direction.angleDeg();

        batch.draw(
            tex,
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
            tex.getWidth(),
            tex.getHeight(),
            false,
            false
        );
    }

    public boolean isCollided(float worldWidth, float worldHeight, ArrayList<Rectangle> obstacles, ArrayList<Polygon> polygons) {
        if (bounds.x < 0 || bounds.x + bounds.width > worldWidth ||
            bounds.y < 0 || bounds.y + bounds.height > worldHeight) {
            return true;
        }

        for (Rectangle rect : obstacles) {
            if (bounds.overlaps(rect)) return true;
        }

        if (polygons != null) {
            Polygon rectPoly = new Polygon(new float[] {
                bounds.x, bounds.y,
                bounds.x + bounds.width, bounds.y,
                bounds.x + bounds.width, bounds.y + bounds.height,
                bounds.x, bounds.y + bounds.height
            });
            for (Polygon poly : polygons) {
                if (Intersector.overlapConvexPolygons(rectPoly, poly)) return true;
            }
        }

        return false;
    }

    public static void disposeTexture() {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
    }
}
