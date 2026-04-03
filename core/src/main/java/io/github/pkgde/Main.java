package io.github.pkgde;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;

public class Main extends Game {

    @Override
    public void create() {
        // Fetch the current display mode
        Graphics.DisplayMode mode = Gdx.graphics.getDisplayMode();

        // Set to borderless windowed fullscreen (matches monitor resolution)
        Gdx.graphics.setWindowedMode(mode.width, mode.height);

        // Alternatively, for true exclusive fullscreen, you would use:
        // Gdx.graphics.setFullscreenMode(mode);

        setScreen(new HomeScreen());
    }
}
