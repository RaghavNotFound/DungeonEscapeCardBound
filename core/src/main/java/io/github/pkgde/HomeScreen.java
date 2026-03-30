package io.github.pkgde;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.viewport.*;
import com.badlogic.gdx.math.Vector3;

public class HomeScreen implements Screen
{
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;

    private Texture background;

    private OrthographicCamera camera;
    private Viewport viewport;

    private SettingsOverlay settings;

    private int selected=0;

    //BLUR SYSTEM
    private FrameBuffer fbo;
    private ShaderProgram blurShader;
    private SpriteBatch blurBatch;

    @Override
    public void show()
    {
        shapeRenderer=new ShapeRenderer();
        batch=new SpriteBatch();
        font=new BitmapFont();

        //improve font quality
        font.getRegion().getTexture().setFilter(
            Texture.TextureFilter.Linear,
            Texture.TextureFilter.Linear
        );

        background=new Texture("HomeScreen/HomeScreen.jpg");
        background.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);

        camera=new OrthographicCamera();
        viewport=new FitViewport(1280,720,camera);
        viewport.apply(true);

        camera.position.set(
            viewport.getWorldWidth()/2f,
            viewport.getWorldHeight()/2f,
            0
        );
        camera.update();

        settings=new SettingsOverlay();

        //DYNAMIC SIZE
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
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();

        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        float worldW=viewport.getWorldWidth();
        float worldH=viewport.getWorldHeight();

        float btnWidth=worldW*0.25f;
        float btnHeight=worldH*0.08f;
        float gap=worldH*0.03f;

        float btnX=worldW*0.65f;
        float playY=worldH*0.55f;
        float settingsY=playY-btnHeight-gap;
        float exitY=settingsY-btnHeight-gap;

        //INPUT
        if (settings.isActive())
        {
            settings.handleInput(viewport);
        }
        else
        {

            if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP))
            {
                selected=(selected+2)%3;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
            {
                selected=(selected+1)%3;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER))
            {
                applySelection();
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            {
                Gdx.app.exit();
            }

            if (Gdx.input.justTouched())
            {
                Vector3 touch=viewport.unproject(
                    new Vector3(Gdx.input.getX(),Gdx.input.getY(),0));

                float x=touch.x;
                float y=touch.y;

                if (inside(x,y,btnX,playY,btnWidth,btnHeight))
                {
                    selected=0;
                }
                else if (inside(x,y,btnX,settingsY,btnWidth,btnHeight))
                {
                    selected=1;
                }
                else if (inside(x,y,btnX,exitY,btnWidth,btnHeight))
                {
                    selected=2;
                }

                applySelection();
            }
        }

        //SETTINGS MODE
        if (settings.isActive())
        {
            fbo.begin();

            batch.begin();
            batch.draw(background,0,0,worldW,worldH);
            batch.end();

            fbo.end();

            Texture tex=fbo.getColorBufferTexture();

            blurBatch.setProjectionMatrix(camera.combined);

            blurBatch.begin();
            blurShader.setUniformf("blur",0.002f);

            blurBatch.draw(
                tex,
                0,0,
                worldW,worldH,
                0,0,
                tex.getWidth(),
                tex.getHeight(),
                false,true
            );

            blurBatch.end();
            Gdx.gl.glEnable(GL20.GL_BLEND);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0,0,0,0.5f);
            shapeRenderer.rect(0,0,worldW,worldH);
            shapeRenderer.end();

            Gdx.gl.glDisable(GL20.GL_BLEND);

            settings.render(shapeRenderer,batch,font,viewport);
            return;
        }

        //NORMAL MENU
        batch.begin();

        batch.draw(background,0,0,worldW,worldH);

        float scale=worldW/800f;

        font.getData().setScale(scale*2f);
        font.draw(batch,"DUNGEON ESCAPE",worldW*0.05f,worldH*0.9f);

        font.getData().setScale(scale*1.2f);

        drawButton(batch,font,"PLAY",btnX,playY,selected==0,scale);
        drawButton(batch,font,"SETTINGS",btnX,settingsY,selected==1,scale);
        drawButton(batch,font,"EXIT",btnX,exitY,selected==2,scale);

        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        drawRect(btnX,playY,btnWidth,btnHeight,selected==0);
        drawRect(btnX,settingsY,btnWidth,btnHeight,selected==1);
        drawRect(btnX,exitY,btnWidth,btnHeight,selected==2);

        shapeRenderer.end();
    }
    private void drawButton(SpriteBatch batch,BitmapFont font,String text,float x,float y,boolean active,float scale)
    {
        float offset=active?8f:0f;
        float s=active?1.05f:1f;

        font.getData().setScale(scale*1.2f*s);
        font.draw(batch,text,x+20,y+40+offset);
    }

    private void drawRect(float x,float y,float w,float h,boolean active)
    {
        shapeRenderer.setColor(active?1:0.6f,1,1,1);
        shapeRenderer.rect(x,y,w,h);
    }

    private boolean inside(float x,float y,float bx,float by,float bw,float bh)
    {
        return x>=bx && x<=bx+bw && y>=by && y<=by+bh;
    }

    private void applySelection()
    {
        if (selected==0)
        {
            ((Main) Gdx.app.getApplicationListener()).setScreen(new ExplorationScreen());
        }
        else if (selected==1)
        {
            settings.show();
        }
        else if (selected==2)
        {
            Gdx.app.exit();
        }
    }

    @Override
    public void resize(int width,int height)
    {
        viewport.update(width,height,true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose()
    {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
        background.dispose();

        fbo.dispose();
        blurBatch.dispose();
        blurShader.dispose();
    }
}
