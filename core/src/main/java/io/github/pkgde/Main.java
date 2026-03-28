package io.github.pkgde;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

public class Main extends Game {

    @Override
    public void create() {

        // 🔥 DO NOT FORCE FULLSCREEN (debugging nightmare)
        Gdx.graphics.setWindowedMode(1280, 720);

        setScreen(new HomeScreen());
    }
}
