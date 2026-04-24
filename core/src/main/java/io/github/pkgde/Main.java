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
        // Launch the initial animated menu
        setScreen(new HomeScreen());
        if (startupReadyCallback != null) {
            Gdx.app.postRunnable(startupReadyCallback);
        }
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
