package com.mygdx.game;

import java.util.Iterator;

import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

public class GameScreen extends ApplicationAdapter implements Screen, InputProcessor {
    final VolleyBall game;

    // Implement pause/resume
    State state = State.RUN;

    SpriteBatch batch;
    Sprite rightHippoSprite, leftHippoSprite, ballSprite, netSprite;
    Texture hippoImg, ballImg, netImg;
    World world;
    Body rightHippo, leftHippo, ball, net;
    Body bottomEdge, topEdge, leftEdge, rightEdge;
    Box2DDebugRenderer debugRenderer;
    Matrix4 debugMatrix;
    OrthographicCamera camera;
    BitmapFont font;
    float torque = 0.0f;
    float rightJumpHeight = 0.0f;
    float leftJumpHeight = 0.0f;
    boolean isMovementAllowed = true; // Enable/disable player control for hippos
    boolean cycleToRight = false; // Used for resetting the game state
    boolean upHeld = false, rightHeld = false, leftHeld = false,
            wHeld = false, dHeld = false, aHeld = false; // Tracking key states for hippo movement
    boolean drawSprite = true, drawDebug = true;

    // Hippo calibrations
    final float HIPPO_DENSITY = 1.25f;
    final float HORIZONTAL_VELOCITY = 25f; // Velocity when pressing or holding Left/Right or A/D
    final float MAX_HORIZONTAL_VELOCITY = 6.75f; // Max horizontal speed
    final float JUMP_VELOCITY = 100f; // Initial velocity when pressing Up or W
    final float JUMP_HOLD_VELOCITY = 75f; // Incremental velocity when holding Up or W
    final float MAX_JUMP_HEIGHT = 400f; // Constraint to prevent hippos from perpetually floating up

    // Other calibrations
    final float WALL_RESTITUTION = 0.5f;
    final float NET_SCALE = 0.75f;
    final float PIXELS_TO_METERS = 100f;

    // Entity definitions
    final short HIPPO_ENTITY = 0x1;
    final short BALL_ENTITY = 0x1 << 1;
    final short WORLD_ENTITY = 0x1 << 2;

