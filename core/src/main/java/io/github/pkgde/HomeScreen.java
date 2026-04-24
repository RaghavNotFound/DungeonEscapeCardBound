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
import java.util.ArrayList;
import java.util.List;

public class HomeScreen implements Screen {

    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout glyphLayout;
    private Texture background;

    private OrthographicCamera camera;
    private Viewport viewport;

    private SettingsOverlay settings;
    private SaveLoadOverlay saveLoadOverlay;

    private List<String> availableLabels;
    private List<Integer> availableOptions;

    private static final int OPTION_CONTINUE = 0;
    private static final int OPTION_NEW_GAME = 1;
    private static final int OPTION_LOAD_GAME = 2;
    private static final int OPTION_SETTINGS = 3;
    private static final int OPTION_EXIT = 4;
    private boolean continueEnabled = false;

    private int selectedIndex = 0;
    private float menuAnimTime = 0f;
    private final Vector3 pointer = new Vector3();
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private final Color accent = new Color(0.25f, 0.85f, 1f, 1f);

    private FrameBuffer fbo;
    private ShaderProgram blurShader;
    private SpriteBatch blurBatch;

    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        glyphLayout = new GlyphLayout();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        background = new Texture("HomeScreen/HomeScreen.jpg");
        background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        camera = new OrthographicCamera();
        viewport = new FitViewport(1280, 720, camera);
        viewport.apply(true);
        camera.position.set(viewport.getWorldWidth() / 2f, viewport.getWorldHeight() / 2f, 0);
        camera.update();

        settings = new SettingsOverlay();
        saveLoadOverlay = new SaveLoadOverlay();

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
        fbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        blurShader = BlurShader.createShader(true);
        blurBatch = new SpriteBatch();
        blurBatch.setShader(blurShader);

