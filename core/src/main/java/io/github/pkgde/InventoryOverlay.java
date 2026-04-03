package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * An overlay UI for managing player inventory.
 * Features a grid-based layout, item counts, and procedural/texture-based icons.
 */
public class InventoryOverlay {

    private static final int GRID_COLS = 5;
    private static final int GRID_ROWS = 4;
    private static final int SLOT_COUNT = GRID_COLS * GRID_ROWS;

    private int selectedCol = 0;
    private int selectedRow = 0;

    private final Texture torchIcon;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    private final Color slotColor = new Color(0.1f, 0.1f, 0.1f, 0.7f);
    private final Color slotOutlineColor = new Color(0.8f, 0.8f, 0.8f, 0.5f);
    private final Color selectedColor = new Color(0.25f, 0.85f, 1f, 1f);

    private final Rectangle[] slots = new Rectangle[SLOT_COUNT];

    public InventoryOverlay() {
        // Use an existing torch animation frame as the static icon
        torchIcon = new Texture("Objects/torch/torch_1.png");
        torchIcon.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = new Rectangle();
        }
    }

    public void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            selectedCol = (selectedCol + 1) % GRID_COLS;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            selectedCol = (selectedCol + GRID_COLS - 1) % GRID_COLS;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selectedRow = (selectedRow + 1) % GRID_ROWS;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selectedRow = (selectedRow + GRID_ROWS - 1) % GRID_ROWS;
        }
    }

    private void updateLayout(Viewport viewport) {
        float totalGridWidth = viewport.getWorldWidth() * 0.6f;
        float totalGridHeight = viewport.getWorldHeight() * 0.7f;
        float slotSize = Math.min(totalGridWidth / GRID_COLS, totalGridHeight / GRID_ROWS) * 0.9f;
        float gap = slotSize * 0.1f;

        float gridW = GRID_COLS * (slotSize + gap) - gap;
        float gridH = GRID_ROWS * (slotSize + gap) - gap;

        float startX = (viewport.getWorldWidth() - gridW) / 2f;
        float startY = (viewport.getWorldHeight() - gridH) / 2f - 40f;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int index = row * GRID_COLS + col;
                slots[index].set(
                    startX + col * (slotSize + gap),
                    startY + (GRID_ROWS - 1 - row) * (slotSize + gap),
                    slotSize,
                    slotSize
                );
            }
        }
    }

    public void render(ShapeRenderer shape, SpriteBatch batch, BitmapFont font, Viewport viewport, Player player) {
        updateLayout(viewport);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // 1. Draw Background Panel
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0, 0, 0, 0.6f);
        Rectangle firstSlot = slots[0];
        Rectangle lastSlot = slots[SLOT_COUNT - 1];
        float panelX = firstSlot.x - 20;
        float panelY = lastSlot.y - 20;
        float panelW = (slots[GRID_COLS - 1].x + slots[GRID_COLS - 1].width) - firstSlot.x + 40;
        float panelH = (firstSlot.y + firstSlot.height) - lastSlot.y + 80;
        shape.rect(panelX, panelY, panelW, panelH);
        shape.end();

        // 2. Draw Slots
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (Rectangle slot : slots) {
            shape.setColor(slotColor);
            shape.rect(slot.x, slot.y, slot.width, slot.height);
        }
        shape.end();

        // 3. Draw Outlines
        shape.begin(ShapeRenderer.ShapeType.Line);
        for (Rectangle slot : slots) {
            shape.setColor(slotOutlineColor);
            shape.rect(slot.x, slot.y, slot.width, slot.height);
        }

        // Selection Highlight
        int selectedIndex = selectedRow * GRID_COLS + selectedCol;
        Rectangle selectedSlot = slots[selectedIndex];
        shape.setColor(selectedColor);
        shape.rect(selectedSlot.x, selectedSlot.y, selectedSlot.width, selectedSlot.height);
        shape.end();

        // 4. Procedural Card Icon (Slot 1)
        if (player.getCardsCount() > 0) {
            shape.begin(ShapeRenderer.ShapeType.Filled);
            Rectangle slot = slots[1];
            float iconSize = slot.height * 0.45f; // Shrunk to prevent overlap
            float cx = slot.x + slot.width / 2f;
            float cy = slot.y + slot.height / 2f; // Centered vertically

            shape.setColor(0.8f, 0.9f, 1f, 1f); // Card base
            shape.rect(cx - iconSize * 0.35f, cy - iconSize * 0.45f, iconSize * 0.7f, iconSize * 0.9f);
            shape.setColor(0.9f, 0.7f, 0.2f, 1f); // Gold pattern
            shape.rect(cx - iconSize * 0.2f, cy - iconSize * 0.3f, iconSize * 0.4f, iconSize * 0.6f);
            shape.setColor(0.2f, 0.6f, 1f, 1f); // Center jewel
            shape.rect(cx - iconSize * 0.1f, cy - iconSize * 0.1f, iconSize * 0.2f, iconSize * 0.2f);
            shape.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        float oldScaleX = font.getData().scaleX;
        float oldScaleY = font.getData().scaleY;
        float shadow = Math.max(1.3f, viewport.getWorldWidth() * 0.0012f);

        // 5. Title Rendering
        font.getData().setScale(viewport.getWorldWidth() / 800f * 1.5f);
        glyphLayout.setText(font, "INVENTORY");
        float titleX = (viewport.getWorldWidth() - glyphLayout.width) / 2f;
        float titleY = slots[0].y + slots[0].height + glyphLayout.height + 20;

        // FIXED: Using "INVENTORY" string directly instead of glyphLayout
        font.setColor(0f, 0f, 0f, 0.72f);
        font.draw(batch, "INVENTORY", titleX + shadow, titleY - shadow);
        font.setColor(Color.WHITE);
        font.draw(batch, "INVENTORY", titleX, titleY);

        // 6. Draw Texture Icons & Quantities
        int torchCount = player.getTorchCount();
        if (torchCount > 0) {
            Rectangle slot = slots[0];
            float iconSize = slot.height * 0.45f; // Shrunk to prevent overlap
            float iconX = slot.x + (slot.width - iconSize) / 2f;
            float iconY = slot.y + (slot.height - iconSize) / 2f; // Centered vertically
            batch.draw(torchIcon, iconX, iconY, iconSize, iconSize);

            renderQuantity(batch, font, String.valueOf(torchCount), slot, viewport, shadow);
        }

        if (player.getCardsCount() > 0) {
            renderQuantity(batch, font, String.valueOf(player.getCardsCount()), slots[1], viewport, shadow);
        }

        // 7. Tooltips
        if (selectedRow == 0 && selectedCol == 0 && torchCount > 0) {
            drawTooltip(batch, font, "Torch", slots[0], viewport, shadow);
        } else if (selectedRow == 0 && selectedCol == 1 && player.getCardsCount() > 0) {
            drawTooltip(batch, font, "Card", slots[1], viewport, shadow);
        }

        font.getData().setScale(oldScaleX, oldScaleY);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void renderQuantity(SpriteBatch batch, BitmapFont font, String count, Rectangle slot, Viewport vp, float shadow) {
        font.getData().setScale(vp.getWorldWidth() / 1280f * 1.1f);
        glyphLayout.setText(font, count);
        float textX = slot.x + slot.width - glyphLayout.width - 4f;
        float textY = slot.y + slot.height - 4f; // Anchored to top-right

        // FIXED: Passing 'count' string instead of glyphLayout to preserve color state
        font.setColor(0f, 0f, 0f, 0.7f);
        font.draw(batch, count, textX + shadow, textY - shadow);
        font.setColor(Color.WHITE);
        font.draw(batch, count, textX, textY);
    }

    private void drawTooltip(SpriteBatch batch, BitmapFont font, String text, Rectangle slot, Viewport viewport, float shadow) {
        font.getData().setScale(viewport.getWorldWidth() / 1280f * 1.0f);
        glyphLayout.setText(font, text);

        float tooltipX = slot.x + (slot.width - glyphLayout.width) / 2f;
        float tooltipY = slot.y + glyphLayout.height + 6f; // Anchored to bottom center

        // FIXED: Passing 'text' string instead of glyphLayout to preserve color state
        font.setColor(0f, 0f, 0f, 0.72f);
        font.draw(batch, text, tooltipX + shadow, tooltipY - shadow);
        font.setColor(Color.WHITE);
        font.draw(batch, text, tooltipX, tooltipY);
    }

    public void dispose() {
        torchIcon.dispose();
    }
}
