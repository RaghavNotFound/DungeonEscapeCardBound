package io.github.pkgde;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.utils.viewport.*;
import com.badlogic.gdx.math.MathUtils;

public class ExplorationScreen implements Screen {

    private OrthographicCamera camera;
    private Viewport viewport;

    private GameWorld world;
    private GameRenderer renderer;

    private float shakeTime = 0f;
    private final float shakeDuration = 0.25f;

    public ExplorationScreen() {

        MapManager mapManager = new MapManager();
        mapManager.load("maps/safeRoom.tmx");

        float w = mapManager.getMapWidth();
        float h = mapManager.getMapHeight();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, w, h);
        camera.position.set(w / 2, h / 2, 0);

        viewport = new FitViewport(w, h, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        world = new GameWorld(mapManager);
        renderer = new GameRenderer(world, camera, mapManager);
    }

    @Override
    public void render(float delta) {

        viewport.apply();

        world.update(delta, camera);

        float offsetX = 0, offsetY = 0;

        if (shakeTime > 0) {
            shakeTime -= delta;
            offsetX = MathUtils.random(-10, 10);
            offsetY = MathUtils.random(-10, 10);
        }

        renderer.render(offsetX, offsetY);
    }

    @Override public void resize(int w, int h) {
        viewport.update(w, h, true);
    }

    @Override public void dispose() {
        renderer.dispose();
        world.dispose();
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
