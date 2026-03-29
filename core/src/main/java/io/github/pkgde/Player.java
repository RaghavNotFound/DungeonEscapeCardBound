package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;
import java.util.ArrayList;

public class Player
{

    private final Animation<TextureRegion> walkAnimation;
    private final Animation<TextureRegion> runAnimation;
    private final Animation<TextureRegion> idleAnimation;
    private final Animation<TextureRegion> idleBlinkingAnimation;

    private final TextureRegion[] walkFrames;
    private final TextureRegion[] runFrames;
    private final TextureRegion[] idleFrames;
    private final TextureRegion[] idleBlinkingFrames;

    private final Texture[] walkingTextures;
    private final Texture[] runTextures;
    private final Texture[] idleTextures;
    private final Texture[] idleBlinkingTextures;

    private TextureRegion currentFrame;

    private float stateTime;
    private boolean isRunning;
    private boolean facingRight=true;
    private boolean playBlink=false;

    private final Vector2 pos;
    public Rectangle bounds;

    private final int WIDTH=128;
    private final int HEIGHT=128;

    //ARROW SYSTEM
    private final ArrayList<Arrow> arrows=new ArrayList<>();

    private float shootTimer=0f;

    //STAMINA SYSTEM
    private float stamina=100f;
    private float maxStamina=100f;

    private float staminaDrain=30f;
    private float staminaRegen=15f;

    private boolean canRun=true;

    // WORLD REFERENCE
    private final GameWorld world;

    public Player(GameWorld world)
    {
        this.world=world;
        pos =new Vector2(200,200);
        bounds=new Rectangle(pos.x, pos.y,WIDTH,HEIGHT);

        int walkingFrameCount=23;
        int runFrameCount=12;
        int idleFrameCount=18;
        int idleBlinkingFrameCount=18;

        walkingTextures=new Texture[walkingFrameCount];
        runTextures=new Texture[runFrameCount];
        idleTextures=new Texture[idleFrameCount];
        idleBlinkingTextures=new Texture[idleBlinkingFrameCount];

        walkFrames=new TextureRegion[walkingFrameCount];
        runFrames=new TextureRegion[runFrameCount];
        idleFrames=new TextureRegion[idleFrameCount];
        idleBlinkingFrames=new TextureRegion[idleBlinkingFrameCount];

        //WALK
        for (int i=0;i<walkingFrameCount;i++)
        {
            walkingTextures[i]=new Texture("Movements/Player/walking/walking_"+(i+1)+".png");
            walkFrames[i]=new TextureRegion(walkingTextures[i]);
        }
        //RUN
        for (int i=0;i<runFrameCount;i++)
        {
            runTextures[i]=new Texture("Movements/Player/running/running_"+(i+1)+".png");
            runFrames[i]=new TextureRegion(runTextures[i]);
        }
        //IDLE
        for (int i=0;i<idleFrameCount;i++)
        {
            idleTextures[i]=new Texture("Movements/Player/idle/idle_"+(i+1)+".png");
            idleFrames[i]=new TextureRegion(idleTextures[i]);
        }
        //IDLE BLINK
        for (int i=0;i<idleBlinkingFrameCount;i++)
        {
            idleBlinkingTextures[i]=new Texture("Movements/Player/idleBlinking/idleBlinking_"+(i+1)+".png");
            idleBlinkingFrames[i]=new TextureRegion(idleBlinkingTextures[i]);
        }

        walkAnimation=new Animation<>(0.025f,walkFrames);
        runAnimation=new Animation<>(0.08f,runFrames);

        idleAnimation=new Animation<>(0.08f,idleFrames);
        idleAnimation.setPlayMode(Animation.PlayMode.NORMAL);

        idleBlinkingAnimation=new Animation<>(0.08f,idleBlinkingFrames);
        idleBlinkingAnimation.setPlayMode(Animation.PlayMode.NORMAL);

        currentFrame=idleFrames[0];
        stateTime=0f;
    }

