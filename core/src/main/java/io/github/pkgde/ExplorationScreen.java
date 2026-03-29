package io.github.pkgde;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.viewport.*;
import com.badlogic.gdx.math.MathUtils;

public class ExplorationScreen implements Screen
{
    private final OrthographicCamera camera;
    private final Viewport viewport;

    private final GameWorld world;
    private final GameRenderer renderer;
    private final InputHandler input;

    private final PauseOverlay pauseOverlay;
    private final SettingsOverlay settingsOverlay;

    public enum State{GAME,PAUSE,SETTINGS}
    private State state=State.GAME;

    //Screen Shake Variables
    private float shakeTime=0f;
    private boolean shakeTriggered=false;

    //Background Blur
    private final FrameBuffer fbo;
    private final ShaderProgram blurShader;
    private final SpriteBatch blurBatch;

    public ExplorationScreen()
    {
        camera=new OrthographicCamera();

        viewport=new FitViewport(1280,720,camera);
        viewport.apply(true);

        camera.position.set(viewport.getWorldWidth()/2f,viewport.getWorldHeight()/2f,0);
        camera.update();

        world=new GameWorld();
        renderer=new GameRenderer(world,camera);
        input=new InputHandler(viewport);

        pauseOverlay=new PauseOverlay();
        settingsOverlay=new SettingsOverlay();

        //DYNAMIC FBO
        fbo=new FrameBuffer(
            Pixmap.Format.RGBA8888,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            false
        );

        fbo.getColorBufferTexture().setFilter(
            Texture.TextureFilter.Linear,
            Texture.TextureFilter.Linear
        );

        blurShader=BlurShader.createShader(true);
        blurBatch=new SpriteBatch();
        blurBatch.setShader(blurShader);
    }

    @Override
    public void render(float delta)
    {
        // ===== PER-STATE INPUT =====
        // Each state fully owns its input — no bleed between states
        if (state==State.GAME)
        {
            InputHandler.Action action=input.handle();

            switch (action)
            {
                case TOGGLE_PAUSE:
                    state=State.PAUSE;
                    break;

                case EXIT_TO_MENU:
                    ((Main)Gdx.app.getApplicationListener()).setScreen(new HomeScreen());
                    break;
            }
        }
        else if (state==State.PAUSE)
        {
            //ESC resumes game
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            {
                state=State.GAME;
            }
            else
            {
                PauseOverlay.Action pauseAction=pauseOverlay.handleInput();

                switch (pauseAction)
                {
                    case RESUME:
                        state=State.GAME;
                        break;

                    case SETTINGS:
                        state=State.SETTINGS;
                        settingsOverlay.show();
                        break;

                    case EXIT:
                        ((Main)Gdx.app.getApplicationListener()).setScreen(new HomeScreen());
                        break;

                    case NONE:
                        break;
                }
            }
        }
        else if (state==State.SETTINGS)
        {
            settingsOverlay.handleInput(viewport);

            if (!settingsOverlay.isActive())
            {
                state=State.PAUSE;
            }
        }

        // ===== UPDATE =====
        if (state==State.GAME)
        {
            world.update(delta,camera);

            if (world.isPlayerNearEnemy())
            {
                if (!shakeTriggered)
                {
                    shakeTime=0.25f;
                    shakeTriggered=true;
                }
            }
            else
            {
                shakeTriggered=false;
            }
        }

        // ===== SHAKE =====
        float offsetX=0,offsetY=0;

        if (shakeTime>0)
        {
            shakeTime-=delta;
            float shakeIntensity=10f;
            offsetX=MathUtils.random(-shakeIntensity,shakeIntensity);
            offsetY=MathUtils.random(-shakeIntensity,shakeIntensity);
        }

        camera.position.set(
            viewport.getWorldWidth()/2f+offsetX,
            viewport.getWorldHeight()/2f+offsetY,
            0
        );
        camera.update();

        // ===== RENDER =====
        if (state==State.PAUSE||state==State.SETTINGS)
        {
            fbo.begin();
            renderer.render();
            fbo.end();

            Texture tex=fbo.getColorBufferTexture();

            blurBatch.setProjectionMatrix(camera.combined);

            blurBatch.begin();
            blurShader.setUniformf("blur",0.002f);

            blurBatch.draw(
                tex,
                camera.position.x-camera.viewportWidth/2f,
                camera.position.y-camera.viewportHeight/2f,
                camera.viewportWidth,
                camera.viewportHeight,
                0,0,
                tex.getWidth(),
                tex.getHeight(),
                false,true
            );

            blurBatch.end();

            //SOFTER OVERLAY
            Gdx.gl.glEnable(GL20.GL_BLEND);

            ShapeRenderer shape=renderer.getShape();
            shape.setProjectionMatrix(camera.combined);

            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(0,0,0,0.5f);
            shape.rect(
                camera.position.x-camera.viewportWidth/2f,
                camera.position.y-camera.viewportHeight/2f,
                camera.viewportWidth,
                camera.viewportHeight
            );
            shape.end();

            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
        else
        {
            renderer.render();
        }

        // ===== UI =====
        SpriteBatch batch=renderer.getBatch();
        ShapeRenderer shape=renderer.getShape();
        BitmapFont font=renderer.getFont();

        if (state==State.PAUSE)
        {
            pauseOverlay.render(shape,batch,font,viewport);
        }

        if (state==State.SETTINGS)
        {
            settingsOverlay.render(shape,batch,font,viewport);
        }
    }

    @Override
    public void resize(int width,int height)
    {
        viewport.update(width,height,true);

        camera.position.set(
            viewport.getWorldWidth()/2f,
            viewport.getWorldHeight()/2f,
            0
        );
        camera.update();
    }

    @Override
    public void dispose()
    {
        renderer.dispose();
        world.dispose();
        fbo.dispose();
        blurBatch.dispose();
        blurShader.dispose();
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
