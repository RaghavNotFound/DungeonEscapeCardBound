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

    private float width = 250, height = 70;

    private float res1X = 275, res1Y = 300;
    private float res2X = 275, res2Y = 220;
    private float fullX = 275, fullY = 140;
    private float backX = 275, backY = 60;

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

        if (!active) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            active = false;
        }

        if (Gdx.input.justTouched()) {

            Vector3 touch = viewport.unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

            float x = touch.x;
            float y = touch.y;

            if (x >= res1X && x <= res1X + width &&
                y >= res1Y && y <= res1Y + height) {

                Gdx.graphics.setWindowedMode(800, 600);
            }

            if (x >= res2X && x <= res2X + width &&
                y >= res2Y && y <= res2Y + height) {

                Gdx.graphics.setWindowedMode(1280, 720);
            }

            if (x >= fullX && x <= fullX + width &&
                y >= fullY && y <= fullY + height) {

                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            }

            if (x >= backX && x <= backX + width &&
                y >= backY && y <= backY + height) {

                active = false;
            }
        }
    }

    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font) {

        if (!active) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(0, 0, 0, 0.7f);
        shapeRenderer.rect(0, 0, 800, 600);

        shapeRenderer.setColor(1, 1, 1, 1);

        shapeRenderer.rect(res1X, res1Y, width, height);
        shapeRenderer.rect(res2X, res2Y, width, height);
        shapeRenderer.rect(fullX, fullY, width, height);
        shapeRenderer.rect(backX, backY, width, height);

        shapeRenderer.end();

        batch.begin();

        font.draw(batch, "800x600", res1X + 70, res1Y + 45);
        font.draw(batch, "1280x720", res2X + 60, res2Y + 45);
        font.draw(batch, "FULLSCREEN", fullX + 50, fullY + 45);
        font.draw(batch, "BACK (ESC)", backX + 50, backY + 45);

        batch.end();
    }
}
