package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;

public class SaveLoadOverlay {
    public enum Mode { LOAD, SAVE }
    public enum ResultAction { NONE, LOAD, SAVE }

    public static class Result {
        public ResultAction action;
        public String saveName;
        public Result(ResultAction action, String saveName) {
            this.action = action;
            this.saveName = saveName;
        }
    }

    private Mode mode;
    private Array<String> saves = new Array<>();
    private int selected = 0;
    private boolean visible = false;

    private boolean subMenuOpen = false;
    private int subSelected = 0;
    private final Array<String> subOptions = new Array<>();
    private String activeSaveName;

    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final Color accent = new Color(0.25f, 0.85f, 1f, 1f);
    private final Vector3 touch = new Vector3();
    private int lastMouseX = -1, lastMouseY = -1;

    private boolean isRenaming = false;
    private String renameText = "";
    private InputProcessor previousProcessor;

    public void show(Mode mode) {
        this.mode = mode;
        this.visible = true;
        this.subMenuOpen = false;
        refreshList();
    }

    public void hide() { visible = false; }
    public boolean isVisible() { return visible; }

    private void refreshList() {
        saves = SaveManager.getAllSaves();
        if (mode == Mode.SAVE) {
            saves.insert(0, "--- NEW SAVE ---");
        }
        selected = 0;
        subMenuOpen = false;
    }

    public Result handleInput(Viewport viewport) {
        if (!visible) return null;
        if (isRenaming) return null; // Input handled by InputProcessor

        boolean mouseMoved = (Gdx.input.getX() != lastMouseX || Gdx.input.getY() != lastMouseY);
        lastMouseX = Gdx.input.getX();
        lastMouseY = Gdx.input.getY();
        boolean keyPressed = false;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (subMenuOpen) subMenuOpen = false;
            else hide();
            return null;
        }

