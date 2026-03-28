package io.github.pkgde;

    import java.util.ArrayList;
    import com.badlogic.gdx.math.Rectangle;


    import com.badlogic.gdx.Gdx;
    import com.badlogic.gdx.Input;
    import com.badlogic.gdx.Screen;
    import com.badlogic.gdx.graphics.Texture;
    import com.badlogic.gdx.graphics.g2d.SpriteBatch;
    import com.badlogic.gdx.graphics.g2d.BitmapFont;
    import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
    import com.badlogic.gdx.utils.ScreenUtils;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.Vector3;
    import com.badlogic.gdx.graphics.OrthographicCamera;
    import com.badlogic.gdx.utils.viewport.FitViewport;
    import com.badlogic.gdx.utils.viewport.Viewport;

    public class ExplorationScreen implements Screen {

    private SpriteBatch batch;
    private Texture background;
    private Player player;
    private ShapeRenderer shape;
    private BitmapFont font;
    private Enemy enemy;
        private ArrayList<Rectangle> obstacles = new ArrayList<>();

        private ScreenMap currentMap;

        private final SpriteBatch batch;
        private final Texture background;
        private final Player player;
        private final ShapeRenderer shape;
        private final BitmapFont font;

        // ===== CAMERA =====
        private final OrthographicCamera camera;
        private final Viewport viewport;
    private OrthographicCamera camera;
    private Viewport viewport;

    private enum MenuState { NONE, PAUSE, SETTINGS }
    private MenuState menuState = MenuState.NONE;
        private final float WORLD_WIDTH = 800;
        private final float WORLD_HEIGHT = 600;

    private FrameBuffer fbo1, fbo2;
    private ShaderProgram blurH, blurV;
    private Texture pausedBackground;
        // ===== PAUSE =====
        private boolean isPaused = false;

    private boolean needsBlurRefresh = false;
        // ===== SETTINGS OVERLAY =====
        private final SettingsOverlay settings;

    private SettingsOverlay settingsOverlay;

    private int pauseSelected = 0;

    public ExplorationScreen() {

        batch = new SpriteBatch();
        background = new Texture("background.png");
        player = new Player();
        enemy = new Enemy();
        shape = new ShapeRenderer();
        font = new BitmapFont();
        public ExplorationScreen() {
            batch = new SpriteBatch();
            background = new Texture("background.png");
            player = new Player();
            shape = new ShapeRenderer();
            font = new BitmapFont();

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
            obstacles.add(new Rectangle(0, 0, Gdx.graphics.getWidth(), 300));

            camera = new OrthographicCamera();
            viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);

        viewport.apply(true);
        camera.position.set(
            viewport.getWorldWidth() / 2f,
            viewport.getWorldHeight() / 2f,
            0
        );
        camera.update();
            viewport.apply(true);
            camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);
            camera.update();

        createFBOs(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        blurH = BlurShader.createShader(true);
        blurV = BlurShader.createShader(false);

        settingsOverlay = new SettingsOverlay();
    }

    private void createFBOs(int w, int h) {
        fbo1 = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        fbo2 = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
    }

    private void captureAndBlur(float w, float h) {

        fbo1.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(background, 0, 0, w, h);
        player.render(batch);
        enemy.render(batch); // ✅ ADDED
        batch.end();

        fbo1.end();

        for (int i = 0; i < 3; i++) {

            batch.setShader(blurH);
            batch.begin();
            blurH.setUniformf("blur", 3f / w);

            fbo2.begin();
            batch.draw(fbo1.getColorBufferTexture(),
                0, 0, w, h,
                0, 1, 1, -1);
            batch.end();
            fbo2.end();

            batch.setShader(blurV);
            batch.begin();
            blurV.setUniformf("blur", 3f / h);

            fbo1.begin();
            batch.draw(fbo2.getColorBufferTexture(),
                0, 0, w, h,
                0, 1, 1, -1);
            batch.end();
            fbo1.end();
        }

        batch.setShader(null);
        pausedBackground = fbo1.getColorBufferTexture();
    }
            settings = new SettingsOverlay();
        }

        @Override
        public void render(float delta) {

            viewport.apply();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();

            ScreenUtils.clear(0, 0, 0, 1);

            batch.setProjectionMatrix(camera.combined);
            shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        shape.setProjectionMatrix(camera.combined);

        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();

        // refresh blur after resize
        if (needsBlurRefresh && menuState != MenuState.NONE) {
            captureAndBlur(w, h);
            needsBlurRefresh = false;
        }

        // ESC
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            // ===== ESC LOGIC =====
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {

                if (settings.isActive()) {
                    settings.hide();
                } else {
                    isPaused = !isPaused;
                }
            }

            // ===== SETTINGS INPUT =====
            if (settings.isActive()) {
                settings.handleInput(viewport);
            }
            if (menuState == MenuState.NONE) {
                menuState = MenuState.PAUSE;
                pauseSelected = 0;
                captureAndBlur(w, h);
            }
            else if (menuState == MenuState.SETTINGS) {
                menuState = MenuState.PAUSE;
                settingsOverlay.hide();
            }
            else {
                menuState = MenuState.NONE;
            }
        }

        // ✅ UPDATED GAME LOGIC
        if (menuState == MenuState.NONE) {
            player.update(delta);
            enemy.update(delta, player);

            // 🔥 COMBAT TRIGGER
            if (enemy.getBounds().overlaps(player.bounds)) {
                System.out.println("COMBAT TRIGGERED");
            }
        }
            // ===== GAME UPDATE =====
            if (!isPaused && !settings.isActive()) {
                player.update(delta, camera, obstacles);
            }

        // draw
        if (menuState == MenuState.NONE) {
            batch.begin();
            batch.draw(background, 0, 0, w, h);
            player.render(batch);
            enemy.render(batch);
            batch.end();
        } else {
            batch.begin();
            batch.draw(pausedBackground,
                0, 0, w, h,
                0, 1, 1, -1);
            batch.end();
        }

        float btnW = w * 0.22f;
        float btnH = h * 0.08f;
        float gap = h * 0.03f;

        float pauseX = w / 2f - btnW / 2f;

        float baseY = h * 0.55f;
            // ===== DRAW GAME =====
            batch.begin();
            batch.draw(background, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
            player.render(batch);
            batch.end();

            // ===== DEBUG (optional) =====
            shape.begin(ShapeRenderer.ShapeType.Line);
        float totalHeight = (btnH * 3) + (gap * 2);
        float startY = h / 2f + totalHeight / 2f;

            for (Rectangle rect : obstacles) {
                shape.rect(rect.x, rect.y, rect.width, rect.height);
            }

            shape.end();
            shape.begin(ShapeRenderer.ShapeType.Line);
            shape.rect(player.bounds.x, player.bounds.y, player.bounds.width, player.bounds.height);
            shape.end();
        float y1 = baseY;
        float y1 = startY - btnH;
        float y2 = y1 - btnH - gap;
        float y3 = y2 - btnH - gap;

            // ===== PAUSE MENU INPUT =====
            // Pause buttons
            float width = 250;
            float height = 70;
            float continueX = 275;
            float continueY = 300;
            float settingsX = 275;
            float settingsY = 200;
            float menuX = 275;
            float menuY = 100;
            if (isPaused && !settings.isActive() && Gdx.input.justTouched()) {
        // ===== INPUT =====
        if (menuState == MenuState.SETTINGS) {
            settingsOverlay.handleInput(viewport);
        float sy1 = baseY;
        float sy2 = sy1 - btnH - gap;
        float sy3 = sy2 - btnH - gap;
        float sy4 = sy3 - btnH - gap;

        // ✅ SINGLE INPUT SYSTEM (FIXED)
        if (Gdx.input.justTouched()) {

            Vector3 touch = viewport.unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                var touch = viewport.unproject(
                    new com.badlogic.gdx.math.Vector3(
                        Gdx.input.getX(), Gdx.input.getY(), 0));

                float x = touch.x;
                float y = touch.y;

            // SETTINGS
            if (menuState == MenuState.SETTINGS) {

                if (x >= settingsX && x <= settingsX + btnW &&
                    y >= sy1 && y <= sy1 + btnH) {
                    Gdx.graphics.setWindowedMode(800, 600);
                    needsBlurRefresh = true;
                    return;
                }

                if (x >= settingsX && x <= settingsX + btnW &&
                    y >= sy2 && y <= sy2 + btnH) {
                    Gdx.graphics.setWindowedMode(1280, 720);
                    needsBlurRefresh = true;
                    return;
                }

                if (x >= settingsX && x <= settingsX + btnW &&
                    y >= sy3 && y <= sy3 + btnH) {
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                    needsBlurRefresh = true;
                    return;
                }
                // Continue
                if (x >= continueX && x <= continueX + width &&
                    y >= continueY && y <= continueY + height) {

                    isPaused = false;
                }
                if (x >= settingsX && x <= settingsX + btnW &&
                    y >= sy4 && y <= sy4 + btnH) {
                    menuState = MenuState.PAUSE;
                    return;
                }
            }

                // Settings
                if (x >= settingsX && x <= settingsX + width &&
                    y >= settingsY && y <= settingsY + height) {
            // PAUSE
            if (!settingsOverlay.isActive()) {
                menuState = MenuState.PAUSE;
            }
        }
        else if (menuState == MenuState.PAUSE) {

            // KEYBOARD
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP))
                pauseSelected = (pauseSelected + 2) % 3;
            if (menuState == MenuState.SETTINGS) {

            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
                pauseSelected = (pauseSelected + 1) % 3;

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                applyPauseSelection();
            }

            // MOUSE
            if (Gdx.input.justTouched()) {

                var touch = viewport.unproject(
                    new com.badlogic.gdx.math.Vector3(
                        Gdx.input.getX(), Gdx.input.getY(), 0));

                float x = touch.x;
                float y = touch.y;
            if (menuState == MenuState.PAUSE) {

                    settings.show();
                }
                if (x >= pauseX && x <= pauseX + btnW &&
                    y >= y1 && y <= y1 + btnH) {
                    pauseSelected = 0;
                }
                else if (x >= pauseX && x <= pauseX + btnW &&

                // Main Menu
                if (x >= menuX && x <= menuX + width &&
                    y >= menuY && y <= menuY + height) {
                if (x >= pauseX && x <= pauseX + btnW &&
                    y >= y2 && y <= y2 + btnH) {
                    pauseSelected = 1;
                }
                else if (x >= pauseX && x <= pauseX + btnW &&

                    ((Main) Gdx.app.getApplicationListener())
                        .setScreen(new HomeScreen());
                }
            }

            // ===== DRAW PAUSE OVERLAY =====
            if (isPaused) {
                if (x >= pauseX && x <= pauseX + btnW &&
                    y >= y3 && y <= y3 + btnH) {
                    pauseSelected = 2;
                }

                applyPauseSelection();
            }
        }

        // UPDATE
        if (menuState == MenuState.NONE) {
            player.update(delta);
        }

        // DRAW GAME / BLUR
        if (menuState == MenuState.NONE) {
            batch.begin();
            batch.draw(background, 0, 0, w, h);
            player.render(batch);
            batch.end();
        } else {
            batch.begin();
            batch.draw(pausedBackground,
                0, 0, w, h,
                0, 1, 1, -1);
            batch.end();
        }

                shape.begin(ShapeRenderer.ShapeType.Filled);
        // DRAW PAUSE
        // ===== DRAW PAUSE =====

        if (menuState == MenuState.PAUSE || menuState == MenuState.SETTINGS) {

                shape.setColor(0, 0, 0, 0.7f);
                shape.rect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);

                shape.setColor(1, 1, 1, 1);

                shape.rect(continueX, continueY, width, height);
                shape.rect(settingsX, settingsY, width, height);
                shape.rect(menuX, menuY, width, height);

                shape.end();
            shape.begin(ShapeRenderer.ShapeType.Line);

            shape.setColor(pauseSelected == 0 ? 1 : 0.5f, 1, 1, 1);
            shape.rect(pauseX, y1, btnW, btnH);

            shape.setColor(pauseSelected == 1 ? 1 : 0.5f, 1, 1, 1);
            shape.rect(pauseX, y2, btnW, btnH);

            shape.setColor(pauseSelected == 2 ? 1 : 0.5f, 1, 1, 1);
            shape.rect(pauseX, y3, btnW, btnH);

            shape.end();

                batch.begin();

            float scale = w / 800f;
            font.getData().setScale(scale * 1.2f);

            font.draw(batch, "GAME PAUSED", w / 2f - 90 * scale, startY + btnH);

            font.draw(batch, "CONTINUE", pauseX + btnW * 0.25f, y1 + btnH * 0.65f);
            font.draw(batch, "SETTINGS", pauseX + btnW * 0.25f, y2 + btnH * 0.65f);
            font.draw(batch, "MAIN MENU", pauseX + btnW * 0.2f, y3 + btnH * 0.65f);
                font.draw(batch, "GAME PAUSED", 310, 450);
                font.draw(batch, "CONTINUE", continueX + 60, continueY + 45);
                font.draw(batch, "SETTINGS", settingsX + 65, settingsY + 45);
                font.draw(batch, "MAIN MENU", menuX + 55, menuY + 45);
            font.draw(batch, "MAIN MENU", pauseX + btnW * 0.20f, y3 + btnH * 0.65f);

                batch.end();
            }

            // ===== SETTINGS OVERLAY =====
            settings.render(shape, batch, font);
        }
        // DRAW SETTINGS
        if (menuState == MenuState.SETTINGS) {
            settingsOverlay.render(shape, batch, font, viewport);
        }
    }

    private void applyPauseSelection() {

        if (pauseSelected == 0) {
            menuState = MenuState.NONE;
        }
        else if (pauseSelected == 1) {
            menuState = MenuState.SETTINGS;
            settingsOverlay.show();
        }
        else if (pauseSelected == 2) {
            ((Main) Gdx.app.getApplicationListener())
                .setScreen(new HomeScreen());
        }
    }

        @Override
        public void resize(int width, int height) {
            viewport.update(width, height, true);
        }
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);

        if (fbo1 != null) fbo1.dispose();
        if (fbo2 != null) fbo2.dispose();

        createFBOs(width, height);
        needsBlurRefresh = true;
    }

        @Override public void show() {}
        @Override public void pause() {}
        @Override public void resume() {}
        @Override public void hide() {}

        @Override
        public void dispose() {
            batch.dispose();
            background.dispose();
            player.dispose();
            shape.dispose();
            font.dispose();
        }
    }
    @Override
    public void dispose() {
        batch.dispose();
        background.dispose();
        player.dispose();
        enemy.dispose(); // ✅ ADDED
        shape.dispose();
        font.dispose();
        fbo1.dispose();
        fbo2.dispose();
        blurH.dispose();
        blurV.dispose();
    }
}
