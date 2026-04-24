package io.github.pkgde.lwjgl3;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

final class StartupLoadingPopup {

    private static final StartupLoadingPopup NO_OP = new StartupLoadingPopup();

    private final JWindow window;
    private final Timer dotTimer;

    private StartupLoadingPopup() {
        this.window = null;
        this.dotTimer = null;
    }

    private StartupLoadingPopup(String title, String subtitle) {
        window = new JWindow();
        window.setAlwaysOnTop(true);

        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(64, 196, 255), 2),
            BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));
        root.setBackground(new Color(17, 19, 24));
        root.setPreferredSize(new Dimension(420, 190));

        JPanel textPanel = new JPanel(new GridLayout(0, 1, 0, 8));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel(subtitle, SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Dialog", Font.PLAIN, 15));
        subtitleLabel.setForeground(new Color(170, 180, 196));

        JLabel statusLabel = new JLabel("Loading world", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        statusLabel.setForeground(new Color(64, 196, 255));

        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);
        textPanel.add(statusLabel);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setBorderPainted(false);
        progressBar.setBackground(new Color(33, 38, 48));
        progressBar.setForeground(new Color(64, 196, 255));
        progressBar.setPreferredSize(new Dimension(0, 12));

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.add(progressBar);

        root.add(textPanel, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        window.setContentPane(root);
        window.pack();
        window.setLocationRelativeTo(null);

        final int[] dotCount = {0};
        dotTimer = new Timer(350, event -> {
            StringBuilder text = new StringBuilder("Loading world");
            for (int i = 0; i < dotCount[0]; i++) {
                text.append('.');
            }
            statusLabel.setText(text.toString());
            dotCount[0] = (dotCount[0] + 1) % 4;
        });
        dotTimer.setInitialDelay(0);
        dotTimer.start();

        window.setVisible(true);
    }

    static StartupLoadingPopup show() {
        if (GraphicsEnvironment.isHeadless()) {
            return NO_OP;
        }

        AtomicReference<StartupLoadingPopup> popupRef = new AtomicReference<>(NO_OP);
        Runnable createPopup = () ->
            popupRef.set(new StartupLoadingPopup(
                "Dungeon Escape",
                "Preparing the main menu and game window"
            ));

        try {
            if (SwingUtilities.isEventDispatchThread()) {
                createPopup.run();
            } else {
                SwingUtilities.invokeAndWait(createPopup);
            }
            return popupRef.get();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException ignored) {
        }

        return NO_OP;
    }

    void close() {
        if (window == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (dotTimer != null) {
                dotTimer.stop();
            }
            window.setVisible(false);
            window.dispose();
        });
    }
}