    public GameScreen(final VolleyBall game) {
        this.game = game;

        batch = new SpriteBatch();
        hippoImg = new Texture("Hippo.png");
        ballImg = new Texture("volleyball.png");
        netImg = new Texture("Net.png");
        leftHippoSprite = new Sprite(hippoImg);
        rightHippoSprite = new Sprite(hippoImg);
        rightHippoSprite.flip(true,false); // Hippo will face towards the left
        ballSprite = new Sprite(ballImg);
        netSprite = new Sprite(netImg);

        netSprite.setPosition(-netSprite.getWidth()/2,-525);
        netSprite.setScale(NET_SCALE);

        world = new World(new Vector2(0, -15f),true);

        /*
            Creating the rightHippo body
        */
        BodyDef rightHippoBodyDef = new BodyDef();
        rightHippoBodyDef.type = BodyDef.BodyType.DynamicBody;
        rightHippoBodyDef.position.set((rightHippoSprite.getX() + rightHippoSprite.getWidth()/2) / PIXELS_TO_METERS,
                (rightHippoSprite.getY() + rightHippoSprite.getHeight()/2) / PIXELS_TO_METERS);

        rightHippo = world.createBody(rightHippoBodyDef);

        // Prevent the hippo's angular facing from changing
        rightHippo.setFixedRotation(true);

        PolygonShape rightHippoShape = new PolygonShape();
        rightHippoShape.setAsBox(rightHippoSprite.getWidth()/2 / PIXELS_TO_METERS,
                rightHippoSprite.getHeight()/2 / PIXELS_TO_METERS);

        FixtureDef rightHippoFixtureDef = new FixtureDef();
        rightHippoFixtureDef.shape = rightHippoShape;
        rightHippoFixtureDef.density = HIPPO_DENSITY;
        rightHippoFixtureDef.restitution = 0.0f; // No bounce on collision with walls or floor
        rightHippoFixtureDef.filter.categoryBits = HIPPO_ENTITY;
        rightHippoFixtureDef.filter.maskBits = BALL_ENTITY|WORLD_ENTITY;

        rightHippo.createFixture(rightHippoFixtureDef);
        rightHippoShape.dispose();

        /*
            Creating the leftHippo body
        */
        BodyDef leftHippoBodyDef = new BodyDef();
        leftHippoBodyDef.type = BodyDef.BodyType.DynamicBody;
        leftHippoBodyDef.position.set((leftHippoSprite.getX() + leftHippoSprite.getWidth()/2) / PIXELS_TO_METERS,
                (leftHippoSprite.getY() + leftHippoSprite.getHeight()/2) / PIXELS_TO_METERS);

        leftHippo = world.createBody(leftHippoBodyDef);

        // Prevent the hippo's angular facing from changing
        leftHippo.setFixedRotation(true);

        PolygonShape leftHippoShape = new PolygonShape();
        leftHippoShape.setAsBox(leftHippoSprite.getWidth()/2 / PIXELS_TO_METERS,
                leftHippoSprite.getHeight()/2 / PIXELS_TO_METERS);

        FixtureDef leftHippoFixtureDef = new FixtureDef();
        leftHippoFixtureDef.shape = leftHippoShape;
        leftHippoFixtureDef.density = HIPPO_DENSITY;
        leftHippoFixtureDef.restitution = 0.0f; // No bounce on collision with walls or floor
        leftHippoFixtureDef.filter.categoryBits = HIPPO_ENTITY;
        leftHippoFixtureDef.filter.maskBits = BALL_ENTITY|WORLD_ENTITY;

        leftHippo.createFixture(leftHippoFixtureDef);
        leftHippoShape.dispose();

        /*
            Creating the ball body
        */
        BodyDef ballBodyDef = new BodyDef();
        ballBodyDef.type = BodyDef.BodyType.DynamicBody;
        ballBodyDef.position.set((ballSprite.getX() + ballSprite.getWidth()/2) / PIXELS_TO_METERS,
                (ballSprite.getY() + ballSprite.getHeight()/2) / PIXELS_TO_METERS);

        ball = world.createBody(ballBodyDef);

        CircleShape ballShape = new CircleShape();
        ballShape.setRadius((ballSprite.getWidth() / 2) / PIXELS_TO_METERS);

        FixtureDef ballFixtureDef = new FixtureDef();
        ballFixtureDef.shape = ballShape;
        ballFixtureDef.density = 1.00f; // Less density than the rightHippo
        ballFixtureDef.restitution = 0.65f; // Unlike the rightHippo, the ball should bounce off surfaces
        ballFixtureDef.filter.categoryBits = BALL_ENTITY;
        ballFixtureDef.filter.maskBits = HIPPO_ENTITY|WORLD_ENTITY;

        ball.createFixture(ballFixtureDef);
        ballShape.dispose();

        /*
            Creating the net body
        */
        BodyDef netBodyDef = new BodyDef();
        netBodyDef.type = BodyDef.BodyType.StaticBody;
        netBodyDef.position.set((netSprite.getX() + netSprite.getWidth()/2) / PIXELS_TO_METERS,
                (netSprite.getY() + netSprite.getHeight()/2) / PIXELS_TO_METERS);

        net = world.createBody(netBodyDef);

        PolygonShape netShape = new PolygonShape();
        netShape.setAsBox(netSprite.getWidth()*NET_SCALE / 2 / PIXELS_TO_METERS,
                netSprite.getHeight()*NET_SCALE / 2 / PIXELS_TO_METERS);

        FixtureDef netFixtureDef = new FixtureDef();
        netFixtureDef.shape = netShape;
        netFixtureDef.restitution = 0.95f; // Hippos and ball will retain most of their velocity after collision
        netFixtureDef.filter.categoryBits = WORLD_ENTITY;
        netFixtureDef.filter.maskBits = BALL_ENTITY|HIPPO_ENTITY;

        net.createFixture(netFixtureDef);
        netShape.dispose();

        /*
            Defining the bottom edge of the screen
        */
        BodyDef bottomBodyDef = new BodyDef();
        bottomBodyDef.type = BodyDef.BodyType.StaticBody;
        float w = Gdx.graphics.getWidth()/PIXELS_TO_METERS;
        // Set the height to just 50 pixels above the bottom of the screen so we can see the edge in the
        // debug renderer
        float h = Gdx.graphics.getHeight()/PIXELS_TO_METERS- 50/PIXELS_TO_METERS;
        //bottomBodyDef.position.set(0, h-10/PIXELS_TO_METERS);
        bottomBodyDef.position.set(0,0);
        FixtureDef bottomFixtureDef = new FixtureDef();
        bottomFixtureDef.filter.categoryBits = WORLD_ENTITY;

        EdgeShape bottomEdgeShape = new EdgeShape();
        bottomFixtureDef.shape = bottomEdgeShape;
        bottomEdgeShape.set(-w/2,-h/2,w/2,-h/2);

        bottomEdge = world.createBody(bottomBodyDef);
        bottomEdge.createFixture(bottomFixtureDef);
        bottomEdgeShape.dispose();

        // We don't want the 50 pixel padding for the other edges, so set height to screen height
        h = Gdx.graphics.getHeight()/PIXELS_TO_METERS;

        /*
            Top screen edge definition
        */
        BodyDef topBodyDef = new BodyDef();
        topBodyDef.type = BodyDef.BodyType.StaticBody;
        topBodyDef.position.set(0,0);
        FixtureDef topFixtureDef = new FixtureDef();
        topFixtureDef.filter.categoryBits = WORLD_ENTITY;

        EdgeShape topEdgeShape = new EdgeShape();
        topFixtureDef.shape = topEdgeShape;
        topEdgeShape.set(w/2,h/2,-w/2,h/2);

        topEdge = world.createBody(topBodyDef);
        topEdge.createFixture(topFixtureDef);
        topEdgeShape.dispose();

        /*
            Left screen edge definition
        */
        BodyDef leftBodyDef = new BodyDef();
        leftBodyDef.type = BodyDef.BodyType.StaticBody;
        leftBodyDef.position.set(0,0);
        FixtureDef leftFixtureDef = new FixtureDef();
        leftFixtureDef.filter.categoryBits = WORLD_ENTITY;

        EdgeShape leftEdgeShape = new EdgeShape();
        leftFixtureDef.shape = leftEdgeShape;
        leftFixtureDef.restitution = WALL_RESTITUTION;
        leftEdgeShape.set(-w/2,-h/2,-w/2,h/2);

        leftEdge = world.createBody(leftBodyDef);
        leftEdge.createFixture(leftFixtureDef);
        leftEdgeShape.dispose();

        /*
            Right screen edge definition
        */
        BodyDef rightBodyDef = new BodyDef();
        rightBodyDef.type = BodyDef.BodyType.StaticBody;
        rightBodyDef.position.set(0,0);
        FixtureDef rightFixtureDef = new FixtureDef();
        rightFixtureDef.filter.categoryBits = WORLD_ENTITY;

        EdgeShape rightEdgeShape = new EdgeShape();
        rightFixtureDef.shape = rightEdgeShape;
        rightFixtureDef.restitution = WALL_RESTITUTION;
        rightEdgeShape.set(w/2,h/2,w/2,-h/2);

        rightEdge = world.createBody(rightBodyDef);
        rightEdge.createFixture(rightFixtureDef);
        rightEdgeShape.dispose();
        // End edge definitions

        Gdx.input.setInputProcessor(this);

        debugRenderer = new Box2DDebugRenderer();
        font = new BitmapFont();
        font.setColor(Color.BLACK);
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        reset(rightHippo);
    }

