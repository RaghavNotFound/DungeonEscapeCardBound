package io.github.pkgde;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import java.lang.reflect.InvocationTargetException;

public final class WindowModeManager {

    public static final int DEFAULT_WINDOW_WIDTH = 1280;
    public static final int DEFAULT_WINDOW_HEIGHT = 720;

    private enum WindowMode {
        WINDOWED,
        BORDERLESS_FULLSCREEN,
        EXCLUSIVE_FULLSCREEN
    }

    private static WindowMode currentMode = WindowMode.WINDOWED;
    private static int windowedWidth = DEFAULT_WINDOW_WIDTH;
    private static int windowedHeight = DEFAULT_WINDOW_HEIGHT;

    private WindowModeManager() {}

    public static void initialize() {
        if (!isDesktop()) {
            return;
        }

        Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
        int currentWidth = Gdx.graphics.getWidth();
        int currentHeight = Gdx.graphics.getHeight();

        if (
            currentWidth > 0 &&
            currentHeight > 0 &&
            (currentWidth != displayMode.width || currentHeight != displayMode.height)
        ) {
            windowedWidth = currentWidth;
            windowedHeight = currentHeight;
        }

        if (isFullscreen()) {
            currentMode = WindowMode.EXCLUSIVE_FULLSCREEN;
        } else if (currentWidth == displayMode.width && currentHeight == displayMode.height) {
            currentMode = WindowMode.BORDERLESS_FULLSCREEN;
        } else {
            currentMode = WindowMode.WINDOWED;
        }
    }

    public static void handleToggleShortcut() {
        if (isDesktop() && Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            toggleBorderlessFullscreen();
        }
    }

    public static void toggleBorderlessFullscreen() {
        if (!isDesktop()) {
            return;
        }

        if (currentMode == WindowMode.WINDOWED) {
            applyBorderlessFullscreen();
        } else {
            applyWindowedMode(windowedWidth, windowedHeight);
        }
    }

    public static void applyWindowedMode(int width, int height) {
        if (!isDesktop()) {
            return;
        }

        windowedWidth = width;
        windowedHeight = height;
        currentMode = WindowMode.WINDOWED;

        Gdx.graphics.setUndecorated(false);
        Gdx.graphics.setWindowedMode(width, height);
        centerWindow(width, height);
    }

    public static void applyBorderlessFullscreen() {
        if (!isDesktop()) {
            return;
        }

        Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
        currentMode = WindowMode.BORDERLESS_FULLSCREEN;

        Gdx.graphics.setUndecorated(true);
        Gdx.graphics.setWindowedMode(displayMode.width, displayMode.height);
        setWindowPosition(0, 0);
    }

    public static void applyExclusiveFullscreen() {
        if (!isDesktop()) {
            return;
        }

        currentMode = WindowMode.EXCLUSIVE_FULLSCREEN;
        Gdx.graphics.setUndecorated(false);
        Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
    }

    private static void centerWindow(int width, int height) {
        Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
        int centeredX = Math.max(0, (displayMode.width - width) / 2);
        int centeredY = Math.max(0, (displayMode.height - height) / 2);
        setWindowPosition(centeredX, centeredY);
    }

    private static void setWindowPosition(int x, int y) {
        try {
            Gdx.graphics
                .getClass()
                .getMethod("setWindowPosition", int.class, int.class)
                .invoke(Gdx.graphics, x, y);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
        }
    }

    private static boolean isFullscreen() {
        try {
            return (boolean) Gdx.graphics.getClass().getMethod("isFullscreen").invoke(Gdx.graphics);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
            return false;
        }
    }

    private static boolean isDesktop() {
        return Gdx.app != null && Gdx.app.getType() == Application.ApplicationType.Desktop;
    }
}
