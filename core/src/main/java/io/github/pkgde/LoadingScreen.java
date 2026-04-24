package io.github.pkgde;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;

public class LoadingScreen implements Screen {

    private static final String[] LOAD_PHASES = {
        "Gathering cards",
        "Tracing corridors",
        "Waking old torches",
        "Opening the gate"
    };

    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout glyphLayout;
    private Texture background;

    private OrthographicCamera camera;
    private Viewport viewport;

    private float time = 0f;
    private float displayedProgress = 0f;
    private boolean assetsQueued = false;
    private final String saveFileToLoad;

    // UI Colors
    private final Color accentColor = new Color(0.25f, 0.85f, 1f, 1f); // Matching HomeScreen accent
    private final Color panelColor = new Color(0.06f, 0.08f, 0.10f, 0.92f);
    private final Color panelEdgeColor = new Color(0.46f, 0.84f, 1f, 0.85f);
    private final Color mutedTextColor = new Color(0.72f, 0.78f, 0.86f, 1f);
    private final Color warmGlowColor = new Color(1f, 0.65f, 0.18f, 0.12f);

    public LoadingScreen(String saveFileToLoad) {
        this.saveFileToLoad = saveFileToLoad;
    }

    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        glyphLayout = new GlyphLayout();

        background = new Texture("HomeScreen/HomeScreen.jpg");
        background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        camera = new OrthographicCamera();
        viewport = new FitViewport(1280, 720, camera);
        viewport.apply(true);
    }

    @Override
    public void render(float delta) {
        time += delta;

        // Queue all game assets once
        if (!assetsQueued) {
            Main.assets.setLoader(TiledMap.class, new TmxMapLoader(new InternalFileHandleResolver()));
            Player.queueAssets(Main.assets);
            Enemy.queueAssets(Main.assets);
            GameRenderer.queueAssets(Main.assets);
            InventoryOverlay.queueAssets(Main.assets);
            assetsQueued = true;
        }

        float progress = Main.assets.getProgress();
        displayedProgress = MathUtils.lerp(displayedProgress, progress, Math.min(1f, delta * 5f));

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        float worldW = viewport.getWorldWidth();
        float worldH = viewport.getWorldHeight();
        float pulse = 0.5f + 0.5f * MathUtils.sin(time * 2.2f);
        float orbit = 0.5f + 0.5f * MathUtils.sin(time * 1.4f);

        float panelW = 540f;
        float panelH = 220f;
        float panelX = (worldW - panelW) / 2f;
        float panelY = (worldH - panelH) / 2f;
        float panelCenterX = panelX + panelW / 2f;
        float panelCenterY = panelY + panelH / 2f;

        // 1. Draw Background
        batch.begin();
        batch.draw(background, 0, 0, worldW, worldH);
        batch.end();

        // 2. Draw Dark Full-Screen Overlay
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.6f);
        shapeRenderer.rect(0, 0, worldW, worldH);

        // Ambient glow to make the loading screen feel less flat.
        shapeRenderer.setColor(accentColor.r, accentColor.g, accentColor.b, 0.10f + pulse * 0.05f);
        shapeRenderer.circle(panelCenterX, panelCenterY + 10f, 180f + pulse * 22f, 48);

        shapeRenderer.setColor(warmGlowColor.r, warmGlowColor.g, warmGlowColor.b, warmGlowColor.a + orbit * 0.04f);
        shapeRenderer.circle(panelCenterX, panelCenterY - 20f, 120f + orbit * 16f, 40);

        // Orbiting card shards to sell the "Cardbound" vibe.
        drawCardShard(panelCenterX - 210f, panelCenterY + 58f, -18f + time * 18f, 0.14f + pulse * 0.05f);
        drawCardShard(panelCenterX + 208f, panelCenterY + 36f, 14f - time * 14f, 0.14f + orbit * 0.05f);
        drawCardShard(panelCenterX, panelCenterY - 116f, 45f + time * 12f, 0.10f + pulse * 0.04f);

        // Panel background
        shapeRenderer.setColor(panelColor);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.setColor(accentColor.r, accentColor.g, accentColor.b, 0.14f + pulse * 0.05f);
        shapeRenderer.rect(panelX, panelY + panelH - 10f, panelW, 10f);

        // Framing glow bars
        shapeRenderer.setColor(accentColor.r, accentColor.g, accentColor.b, 0.18f + pulse * 0.05f);
        shapeRenderer.rect(panelX + 18f, panelY + 18f, 6f, panelH - 36f);
        shapeRenderer.rect(panelX + panelW - 24f, panelY + 18f, 6f, panelH - 36f);

        // Progress Bar Background
        float barW = panelW * 0.76f;
        float barH = 14f;
        float barX = panelX + (panelW - barW) / 2f;
        float barY = panelY + 48f;
        shapeRenderer.setColor(0.18f, 0.20f, 0.24f, 1f);
        shapeRenderer.rect(barX, barY, barW, barH);
        shapeRenderer.setColor(0f, 0f, 0f, 0.35f);
        shapeRenderer.rect(barX + 2f, barY + 2f, barW - 4f, barH - 4f);

        // 3. Update Progress and Draw Bar
        shapeRenderer.setColor(accentColor);
        shapeRenderer.rect(barX, barY, barW * displayedProgress, barH);

        // Sweeping highlight
        float sweepWidth = 42f;
        float sweepX = barX - sweepWidth + (barW + sweepWidth * 2f) * ((time * 0.55f) % 1f);
        shapeRenderer.setColor(1f, 1f, 1f, 0.12f + pulse * 0.04f);
        shapeRenderer.rect(sweepX, barY - 2f, sweepWidth, barH + 4f);

        // Small phase markers
        for (int i = 1; i < LOAD_PHASES.length; i++) {
            float markerX = barX + barW * (i / (float) LOAD_PHASES.length);
            shapeRenderer.setColor(1f, 1f, 1f, 0.18f);
            shapeRenderer.rect(markerX, barY - 4f, 2f, barH + 8f);
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(panelEdgeColor.r, panelEdgeColor.g, panelEdgeColor.b, panelEdgeColor.a);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.rect(panelX + 8f, panelY + 8f, panelW - 16f, panelH - 16f);
        shapeRenderer.setColor(1f, 1f, 1f, 0.34f);
        shapeRenderer.rect(barX, barY, barW, barH);

        float ringRadius = 34f + pulse * 4f;
        shapeRenderer.setColor(accentColor.r, accentColor.g, accentColor.b, 0.55f);
        shapeRenderer.circle(panelCenterX, panelY + panelH - 54f, ringRadius, 36);
        shapeRenderer.setColor(1f, 1f, 1f, 0.22f + orbit * 0.10f);
        shapeRenderer.circle(panelCenterX, panelY + panelH - 54f, ringRadius + 9f, 36);
        shapeRenderer.end();

        // 4. Draw Loading Text and Percentage
        batch.begin();
        float titleY = panelY + panelH - 44f;
        String loadingText = "ENTERING THE DUNGEON";
        String phaseText = getPhaseText(displayedProgress) + getDots();
        String detailText = saveFileToLoad == null
            ? "Preparing a fresh descent"
            : "Restoring your last descent";
        String percentText = (int)(displayedProgress * 100f) + "%";
        String hintText = "F11 toggles borderless fullscreen";

        font.getData().setScale(1.45f);
        glyphLayout.setText(font, loadingText);
        float titleX = panelCenterX - glyphLayout.width / 2f;
        font.setColor(0f, 0f, 0f, 0.75f);
        font.draw(batch, glyphLayout, titleX + 3f, titleY - 3f);
        font.setColor(Color.WHITE);
        font.draw(batch, glyphLayout, titleX, titleY);

        font.getData().setScale(0.95f);
        glyphLayout.setText(font, phaseText);
        font.setColor(accentColor);
        font.draw(batch, glyphLayout, panelCenterX - glyphLayout.width / 2f, panelY + panelH - 82f);

        font.getData().setScale(0.85f);
        glyphLayout.setText(font, detailText);
        font.setColor(mutedTextColor);
        font.draw(batch, glyphLayout, panelCenterX - glyphLayout.width / 2f, panelY + panelH - 108f);

        font.getData().setScale(1.05f);
        glyphLayout.setText(font, percentText);
        font.setColor(Color.WHITE);
        font.draw(batch, glyphLayout, barX + barW - glyphLayout.width, barY + 34f);

        font.getData().setScale(0.78f);
        glyphLayout.setText(font, hintText);
        font.setColor(1f, 1f, 1f, 0.55f);
        font.draw(batch, glyphLayout, panelCenterX - glyphLayout.width / 2f, panelY + 24f);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Update AssetManager and transition when finished
        if (Main.assets.update(16) && time > 0.5f) {
            displayedProgress = 1f;
            ((Game) Gdx.app.getApplicationListener()).setScreen(new ExplorationScreen(saveFileToLoad));
        }
    }

    private void drawCardShard(float centerX, float centerY, float rotation, float alpha) {
        float cardW = 28f;
        float cardH = 40f;
        shapeRenderer.setColor(accentColor.r, accentColor.g, accentColor.b, alpha);
        shapeRenderer.rect(
            centerX - cardW / 2f,
            centerY - cardH / 2f,
            cardW / 2f,
            cardH / 2f,
            cardW,
            cardH,
            1f,
            1f,
            rotation
        );
        shapeRenderer.setColor(1f, 1f, 1f, alpha * 0.45f);
        shapeRenderer.rect(
            centerX - 6f,
            centerY - 6f,
            6f,
            6f,
            12f,
            12f,
            1f,
            1f,
            rotation + 45f
        );
    }

    private String getPhaseText(float progress) {
        int phaseIndex = Math.min(
            LOAD_PHASES.length - 1,
            MathUtils.floor(progress * LOAD_PHASES.length)
        );
        return LOAD_PHASES[phaseIndex];
    }

    private String getDots() {
        int numDots = (int) (time * 2f) % 4;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numDots; i++) sb.append(".");
        return sb.toString();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
        background.dispose();
    }
}