    @Override
    public void render(float delta) {
        camera.update();

        switch (state) {
            case RUN:
                // Step the physics simulation forward at a rate of 60hz
                world.step(1f/60f, 6, 2);

                // Check if any of the keys are being held, then apply force accordingly
                if(rightHeld && rightHippo.getLinearVelocity().x <= MAX_HORIZONTAL_VELOCITY)
                    rightHippo.applyForceToCenter(HORIZONTAL_VELOCITY,0f,true);
                if(leftHeld && rightHippo.getLinearVelocity().x >= -MAX_HORIZONTAL_VELOCITY)
                    rightHippo.applyForceToCenter(-HORIZONTAL_VELOCITY,0f,true);
                if(upHeld && rightJumpHeight < MAX_JUMP_HEIGHT) {
                    rightHippo.applyForceToCenter(0f, JUMP_HOLD_VELOCITY, true);
                    rightJumpHeight = rightJumpHeight + JUMP_HOLD_VELOCITY;
                }

                if(dHeld && leftHippo.getLinearVelocity().x <= MAX_HORIZONTAL_VELOCITY)
                    leftHippo.applyForceToCenter(HORIZONTAL_VELOCITY,0f,true);
                if(aHeld && leftHippo.getLinearVelocity().x >= -MAX_HORIZONTAL_VELOCITY)
                    leftHippo.applyForceToCenter(-HORIZONTAL_VELOCITY,0f,true);
                if(wHeld && leftJumpHeight < MAX_JUMP_HEIGHT) {
                    leftHippo.applyForceToCenter(0f, JUMP_HOLD_VELOCITY, true);
                    leftJumpHeight = leftJumpHeight + JUMP_HOLD_VELOCITY;
                }
                break; // End RESUME case
            case PAUSE:
                break;
        } // End switch

        rightHippoSprite.setPosition(
                (rightHippo.getPosition().x * PIXELS_TO_METERS) - rightHippoSprite.getWidth()/2,
                (rightHippo.getPosition().y * PIXELS_TO_METERS) - rightHippoSprite.getHeight()/2);
        rightHippoSprite.setRotation((float)Math.toDegrees(rightHippo.getAngle()));

        leftHippoSprite.setPosition(
                (leftHippo.getPosition().x * PIXELS_TO_METERS) - leftHippoSprite.getWidth()/2,
                (leftHippo.getPosition().y * PIXELS_TO_METERS) - leftHippoSprite.getHeight()/2);
        leftHippoSprite.setRotation((float)Math.toDegrees(leftHippo.getAngle()));

        ballSprite.setPosition(
                (ball.getPosition().x * PIXELS_TO_METERS) - ballSprite.getWidth() / 2,
                (ball.getPosition().y * PIXELS_TO_METERS) - ballSprite.getHeight() / 2);
        ballSprite.setRotation((float) Math.toDegrees(ball.getAngle()));

        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        debugMatrix = batch.getProjectionMatrix().cpy().scale(PIXELS_TO_METERS,	PIXELS_TO_METERS, 0);
        batch.begin();

        if (drawSprite) {
            // Draw the rightHippo sprite
            batch.draw(rightHippoSprite, rightHippoSprite.getX(), rightHippoSprite.getY(),
                    rightHippoSprite.getOriginX(), rightHippoSprite.getOriginY(),
                    rightHippoSprite.getWidth(), rightHippoSprite.getHeight(),
                    rightHippoSprite.getScaleX(), rightHippoSprite.getScaleY(),
                    rightHippoSprite.getRotation());

            // Draw the leftHippo sprite
            batch.draw(leftHippoSprite, leftHippoSprite.getX(), leftHippoSprite.getY(),
                    leftHippoSprite.getOriginX(), leftHippoSprite.getOriginY(),
                    leftHippoSprite.getWidth(), leftHippoSprite.getHeight(),
                    leftHippoSprite.getScaleX(), leftHippoSprite.getScaleY(),
                    leftHippoSprite.getRotation());

            // Draw the ball sprite
            batch.draw(ballSprite, ballSprite.getX(), ballSprite.getY(),
                    ballSprite.getOriginX(), ballSprite.getOriginY(),
                    ballSprite.getWidth(), ballSprite.getHeight(),
                    ballSprite.getScaleX(), ballSprite.getScaleY(),
                    ballSprite.getRotation());

            // Draw the net sprite
            batch.draw(netSprite, netSprite.getX(), netSprite.getY(),
                    netSprite.getOriginX(), netSprite.getOriginY(),
                    netSprite.getWidth(), netSprite.getHeight(),
                    netSprite.getScaleX(), netSprite.getScaleY(),
                    netSprite.getRotation());
        }

        font.draw(batch,
                "Ball restitution: " + ball.getFixtureList().first().getRestitution(),
                -Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);

        batch.end();

        if (drawDebug)
            debugRenderer.render(world, debugMatrix);
    }

