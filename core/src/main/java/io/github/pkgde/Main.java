package io.github.pkgde;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;

public class Main extends Game {

    @Override
    public void create() {
<<<<<<< HEAD

        Graphics.DisplayMode mode = Gdx.graphics.getDisplayMode();

        Gdx.graphics.setWindowedMode(mode.width, mode.height);
=======
        // Fetch the current display mode
        Graphics.DisplayMode mode = Gdx.graphics.getDisplayMode();

        // Set to borderless windowed fullscreen (matches monitor resolution)
        Gdx.graphics.setFullscreenMode(mode);

        // Alternatively, for true exclusive fullscreen, you would use:
        // Gdx.graphics.setFullscreenMode(mode);
>>>>>>> 49f371b632c2e7533ec48991d6c1ff1c20c2baf7

        setScreen(new HomeScreen());
    }
}
