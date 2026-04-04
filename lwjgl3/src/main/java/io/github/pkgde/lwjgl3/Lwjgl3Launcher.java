package io.github.pkgde.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.github.pkgde.Main;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return;
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new Main(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();

        configuration.setTitle("DungeonEscapeCardbound");

        // Performance & Sync
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);

        // Window Configuration
        // Sets a sensible default window size (1280x720) for the native aspect ratio
        configuration.setWindowedMode(1280, 720);

        configuration.setDecorated(true);
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
