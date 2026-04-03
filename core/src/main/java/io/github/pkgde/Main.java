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
        // Fetch the current monitor's display mode to determine resolution
        Graphics.DisplayMode mode = Gdx.graphics.getDisplayMode();

        // Set the game to a borderless-style windowed mode that matches monitor resolution
        Gdx.graphics.setWindowedMode(mode.width, mode.height);

        // NOTE: For true exclusive fullscreen in the future, use:
        // Gdx.graphics.setFullscreenMode(mode);

        // Launch the initial animated menu
        setScreen(new HomeScreen());
    }
}
