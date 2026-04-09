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
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;

public class LoadingScreen implements Screen {

    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout glyphLayout;
    private Texture background;

    private OrthographicCamera camera;
    private Viewport viewport;

    private float time = 0f;
    private boolean assetsQueued = false;

    private final String saveFileToLoad;

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
        camera.position.set(viewport.getWorldWidth() / 2f, viewport.getWorldHeight() / 2f, 0);
        camera.update();
    }

    @Override
    public void render(float delta) {
        time += delta;

        if (!assetsQueued) {
            Main.assets.setLoader(TiledMap.class, new TmxMapLoader(new InternalFileHandleResolver()));
            Main.assets.load("Maps/safeRoom.tmx", TiledMap.class);
            Player.queueAssets(Main.assets);
            Enemy.queueAssets(Main.assets);
            GameRenderer.queueAssets(Main.assets);
            InventoryOverlay.queueAssets(Main.assets);
            assetsQueued = true;
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();

        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        float worldW = viewport.getWorldWidth();
        float worldH = viewport.getWorldHeight();

        // Draw background
        batch.begin();
        batch.draw(background, 0, 0, worldW, worldH);
        batch.end();

        // Draw dark overlay to match home screen styling and improve text visibility
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.4f);
        shapeRenderer.rect(0, 0, worldW, worldH);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Calculate Loading text with dots
        int numDots = (int) (time * 2f) % 4; // Cycles 0, 1, 2, 3
        StringBuilder textBuilder = new StringBuilder("Loading");
        for (int i = 0; i < numDots; i++) {
            textBuilder.append(" .");
        }
        String loadingText = textBuilder.toString();

        batch.begin();
        float scale = worldW / 800f;
        font.getData().setScale(scale * 1.5f);
        glyphLayout.setText(font, loadingText);

        // Bottom left corner
        float textX = 50f;
        float textY = 50f + glyphLayout.height;

        // Shadow
        float shadowOffset = 2f;
        font.setColor(0f, 0f, 0f, 0.8f);
        font.draw(batch, glyphLayout, textX + shadowOffset, textY - shadowOffset);

        // Text
        font.setColor(Color.WHITE);
        font.draw(batch, glyphLayout, textX, textY);

        batch.end();

        // Update AssetManager (Limits GL upload to 16ms to prevent hanging!)
        // It returns true when everything is completely loaded.
        if (Main.assets.update(16) && time > 0.2f) {
            ((Game) Gdx.app.getApplicationListener()).setScreen(new ExplorationScreen(saveFileToLoad));
        }
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
