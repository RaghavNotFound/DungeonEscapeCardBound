package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.Viewport;

public class PauseOverlay {

    private int selected = 0;

    private String[] options = {
        "RESUME",
        "SETTINGS",
        "EXIT"
    };

    public enum Action {
        NONE,
        RESUME,
        SETTINGS,
        EXIT
    }

    public Action handleInput() {

        if (Gdx.input.isKeyJustPressed(Input.Keys.W) ||
            Gdx.input.isKeyJustPressed(Input.Keys.UP)) {

            selected = (selected + options.length - 1) % options.length;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.S) ||
            Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {

            selected = (selected + 1) % options.length;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {

            switch (selected) {
                case 0: return Action.RESUME;
                case 1: return Action.SETTINGS;
                case 2: return Action.EXIT;
            }
        }

        return Action.NONE;
    }

    public void render(ShapeRenderer shape, SpriteBatch batch,
                       BitmapFont font, Viewport viewport) {

        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();

        float boxW = w * 0.35f;
        float boxH = h * 0.08f;
        float gap = h * 0.035f;

        float centerX = w / 2f - boxW / 2f;
        float baseY = h * 0.55f;

        float[] ys = {
            baseY,
            baseY - (boxH + gap),
            baseY - 2 * (boxH + gap)
        };

        // ===== SHAPES =====
        shape.begin(ShapeRenderer.ShapeType.Line);

        for (int i = 0; i < options.length; i++) {
            shape.setColor(selected == i ? 1 : 0.6f, 1, 1, 1);
            shape.rect(centerX, ys[i], boxW, boxH);
        }

        shape.end();

        // ===== TEXT =====
        batch.begin();

        float scale = w / 800f;

        for (int i = 0; i < options.length; i++) {

            float lift = (selected == i) ? 8f : 0f;
            float s = (selected == i) ? 1.05f : 1f;

            font.getData().setScale(scale * 1.2f * s);

            font.draw(
                batch,
                options[i],
                centerX + boxW * 0.3f,
                ys[i] + boxH * 0.65f + lift
            );
        }

        batch.end();
    }
}
