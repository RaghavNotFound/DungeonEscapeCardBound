package io.github.pkgde;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * A full-screen victory celebration shown after the player escapes or defeats all enemies.
 * Displays animated title, game stats, golden particle effects, and menu navigation.
 */
public class VictoryScreen implements Screen {

    // ===== OPTIONS =====
    private static final String[] OPTIONS = {"CONTINUE", "RETRY"};
    private static final int OPTION_COUNT = OPTIONS.length;

    // ===== STATS =====
    private final int enemiesDefeated;
    private final int torchesCollected;
    private final float timeSurvived;

    // ===== RENDERING =====
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final SpriteBatch batch;
    private final ShapeRenderer shape;
    private final BitmapFont font;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    // ===== PARTICLES =====
    private static final int PARTICLE_COUNT = 60;
    private final float[] pX = new float[PARTICLE_COUNT];
    private final float[] pY = new float[PARTICLE_COUNT];
    private final float[] pVx = new float[PARTICLE_COUNT];
    private final float[] pVy = new float[PARTICLE_COUNT];
    private final float[] pLife = new float[PARTICLE_COUNT];
    private final float[] pMaxLife = new float[PARTICLE_COUNT];
    private final float[] pSize = new float[PARTICLE_COUNT];

    // ===== UI STATE =====
    private int selected = 0;
    private float animTime = 0f;
    private float appearTimer = 0f;

    private final float[] optionYs = new float[OPTION_COUNT];
    private float boxW, boxH, centerX;

    private final Vector3 touch = new Vector3();
    private int lastMouseX = -1, lastMouseY = -1;

    private final Color goldAccent = new Color(0.95f, 0.78f, 0.15f, 1f);
    private final Color inactiveOutline = new Color(1f, 1f, 1f, 0.72f);

