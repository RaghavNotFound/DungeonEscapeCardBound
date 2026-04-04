package io.github.pkgde;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.assets.AssetManager;

/**
 * The main entry point for the LibGDX game.
 * Handles the initial window configuration and sets the starting screen.
 */
public class Main extends Game {

    public static AssetManager assets;

    @Override
    public void create() {
        assets = new AssetManager();
        // Launch the initial animated menu
        setScreen(new HomeScreen());
    }

    @Override
    public void dispose() {
        super.dispose();
        if (assets != null) assets.dispose();
    }
}
