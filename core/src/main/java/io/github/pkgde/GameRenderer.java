package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class GameRenderer
{
    private final GameWorld world;

    private final SpriteBatch batch;
    private final ShapeRenderer shape;
    private final BitmapFont font;

    private final Texture background;
    private final OrthographicCamera camera;

    public GameRenderer(GameWorld world,OrthographicCamera camera)
    {
        this.world=world;
        this.camera=camera;

        batch=new SpriteBatch();
        shape=new ShapeRenderer();
        font=new BitmapFont();

        //LOAD BACKGROUND WITH QUALITY SETTINGS
        background=new Texture("background.png");
        background.setFilter(
            Texture.TextureFilter.Linear,
            Texture.TextureFilter.Linear
        );

        //SHARP TEXT
        font.getRegion().getTexture().setFilter(
            Texture.TextureFilter.Linear,
            Texture.TextureFilter.Linear
        );
    }

    public void render()
    {
        //CLEAR
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        shape.setProjectionMatrix(camera.combined);

        batch.begin();

        //BACKGROUND CAMERA
        float camX=camera.position.x-camera.viewportWidth/2f;
        float camY=camera.position.y-camera.viewportHeight/2f;

        batch.draw(
            background,
            camX,
            camY,
            camera.viewportWidth,
            camera.viewportHeight
        );

        //GAME OBJECTS (DEPTH LAYERING)
        if (world.getPlayer().getPos().y>world.getEnemy().getBounds().y)
        {
            world.getEnemy().render(batch);
        }

        world.getPlayer().render(batch);

        if (world.getPlayer().getPos().y<=world.getEnemy().getBounds().y)
        {
            world.getEnemy().render(batch);
        }

        batch.end();

    }

    public SpriteBatch getBatch() { return batch; }
    public ShapeRenderer getShape() { return shape; }
    public BitmapFont getFont() { return font; }

    public void dispose()
    {
        batch.dispose();
        shape.dispose();
        font.dispose();
        background.dispose();
    }
}
