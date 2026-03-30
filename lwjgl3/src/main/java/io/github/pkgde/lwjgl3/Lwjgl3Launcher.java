package io.github.pkgde.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.github.pkgde.Main;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher
{
    public static void main(String[] args)
    {
        if (StartupHelper.startNewJvmIfRequired()) return;
        createApplication();
    }

    private static void createApplication()
    {
        new Lwjgl3Application(new Main(),getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration()
    {
        Lwjgl3ApplicationConfiguration configuration=new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("DungeonEscapeCardbound");
        configuration.useVsync(true);
        configuration.setForegroundFPS(
            Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate+1
        );

        var mode=Lwjgl3ApplicationConfiguration.getDisplayMode();
        configuration.setWindowedMode(mode.width,mode.height);
        configuration.setDecorated(true);   // keeps window frame
        configuration.setMaximized(true);   // fills screen like fullscreen
        configuration.setForegroundFPS(60);
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