    public VictoryScreen(int enemiesDefeated, int torchesCollected, float timeSurvived) {
        this.enemiesDefeated = enemiesDefeated;
        this.torchesCollected = torchesCollected;
        this.timeSurvived = timeSurvived;

        camera = new OrthographicCamera();
        float worldW = 1280f;
        float worldH = 720f;
        camera.setToOrtho(false, worldW, worldH);
        viewport = new FitViewport(worldW, worldH, camera);

        batch = new SpriteBatch();
        shape = new ShapeRenderer();
        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // Initialize particles
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            respawnParticle(i, true);
        }
    }

    private void respawnParticle(int i, boolean randomizeLife) {
        float worldW = viewport.getWorldWidth();
        float worldH = viewport.getWorldHeight();
        pX[i] = MathUtils.random(0f, worldW);
        pY[i] = MathUtils.random(-20f, worldH * 0.1f);
        pVx[i] = MathUtils.random(-15f, 15f);
        pVy[i] = MathUtils.random(30f, 90f);
        pMaxLife[i] = MathUtils.random(2f, 5f);
        pLife[i] = randomizeLife ? MathUtils.random(0f, pMaxLife[i]) : 0f;
        pSize[i] = MathUtils.random(2f, 6f);
    }

    @Override
    public void render(float delta) {
        animTime += delta;
        appearTimer = Math.min(appearTimer + delta, 1.5f);
        float appear = MathUtils.clamp(appearTimer / 1.0f, 0f, 1f);

        Gdx.gl.glClearColor(0.04f, 0.03f, 0.02f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();

        float worldW = viewport.getWorldWidth();
        float worldH = viewport.getWorldHeight();

        // ===== INPUT =====
        boolean mouseMovedThisFrame = (Gdx.input.getX() != lastMouseX || Gdx.input.getY() != lastMouseY);
        lastMouseX = Gdx.input.getX();
        lastMouseY = Gdx.input.getY();

        boolean keyPressed = false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selected = (selected + OPTION_COUNT - 1) % OPTION_COUNT;
            keyPressed = true;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selected = (selected + 1) % OPTION_COUNT;
            keyPressed = true;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            executeOption(selected);
            return;
        }

        touch.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(touch);

        if (Gdx.input.justTouched()) {
            int clicked = pointerIndex();
            if (clicked >= 0) {
                selected = clicked;
                executeOption(clicked);
                return;
            }
        }

        if (mouseMovedThisFrame && !keyPressed) {
            int hovered = pointerIndex();
            if (hovered >= 0) {
                selected = hovered;
            }
        }

        // ===== UPDATE PARTICLES =====
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            pLife[i] += delta;
            pX[i] += pVx[i] * delta;
            pY[i] += pVy[i] * delta;
            pVx[i] += MathUtils.random(-5f, 5f) * delta;
            if (pLife[i] >= pMaxLife[i]) {
                respawnParticle(i, false);
            }
        }

        updateLayout();

        // ===== DRAW BACKGROUND GRADIENT =====
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);

        shape.setColor(0.08f, 0.06f, 0.02f, 1f);
        shape.rect(0, 0, worldW, worldH);

        float glowAlpha = 0.12f + 0.04f * MathUtils.sin(animTime * 2f);
        shape.setColor(0.9f, 0.7f, 0.1f, glowAlpha * appear);
        shape.circle(worldW / 2f, worldH * 0.6f, 300f);
        shape.end();

        // ===== DRAW PARTICLES =====
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float lifeRatio = pLife[i] / pMaxLife[i];
            float alpha = (1f - lifeRatio) * 0.7f * appear;
            float hue = 0.12f + MathUtils.sin(animTime + i) * 0.04f;
            shape.setColor(new Color(0.95f, 0.75f + hue, 0.1f, alpha));
            shape.circle(pX[i], pY[i], pSize[i] * (1f - lifeRatio * 0.5f));
        }
        shape.end();

        // ===== DRAW OPTION BOXES =====
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < OPTION_COUNT; i++) {
            if (selected == i) {
                float pulseAlpha = 0.22f + 0.08f * (0.5f + 0.5f * MathUtils.sin(animTime * 7f));
                shape.setColor(goldAccent.r, goldAccent.g, goldAccent.b, pulseAlpha * appear);
            } else {
                shape.setColor(0f, 0f, 0f, 0.3f * appear);
            }
            shape.rect(centerX, optionYs[i], boxW, boxH);
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < OPTION_COUNT; i++) {
            Color c = selected == i ? goldAccent : inactiveOutline;
            shape.setColor(c.r, c.g, c.b, c.a * appear);
            shape.rect(centerX, optionYs[i], boxW, boxH);
        }
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ===== DRAW TEXT =====
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        float scale = worldW / 800f;
        float shadow = Math.max(1.5f, worldW * 0.0015f);

        // --- Title ---
        float titleScale = 2.8f + 0.15f * MathUtils.sin(animTime * 3f);
        font.getData().setScale(scale * titleScale);
        glyphLayout.setText(font, "VICTORY ACHIEVED");
        float titleX = (worldW - glyphLayout.width) / 2f;
        float titleY = worldH * 0.82f;

        font.setColor(0.4f, 0.3f, 0f, 0.8f * appear);
        font.draw(batch, glyphLayout, titleX + shadow * 2, titleY - shadow * 2);

        float titleGlow = 0.85f + 0.15f * MathUtils.sin(animTime * 4f);
        font.setColor(goldAccent.r * titleGlow, goldAccent.g * titleGlow, goldAccent.b, appear);
        font.draw(batch, glyphLayout, titleX, titleY);

        // --- Decorative line ---
        font.getData().setScale(scale * 0.8f);
        glyphLayout.setText(font, "~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~");
        float lineX = (worldW - glyphLayout.width) / 2f;
        font.setColor(goldAccent.r, goldAccent.g, goldAccent.b, 0.4f * appear);
        font.draw(batch, glyphLayout, lineX, titleY - 40f);

        // --- Stats ---
        float statsY = worldH * 0.62f;
        font.getData().setScale(scale * 1.1f);
        String[] statLabels = {
            "Enemies Defeated: " + enemiesDefeated,
            "Torches Collected: " + torchesCollected,
            "Time Survived: " + formatTime(timeSurvived)
        };

        for (int i = 0; i < statLabels.length; i++) {
            glyphLayout.setText(font, statLabels[i]);
            float sx = (worldW - glyphLayout.width) / 2f;
            float sy = statsY - i * 35f;

            font.setColor(0f, 0f, 0f, 0.6f * appear);
            font.draw(batch, glyphLayout, sx + shadow, sy - shadow);
            font.setColor(0.9f, 0.85f, 0.7f, appear);
            font.draw(batch, glyphLayout, sx, sy);
        }

        // --- Options ---
        for (int i = 0; i < OPTION_COUNT; i++) {
            float pulse = selected == i ? 0.02f * MathUtils.sin(animTime * 7f) : 0f;
            float s = (selected == i) ? 1.05f + pulse : 1f;
            font.getData().setScale(scale * 1.2f * s);
            glyphLayout.setText(font, OPTIONS[i]);

            float textX = centerX + (boxW - glyphLayout.width) * 0.5f;
            float textY = optionYs[i] + (boxH + glyphLayout.height) * 0.5f;

            font.setColor(0f, 0f, 0f, 0.72f * appear);
            font.draw(batch, glyphLayout, textX + shadow, textY - shadow);
            font.setColor(1f, 1f, 1f, appear);
            font.draw(batch, glyphLayout, textX, textY);
        }

        font.setColor(Color.WHITE);
        font.getData().setScale(1f);
        batch.end();
    }

    private void executeOption(int index) {
        Main main = (Main) Gdx.app.getApplicationListener();
        switch (index) {
            case 0 -> main.setScreen(new HomeScreen());
            case 1 -> main.setScreen(new ExplorationScreen());
        }
    }

    private void updateLayout() {
        float worldW = viewport.getWorldWidth();
        float worldH = viewport.getWorldHeight();

        boxW = worldW * 0.3f;
        boxH = worldH * 0.08f;
        float gap = worldH * 0.035f;
        centerX = worldW * 0.5f - boxW * 0.5f;
        float baseY = worldH * 0.28f;

        for (int i = 0; i < OPTION_COUNT; i++) {
            optionYs[i] = baseY - i * (boxH + gap);
        }
    }

    private int pointerIndex() {
        for (int i = 0; i < OPTION_COUNT; i++) {
            if (touch.x >= centerX && touch.x <= centerX + boxW &&
                touch.y >= optionYs[i] && touch.y <= optionYs[i] + boxH) {
                return i;
            }
        }
        return -1;
    }

    private String formatTime(float seconds) {
        int mins = (int) (seconds / 60f);
        int secs = (int) (seconds % 60f);
        return String.format("%d:%02d", mins, secs);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        shape.dispose();
        font.dispose();
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
