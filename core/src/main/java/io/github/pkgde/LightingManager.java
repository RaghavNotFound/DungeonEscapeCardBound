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

    // Player light
    private final Vector2 playerLightCenter = new Vector2();
    private boolean playerHasTorch = false;
    private static final float PLAYER_TORCH_RADIUS = 180f;

    private FrameBuffer lightFbo;

    public LightingManager() {
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    // Base logic (File 1): Preserved for backward compatibility
    public void updatePlayerLight(float cx, float cy) {
        playerLightCenter.set(cx, cy);
    }

    // Added from File 2: Integrated state management for the torch
    public void setPlayerTorchState(boolean hasTorch, float px, float py) {
        this.playerHasTorch = hasTorch;
        this.playerLightCenter.set(px, py);
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
        shape.setColor(0f, 0f, 0f, 0.85f); // 85% darkness so the map is slightly visible
        float cx = camera.position.x, cy = camera.position.y;
        float vw = camera.viewportWidth, vh = camera.viewportHeight;
        shape.rect(cx - vw / 2f, cy - vh / 2f, vw, vh);
        shape.end();

        // 2. Punch Light Holes (using GL subtraction)
        Gdx.gl.glBlendFunc(GL20.GL_ZERO, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);

        // Base logic (File 1): Player's personal small light ring (always visible)
        shape.setColor(0f, 0f, 0f, 1f);
        shape.circle(playerLightCenter.x, playerLightCenter.y, 50f);

        // Added from File 2: Expanding tiered torch light if player holds a torch
        if (playerHasTorch) {
            shape.setColor(0f, 0f, 0f, 0.25f); shape.circle(playerLightCenter.x, playerLightCenter.y, PLAYER_TORCH_RADIUS);
            shape.setColor(0f, 0f, 0f, 0.55f); shape.circle(playerLightCenter.x, playerLightCenter.y, PLAYER_TORCH_RADIUS * 0.7f);
            shape.setColor(0f, 0f, 0f, 1f);    shape.circle(playerLightCenter.x, playerLightCenter.y, PLAYER_TORCH_RADIUS * 0.45f);
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
