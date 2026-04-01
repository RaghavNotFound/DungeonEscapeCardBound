package io.github.pkgde;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.viewport.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class HomeScreen implements Screen
{
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout glyphLayout;

    private Texture background;

    private OrthographicCamera camera;
    private Viewport viewport;

    private SettingsOverlay settings;

    private int selected = 0;
    private float menuAnimTime = 0f;
    private final Vector3 pointer = new Vector3();
    private final Color accent = new Color(0.25f, 0.85f, 1f, 1f);

    //BLUR SYSTEM
    private FrameBuffer fbo;
    private ShaderProgram blurShader;
    private SpriteBatch blurBatch;

    @Override
    public void show() {

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        glyphLayout = new GlyphLayout();

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
    public void render(float delta) {

        menuAnimTime += delta;

        Gdx.gl.glClearColor(0, 0, 0, 1);
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
        if (settings.isOverlayVisible())
        {
            settings.update(delta);
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

            if (Gdx.input.justTouched()) {
                int clicked = getPointerSelection(btnX, playY, settingsY, exitY, btnWidth, btnHeight);
                if (clicked >= 0) {
                    selected = clicked;
                    applySelection();
                }
            }

            int hovered = getPointerSelection(btnX, playY, settingsY, exitY, btnWidth, btnHeight);
            if (hovered >= 0) {
                selected = hovered;
            }
        }

        //SETTINGS MODE
        if (settings.isOverlayVisible())
        {
            fbo.begin();

            batch.begin();
            batch.draw(background,0,0,worldW,worldH);
            batch.end();

            fbo.end();

            Texture tex=fbo.getColorBufferTexture();

            blurBatch.setProjectionMatrix(camera.combined);

            blurBatch.begin();
            float progress = settings.getTransitionProgress();
            blurShader.setUniformf("blur", progress * 0.002f);
            
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
            shapeRenderer.setColor(0, 0, 0, progress * 0.5f);
            shapeRenderer.rect(0,0,worldW,worldH);
            shapeRenderer.end();

            Gdx.gl.glDisable(GL20.GL_BLEND);

            settings.render(shapeRenderer,batch,font,viewport);
            return;
        }

        //NORMAL MENU
        batch.begin();
        batch.draw(background, 0, 0, worldW, worldH);
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);

        // Soft dark panel improves text contrast over bright backgrounds.
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.32f);
        shapeRenderer.rect(worldW * 0.6f, worldH * 0.2f, worldW * 0.35f, worldH * 0.5f);

        drawFill(btnX, playY, btnWidth, btnHeight, selected == 0);
        drawFill(btnX, settingsY, btnWidth, btnHeight, selected == 1);
        drawFill(btnX, exitY, btnWidth, btnHeight, selected == 2);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        drawRect(btnX, playY, btnWidth, btnHeight, selected == 0);
        drawRect(btnX, settingsY, btnWidth, btnHeight, selected == 1);
        drawRect(btnX, exitY, btnWidth, btnHeight, selected == 2);
        shapeRenderer.end();

        batch.begin();

        float scale = worldW / 800f;

        font.getData().setScale(scale * 2.1f);
        glyphLayout.setText(font, "DUNGEON ESCAPE");
        float titleX = worldW * 0.06f;
        float titleY = worldH * 0.9f;
        font.setColor(0f, 0f, 0f, 0.7f);
        font.draw(batch, glyphLayout, titleX + 3f, titleY - 3f);
        font.setColor(1f, 1f, 1f, 1f);
        font.draw(batch, glyphLayout, titleX, titleY);

        font.getData().setScale(scale * 1.2f);

        drawButton(batch, font, "PLAY", btnX, playY, btnWidth, btnHeight, selected == 0, scale, worldW);
        drawButton(batch, font, "SETTINGS", btnX, settingsY, btnWidth, btnHeight, selected == 1, scale, worldW);
        drawButton(batch, font, "EXIT", btnX, exitY, btnWidth, btnHeight, selected == 2, scale, worldW);

        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawButton(SpriteBatch batch, BitmapFont font,
                            String text, float x, float y, float w, float h,
                            boolean active, float scale, float worldW) {
        float pulse = active ? (0.02f * MathUtils.sin(menuAnimTime * 6f)) : 0f;
        float s = active ? 1.07f + pulse : 1f;

        font.getData().setScale(scale * 1.15f * s);
        glyphLayout.setText(font, text);

        float textX = x + (w - glyphLayout.width) * 0.5f;
        float textY = y + (h + glyphLayout.height) * 0.5f;

        float shadowOffset = Math.max(1.5f, worldW * 0.0013f);
        font.setColor(0f, 0f, 0f, 0.75f);
        font.draw(batch, glyphLayout, textX + shadowOffset, textY - shadowOffset);
        font.setColor(active ? accent : Color.WHITE);
        font.draw(batch, glyphLayout, textX, textY);
        font.setColor(Color.WHITE);
    }

    private void drawRect(float x, float y, float w, float h, boolean active) {
        if (active) {
            shapeRenderer.setColor(accent);
        } else {
            shapeRenderer.setColor(1f, 1f, 1f, 0.7f);
        }
        shapeRenderer.rect(x, y, w, h);
    }

    private void drawFill(float x, float y, float w, float h, boolean active) {
        if (active) {
            float alpha = 0.22f + 0.10f * (0.5f + 0.5f * MathUtils.sin(menuAnimTime * 6f));
            shapeRenderer.setColor(accent.r, accent.g, accent.b, alpha);
        } else {
            shapeRenderer.setColor(0f, 0f, 0f, 0.34f);
        }
        shapeRenderer.rect(x, y, w, h);
    }

    private int getPointerSelection(float btnX, float playY, float settingsY,
                                    float exitY, float btnWidth, float btnHeight) {
        pointer.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(pointer);

        float x = pointer.x;
        float y = pointer.y;

        if (inside(x, y, btnX, playY, btnWidth, btnHeight)) return 0;
        if (inside(x, y, btnX, settingsY, btnWidth, btnHeight)) return 1;
        if (inside(x, y, btnX, exitY, btnWidth, btnHeight)) return 2;
        return -1;
    }

    private boolean inside(float x, float y, float bx, float by, float bw, float bh) {
        return x >= bx && x <= bx + bw && y >= by && y <= by + bh;
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
