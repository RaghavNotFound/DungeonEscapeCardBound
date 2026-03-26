package io.github.pkgde;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class ExplorationScreen implements Screen {

    private final SpriteBatch batch;
    private final Texture background;
    private final Player player;
    private final ShapeRenderer shape;

    public ExplorationScreen() {
        batch = new SpriteBatch();
        background = new Texture("background.png");
        player = new Player();
        shape = new ShapeRenderer();
    }

    @Override
    public void render(float delta) {
        update(delta);

        ScreenUtils.clear(0, 0, 0, 1);

        batch.begin();
        batch.draw(background, 0, 0);
        player.render(batch);
        batch.end();

        // Debug collision box
        shape.begin(ShapeRenderer.ShapeType.Line);
//        shape.rect(player.bounds.x, player.bounds.y, player.bounds.width, player.bounds.height);
        shape.end();
    }

    private void update(float delta) {
        player.update(delta);
    }

    @Override
    public void dispose() {
        batch.dispose();
        background.dispose();
        player.dispose();
        shape.dispose();
    }

    public void show() {}
    public void resize(int width, int height) {}
    public void pause() {}
    public void resume() {}
    public void hide() {}
}
