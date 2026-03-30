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
        TOGGLE_INVENTORY,
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            return Action.OPEN_SETTINGS;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            return Action.TOGGLE_INVENTORY;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            return Action.EXIT_TO_MENU;
        }

        return Action.NONE;
    }
}
