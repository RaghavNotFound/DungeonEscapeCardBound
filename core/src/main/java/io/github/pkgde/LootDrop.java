package io.github.pkgde;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class LootDrop {

    public enum Type { HEALTH, TORCH }

    private Type type;
    private Rectangle bounds;
    private float floatTimer = 0f;
    private float baseY;

    public LootDrop(Type type, float x, float y) {
        this.type = type;
        this.bounds = new Rectangle(x, y, 20f, 20f);
        this.baseY = y;
    }

    public Type getType() {
        return type;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void update(float delta) {
        floatTimer += delta * 4f;
        bounds.y = baseY + (float) Math.sin(floatTimer) * 4f;
    }

    public void render(SpriteBatch batch, ShapeRenderer shape) {
        // Drop shadow
        shape.setColor(0f, 0f, 0f, 0.4f);
        shape.ellipse(bounds.x, baseY - 4f, bounds.width, 8f);

        // Core visual
        if (type == Type.HEALTH) {
            shape.setColor(Color.RED);
            shape.rect(bounds.x, bounds.y, bounds.width, bounds.height);
            shape.setColor(Color.WHITE);
            shape.rect(bounds.x + 8f, bounds.y + 2f, 4f, 16f);
            shape.rect(bounds.x + 2f, bounds.y + 8f, 16f, 4f);
        } else {
            shape.setColor(Color.ORANGE);
            shape.rect(bounds.x + 6f, bounds.y, 8f, 14f);
            shape.setColor(Color.YELLOW);
            shape.circle(bounds.x + 10f, bounds.y + 18f, 6f);
        }
    }
}
