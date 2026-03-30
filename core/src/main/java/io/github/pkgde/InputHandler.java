package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.viewport.Viewport;

public class InputHandler
{

    public enum Action
    {
        NONE,
        TOGGLE_PAUSE,
        OPEN_SETTINGS,
        EXIT_TO_MENU
    }

    public InputHandler(Viewport viewport) {}

    public Action handle()
    {

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
        {
            return Action.TOGGLE_PAUSE;
        }
        return Action.NONE;
    }
}
