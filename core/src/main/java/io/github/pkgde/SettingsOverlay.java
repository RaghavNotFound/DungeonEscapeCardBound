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

    public void show() {
        active = true;
    }

    public void hide() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public void handleInput(Viewport viewport) {

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            hide();
            return;
        }

        if (Gdx.input.justTouched()) {

            Vector3 touch = viewport.unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

            float x = touch.x;
            float y = touch.y;

            float w = viewport.getWorldWidth();
            float h = viewport.getWorldHeight();

            float boxW = w * 0.25f;
            float boxH = h * 0.08f;

            float centerX = w / 2f - boxW / 2f;
            float baseY = h * 0.55f;
            float gap = h * 0.03f;

            float y1 = baseY;
            float y2 = y1 - boxH - gap;
            float y3 = y2 - boxH - gap;
            float y4 = y3 - boxH - gap;

            // 800x600
            if (x >= centerX && x <= centerX + boxW &&
                y >= y1 && y <= y1 + boxH) {

                Gdx.graphics.setWindowedMode(800, 600);
            }

            // 1280x720
            if (x >= centerX && x <= centerX + boxW &&
                y >= y2 && y <= y2 + boxH) {

                Gdx.graphics.setWindowedMode(1280, 720);
            }

            // Fullscreen
            if (x >= centerX && x <= centerX + boxW &&
                y >= y3 && y <= y3 + boxH) {

                var mode = Gdx.graphics.getDisplayMode();
                Gdx.graphics.setWindowedMode(mode.width, mode.height-58);            }

            // Back
            if (x >= centerX && x <= centerX + boxW &&
                y >= y4 && y <= y4 + boxH) {

                hide();
            }
        }
    }

    public void render(ShapeRenderer shape, SpriteBatch batch, BitmapFont font, Viewport viewport) {

        if (!active) return;

        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();

        float boxW = w * 0.25f;
        float boxH = h * 0.08f;

        float centerX = w / 2f - boxW / 2f;
        float baseY = h * 0.55f;
        float gap = h * 0.03f;

        float y1 = baseY;
        float y2 = y1 - boxH - gap;
        float y3 = y2 - boxH - gap;
        float y4 = y3 - boxH - gap;

        // ===== BUTTON BOXES =====
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(1, 1, 1, 1);

        shape.rect(centerX, y1, boxW, boxH);
        shape.rect(centerX, y2, boxW, boxH);
        shape.rect(centerX, y3, boxW, boxH);
        shape.rect(centerX, y4, boxW, boxH);

        shape.end();

        // ===== TEXT =====
        batch.begin();

        float scale = w / 800f;
        font.getData().setScale(scale * 1.2f);

        font.draw(batch, "800x600", centerX + boxW * 0.30f, y1 + boxH * 0.65f);
        font.draw(batch, "1280x720", centerX + boxW * 0.25f, y2 + boxH * 0.65f);
        font.draw(batch, "FULLSCREEN", centerX + boxW * 0.20f, y3 + boxH * 0.65f);
        font.draw(batch, "BACK (ESC)", centerX + boxW * 0.25f, y4 + boxH * 0.65f);

        batch.end();
    }
}
