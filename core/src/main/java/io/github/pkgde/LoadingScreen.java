package io.github.pkgde;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;

public class LoadingScreen implements Screen {

    private ShapeRenderer shapeRenderer;
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

        // Dark background
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        shapeRenderer.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        float cx = viewport.getWorldWidth() / 2f;
        float cy = viewport.getWorldHeight() / 2f;
        float radius = 45f;
        
        // Draw spinning major sector (270 degrees)
        shapeRenderer.setColor(0.25f, 0.85f, 1f, 1f); // Accent blue
        float startAngle = -time * 400f; // Spin speed
        shapeRenderer.arc(cx, cy, radius, startAngle, 270f, 64);
        
        // Punch a hole in the middle to create a thick ring
        shapeRenderer.setColor(0.05f, 0.05f, 0.05f, 1f); // Same as background
        shapeRenderer.circle(cx, cy, radius * 0.7f, 64);
        
        shapeRenderer.end();

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
    @Override public void dispose() { shapeRenderer.dispose(); }
}