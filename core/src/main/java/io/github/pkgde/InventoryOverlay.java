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
import java.util.ArrayList;

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

    public static void queueAssets(com.badlogic.gdx.assets.AssetManager manager) {
        manager.load("Objects/torch/torch_1.png", Texture.class);
    }

    public InventoryOverlay() {
        torchIcon = Main.assets.get("Objects/torch/torch_1.png", Texture.class);
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
        // FIXED: Reduced from 0.6 to 0.45 so the inventory actually fits inside the screen nicely
        float totalGridWidth = viewport.getWorldWidth() * 0.45f;
        float totalGridHeight = viewport.getWorldHeight() * 0.55f;
        float slotSize = Math.min(totalGridWidth / GRID_COLS, totalGridHeight / GRID_ROWS) * 0.9f;
        float gap = slotSize * 0.1f;

        float gridW = GRID_COLS * (slotSize + gap) - gap;
        float gridH = GRID_ROWS * (slotSize + gap) - gap;

        float startX = (viewport.getWorldWidth() - gridW) / 2f;
        float startY = (viewport.getWorldHeight() - gridH) / 2f - 60f; // Pushed down slightly

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

        int selectedIndex = selectedRow * GRID_COLS + selectedCol;
        Rectangle selectedSlot = slots[selectedIndex];
        shape.setColor(selectedColor);
        shape.rect(selectedSlot.x, selectedSlot.y, selectedSlot.width, selectedSlot.height);
        shape.end();

        ArrayList<String> inv = player.getInventoryOrder();
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < inv.size(); i++) {
            if (i >= SLOT_COUNT) break;
            if (inv.get(i).equals("Card")) {
                Rectangle slot = slots[i];
                float iconSize = slot.height * 0.45f;
                float cx = slot.x + slot.width / 2f;
                float cy = slot.y + slot.height / 2f;
                shape.setColor(0.8f, 0.9f, 1f, 1f);
                shape.rect(cx - iconSize * 0.35f, cy - iconSize * 0.45f, iconSize * 0.7f, iconSize * 0.9f);
                shape.setColor(0.9f, 0.7f, 0.2f, 1f);
                shape.rect(cx - iconSize * 0.2f, cy - iconSize * 0.3f, iconSize * 0.4f, iconSize * 0.6f);
                shape.setColor(0.2f, 0.6f, 1f, 1f);
                shape.rect(cx - iconSize * 0.1f, cy - iconSize * 0.1f, iconSize * 0.2f, iconSize * 0.2f);
            }
        }
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        float oldScaleX = font.getData().scaleX;
        float oldScaleY = font.getData().scaleY;
        float shadow = Math.max(1.3f, viewport.getWorldWidth() * 0.0012f);

        font.getData().setScale(viewport.getWorldWidth() / 800f * 1.5f);
        glyphLayout.setText(font, "INVENTORY");
        float titleX = (viewport.getWorldWidth() - glyphLayout.width) / 2f;
        // FIXED: Pushed title up slightly so it doesn't touch the top row of inventory slots
        float titleY = slots[0].y + slots[0].height + glyphLayout.height + 40f;

        font.setColor(0f, 0f, 0f, 0.72f);
        font.draw(batch, "INVENTORY", titleX + shadow, titleY - shadow);
        font.setColor(Color.WHITE);
        font.draw(batch, "INVENTORY", titleX, titleY);

        for (int i = 0; i < inv.size(); i++) {
            if (i >= SLOT_COUNT) break;
            String item = inv.get(i);
            Rectangle slot = slots[i];

            if (item.equals("Torch")) {
                float iconSize = slot.height * 0.45f;
                float iconX = slot.x + (slot.width - iconSize) / 2f;
                float iconY = slot.y + (slot.height - iconSize) / 2f;
                batch.draw(torchIcon, iconX, iconY, iconSize, iconSize);
                renderQuantity(batch, font, String.valueOf(player.getTorchCount()), slot, viewport, shadow);
            } else if (item.equals("Card")) {
                renderQuantity(batch, font, String.valueOf(player.getCardsCount()), slot, viewport, shadow);
            }

            if (i == selectedIndex) {
                drawTooltip(batch, font, item, slot, viewport, shadow);
            }
        }

        font.getData().setScale(oldScaleX, oldScaleY);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void renderQuantity(SpriteBatch batch, BitmapFont font, String count, Rectangle slot, Viewport vp, float shadow) {
        font.getData().setScale(vp.getWorldWidth() / 1280f * 1.1f);
        glyphLayout.setText(font, count);
        float textX = slot.x + slot.width - glyphLayout.width - 4f;
        float textY = slot.y + slot.height - 4f;

        font.setColor(0f, 0f, 0f, 0.7f);
        font.draw(batch, count, textX + shadow, textY - shadow);
        font.setColor(Color.WHITE);
        font.draw(batch, count, textX, textY);
    }

    private void drawTooltip(SpriteBatch batch, BitmapFont font, String text, Rectangle slot, Viewport viewport, float shadow) {
        font.getData().setScale(viewport.getWorldWidth() / 1280f * 1.0f);
        glyphLayout.setText(font, text);

        float tooltipX = slot.x + (slot.width - glyphLayout.width) / 2f;
        float tooltipY = slot.y + glyphLayout.height + 6f;

        font.setColor(0f, 0f, 0f, 0.72f);
        font.draw(batch, text, tooltipX + shadow, tooltipY - shadow);
        font.setColor(Color.WHITE);
        font.draw(batch, text, tooltipX, tooltipY);
    }

    public void dispose() {}
}
