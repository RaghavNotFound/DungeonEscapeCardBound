package io.github.pkgde;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;

public class GameWorld
{
    private final Player player;
    private final Enemy enemy;

    public static final float WORLD_WIDTH = 1280;
    public static final float WORLD_HEIGHT = 720;
    public static final float FLOOR_OFFSET = 120f;

    private final ArrayList<Rectangle> boundaries;

    public GameWorld()
    {
        player = new Player();
        enemy = new Enemy();

        boundaries = new ArrayList<>();

        float t = 10f;

        boundaries.add(new Rectangle(-t,0,t,WORLD_HEIGHT));
        boundaries.add(new Rectangle(WORLD_WIDTH,0,t,WORLD_HEIGHT));
        boundaries.add(new Rectangle(0,0,WORLD_WIDTH,FLOOR_OFFSET));
        boundaries.add(new Rectangle(0,WORLD_HEIGHT,WORLD_WIDTH,t));
    }

    public void update(float delta, OrthographicCamera camera)
    {
        player.update(delta,camera);
        enemy.update(delta,player);

        ArrayList<Arrow> arrows = player.getArrows();

        for (int i = arrows.size()-1;i>=0;i--)
        {
            Arrow arrow = arrows.get(i);

            if (arrow.getBounds().overlaps(enemy.getBounds()))
            {
                arrows.remove(i);
                enemy.takeDamage(1);
            }
        }
    }

    public boolean isPlayerNearEnemy()
    {
        Vector2 p = player.getPos();
        Rectangle b = enemy.getBounds();

        Vector2 e = new Vector2(
            b.x + b.width/2f,
            b.y + b.height/2f
        );

        return p.dst(e) < 150f;
    }

    public ArrayList<Rectangle> getBoundaries() { return boundaries; }
    public Player getPlayer() { return player; }
    public Enemy getEnemy() { return enemy; }

    public void dispose()
    {
        player.dispose();
        enemy.dispose();
    }
}
