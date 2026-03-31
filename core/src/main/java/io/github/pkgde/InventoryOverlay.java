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

public class InventoryOverlay
{

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
        // Use one of the existing torch animation frames as the icon.
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
        float totalGridWidth = viewport.getWorldWidth() * 0.5f;
        float totalGridHeight = viewport.getWorldHeight() * 0.6f;
        float slotSize = Math.min(totalGridWidth / GRID_COLS, totalGridHeight / GRID_ROWS) * 0.9f;
        float gap = slotSize * 0.1f;

        float gridW = GRID_COLS * (slotSize + gap) - gap;
        float gridH = GRID_ROWS * (slotSize + gap) - gap;

        float startX = (viewport.getWorldWidth() - gridW) / 2f;
        float startY = (viewport.getWorldHeight() - gridH) / 2f - 40f; // Shift down for title

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

        // Draw background panel
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0, 0, 0, 0.6f);
        Rectangle firstSlot = slots[0];
        Rectangle lastSlot = slots[SLOT_COUNT - 1];
        float panelX = firstSlot.x - 20;
        float panelY = lastSlot.y - 20;
        float panelW = (slots[GRID_COLS - 1].x + slots[GRID_COLS - 1].width) - firstSlot.x + 40;
        float panelH = (firstSlot.y + firstSlot.height) - lastSlot.y + 80; // Extra space for title
        shape.rect(panelX, panelY, panelW, panelH);
        shape.end();

        // Draw slots
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (Rectangle slot : slots) {
            shape.setColor(slotColor);
            shape.rect(slot.x, slot.y, slot.width, slot.height);
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        for (Rectangle slot : slots) {
            shape.setColor(slotOutlineColor);
            shape.rect(slot.x, slot.y, slot.width, slot.height);
        }
        // Draw selected slot
        int selectedIndex = selectedRow * GRID_COLS + selectedCol;
        Rectangle selectedSlot = slots[selectedIndex];
        shape.setColor(selectedColor);
        shape.rect(selectedSlot.x, selectedSlot.y, selectedSlot.width, selectedSlot.height);
        shape.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        float oldScaleX = font.getData().scaleX;
        float oldScaleY = font.getData().scaleY;
        float shadow = Math.max(1.3f, viewport.getWorldWidth() * 0.0012f);

        // Draw title
        font.getData().setScale(viewport.getWorldWidth() / 800f * 1.5f);
        glyphLayout.setText(font, "INVENTORY");
        float titleX = (viewport.getWorldWidth() - glyphLayout.width) / 2f;
        float titleY = slots[0].y + slots[0].height + glyphLayout.height + 20;

        font.setColor(0f, 0f, 0f, 0.72f);
        font.draw(batch, glyphLayout, titleX + shadow, titleY - shadow);
        font.setColor(Color.WHITE);
        font.draw(batch, glyphLayout, titleX, titleY);

        // Draw items (just torches for now)
        int torchCount = player.getTorchCount();
        if (torchCount > 0) {
            Rectangle slot = slots[0];
            float iconSize = slot.height * 0.8f;
            float iconX = slot.x + (slot.width - iconSize) / 2f;
            float iconY = slot.y + (slot.height - iconSize) / 2f;
            batch.draw(torchIcon, iconX, iconY, iconSize, iconSize);

            // Draw quantity
            font.getData().setScale(viewport.getWorldWidth() / 1280f * 1.1f);
            String quantityStr = String.valueOf(torchCount);
            glyphLayout.setText(font, quantityStr);
            float textX = slot.x + slot.width - glyphLayout.width - slot.width * 0.1f;
            float textY = slot.y + glyphLayout.height + slot.height * 0.1f;

            font.setColor(0f, 0f, 0f, 0.7f);
            font.draw(batch, glyphLayout, textX + shadow, textY - shadow);
            font.setColor(Color.WHITE);
            font.draw(batch, glyphLayout, textX, textY);
        }

        // Draw tooltip for selected item
        if (selectedRow == 0 && selectedCol == 0 && player.getTorchCount() > 0) {
            String tooltipText = "Torch";
            font.getData().setScale(viewport.getWorldWidth() / 1280f * 1.0f);
            glyphLayout.setText(font, tooltipText);

            float tooltipX = selectedSlot.x + (selectedSlot.width - glyphLayout.width) / 2;
            float tooltipY = selectedSlot.y - 15;

            font.setColor(0f, 0f, 0f, 0.72f);
            font.draw(batch, glyphLayout, tooltipX + shadow, tooltipY - shadow);
            font.setColor(Color.WHITE);
            font.draw(batch, glyphLayout, tooltipX, tooltipY);
        }

        font.getData().setScale(oldScaleX, oldScaleY);
        font.setColor(Color.WHITE);
        batch.end();
    }

    public void dispose() {
        torchIcon.dispose();
    }
}
