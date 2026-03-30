package io.github.pkgde;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;

public class GameWorld
{

    private final Player player;
    private final Enemy enemy;

    private final ArrayList<Rectangle> boundaries;

    public GameWorld(MapManager mapManager) {

        boundaries=new ArrayList<>();

        boundaries = mapManager.getCollisionRects();
        player.setBoundaries(boundaries);

        player.getPosition().set(mapManager.getPlayerSpawn());

        if (!mapManager.getEnemySpawns().isEmpty()) {
            enemy.getBounds().setPosition(mapManager.getEnemySpawns().get(0));
        }
    }

    public void update(float delta,OrthographicCamera camera)
    {
        player.update(delta,camera);
        enemy.update(delta,player);

        //ARROW–ENEMY COLLISION
        ArrayList<Arrow> arrows=player.getArrows();

        for (int i=arrows.size()-1;i>=0;i--)
        {
            Arrow arrow=arrows.get(i);

            if (arrow.getBounds().overlaps(enemy.getBounds()))
            {
                arrows.remove(i);
                enemy.takeDamage(1);
            }
        }
    }

    public boolean isPlayerNearEnemy() {
        Vector2 p = player.getPosition();
        Rectangle b = enemy.getBounds();

        Vector2 e = new Vector2(
            b.x + b.width / 2f,
            b.y + b.height / 2f
        );

        return p.dst(e) < 150f;
    }

    public Player getPlayer() { return player; }
    public Enemy getEnemy() { return enemy; }
    public ArrayList<Rectangle> getBoundaries() { return boundaries; }

    public void dispose()
    {
        player.dispose();
        enemy.dispose();
    }
}
