package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.Vector3;

public class SettingsOverlay {

    private boolean active = false;
    private int selected = 0;

    public void show() {
        active = true;
        selected = 0;
    }

    public void hide() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public void handleInput(Viewport viewport) {

        if (!active) return;

        // 🔥 ESC BACK (instant + clean)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            hide();
            return;
        }

        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();

        float boxW = w * 0.25f;
        float boxH = h * 0.08f;
        float gap = h * 0.03f;

        float centerX = w / 2f - boxW / 2f;
        float baseY = h * 0.55f;

        float[] ys = {
            baseY,
            baseY - (boxH + gap),
            baseY - 2 * (boxH + gap),
            baseY - 3 * (boxH + gap)
        };

        // ===== KEYBOARD NAV =====
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) ||
            Gdx.input.isKeyJustPressed(Input.Keys.UP)) {

            selected = (selected + 3) % 4;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.S) ||
            Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {

            selected = (selected + 1) % 4;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            applySelection();
            return;
        }

        // ===== MOUSE =====
        if (Gdx.input.justTouched()) {

            Vector3 touch = viewport.unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

            for (int i = 0; i < 4; i++) {
                if (touch.x >= centerX && touch.x <= centerX + boxW &&
                    touch.y >= ys[i] && touch.y <= ys[i] + boxH) {

                    selected = i;
                    applySelection();
                    break;
                }
            }
        }
    }

    private void applySelection() {

        switch (selected) {

            case 0:
                Gdx.graphics.setWindowedMode(800, 600);
                break;

            case 1:
                Gdx.graphics.setWindowedMode(1280, 720);
                break;

            case 2:
                var mode = Gdx.graphics.getDisplayMode();
                Gdx.graphics.setWindowedMode(mode.width, mode.height);
                break;

            case 3:
                hide();
                break;
        }
    }

    public void render(ShapeRenderer shape, SpriteBatch batch,
                       BitmapFont font, Viewport viewport) {

        if (!active) return;

        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();

        float boxW = w * 0.25f;
        float boxH = h * 0.08f;
        float gap = h * 0.03f;

        float centerX = w / 2f - boxW / 2f;
        float baseY = h * 0.55f;

        float[] ys = {
            baseY,
            baseY - (boxH + gap),
            baseY - 2 * (boxH + gap),
            baseY - 3 * (boxH + gap)
        };

        String[] labels = {
            "800 x 600",
            "1280 x 720",
            "FULLSCREEN",
            "BACK (ESC)"
        };

        // ===== SHAPES =====
        shape.begin(ShapeRenderer.ShapeType.Line);

        for (int i = 0; i < 4; i++) {
            shape.setColor(selected == i ? 1 : 0.5f, 1, 1, 1);
            shape.rect(centerX, ys[i], boxW, boxH);
        }

        shape.end();

        // ===== TEXT =====
        batch.begin();

        float scale = w / 800f;
        font.getData().setScale(scale * 1.2f);

        for (int i = 0; i < 4; i++) {
            font.draw(
                batch,
                labels[i],
                centerX + boxW * 0.25f,
                ys[i] + boxH * 0.65f
            );
        }

        batch.end();
    }

    public boolean wasClosed() {
        return !active;
    }
}
