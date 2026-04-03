package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class LightingManager {

    private boolean isLit = false;
    private float lightRadius = 0f;
    private final Vector2 lightCenter = new Vector2();
    private final float maxLightRadius = 1500f; // Large enough to cover the screen

    private FrameBuffer lightFbo;

    public LightingManager() {
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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
            lightRadius += 500f * delta; // Expansion speed
            if (lightRadius > maxLightRadius) {
                lightRadius = maxLightRadius;
            }
        }
    }

    public void render(OrthographicCamera camera, SpriteBatch batch, ShapeRenderer shape) {
        if (isLit && lightRadius >= maxLightRadius) return; // Screen is fully lit
        if (lightFbo == null) return;

        lightFbo.begin();
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // 1. Draw Pitch Black Overlay
        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0f, 0f, 0.96f); // 96% darkness
        float cx = camera.position.x, cy = camera.position.y;
        float vw = camera.viewportWidth, vh = camera.viewportHeight;
        shape.rect(cx - vw / 2f, cy - vh / 2f, vw, vh);
        shape.end();

        // 2. Punch Expanding Light Hole (using GL subtraction)
        if (isLit || lightRadius > 0) {
            Gdx.gl.glBlendFunc(GL20.GL_ZERO, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(0f, 0f, 0f, 0.3f); shape.circle(lightCenter.x, lightCenter.y, lightRadius);
            shape.setColor(0f, 0f, 0f, 0.6f); shape.circle(lightCenter.x, lightCenter.y, lightRadius * 0.8f);
            shape.setColor(0f, 0f, 0f, 1f);   shape.circle(lightCenter.x, lightCenter.y, lightRadius * 0.6f);
            shape.end();
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
        lightFbo.end();

        // 3. Draw FBO Output to Screen
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        Texture tex = lightFbo.getColorBufferTexture();
        batch.draw(tex, cx - vw / 2f, cy - vh / 2f, vw, vh, 0, 0, tex.getWidth(), tex.getHeight(), false, true);
        batch.end();
    }

    public void resize(int w, int h) {
        if (lightFbo != null) lightFbo.dispose();
        if (w > 0 && h > 0) {
            lightFbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
            lightFbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
    }

    public void dispose() {
        if (lightFbo != null) lightFbo.dispose();
    }
}