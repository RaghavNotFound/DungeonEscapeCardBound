package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.Vector3;

public class SettingsOverlay {

    private static final int OPTION_WINDOW_800_600 = 0;
    private static final int OPTION_WINDOW_1280_720 = 1;
    private static final int OPTION_FULLSCREEN = 2;
    private static final int OPTION_BACK = 3;

    private static final String[] LABELS = {
        "800 x 600",
        "1280 x 720",
        "FULLSCREEN",
        "BACK (ESC)"
    };

    private static final int OPTION_COUNT = LABELS.length;

    private int selected = 0;

    private enum State { INACTIVE, TRANSITION_IN, ACTIVE, TRANSITION_OUT }
    private State state = State.INACTIVE;
    private float transitionTimer = 0f;
    private static final float TRANSITION_DURATION = 0.3f;

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

    public void show() {
        if (state == State.INACTIVE || state == State.TRANSITION_OUT) {
            state = State.TRANSITION_IN;
            transitionTimer = 0f;
            selected = 0;
        }
    }

    public void hide() {
        if (state == State.ACTIVE || state == State.TRANSITION_IN) {
            state = State.TRANSITION_OUT;
            transitionTimer = 0f;
        }
    }

    public boolean isOverlayVisible() {
        return state != State.INACTIVE;
    }

    public void handleInput(Viewport viewport) {
        if (state != State.ACTIVE && state != State.TRANSITION_IN) return;
        
        updateLayout(viewport);
        updatePointer(viewport);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            hide();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.W) ||
            Gdx.input.isKeyJustPressed(Input.Keys.UP)) {

            selected = (selected + OPTION_COUNT - 1) % OPTION_COUNT;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.S) ||
            Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {

            selected = (selected + 1) % OPTION_COUNT;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            applySelection();
            return;
        }

        if (Gdx.input.justTouched()) {
            int clicked = pointerIndex();
            if (clicked >= 0) {
                selected = clicked;
                applySelection();
            }
        }

        int hovered = pointerIndex();
        if (hovered >= 0) {
            selected = hovered;
        }
    }

    private void applySelection() {

        switch (selected) {

            case OPTION_WINDOW_800_600:
                Gdx.graphics.setWindowedMode(800, 600);
                break;

            case OPTION_WINDOW_1280_720:
                Gdx.graphics.setWindowedMode(1280, 720);
                break;

            case OPTION_FULLSCREEN:
                Graphics.DisplayMode mode = Gdx.graphics.getDisplayMode();
                Gdx.graphics.setFullscreenMode(mode);
                break;

            case OPTION_BACK:
                hide();
                break;
        }
    }

    private void updateLayout(Viewport viewport) {
        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();

        if (w == lastLayoutWidth && h == lastLayoutHeight) return;

        lastLayoutWidth = w;
        lastLayoutHeight = h;

        boxW = w * 0.25f;
        boxH = h * 0.08f;
        float gap = h * 0.03f;

        centerX = w * 0.5f - boxW * 0.5f;
        float baseY = h * 0.55f;

        for (int i = 0; i < OPTION_COUNT; i++) {
            ys[i] = baseY - i * (boxH + gap);
        }
    }

    public void update(float delta) {
        animTime += delta;

        if (state == State.TRANSITION_IN) {
            transitionTimer += delta;
            if (transitionTimer >= TRANSITION_DURATION) {
                transitionTimer = TRANSITION_DURATION;
                state = State.ACTIVE;
            }
        } else if (state == State.TRANSITION_OUT) {
            transitionTimer += delta;
            if (transitionTimer >= TRANSITION_DURATION) {
                transitionTimer = TRANSITION_DURATION;
                state = State.INACTIVE;
            }
        }
    }

    public float getTransitionProgress() {
        if (state == State.TRANSITION_IN) {
            return Interpolation.pow2Out.apply(transitionTimer / TRANSITION_DURATION);
        }
        if (state == State.TRANSITION_OUT) {
            return Interpolation.pow2Out.apply(1f - (transitionTimer / TRANSITION_DURATION));
        }
        return (state == State.ACTIVE) ? 1f : 0f;
    }

    public void render(ShapeRenderer shape, SpriteBatch batch,
                       BitmapFont font, Viewport viewport) {

        if (!isOverlayVisible()) return;

        updateLayout(viewport);
        float w = viewport.getWorldWidth();

        float progress = getTransitionProgress();
        float yOffset = (1 - progress) * viewport.getWorldHeight() * 0.3f;

        shape.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < OPTION_COUNT; i++) {
            if (selected == i) {
                float alpha = 0.22f + 0.08f * (0.5f + 0.5f * MathUtils.sin(animTime * 7f));
                shape.setColor(accent.r, accent.g, accent.b, alpha * progress);
            } else {
                shape.setColor(0f, 0f, 0f, 0.3f * progress);
            }
            shape.rect(centerX, ys[i] + yOffset, boxW, boxH);
        }

        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < OPTION_COUNT; i++) {
            Color c = (selected == i ? accent : inactiveOutline);
            shape.setColor(c.r, c.g, c.b, c.a * progress);
            shape.rect(centerX, ys[i] + yOffset, boxW, boxH);
        }
        shape.end();

        batch.begin();

        float scale = w / 800f; // ✅ fixed (only once)

        float oldScaleX = font.getData().scaleX;
        float oldScaleY = font.getData().scaleY;

        for (int i = 0; i < OPTION_COUNT; i++) {
            float pulse = selected == i ? 0.02f * MathUtils.sin(animTime * 7f) : 0f;
            float textScale = scale * 1.18f * (selected == i ? 1.05f + pulse : 1f);

            font.getData().setScale(textScale);
            glyphLayout.setText(font, LABELS[i]);

            float textX = centerX + (boxW - glyphLayout.width) * 0.5f;
            float textY = ys[i] + (boxH + glyphLayout.height) * 0.5f + yOffset;
            float shadow = Math.max(1.3f, w * 0.0012f);

            font.setColor(0f, 0f, 0f, 0.72f * progress);
            font.draw(batch, glyphLayout, textX + shadow, textY - shadow);

            font.setColor(1f, 1f, 1f, (selected == i ? 1f : 0.9f) * progress);
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
