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

    private final Color bgColor = new Color(0.02f, 0.04f, 0.06f, 0.88f);
    private final Color slotColor = new Color(0.08f, 0.12f, 0.16f, 0.9f);
    private final Color slotOutlineColor = new Color(0.2f, 0.3f, 0.4f, 1f);
    private final Color selectedColor = new Color(0.25f, 0.85f, 1f, 1f);
    private final Color titleColor = new Color(0.25f, 0.85f, 1f, 1f);

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
        float slotSize = Math.min(viewport.getWorldWidth() * 0.08f, viewport.getWorldHeight() * 0.12f);
        float gap = slotSize * 0.15f;

        float gridW = GRID_COLS * (slotSize + gap) - gap;
        float gridH = GRID_ROWS * (slotSize + gap) - gap;

        float startX = (viewport.getWorldWidth() - gridW) / 2f;
        float startY = (viewport.getWorldHeight() - gridH) / 2f - 20f;

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

        // Calculate panel dimensions
        Rectangle firstSlot = slots[0];
        float panelW = (slots[GRID_COLS - 1].x + slots[GRID_COLS - 1].width) - firstSlot.x + 80;
        float panelH = (firstSlot.y + firstSlot.height) - slots[SLOT_COUNT-1].y + 180;
        float panelX = (viewport.getWorldWidth() - panelW) / 2f;
        float panelY = (viewport.getWorldHeight() - panelH) / 2f;

        // 1. Draw Background Panel
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(bgColor);
        shape.rect(panelX, panelY, panelW, panelH);
        
        // Header highlight
        shape.setColor(0.1f, 0.2f, 0.3f, 0.5f);
        shape.rect(panelX, panelY + panelH - 70, panelW, 70);

        // 2. Draw Slots
        for (Rectangle slot : slots) {
            shape.setColor(slotColor);
            shape.rect(slot.x, slot.y, slot.width, slot.height);
        }
        shape.end();

        // 3. Draw Borders
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.4f, 0.5f, 0.6f, 0.3f);
        shape.rect(panelX, panelY, panelW, panelH); // Outer panel border
        
        for (Rectangle slot : slots) {
            shape.setColor(slotOutlineColor);
            shape.rect(slot.x, slot.y, slot.width, slot.height);
        }

        // Selection glow
        int selectedIndex = selectedRow * GRID_COLS + selectedCol;
        Rectangle sel = slots[selectedIndex];
        shape.setColor(selectedColor);
        shape.rect(sel.x - 2, sel.y - 2, sel.width + 4, sel.height + 4);
        shape.rect(sel.x - 1, sel.y - 1, sel.width + 2, sel.height + 2);
        shape.end();

        // 4. Draw Items (Simple shapes)
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
            }
        }
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 5. Draw Text and Textures
        batch.begin();
        float oldScaleX = font.getData().scaleX;
        float oldScaleY = font.getData().scaleY;

        // Title
        font.getData().setScale(viewport.getWorldWidth() / 1280f * 1.8f);
        font.setColor(titleColor);
        glyphLayout.setText(font, "INVENTORY");
        font.draw(batch, "INVENTORY", (viewport.getWorldWidth() - glyphLayout.width) / 2f, panelY + panelH - 25);

        // Description Area at Bottom
        font.getData().setScale(viewport.getWorldWidth() / 1280f * 1.1f);
        String selectedItem = (selectedIndex < inv.size()) ? inv.get(selectedIndex) : "Empty Slot";
        font.setColor(Color.LIGHT_GRAY);
        glyphLayout.setText(font, selectedItem);
        font.draw(batch, selectedItem, (viewport.getWorldWidth() - glyphLayout.width) / 2f, panelY + 45);

        // Render Torch Icon and quantities
        for (int i = 0; i < inv.size(); i++) {
            if (i >= SLOT_COUNT) break;
            String item = inv.get(i);
            Rectangle slot = slots[i];

            if (item.equals("Torch")) {
                float iconSize = slot.height * 0.5f;
                float iconX = slot.x + (slot.width - iconSize) / 2f;
                float iconY = slot.y + (slot.height - iconSize) / 2f;
                batch.draw(torchIcon, iconX, iconY, iconSize, iconSize);
                renderQuantity(batch, font, String.valueOf(player.getTorchCount()), slot);
            } else if (item.equals("Card")) {
                renderQuantity(batch, font, String.valueOf(player.getCardsCount()), slot);
            }
        }

        font.getData().setScale(oldScaleX, oldScaleY);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void renderQuantity(SpriteBatch batch, BitmapFont font, String count, Rectangle slot) {
        float oldScaleX = font.getData().scaleX;
        float oldScaleY = font.getData().scaleY;
        font.getData().setScale(oldScaleX * 0.8f);
        font.setColor(Color.WHITE);
        font.draw(batch, count, slot.x + slot.width - 20, slot.y + 20);
        font.getData().setScale(oldScaleX, oldScaleY);
    }

    public void dispose() {}
}
