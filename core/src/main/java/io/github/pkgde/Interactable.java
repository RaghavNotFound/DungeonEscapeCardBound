package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/**
 * Represents an in-world object the player can interact with (chests, signs, barrels).
 * Handles its own rendering, prompt display, and interaction logic.
 */
public class Interactable {

    public enum Type { CHEST, SIGN, BARREL, DOOR, CENTER_FIRE }

    private final Type type;
    private final Rectangle bounds;
    private boolean interacted = false;
    private float animTime = 0f;

    // For SIGN type
    private String signText = "";

    // For showing the text popup after interaction
    private float popupTimer = 0f;
    private static final float POPUP_DURATION = 3f;

    // Interaction range (how close the player must be)
    private static final float INTERACT_RANGE = 80f;

    private final GlyphLayout glyphLayout = new GlyphLayout();

    public Interactable(Type type, float x, float y, float width, float height) {
        this.type = type;
        this.bounds = new Rectangle(x, y, width, height);
    }

    public Interactable(Type type, float x, float y, float width, float height, String signText) {
        this(type, x, y, width, height);
        this.signText = signText;
    }

    public boolean isPlayerInRange(Player player) {
        Rectangle pBounds = player.getBounds();
        float px = pBounds.x + pBounds.width / 2f;
        float py = pBounds.y + pBounds.height / 2f;
        float ix = bounds.x + bounds.width / 2f;
        float iy = bounds.y + bounds.height / 2f;
        float dx = px - ix;
        float dy = py - iy;
        return (dx * dx + dy * dy) <= INTERACT_RANGE * INTERACT_RANGE;
    }

    /**
     * Perform the interaction. Returns true if something happened.
     */
    public boolean interact(Player player) {
        if (interacted && type != Type.SIGN) return false;

        interacted = true;
        popupTimer = POPUP_DURATION;
        return true;
    }

    public void update(float delta) {
        animTime += delta;
        if (popupTimer > 0f) {
            popupTimer -= delta;
        }
    }

    public void render(SpriteBatch batch, ShapeRenderer shape, BitmapFont font, boolean playerInRange) {
        float cx = bounds.x + bounds.width / 2f;
        float cy = bounds.y + bounds.height / 2f;

        // 1. Render the Object Shape
        switch (type) {
            case CHEST -> renderChest(shape, playerInRange);
            case SIGN -> renderSign(shape, playerInRange);
            case BARREL -> renderBarrel(shape, playerInRange);
            case CENTER_FIRE -> renderCenterFire(shape, playerInRange);
        }

        // 2. Draw "Press [G]" Prompt
        if (playerInRange && (!interacted || type == Type.SIGN)) {
            renderPrompt(batch, shape, font, cx);
        }

        // 3. Draw Sign Text Popup
        if (type == Type.SIGN && popupTimer > 0f && !signText.isEmpty()) {
            renderSignPopup(batch, shape, font, cx);
        }
    }

    private void renderPrompt(SpriteBatch batch, ShapeRenderer shape, BitmapFont font, float cx) {
        float promptY = bounds.y + bounds.height + 20f;
        float bobOffset = MathUtils.sin(animTime * 5f) * 3f;

        batch.begin();
        font.getData().setScale(0.9f);
        glyphLayout.setText(font, "Press [G]");
        float promptX = cx - glyphLayout.width / 2f;
        batch.end();

        // Background pill
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0f, 0f, 0.7f);
        shape.rect(promptX - 6f, promptY + bobOffset - glyphLayout.height - 4f,
            glyphLayout.width + 12f, glyphLayout.height + 8f);
        shape.end();

