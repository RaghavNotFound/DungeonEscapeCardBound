package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class LightingManager {

    private boolean isLit = false;
    private float lightRadius = 0f;
    private final Vector2 lightCenter = new Vector2();
    private final float maxLightRadius = 1500f;

    // Player light
    private final Vector2 playerLightCenter = new Vector2();
    private float playerLightRadius = 150f;

    private FrameBuffer lightFbo;

    public LightingManager() {
        // Initialization handled dynamically in render() now!
    }

    public void updatePlayerLight(float cx, float cy) {
        playerLightCenter.set(cx, cy);
    }

    public void triggerLighting(float cx, float cy) {
        isLit = true;
        lightCenter.set(cx, cy);
        lightRadius = 0f;
    }

    public boolean isLit() {
        return isLit;
    }

    public void update(float delta) {
        if (isLit && lightRadius < maxLightRadius) {
            lightRadius += 500f * delta;
            if (lightRadius > maxLightRadius) {
                lightRadius = maxLightRadius;
            }
        }
    }

    public void updateLightFbo(OrthographicCamera camera, ShapeRenderer shape) {
        if (isLit && lightRadius >= maxLightRadius) return;

        // THE GOLDEN FIX: Lock the FBO strictly to the camera's true viewport size!
        // This makes aspect ratio drifting mathematically impossible.
        int logicWidth = (int) camera.viewportWidth;
        int logicHeight = (int) camera.viewportHeight;

        // Rebuild FBO automatically if it doesn't match the exact logical dimensions
        if (lightFbo == null || lightFbo.getWidth() != logicWidth || lightFbo.getHeight() != logicHeight) {
            if (lightFbo != null) lightFbo.dispose();
            lightFbo = new FrameBuffer(Pixmap.Format.RGBA8888, logicWidth, logicHeight, false);
            lightFbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }

        lightFbo.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 0.85f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // Punch Light Holes
        Gdx.gl.glBlendFunc(GL20.GL_ZERO, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);

        // Player's personal light
        int lightBands = 80;
        float maxAlphaPerBand = 0.08f;

        for (int i = 0; i < lightBands; i++) {
            float progress = (float) i / lightBands;
            float currentRadius = playerLightRadius * (1f - progress);
            float currentAlpha = maxAlphaPerBand * (progress * progress);

            shape.setColor(0f, 0f, 0f, currentAlpha);
            shape.circle(playerLightCenter.x, playerLightCenter.y, currentRadius);
        }

        // Center Fire Expanding Light
        if (isLit || lightRadius > 0) {
            shape.setColor(0f, 0f, 0f, 0.3f); shape.circle(lightCenter.x, lightCenter.y, lightRadius);
            shape.setColor(0f, 0f, 0f, 0.6f); shape.circle(lightCenter.x, lightCenter.y, lightRadius * 0.8f);
            shape.setColor(0f, 0f, 0f, 1f);   shape.circle(lightCenter.x, lightCenter.y, lightRadius * 0.6f);
        }

        shape.end();
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        lightFbo.end();
    }

    public void render(OrthographicCamera camera, SpriteBatch batch, ShapeRenderer shape) {
        if (isLit && lightRadius >= maxLightRadius) return;

        // Draw FBO Output to Screen
        float cx = camera.position.x;
        float cy = camera.position.y;
        float drawW = camera.viewportWidth * camera.zoom;
        float drawH = camera.viewportHeight * camera.zoom;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Extract perfectly sized region
        if (lightFbo != null) {
            Texture tex = lightFbo.getColorBufferTexture();
            TextureRegion fboRegion = new TextureRegion(tex, 0, 0, lightFbo.getWidth(), lightFbo.getHeight());
            fboRegion.flip(false, true);
            batch.draw(fboRegion, cx - drawW / 2f, cy - drawH / 2f, drawW, drawH);
        }
        batch.end();
    }

    public void dispose() {
        if (lightFbo != null) lightFbo.dispose();
    }

    public void resize(int w, int h) {
    }
}
