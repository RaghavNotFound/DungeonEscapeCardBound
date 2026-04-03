package io.github.pkgde;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class LootDrop {

    public enum Type { HEALTH, TORCH, ARROW, STAMINA, CARD }

    private Type type;
    private Rectangle bounds;
    private float floatTimer = 0f;
    private float baseY;

    // ===== LIFETIME =====
    private static final float MAX_LIFETIME = 15f;
    private static final float BLINK_START = 12f; // Start blinking 3s before expiry
    private float lifetime = 0f;
    private boolean expired = false;

    // ===== MAGNET =====
    private static final float MAGNET_RANGE = 100f;
    private static final float MAGNET_RANGE_SQ = MAGNET_RANGE * MAGNET_RANGE;
    private static final float MAGNET_SPEED = 250f;

    // ===== GLOW =====
    private float glowTimer = 0f;

    public LootDrop(Type type, float x, float y) {
        this.type = type;
        this.bounds = new Rectangle(x - 10f, y - 10f, 20f, 20f);
        this.baseY = y - 10f;
    }

    public Type getType() {
        return type;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public boolean isExpired() {
        return expired;
    }

    public void update(float delta) {
        lifetime += delta;
        if (lifetime >= MAX_LIFETIME) {
            expired = true;
            return;
        }

        floatTimer += delta * 4f;
        glowTimer += delta;
        bounds.y = baseY + (float) Math.sin(floatTimer) * 5f;
    }

    /**
     * Apply magnet attraction toward the player. Call this from GameWorld.
     */
    public void attractToward(float targetX, float targetY, float delta) {
        float cx = bounds.x + bounds.width / 2f;
        float cy = bounds.y + bounds.height / 2f;
        float dx = targetX - cx;
        float dy = targetY - cy;
        float distSq = dx * dx + dy * dy;

        if (distSq < MAGNET_RANGE_SQ && distSq > 1f) {
            float dist = (float) Math.sqrt(distSq);
            float factor = 1f - (dist / MAGNET_RANGE); // Stronger when closer
            float speed = MAGNET_SPEED * factor * factor;
            float nx = dx / dist;
            float ny = dy / dist;
            bounds.x += nx * speed * delta;
            bounds.y += ny * speed * delta;
            baseY += ny * speed * delta;
        }
    }

    public void render(SpriteBatch batch, ShapeRenderer shape) {
        // Blinking effect near end of lifetime
        if (lifetime >= BLINK_START) {
            float blinkSpeed = 8f + (lifetime - BLINK_START) * 4f; // Faster as it expires
            if (MathUtils.sin(lifetime * blinkSpeed) < 0f) {
                return; // Skip render frame for blink
            }
        }

        float cx = bounds.x + bounds.width / 2f;
        float cy = bounds.y + bounds.height / 2f;

        // Pulsing glow ring
        float glowAlpha = 0.15f + 0.1f * MathUtils.sin(glowTimer * 5f);
        Color glowColor = getGlowColor();
        shape.setColor(glowColor.r, glowColor.g, glowColor.b, glowAlpha);
        shape.circle(cx, cy, bounds.width * 0.8f);

        // Drop shadow
        shape.setColor(0f, 0f, 0f, 0.4f);
        shape.ellipse(bounds.x, baseY - 4f, bounds.width, 8f);

        // Core visual
        switch (type) {
            case HEALTH:
                renderHealth(shape);
                break;
            case TORCH:
                renderTorch(shape);
                break;
            case ARROW:
                renderArrow(shape);
                break;
            case STAMINA:
                renderStamina(shape);
                break;
            case CARD:
                renderCard(shape);
                break;
        }
    }

    private void renderHealth(ShapeRenderer shape) {
        // Red cross
        shape.setColor(0.85f, 0.15f, 0.15f, 1f);
        shape.rect(bounds.x + 2f, bounds.y + 2f, bounds.width - 4f, bounds.height - 4f);
        shape.setColor(1f, 1f, 1f, 1f);
        shape.rect(bounds.x + 8f, bounds.y + 3f, 4f, 14f);
        shape.rect(bounds.x + 3f, bounds.y + 8f, 14f, 4f);
    }

    private void renderTorch(ShapeRenderer shape) {
        // Brown stick
        shape.setColor(0.55f, 0.35f, 0.12f, 1f);
        shape.rect(bounds.x + 7f, bounds.y, 6f, 12f);
        // Flame
        float flicker = 0.8f + 0.2f * MathUtils.sin(glowTimer * 12f);
        shape.setColor(1f * flicker, 0.6f * flicker, 0.1f, 1f);
        shape.circle(bounds.x + 10f, bounds.y + 16f, 5f);
        shape.setColor(1f, 0.9f * flicker, 0.3f, 0.8f);
        shape.circle(bounds.x + 10f, bounds.y + 18f, 3f);
    }

    private void renderArrow(ShapeRenderer shape) {
        float cx = bounds.x + bounds.width / 2f;
        // Shaft
        shape.setColor(0.6f, 0.5f, 0.3f, 1f);
        shape.rect(cx - 1.5f, bounds.y + 2f, 3f, 14f);
        // Arrowhead
        shape.setColor(0.7f, 0.7f, 0.75f, 1f);
        shape.triangle(
            cx - 4f, bounds.y + 14f,
            cx + 4f, bounds.y + 14f,
            cx, bounds.y + 20f
        );
        // Fletching
        shape.setColor(0.3f, 0.6f, 0.3f, 1f);
        shape.triangle(
            cx - 3f, bounds.y + 2f,
            cx + 3f, bounds.y + 2f,
            cx, bounds.y + 5f
        );
    }

    private void renderStamina(ShapeRenderer shape) {
        float cx = bounds.x + bounds.width / 2f;
        float cy = bounds.y + bounds.height / 2f;
        // Green energy orb
        float pulse = 0.85f + 0.15f * MathUtils.sin(glowTimer * 8f);
        shape.setColor(0.1f, 0.85f * pulse, 0.3f, 0.4f);
        shape.circle(cx, cy, 10f);
        shape.setColor(0.2f, 0.95f * pulse, 0.4f, 0.8f);
        shape.circle(cx, cy, 6f);
        shape.setColor(0.5f, 1f, 0.7f, 1f);
        shape.circle(cx, cy, 3f);
    }

    private void renderCard(ShapeRenderer shape) {
        float cx = bounds.x + bounds.width / 2f;
        float cy = bounds.y + bounds.height / 2f;
        // Draw a light blue rectangular card
        shape.setColor(0.8f, 0.9f, 1f, 1f);
        shape.rect(cx - 8f, cy - 10f, 16f, 20f);
        // Draw a golden border/pattern inside
        float pulse = 0.8f + 0.2f * MathUtils.sin(glowTimer * 6f);
        shape.setColor(0.9f * pulse, 0.7f * pulse, 0.2f, 1f);
        shape.rect(cx - 5f, cy - 7f, 10f, 14f);
        // Center jewel/symbol
        shape.setColor(0.2f, 0.6f, 1f, 1f);
        shape.rect(cx - 2f, cy - 2f, 4f, 4f);
    }

    private Color getGlowColor() {
        switch (type) {
            case HEALTH:  return new Color(1f, 0.2f, 0.2f, 1f);
            case TORCH:   return new Color(1f, 0.7f, 0.1f, 1f);
            case ARROW:   return new Color(0.7f, 0.7f, 0.8f, 1f);
            case STAMINA: return new Color(0.2f, 1f, 0.4f, 1f);
            case CARD:    return new Color(0.4f, 0.8f, 1f, 1f);
            default:      return Color.WHITE;
        }
    }
}
