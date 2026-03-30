package io.github.pkgde;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;

public class Main extends Game
{
    @Override
    public void create() {

        Graphics.DisplayMode mode = Gdx.graphics.getDisplayMode();

        Gdx.graphics.setWindowedMode(mode.width, mode.height);

        setScreen(new HomeScreen());
    }
}
