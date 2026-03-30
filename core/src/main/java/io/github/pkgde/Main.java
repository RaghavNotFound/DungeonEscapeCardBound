package io.github.pkgde;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

public class Main extends Game
{
    @Override
    public void create()
    {
        Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        setScreen(new HomeScreen());
    }
}