        batch.begin();
        font.setColor(0.95f, 0.85f, 0.3f, 1f);
        font.draw(batch, glyphLayout, promptX, promptY + bobOffset);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void renderSignPopup(SpriteBatch batch, ShapeRenderer shape, BitmapFont font, float cx) {
        float popupAlpha = MathUtils.clamp(popupTimer / 0.5f, 0f, 1f);
        float popupY = bounds.y + bounds.height + 40f;

        batch.begin();
        font.getData().setScale(0.85f);
        glyphLayout.setText(font, signText);
        float popupX = cx - glyphLayout.width / 2f;
        batch.end();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.05f, 0.05f, 0.08f, 0.85f * popupAlpha);
        shape.rect(popupX - 10f, popupY - glyphLayout.height - 8f, glyphLayout.width + 20f, glyphLayout.height + 16f);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.8f, 0.7f, 0.3f, popupAlpha);
        shape.rect(popupX - 10f, popupY - glyphLayout.height - 8f, glyphLayout.width + 20f, glyphLayout.height + 16f);
        shape.end();

        batch.begin();
        font.setColor(1f, 1f, 1f, popupAlpha);
        font.draw(batch, glyphLayout, popupX, popupY);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void renderChest(ShapeRenderer shape, boolean playerInRange) {
        float x = bounds.x, y = bounds.y, w = bounds.width, h = bounds.height;
        shape.begin(ShapeRenderer.ShapeType.Filled);

        if (interacted) {
            shape.setColor(0.35f, 0.22f, 0.08f, 1f); // Interior
            shape.rect(x, y, w, h * 0.6f);
            shape.setColor(0.45f, 0.3f, 0.1f, 1f); // Open lid
            shape.rect(x - 2, y + h * 0.6f, w + 4, h * 0.25f);
        } else {
            shape.setColor(0.55f, 0.35f, 0.1f, 1f); // Body
            shape.rect(x, y, w, h * 0.7f);
            shape.setColor(0.65f, 0.4f, 0.12f, 1f); // Lid
            shape.rect(x - 2, y + h * 0.7f, w + 4, h * 0.3f);

            float glint = 0.7f + 0.3f * MathUtils.sin(animTime * 4f);
            shape.setColor(0.9f * glint, 0.75f * glint, 0.1f, 1f); // Lock
            shape.rect(x + w / 2f - 4f, y + h * 0.55f, 8f, 12f);
        }

        if (playerInRange && !interacted) {
            float glow = 0.15f + 0.1f * MathUtils.sin(animTime * 6f);
            shape.setColor(1f, 0.85f, 0.3f, glow);
            shape.rect(x - 4, y - 4, w + 8, h + 8);
        }
        shape.end();
    }

    private void renderSign(ShapeRenderer shape, boolean playerInRange) {
        float x = bounds.x, y = bounds.y, w = bounds.width, h = bounds.height;
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.4f, 0.28f, 0.12f, 1f); // Post
        shape.rect(x + w / 2f - 3f, y, 6f, h * 0.5f);
        shape.setColor(0.55f, 0.4f, 0.18f, 1f); // Board
        shape.rect(x, y + h * 0.45f, w, h * 0.55f);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.3f, 0.2f, 0.05f, 1f);
        shape.rect(x, y + h * 0.45f, w, h * 0.55f);
        if (playerInRange) {
            float glow = 0.5f + 0.3f * MathUtils.sin(animTime * 5f);
            shape.setColor(1f, 0.9f, 0.5f, glow);
            shape.rect(x - 2, y + h * 0.43f, w + 4, h * 0.59f);
        }
        shape.end();
    }

    private void renderBarrel(ShapeRenderer shape, boolean playerInRange) {
        float x = bounds.x, y = bounds.y, w = bounds.width, h = bounds.height;
        shape.begin(ShapeRenderer.ShapeType.Filled);

        if (interacted) {
            shape.setColor(0.35f, 0.25f, 0.1f, 0.6f);
            shape.rect(x, y, w, h * 0.3f);
        } else {
            shape.setColor(0.5f, 0.33f, 0.12f, 1f);
            shape.rect(x + 2, y, w - 4, h);
            shape.setColor(0.38f, 0.28f, 0.1f, 1f); // Rings
            shape.rect(x, y + h * 0.05f, w, h * 0.08f);
            shape.rect(x, y + h * 0.87f, w, h * 0.08f);
            shape.rect(x, y + h * 0.45f, w, h * 0.08f);

            if (playerInRange) {
                float glow = 0.15f + 0.1f * MathUtils.sin(animTime * 6f);
                shape.setColor(1f, 0.85f, 0.3f, glow);
                shape.rect(x - 3, y - 3, w + 6, h + 6);
            }
        }
        shape.end();
    }

    private void renderCenterFire(ShapeRenderer shape, boolean playerInRange) {
        float cx = bounds.x + bounds.width / 2f;
        float cy = bounds.y + bounds.height / 2f;
        float radius = Math.min(bounds.width, bounds.height) / 2f;

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.3f, 0.3f, 0.35f, 1f); // Stone ring
        shape.circle(cx, cy, radius);
        shape.setColor(0.15f, 0.15f, 0.15f, 1f); // Inside pit
        shape.circle(cx, cy, radius * 0.7f);

        shape.setColor(0.25f, 0.15f, 0.05f, 1f); // Unlit logs
        shape.rect(cx - radius * 0.6f, cy - 3f, radius * 1.2f, 6f);
        shape.rect(cx - 3f, cy - radius * 0.6f, 6f, radius * 1.2f);

        if (playerInRange && !interacted) {
            float glow = 0.15f + 0.1f * MathUtils.sin(animTime * 6f);
            shape.setColor(1f, 0.85f, 0.3f, glow);
            shape.circle(cx, cy, radius + 4f);
        }
        shape.end();
    }

    // ===== GETTERS & SETTERS =====
    public Type getType() { return type; }
    public Rectangle getBounds() { return bounds; }
    public boolean isInteracted() { return interacted; }
    public void setInteracted(boolean interacted) { this.interacted = interacted; }
    public String getSignText() { return signText; }
    public float getPopupTimer() { return popupTimer; }
}
