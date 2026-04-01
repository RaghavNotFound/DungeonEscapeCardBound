package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

public class PauseOverlay {

    private static final String[] OPTIONS = {
        "RESUME",
        "SETTINGS",
        "EXIT"
    };

    private static final int OPTION_COUNT = OPTIONS.length;

    private int selected = 0;
    private final Vector3 touch = new Vector3();
    private final float[] ys = new float[OPTION_COUNT];
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final Color accent = new Color(0.25f, 0.85f, 1f, 1f);
    private final Color inactiveOutline = new Color(1f, 1f, 1f, 0.72f);

    private float animTime = 0f;

    private float lastLayoutWidth = -1f;
    private float lastLayoutHeight = -1f;
    private float boxW;
    private float boxH;
    private float centerX;
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    public enum Action {
        NONE,
        RESUME,
        SETTINGS,
        EXIT
    }

    public Action handleInput(Viewport viewport) {
        updateLayout(viewport);
        updatePointer(viewport);
        
        boolean mouseMovedThisFrame = (Gdx.input.getX() != lastMouseX || Gdx.input.getY() != lastMouseY);
        lastMouseX = Gdx.input.getX();
        lastMouseY = Gdx.input.getY();

        // --- Keyboard Navigation ---
        boolean keyPressed = false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selected = (selected + OPTION_COUNT - 1) % OPTION_COUNT;
            keyPressed = true;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selected = (selected + 1) % OPTION_COUNT;
            keyPressed = true;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            return toAction(selected);
        }

        // --- Mouse Click ---
        if (Gdx.input.justTouched()) {
            int clicked = pointerIndex();
            if (clicked >= 0) {
                selected = clicked;
                return toAction(clicked);
            }
        }

        // --- Mouse Hover (only if mouse moved and no keyboard input) ---
        if (mouseMovedThisFrame && !keyPressed) {
            int hovered = pointerIndex();
            if (hovered >= 0) {
                selected = hovered;
            }
        }

        return Action.NONE;
    }

    private Action toAction(int optionIndex) {
        switch (optionIndex) {
            case 0:
                return Action.RESUME;
            case 1:
                return Action.SETTINGS;
            case 2:
                return Action.EXIT;
            default:
                return Action.NONE;
        }
    }

    private void updateLayout(Viewport viewport) {
        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();

        if (w == lastLayoutWidth && h == lastLayoutHeight) {
            return;
        }

        lastLayoutWidth = w;
        lastLayoutHeight = h;

        boxW = w * 0.35f;
        boxH = h * 0.08f;
        float gap = h * 0.035f;

        centerX = w * 0.5f - boxW * 0.5f;
        float baseY = h * 0.55f;

        for (int i = 0; i < OPTION_COUNT; i++) {
            ys[i] = baseY - i * (boxH + gap);
        }
    }

    public void render(ShapeRenderer shape, SpriteBatch batch,
                       BitmapFont font, Viewport viewport) {
        render(shape, batch, font, viewport, 1f);
    }

    public void render(ShapeRenderer shape, SpriteBatch batch,
                       BitmapFont font, Viewport viewport, float alpha) {
        updateLayout(viewport);
        float w = viewport.getWorldWidth();
        animTime += Gdx.graphics.getDeltaTime();

        // ===== SHAPES =====
        shape.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < OPTION_COUNT; i++) {
            if (selected == i) {
                float pulseAlpha = 0.22f + 0.08f * (0.5f + 0.5f * MathUtils.sin(animTime * 7f));
                shape.setColor(accent.r, accent.g, accent.b, pulseAlpha * alpha);
            } else {
                shape.setColor(0f, 0f, 0f, 0.3f * alpha);
            }
            shape.rect(centerX, ys[i], boxW, boxH);
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);

        for (int i = 0; i < OPTION_COUNT; i++) {
            Color c = selected == i ? accent : inactiveOutline;
            shape.setColor(c.r, c.g, c.b, c.a * alpha);
            shape.rect(centerX, ys[i], boxW, boxH);
        }

        shape.end();

        // ===== TEXT =====
        batch.begin();

        float scale = w / 800f;
        float oldScaleX = font.getData().scaleX;
        float oldScaleY = font.getData().scaleY;

        for (int i = 0; i < OPTION_COUNT; i++) {

            float pulse = selected == i ? 0.02f * MathUtils.sin(animTime * 7f) : 0f;
            float s = (selected == i) ? 1.05f + pulse : 1f;

            font.getData().setScale(scale * 1.2f * s);
            glyphLayout.setText(font, OPTIONS[i]);

            float textX = centerX + (boxW - glyphLayout.width) * 0.5f;
            float textY = ys[i] + (boxH + glyphLayout.height) * 0.5f;
            float shadow = Math.max(1.3f, w * 0.0012f);

            font.setColor(0f, 0f, 0f, 0.72f * alpha);
            font.draw(batch, glyphLayout, textX + shadow, textY - shadow);
            font.setColor(1f, 1f, 1f, (selected == i ? 1f : 0.9f) * alpha);
            font.draw(batch, glyphLayout, textX, textY);
        }

        font.setColor(Color.WHITE);
        font.getData().setScale(oldScaleX, oldScaleY);

        batch.end();
    }

    private int pointerIndex() {
        for (int i = 0; i < OPTION_COUNT; i++) {
            if (touch.x >= centerX && touch.x <= centerX + boxW &&
                touch.y >= ys[i] && touch.y <= ys[i] + boxH) {
                return i;
            }
        }
        return -1;
    }

    private void updatePointer(Viewport viewport) {
        touch.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(touch);
    }
}
