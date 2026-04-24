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

    private final Color accentColor = new Color(0.25f, 0.85f, 1f, 1f);
    private final Color panelColor = new Color(0.06f, 0.08f, 0.10f, 0.92f);

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

        // Ensure progress is exactly clamped between 0 and 1
        float actualProgress = MathUtils.clamp(displayedProgress, 0f, 1f);

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        float worldW = viewport.getWorldWidth();
        float worldH = viewport.getWorldHeight();
        float pulse = 0.5f + 0.5f * MathUtils.sin(time * 2.2f);

        float panelW = 540f;
        float panelH = 220f;
        float panelX = (worldW - panelW) / 2f;
        float panelY = (worldH - panelH) / 2f;
        float panelCenterX = panelX + panelW / 2f;

        batch.begin();
        batch.draw(background, 0, 0, worldW, worldH);
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.6f);
        shapeRenderer.rect(0, 0, worldW, worldH);

        shapeRenderer.setColor(panelColor);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.setColor(accentColor.r, accentColor.g, accentColor.b, 0.14f + pulse * 0.05f);
        shapeRenderer.rect(panelX, panelY + panelH - 10f, panelW, 10f);

        float barW = panelW * 0.76f;
        float barH = 14f;
        float barX = panelX + (panelW - barW) / 2f;
        float barY = panelY + 48f;

        shapeRenderer.setColor(0.18f, 0.20f, 0.24f, 1f);
        shapeRenderer.rect(barX, barY, barW, barH);
        shapeRenderer.setColor(0f, 0f, 0f, 0.35f);
        shapeRenderer.rect(barX + 2f, barY + 2f, barW - 4f, barH - 4f);

        // FIXED: The blue bar perfectly lines up with the grey background now!
        shapeRenderer.setColor(accentColor);
        shapeRenderer.rect(barX, barY, barW * actualProgress, barH);

        float sweepWidth = 42f;
        float sweepX = barX - sweepWidth + (barW + sweepWidth * 2f) * ((time * 0.55f) % 1f);

        // Simple logic to ensure the white "sweep" animation never goes outside the blue bar
        float sweepStart = Math.max(barX, sweepX);
        float sweepEnd = Math.min(barX + barW * actualProgress, sweepX + sweepWidth);
        float sweepDrawWidth = sweepEnd - sweepStart;

        if (sweepDrawWidth > 0) {
            shapeRenderer.setColor(1f, 1f, 1f, 0.12f + pulse * 0.04f);
            shapeRenderer.rect(sweepStart, barY - 2f, sweepDrawWidth, barH + 4f);
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        float titleY = panelY + panelH - 44f;
        String loadingText = "ENTERING THE DUNGEON";
        String phaseText = getPhaseText(actualProgress) + getDots();
        String percentText = (int)(actualProgress * 100f) + "%";

        font.getData().setScale(1.45f);
        glyphLayout.setText(font, loadingText);
        font.setColor(Color.WHITE);
        font.draw(batch, glyphLayout, panelCenterX - glyphLayout.width / 2f, titleY);

        font.getData().setScale(0.95f);
        glyphLayout.setText(font, phaseText);
        font.setColor(accentColor);
        font.draw(batch, glyphLayout, panelCenterX - glyphLayout.width / 2f, panelY + panelH - 82f);

        font.getData().setScale(1.05f);
        glyphLayout.setText(font, percentText);
        font.setColor(Color.WHITE);
        font.draw(batch, glyphLayout, barX + barW - glyphLayout.width, barY + 34f);

        font.getData().setScale(1f);
        batch.end();

        if (Main.assets.update(16) && time > 0.5f) {
            displayedProgress = 1f;
            ((Game) Gdx.app.getApplicationListener()).setScreen(new ExplorationScreen(saveFileToLoad));
            this.dispose();
            return;
        }
    }

    private String getPhaseText(float progress) {
        int phaseIndex = Math.min(LOAD_PHASES.length - 1, MathUtils.floor(progress * LOAD_PHASES.length));
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
