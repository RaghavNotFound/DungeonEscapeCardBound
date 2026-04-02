package io.github.pkgde;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.Viewport;

public class VictoryOverlay {

    private final GlyphLayout glyphLayout = new GlyphLayout();

    public void render(ShapeRenderer shape, SpriteBatch batch, BitmapFont font, Viewport viewport) {
        float worldW = viewport.getWorldWidth();
        float worldH = viewport.getWorldHeight();
        float cameraX = viewport.getCamera().position.x;
        float cameraY = viewport.getCamera().position.y;

        // Dim background with a celebratory color
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.8f, 0.6f, 0.1f, 0.6f);
        shape.rect(cameraX - worldW / 2f, cameraY - worldH / 2f, worldW, worldH);
        shape.end();

        batch.begin();
        font.setColor(Color.WHITE);

        // Draw "VICTORY ACHIEVED"
        font.getData().setScale(3f);
        glyphLayout.setText(font, "VICTORY ACHIEVED");
        font.draw(batch, glyphLayout, cameraX - glyphLayout.width / 2f, cameraY + 70f);

        // Draw subtitle
        font.getData().setScale(1.5f);
        glyphLayout.setText(font, "Press ESCAPE to return to Menu");
        font.draw(batch, glyphLayout, cameraX - glyphLayout.width / 2f, cameraY - 10f);

        // Reset font scale and color for other UI elements
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }
}