    public void reset(Body whichHippo) {
        // Move hippo back to starting position
        rightHippo.setLinearVelocity(0f, 0f);
        rightHippo.setAngularVelocity(0f);
        rightHippo.setTransform(rightHippoSprite.getWidth()*4f/PIXELS_TO_METERS,-rightHippoSprite.getHeight()*5.1f/PIXELS_TO_METERS,0f);

        leftHippo.setLinearVelocity(0f, 0f);
        leftHippo.setAngularVelocity(0f);
        leftHippo.setTransform(-leftHippoSprite.getWidth()*4f/PIXELS_TO_METERS,-leftHippoSprite.getHeight()*5.1f/PIXELS_TO_METERS,0f);

        // If the game is paused, reset key states to avoid weird bugs
        if (this.state == State.PAUSE) {
            upHeld = false;
            rightHeld = false;
            leftHeld = false;

            wHeld = false;
            dHeld = false;
            aHeld = false;
        }

        ball.setLinearVelocity(0f, 0f);
        ball.setAngularVelocity(1f);
        ball.setTransform(whichHippo.getPosition().x, whichHippo.getPosition().y*5f/PIXELS_TO_METERS,0f);
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
        this.state = State.PAUSE;
        isMovementAllowed = false;
    }

    @Override
    public void resume() {
        this.state = State.RUN;
        isMovementAllowed = true;
    }