    public void update(float delta,OrthographicCamera camera)
    {
        boolean moved=handleMovement(delta);
        stateTime+=delta;

        //ANIMATION
        if (moved)
        {
            currentFrame=isRunning?runAnimation.getKeyFrame(stateTime,true):walkAnimation.getKeyFrame(stateTime,true);
            playBlink=false;
        }
        else
        {
            if (!playBlink)
            {
                currentFrame=idleAnimation.getKeyFrame(stateTime,false);

                if (idleAnimation.isAnimationFinished(stateTime))
                {
                    stateTime=0;
                    playBlink=true;
                }

            }
            else
            {
                currentFrame=idleBlinkingAnimation.getKeyFrame(stateTime,false);

                if (idleBlinkingAnimation.isAnimationFinished(stateTime))
                {
                    stateTime=0;
                    playBlink=false;
                }
            }
        }

        bounds.setPosition(pos.x, pos.y);

        //COOLDOWN
        if (shootTimer>0)
        {
            shootTimer-=delta;
        }
        //SHOOT
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT))
        {
            if (shootTimer<=0f)
            {
                shootArrow(camera);
                //COOLDOWN
                shootTimer=1.0f;
            }
        }

        //UPDATE ARROWS
        for (int i=arrows.size()-1;i>=0;i--)
        {
            Arrow arrow=arrows.get(i);
            arrow.update(delta);

            if (arrow.isCollided(GameWorld.WORLD_WIDTH,GameWorld.WORLD_HEIGHT, world.getBoundaries()))
            {
                arrows.remove(i);
            }
        }

        //STAMINA REGEN
        if (!isRunning)
        {
            stamina+=staminaRegen*delta;

            if (stamina>=maxStamina)
            {
                stamina=maxStamina;
                canRun=true;
            }
        }
        stamina=MathUtils.clamp(stamina,0,maxStamina);
    }
    private void shootArrow(OrthographicCamera camera)
    {

        Vector3 mouse=new Vector3(Gdx.input.getX(),Gdx.input.getY(),0);
        camera.unproject(mouse);

        Vector2 direction=new Vector2(
            mouse.x-(pos.x+WIDTH/2f),
            mouse.y-(pos.y+HEIGHT/2f)
        ).nor();

        arrows.add(new Arrow(
            pos.x+WIDTH/2f,
            pos.y+HEIGHT/2f,
            direction
        ));
    }

    private boolean handleMovement(float delta)
    {

        float oldX= pos.x;
        float oldY= pos.y;

        float newX= pos.x;
        float newY= pos.y;

        boolean up=Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean down=Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean left=Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean right=Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        if (up && down)
        {
            up=false;
            down=false;
        }
        if (left && right)
        {
            left=false;
            right=false;
        }

        boolean wantsToRun=
            Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ||
                Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        if (wantsToRun && canRun && stamina>0 && (up || down || left || right))
        {
            isRunning=true;
            stamina-=staminaDrain*delta;

            if (stamina<=0)
            {
                stamina=0;
                isRunning=false;
                canRun=false;
            }
        }
        else
        {
            isRunning=false;
        }

        float speed=100;
        float currentSpeed=isRunning?speed*1.5f:speed;

        if (up)
        {
            newY+=currentSpeed*delta;
        }
        if (down)
        {
            newY-=currentSpeed*delta;
        }

        if (left)
        {
            newX-=currentSpeed*delta;
            if (facingRight)
            {
                flipFrames();
                facingRight=false;
            }
        }

        if (right)
        {
            newX+=currentSpeed*delta;
            if (!facingRight)
            {
                flipFrames();
                facingRight=true;
            }
        }

        //X AXIS COLLISION
        Rectangle xBounds=new Rectangle(newX, pos.y,WIDTH,HEIGHT);

        boolean collideX=false;
        for (Rectangle wall:world.getBoundaries())
        {
            if (xBounds.overlaps(wall))
            {
                collideX=true;
                break;
            }
        }

        if (!collideX)
        {
            pos.x=newX;
        }

        //Y AXIS COLLISION
        Rectangle yBounds=new Rectangle(pos.x,newY,WIDTH,HEIGHT);

        boolean collideY=false;
        for (Rectangle wall:world.getBoundaries())
        {
            if (yBounds.overlaps(wall))
            {
                collideY=true;
                break;
            }
        }

        if (!collideY)
        {
            pos.y=newY;
        }

        return (oldX!= pos.x || oldY!= pos.y);
    }

    private void flipFrames()
    {
        for (TextureRegion f:walkFrames)
        {
            f.flip(true,false);
        }
        for (TextureRegion f:runFrames)
        {
            f.flip(true,false);
        }
        for (TextureRegion f:idleFrames)
        {
            f.flip(true,false);
        }
        for (TextureRegion f:idleBlinkingFrames)
        {
            f.flip(true,false);
        }
    }

    public void render(SpriteBatch batch)
    {
        batch.draw(currentFrame,pos.x,pos.y,WIDTH,HEIGHT);

        for (Arrow arrow:arrows)
        {
            arrow.render(batch);
        }
    }

    public void dispose()
    {
        for (Texture t:walkingTextures)
        {
            t.dispose();
        }
        for (Texture t:runTextures)
        {
            t.dispose();
        }
        for (Texture t:idleTextures)
        {
            t.dispose();
        }
        for (Texture t:idleBlinkingTextures)
        {
            t.dispose();
        }
    }
    public Vector2 getPos()
    {
        return new Vector2(bounds.x,bounds.y);
    }
    public ArrayList<Arrow> getArrows()
    {
        return arrows;
    }
    public float getStamina()
    {
        return stamina;
    }
    public float getMaxStamina()
    {
        return maxStamina;
    }
    public float getShootCooldownPercent()
    {
        return 1f-(shootTimer/1.0f);
    }
}