        updateAvailableOptions();
        selectedIndex = 0;
    }

    private void updateAvailableOptions() {
        continueEnabled = SaveManager.getLatestSave() != null;
        availableLabels = new ArrayList<>();
        availableOptions = new ArrayList<>();

        if (continueEnabled) {
            availableLabels.add("CONTINUE");
            availableOptions.add(OPTION_CONTINUE);
        }

        availableLabels.add("NEW GAME");
        availableOptions.add(OPTION_NEW_GAME);

        if (continueEnabled) {
            availableLabels.add("LOAD GAME");
            availableOptions.add(OPTION_LOAD_GAME);
        }

        availableLabels.add("SETTINGS");
        availableOptions.add(OPTION_SETTINGS);

        availableLabels.add("EXIT");
        availableOptions.add(OPTION_EXIT);

        if (selectedIndex >= availableOptions.size()) {
            selectedIndex = availableOptions.size() - 1;
        }
    }

    @Override
    public void render(float delta) {
        menuAnimTime += delta;

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        float worldW = viewport.getWorldWidth();
        float worldH = viewport.getWorldHeight();
        float btnWidth = worldW * 0.25f;
        float btnHeight = worldH * 0.08f;
        float gap = worldH * 0.03f;
        float btnX = worldW * 0.62f;

        float panelY = worldH * 0.1f;
        float panelHeight = worldH * 0.7f;
        float totalHeight = availableOptions.size() * btnHeight + (availableOptions.size() - 1) * gap;
        float startY = panelY + (panelHeight + totalHeight) / 2f - btnHeight;

        float[] btnYs = new float[availableOptions.size()];
        for (int i = 0; i < availableOptions.size(); i++) {
            btnYs[i] = startY - i * (btnHeight + gap);
        }

        boolean mouseMovedThisFrame = (Gdx.input.getX() != lastMouseX || Gdx.input.getY() != lastMouseY);
        lastMouseX = Gdx.input.getX();
        lastMouseY = Gdx.input.getY();

        Screen currentScreen = ((Game) Gdx.app.getApplicationListener()).getScreen();

        if (settings.isOverlayVisible()) {
            settings.update(delta);
            settings.handleInput(viewport);
        } else if (saveLoadOverlay.isVisible()) {
            SaveLoadOverlay.Result res = saveLoadOverlay.handleInput(viewport);
            if (res != null && res.action == SaveLoadOverlay.ResultAction.LOAD) {
                ((Main) Gdx.app.getApplicationListener()).setScreen(new LoadingScreen(res.saveName));
                this.dispose();
                return;
            } else if (!saveLoadOverlay.isVisible()) {
                updateAvailableOptions();
            }
        } else {
            boolean keyPressed = false;
            if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                selectedIndex = (selectedIndex + availableOptions.size() - 1) % availableOptions.size();
                keyPressed = true;
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                selectedIndex = (selectedIndex + 1) % availableOptions.size();
                keyPressed = true;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                applySelection();
                if (((Game) Gdx.app.getApplicationListener()).getScreen() != currentScreen) return;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                Gdx.app.exit();
                return;
            }

            if (Gdx.input.justTouched()) {
                int clicked = getPointerSelection(btnX, btnWidth, btnHeight, btnYs);
                if (clicked >= 0) {
                    selectedIndex = clicked;
                    applySelection();
                    if (((Game) Gdx.app.getApplicationListener()).getScreen() != currentScreen) return;
                }
            }

            if (mouseMovedThisFrame && !keyPressed) {
                int hovered = getPointerSelection(btnX, btnWidth, btnHeight, btnYs);
                if (hovered >= 0) selectedIndex = hovered;
            }
        }

        // ===== RENDERING =====
        if (settings.isOverlayVisible() || saveLoadOverlay.isVisible()) {
            fbo.begin();
            batch.begin();
            batch.draw(background, 0, 0, worldW, worldH);
            batch.end();
            fbo.end();

            Texture tex = fbo.getColorBufferTexture();
            blurBatch.setProjectionMatrix(camera.combined);
            blurBatch.begin();
            float progress = settings.getTransitionProgress();
            blurShader.setUniformf("blur", progress * 0.002f);
            blurBatch.draw(tex, 0, 0, worldW, worldH, 0, 0, tex.getWidth(), tex.getHeight(), false, true);
            blurBatch.end();

            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, progress * 0.5f);
            shapeRenderer.rect(0, 0, worldW, worldH);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        if (settings.isOverlayVisible()) {
            settings.render(shapeRenderer, batch, font, viewport);
            return;
        }
        if (saveLoadOverlay.isVisible()) {
            saveLoadOverlay.render(shapeRenderer, batch, font, viewport);
            return;
        }

        batch.begin();
        batch.draw(background, 0, 0, worldW, worldH);
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < availableOptions.size(); i++) drawFill(btnX, btnYs[i], btnWidth, btnHeight, selectedIndex == i, true);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < availableOptions.size(); i++) drawRect(btnX, btnYs[i], btnWidth, btnHeight, selectedIndex == i, true);
        shapeRenderer.end();

        batch.begin();
        float scale = worldW / 800f;
        font.getData().setScale(scale * 2.1f);

        float titleX = worldW * 0.06f;
        float titleY = worldH * 0.9f;

        font.setColor(0f, 0f, 0f, 0.7f);
        glyphLayout.setText(font, "DUNGEON ESCAPE");
        font.draw(batch, glyphLayout, titleX + 3f, titleY - 3f);

        font.setColor(1f, 1f, 1f, 1f);
        glyphLayout.setText(font, "DUNGEON ESCAPE");
        font.draw(batch, glyphLayout, titleX, titleY);

        font.getData().setScale(scale * 1.2f);
        for (int i = 0; i < availableOptions.size(); i++) {
            drawButton(batch, font, availableLabels.get(i), btnX, btnYs[i], btnWidth, btnHeight, selectedIndex == i, scale, worldW, true);
        }

        font.setColor(Color.WHITE);
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawButton(SpriteBatch batch, BitmapFont font, String text, float x, float y, float w, float h, boolean active, float scale, float worldW, boolean enabled) {
        float pulse = active ? (0.02f * MathUtils.sin(menuAnimTime * 6f)) : 0f;
        float s = active ? 1.07f + pulse : 1f;

        font.getData().setScale(scale * 1.15f * s);

        float shadowOffset = Math.max(1.5f, worldW * 0.0013f);

        font.setColor(0f, 0f, 0f, enabled ? 0.75f : 0.4f);
        glyphLayout.setText(font, text);
        float textX = x + (w - glyphLayout.width) * 0.5f;
        float textY = y + (h + glyphLayout.height) * 0.5f;
        font.draw(batch, glyphLayout, textX + shadowOffset, textY - shadowOffset);

        Color c = active ? accent : Color.WHITE;
        font.setColor(c.r, c.g, c.b, enabled ? 1f : 0.5f);
        glyphLayout.setText(font, text);
        font.draw(batch, glyphLayout, textX, textY);
    }

    private void drawRect(float x, float y, float w, float h, boolean active, boolean enabled) {
        float alpha = enabled ? 1f : 0.4f;
        if (active) shapeRenderer.setColor(accent.r, accent.g, accent.b, alpha);
        else shapeRenderer.setColor(1f, 1f, 1f, 0.7f * alpha);
        shapeRenderer.rect(x, y, w, h);
    }

    private void drawFill(float x, float y, float w, float h, boolean active, boolean enabled) {
        float alpha = enabled ? 1f : 0.4f;
        if (active) {
            float pulseAlpha = 0.22f + 0.10f * (0.5f + 0.5f * MathUtils.sin(menuAnimTime * 6f));
            shapeRenderer.setColor(accent.r, accent.g, accent.b, pulseAlpha * alpha);
        } else shapeRenderer.setColor(0f, 0f, 0f, 0.34f * alpha);
        shapeRenderer.rect(x, y, w, h);
    }

    private int getPointerSelection(float btnX, float btnWidth, float btnHeight, float[] ys) {
        pointer.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(pointer);
        for (int i = 0; i < ys.length; i++) {
            if (pointer.x >= btnX && pointer.x <= btnX + btnWidth && pointer.y >= ys[i] && pointer.y <= ys[i] + btnHeight) return i;
        }
        return -1;
    }

    private void applySelection() {
        int selectedOption = availableOptions.get(selectedIndex);
        Main main = (Main) Gdx.app.getApplicationListener();
        switch (selectedOption) {
            case OPTION_CONTINUE:
                if (continueEnabled) {
                    main.setScreen(new LoadingScreen(SaveManager.getLatestSave()));
                    this.dispose();
                }
                break;
            case OPTION_NEW_GAME:
                main.setScreen(new LoadingScreen(null));
                this.dispose();
                break;
            case OPTION_LOAD_GAME:
                saveLoadOverlay.show(SaveLoadOverlay.Mode.LOAD);
                break;
            case OPTION_SETTINGS:
                settings.show();
                break;
            case OPTION_EXIT:
                Gdx.app.exit();
                break;
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
        fbo.dispose();
        blurBatch.dispose();
        blurShader.dispose();
    }
}