        if (subMenuOpen) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                subSelected = (subSelected + subOptions.size - 1) % subOptions.size;
                keyPressed = true;
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                subSelected = (subSelected + 1) % subOptions.size;
                keyPressed = true;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) return executeSubOption(subSelected);

            if (Gdx.input.justTouched()) {
                int clicked = getSubPointerSelection(viewport);
                if (clicked != -1) return executeSubOption(clicked);
                else subMenuOpen = false; // Click outside cancels
            }

            if (mouseMoved && !keyPressed) {
                int hovered = getSubPointerSelection(viewport);
                if (hovered != -1) subSelected = hovered;
            }
        } else {
            if (saves.isEmpty()) return null;

            if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                selected = (selected + saves.size - 1) % saves.size;
                keyPressed = true;
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                selected = (selected + 1) % saves.size;
                keyPressed = true;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) return executeMainOption(selected);

            if (Gdx.input.justTouched()) {
                int clicked = getMainPointerSelection(viewport);
                if (clicked != -1) return executeMainOption(clicked);
            }

            if (mouseMoved && !keyPressed) {
                int hovered = getMainPointerSelection(viewport);
                if (hovered != -1) selected = hovered;
            }
        }
        return null;
    }

    private Result executeMainOption(int index) {
        selected = index;
        if (mode == Mode.SAVE && index == 0) {
            hide();
            return new Result(ResultAction.SAVE, SaveManager.generateNewSaveName());
        }
        activeSaveName = saves.get(index);
        subMenuOpen = true;
        subOptions.clear();

        if (activeSaveName.equals("auto_save")) {
            // Limited options for auto_save
            subOptions.add("LOAD");
            subOptions.add("DELETE");
            subOptions.add("CANCEL");
        } else {
            // Full options for regular saves
            if (mode == Mode.SAVE) subOptions.add("OVERWRITE");
            subOptions.add("LOAD");
            subOptions.add("RENAME");
            subOptions.add("DELETE");
            subOptions.add("CANCEL");
        }

        subSelected = 0;
        return null;
    }

    private Result executeSubOption(int index) {
        String opt = subOptions.get(index);
        if (opt.equals("LOAD")) {
            hide();
            return new Result(ResultAction.LOAD, activeSaveName);
        } else if (opt.equals("OVERWRITE")) {
            hide();
            return new Result(ResultAction.SAVE, activeSaveName);
        } else if (opt.equals("RENAME")) {
            isRenaming = true;
            renameText = activeSaveName;
            subMenuOpen = false;
            previousProcessor = Gdx.input.getInputProcessor();
            Gdx.input.setInputProcessor(new InputAdapter() {
                @Override
                public boolean keyTyped(char character) {
                    if (character == '\b' && renameText.length() > 0) {
                        renameText = renameText.substring(0, renameText.length() - 1);
                    } else if (character == '\r' || character == '\n') {
                        finishRenaming(true);
                    } else if (character == 27) { // ESCAPE
                        finishRenaming(false);
                    } else if (character >= 32 && character <= 126 && renameText.length() < 24) {
                        renameText += character;
                    }
                    return true;
                }
            });
        } else if (opt.equals("DELETE")) {
            SaveManager.deleteSave(activeSaveName);
            refreshList();
        } else if (opt.equals("CANCEL")) {
            subMenuOpen = false;
        }
        return null;
    }

    private void finishRenaming(boolean apply) {
        Gdx.app.postRunnable(() -> {
            isRenaming = false;
            Gdx.input.setInputProcessor(previousProcessor);
            if (apply) {
                String newName = renameText.trim();
                if (!newName.isEmpty() && !newName.equals(activeSaveName)) {
                    SaveManager.renameSave(activeSaveName, newName);
                    refreshList();
                }
            }
        });
    }

    public void render(ShapeRenderer shape, SpriteBatch batch, BitmapFont font, Viewport viewport) {
        if (!visible) return;
        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();

        float boxW = w * 0.4f, boxH = h * 0.08f, gap = h * 0.02f;
        float centerX = (w - boxW) / 2f;
        float startY = h * 0.85f;

        shape.begin(ShapeRenderer.ShapeType.Filled);

        if (!subMenuOpen) {
            shape.setColor(0, 0, 0, 0.85f);
            float totalHeight = (saves.size * (boxH + gap)) - gap;
            shape.rect(centerX - 20, startY - totalHeight - 20, boxW + 40, totalHeight + 40);

            for (int i = 0; i < saves.size; i++) {
                float rectY = startY - i * (boxH + gap);
                shape.setColor(selected == i ? accent : new Color(0.2f, 0.2f, 0.2f, 1f));
                if (mode == Mode.SAVE && i == 0) shape.setColor(selected == i ? accent : new Color(0.2f, 0.6f, 0.2f, 1f));
                shape.rect(centerX, rectY, boxW, boxH);
            }
        } else {
            shape.setColor(0, 0, 0, 0.6f);
            shape.rect(0, 0, w, h);
            float subBoxW = w * 0.25f, subBoxH = h * 0.07f, subGap = h * 0.015f;
            float subCenterX = (w - subBoxW) / 2f;
            float subStartY = h * 0.5f + (subOptions.size * (subBoxH + subGap)) / 2f;

            for (int i = 0; i < subOptions.size; i++) {
                shape.setColor(subSelected == i ? accent : new Color(0.3f, 0.3f, 0.3f, 1f));
                // Corrected the Y coordinate for drawing the background rectangle
                shape.rect(subCenterX, subStartY - i * (subBoxH + subGap), subBoxW, subBoxH);
            }
        }
        shape.end();

        batch.begin();
        float fontScale = w / 800f;

        if (!subMenuOpen) {
            for (int i = 0; i < saves.size; i++) {
                font.getData().setScale(fontScale * 1.1f);
                glyphLayout.setText(font, saves.get(i));
                font.setColor(Color.WHITE);
                if (mode == Mode.SAVE && i == 0) font.setColor(Color.LIME);
                float rectY = startY - i * (boxH + gap);
                font.draw(batch, glyphLayout, centerX + (boxW - glyphLayout.width) / 2f, rectY + boxH / 2f + glyphLayout.height / 2f);
            }
        } else {
            float subBoxW = w * 0.25f, subBoxH = h * 0.07f, subGap = h * 0.015f;
            float subCenterX = (w - subBoxW) / 2f;
            float subStartY = h * 0.5f + (subOptions.size * (subBoxH + subGap)) / 2f;

            for (int i = 0; i < subOptions.size; i++) {
                font.getData().setScale(fontScale * 1f);
                glyphLayout.setText(font, subOptions.get(i));
                font.setColor(Color.WHITE);
                if (subOptions.get(i).equals("DELETE")) font.setColor(1f, 0.3f, 0.3f, 1f);
                font.draw(batch, glyphLayout, subCenterX + (subBoxW - glyphLayout.width) / 2f, subStartY - i * (subBoxH + subGap) + subBoxH / 2f + glyphLayout.height / 2f);
            }
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        if (isRenaming) {
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(0, 0, 0, 0.85f);
            shape.rect(0, 0, w, h);

            float inputW = w * 0.5f, inputH = h * 0.12f;
            float inputX = (w - inputW) / 2f, inputY = (h - inputH) / 2f;

            shape.setColor(0.1f, 0.1f, 0.1f, 1f);
            shape.rect(inputX, inputY, inputW, inputH);
            shape.end();

            shape.begin(ShapeRenderer.ShapeType.Line);
            shape.setColor(accent);
            shape.rect(inputX, inputY, inputW, inputH);
            shape.end();

            batch.begin();

            font.getData().setScale(fontScale * 1.1f);
            glyphLayout.setText(font, "ENTER NEW NAME (Press ENTER to Save, ESC to Cancel)");
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, glyphLayout, inputX, inputY + inputH + 20f);

            font.getData().setScale(fontScale * 1.5f);
            boolean cursorVisible = (System.currentTimeMillis() / 500) % 2 == 0;
            glyphLayout.setText(font, renameText + (cursorVisible ? "|" : ""));
            font.setColor(Color.WHITE);
            font.draw(batch, glyphLayout, inputX + 20f, inputY + inputH / 2f + glyphLayout.height / 2f);

            font.getData().setScale(1f);
            batch.end();
        }
    }

    private int getMainPointerSelection(Viewport viewport) {
        touch.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(touch);
        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();
        float boxW = w * 0.4f, boxH = h * 0.08f, gap = h * 0.02f;
        float centerX = (w - boxW) / 2f;
        float startY = h * 0.85f;
        for (int i = 0; i < saves.size; i++) {
            float rectY = startY - i * (boxH + gap);
            if (touch.x >= centerX && touch.x <= centerX + boxW && touch.y >= rectY && touch.y <= rectY + boxH) return i;
        }
        return -1;
    }

    private int getSubPointerSelection(Viewport viewport) {
        touch.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(touch);
        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();
        float subBoxW = w * 0.25f, subBoxH = h * 0.07f, subGap = h * 0.015f;
        float subCenterX = (w - subBoxW) / 2f;
        // Matches the corrected Y coordinate logic in render()
        float subStartY = h * 0.5f + (subOptions.size * (subBoxH + subGap)) / 2f;
        for (int i = 0; i < subOptions.size; i++) {
            float rectY = subStartY - i * (subBoxH + subGap);
            if (touch.x >= subCenterX && touch.x <= subCenterX + subBoxW && touch.y >= rectY && touch.y <= rectY + subBoxH) return i;
        }
        return -1;
    }
}
