package io.github.pkgde;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;

/**
 * The main entry point for the LibGDX game.
 * Handles the initial window configuration and sets the starting screen.
 */
public class Main extends Game {

    public static AssetManager assets;
    private final Runnable startupReadyCallback;

    public Main() {
        this(null);
    }

    public Main(Runnable startupReadyCallback) {
        this.startupReadyCallback = startupReadyCallback;
    }

    @Override
    public void create() {
        assets = new AssetManager();
        WindowModeManager.initialize();
        // Force the OS to update window properties, which often helps grab keyboard focus
        // on startup in LWJGL3/Windows environments when running from an IDE.
        // It is standard behavior that launching from an IDE might occasionally require a single
        // physical mouse click on the window, but this issue disappears entirely when exporting the final .jar/.exe.
        Gdx.graphics.setTitle("Dungeon Escape");
        // Launch the initial animated menu
        setScreen(new HomeScreen());
        if (startupReadyCallback != null) {
            Gdx.app.postRunnable(startupReadyCallback);
        }

        // Hack to steal keyboard focus back from the IDE console on startup
        Gdx.app.postRunnable(() -> {
            if (Gdx.graphics.getWidth() > 0 && Gdx.graphics.getHeight() > 0) {
                Gdx.graphics.setWindowedMode(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            }
        });
    }

    @Override
    public void render() {
        WindowModeManager.handleToggleShortcut();
        super.render();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (assets != null) assets.dispose();
    }
}
