package io.github.pkgde.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import io.github.pkgde.Main;
import java.util.Locale;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.windows.User32;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return;
        StartupLoadingPopup startupLoadingPopup = StartupLoadingPopup.show();
        try {
            createApplication(startupLoadingPopup);
        } catch (RuntimeException runtimeException) {
            startupLoadingPopup.close();
            throw runtimeException;
        }
    }

    private static Lwjgl3Application createApplication(StartupLoadingPopup startupLoadingPopup) {
        return new Lwjgl3Application(
            new Main(() -> onGameReady(startupLoadingPopup)),
            getDefaultConfiguration()
        );
    }

    private static void onGameReady(StartupLoadingPopup startupLoadingPopup) {
        startupLoadingPopup.close();


        
        // Wait a frame so the OS finishes drawing the window, then steal focus.
        // This fixes the issue where the loading popup or IDE steals focus
        // back right as the main game window becomes visible!
        Gdx.app.postRunnable(() -> {
            focusGameWindow(false);
            Gdx.app.postRunnable(() -> focusGameWindow(true));
        });
    }

    private static void focusGameWindow(boolean requestAttentionIfNeeded) {
        if (!(Gdx.graphics instanceof Lwjgl3Graphics lwjgl3Graphics)) {
            return;
        }

        var window = lwjgl3Graphics.getWindow();
        window.setVisible(true);
        if (window.isIconified()) {
            window.restoreWindow();
        }

        forceWindowToFront(window.getWindowHandle());
        window.focusWindow();

        if (requestAttentionIfNeeded && !window.isFocused()) {
            window.flash();
            forceWindowToFront(window.getWindowHandle());
            window.focusWindow();
        }
    }

    private static void forceWindowToFront(long glfwWindowHandle) {
        if (!isWindows()) {
            return;
        }

        long hwnd = GLFWNativeWin32.glfwGetWin32Window(glfwWindowHandle);
        if (hwnd == 0L) {
            return;
        }

        int activateFlags = User32.SWP_NOMOVE | User32.SWP_NOSIZE | User32.SWP_SHOWWINDOW;

        User32.ShowWindow(hwnd, User32.SW_RESTORE);
        User32.SetWindowPos(null, hwnd, User32.HWND_TOPMOST, 0, 0, 0, 0, activateFlags);
        User32.BringWindowToTop(hwnd);
        User32.SetWindowPos(null, hwnd, User32.HWND_NOTOPMOST, 0, 0, 0, 0, activateFlags);
        User32.BringWindowToTop(hwnd);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        Graphics.DisplayMode displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode();

        configuration.setTitle("DungeonEscapeCardbound");
        configuration.setInitialVisible(false);

        // Performance & Sync (Integrated from File 2: Matches monitor refresh rate)
        configuration.useVsync(true);
        configuration.setForegroundFPS(
            displayMode.refreshRate + 1
        );

        // Borderless fullscreen: use monitor size in an undecorated window.
        configuration.setWindowedMode(displayMode.width, displayMode.height);
        configuration.setWindowPosition(0, 0);
        configuration.setDecorated(false);
        configuration.setMaximized(false);
        configuration.setResizable(false);

        // Icons
        configuration.setWindowIcon(
            "libgdx128.png",
            "libgdx64.png",
            "libgdx32.png",
            "libgdx16.png"
        );

        return configuration;
    }
}
