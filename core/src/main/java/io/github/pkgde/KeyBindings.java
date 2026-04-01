package io.github.pkgde;

import com.badlogic.gdx.Input;

public class KeyBindings {

    public static int MOVE_UP = Input.Keys.W;
    public static int MOVE_DOWN = Input.Keys.S;
    public static int MOVE_LEFT = Input.Keys.A;
    public static int MOVE_RIGHT = Input.Keys.D;

    public static int RUN = Input.Keys.SHIFT_LEFT;

    public static String getKeyName(int key) {
        return Input.Keys.toString(key);
    }
}