    @Override
    public void dispose() {
        hippoImg.dispose();
        ballImg.dispose();
        world.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (isMovementAllowed) {
            // When the user presses an arrow key, apply an initial force and enable additional
            // force to be added during the render loop
            if (keycode == Input.Keys.RIGHT) {
                rightHippo.applyForceToCenter(HORIZONTAL_VELOCITY, 0f, true);
                rightHeld = true;
            }
            if (keycode == Input.Keys.LEFT) {
                rightHippo.applyForceToCenter(-HORIZONTAL_VELOCITY, 0f, true);
                leftHeld = true;
            }
            if (keycode == Input.Keys.UP) {
                // Only allow jumping if the hippo is actually touching the ground
                if (rightHippo.getLinearVelocity().y <= 1 && rightHippo.getLinearVelocity().y >= 0 && rightHippo.getPosition().y < -2) {
                    rightHippo.applyForceToCenter(0f, JUMP_VELOCITY, true);
                    rightJumpHeight = JUMP_VELOCITY;
                    upHeld = true;
                }
            }

            if (keycode == Input.Keys.D) {
                leftHippo.applyForceToCenter(HORIZONTAL_VELOCITY, 0f, true);
                dHeld = true;
            }
            if (keycode == Input.Keys.A) {
                leftHippo.applyForceToCenter(-HORIZONTAL_VELOCITY, 0f, true);
                aHeld = true;
            }
            if (keycode == Input.Keys.W) {
                // Only allow jumping if the hippo is actually touching the ground
                if (leftHippo.getLinearVelocity().y <= 1 && leftHippo.getLinearVelocity().y >= 0 && leftHippo.getPosition().y < -2) {
                    leftHippo.applyForceToCenter(0f, JUMP_VELOCITY, true);
                    leftJumpHeight = JUMP_VELOCITY;
                    wHeld = true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        // When the user releases an arrow key, disable force during the render loop
        if (keycode == Input.Keys.RIGHT)
            rightHeld = false;
        if (keycode == Input.Keys.LEFT)
            leftHeld = false;
        if (keycode == Input.Keys.UP)
            upHeld = false;

        if (keycode == Input.Keys.D)
            dHeld = false;
        if (keycode == Input.Keys.A)
            aHeld = false;
        if (keycode == Input.Keys.W)
            wHeld = false;

        // If user hits spacebar, reset everything back to normal
        if(keycode == Input.Keys.SPACE|| keycode == Input.Keys.NUM_2) {
            if (cycleToRight)
                reset(rightHippo);
            else
                reset(leftHippo);
            cycleToRight = !cycleToRight;
        }

        // Allow user to change ball restitution using comma and period keys
        if(keycode == Input.Keys.COMMA) {
            ball.getFixtureList().first().setRestitution(ball.getFixtureList().first().getRestitution() - 0.1f);
        }
        if(keycode == Input.Keys.PERIOD) {
            ball.getFixtureList().first().setRestitution(ball.getFixtureList().first().getRestitution()+0.1f);
        }
        if(keycode == Input.Keys.ESCAPE) {
            if (state == State.RUN)
                pause();
            else
                resume();
        }
        if(keycode == Input.Keys.ENTER) {
            drawDebug = !drawDebug;
            if (!drawDebug && !drawSprite)
                drawSprite = true;
        }

        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
}