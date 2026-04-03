package io.github.pkgde;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;

/**
 * The main entry point for the LibGDX game.
 * Handles the initial window configuration and sets the starting screen.
 */
public class Main extends Game {

    @Override
    public void create() {
        // Set to Borderless Fullscreen (Matches SettingsOverlay logic exactly)
        Gdx.graphics.setUndecorated(true);
        Graphics.DisplayMode mode = Gdx.graphics.getDisplayMode();
        Gdx.graphics.setFullscreenMode(mode);

        // Launch the initial animated menu
        setScreen(new HomeScreen());
    }
}
