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
    public static final int SCREEN_HEIGHT = 900;
    public static final int SCREEN_WIDTH = 1200;
    final VolleyBall game;

    // Implement pause/resume
    State state = State.RUN;

    SpriteBatch batch;
    Sprite rightHippoSprite, leftHippoSprite, ballSprite, netSprite;
    Texture hippoImg, hippoRed, ballImg, netImg;
    World world;
    Body rightHippo, leftHippo, ball, net;
    Body bottomEdge, topEdge, leftEdge, rightEdge;
    Box2DDebugRenderer debugRenderer;
    Matrix4 debugMatrix;
    OrthographicCamera camera;
    BitmapFont font;
    Texture background;
    Sprite backgroundSprite;

    // Variables for handling hippo movement
    float rightJumpHeight = 0.0f;
    float leftJumpHeight = 0.0f;
    boolean isRightHippoFlipped = true, isLeftHippoFlipped = false;
    boolean isMovementAllowed = true; // Enable/disable player control for hippos
    boolean upHeld = false, rightHeld = false, leftHeld = false,
            wHeld = false, dHeld = false, aHeld = false; // Tracking key states for hippo movement

    // Variables for score handling and round transition
    int leftScore = 0, rightScore = 0; // Tracking score for both players
    int roundCount = 0; // Tracking number of rounds
    boolean hasBallLanded = false; // If the ball has already landed once for the round, disregard any other times
    boolean winningHippo; // Which hippo will serve next round? False = leftHippo, true = rightHippo
    boolean isNextRoundStarting; // Do not allow players to pause/resume the game while this is true
    float timeSinceLanding = 0f; // Time to wait until game state is paused and reset (players cannot pause/resume during this time)
    float timeUntilStart; // Time to wait until game state is resumed (players cannot pause/resume during this time)

    // Variables for game rendering
    boolean drawSprite = true, drawDebug = true;

    // Hippo calibrations
    final float HIPPO_SCALE = 1.5f;
    final float HIPPO_DENSITY = 0.32f;
    final float HIPPO_RESTITUTION = 0.0f; // No bounce on collision with the ground
    final float HIPPO_FRICTION = 50f;
    final float HORIZONTAL_VELOCITY = 50f*HIPPO_SCALE*HIPPO_SCALE; // Velocity when pressing or holding Left/Right or A/D
    final float MAX_HORIZONTAL_VELOCITY = 6.75f; // Max horizontal speed
    final float JUMP_VELOCITY = 100f*HIPPO_SCALE*HIPPO_SCALE; // Initial velocity when pressing Up or W
    final float JUMP_HOLD_VELOCITY = 75f*HIPPO_SCALE*HIPPO_SCALE; // Incremental velocity when holding Up or W
    final float MAX_JUMP_HEIGHT = 400f; // Constraint to prevent hippos from perpetually floating up

    // Other calibrations
    final float BALL_DENSITY = 1.00f; // Less density than the hippos
    final float BALL_RESTITUTION = 0.65f; // Unlike the hippos, the ball will bounce off everything
    final float WALL_RESTITUTION = 0.5f; // Hippos and ball lose half of their velocity upon collision with walls
    final float NET_RESTITUTION = 0.95f; // Hippos and ball will retain most of their velocity upon collision with the net
    final float NET_SCALE = 0.75f;
    final float TIME_TO_NEXT_ROUND = 2f; // Wait 2 seconds after scoring to start next round
    final float PIXELS_TO_METERS = 100f;

    // Entity definitions
    final short HIPPO_ENTITY = 0x1; // Hippos will not collide with each other
    final short BALL_ENTITY = 0x1 << 1; // The ball collides with hippos and the world
    final short WORLD_ENTITY = 0x1 << 2;

    public GameScreen(final VolleyBall game) {
        this.game = game;

        batch = new SpriteBatch();
        hippoImg = new Texture("Hippoblue.png");
        hippoRed = new Texture("Hippored.png");
        ballImg = new Texture("volleyball.png");
        netImg = new Texture("Net.png");
        leftHippoSprite = new Sprite(hippoImg); // Faces towards the right by default
        leftHippoSprite.setScale(HIPPO_SCALE);
        rightHippoSprite = new Sprite(hippoRed);
        rightHippoSprite.flip(true,false); // Flip horizontally so the right hippo will face towards the left by default
        rightHippoSprite.setScale(HIPPO_SCALE);
        ballSprite = new Sprite(ballImg);
        netSprite = new Sprite(netImg);
        background = new Texture("anime_style_background___beach_by_azuki_sato-d2zhqix.jpg");
        backgroundSprite =new Sprite(background);
        backgroundSprite.setBounds(0f, 0f, 1200f, 900f);
        backgroundSprite.setCenter(0, 0);
        rightHippoSprite.setScale(HIPPO_SCALE);
        leftHippoSprite.setScale(HIPPO_SCALE);

        netSprite.setPosition(-netSprite.getWidth()/2,-525);
        netSprite.setScale(NET_SCALE);

        world = new World(new Vector2(0, -15f),true);

        // Begin body definitions
        /*
        //  RIGHT HIPPO BODY
        */
        BodyDef rightHippoBodyDef = new BodyDef();
        rightHippoBodyDef.type = BodyDef.BodyType.DynamicBody;
        rightHippoBodyDef.position.set((rightHippoSprite.getX() + rightHippoSprite.getWidth()/2) / PIXELS_TO_METERS,
                (rightHippoSprite.getY() + rightHippoSprite.getHeight()/2) / PIXELS_TO_METERS);

        rightHippo = world.createBody(rightHippoBodyDef);

        // Prevent the hippo's angular facing from changing
        rightHippo.setFixedRotation(true);

        PolygonShape rightHippoShape = new PolygonShape();
        rightHippoShape.setAsBox(rightHippoSprite.getWidth()*HIPPO_SCALE / 2 / PIXELS_TO_METERS,
                rightHippoSprite.getHeight()*HIPPO_SCALE / 2 / PIXELS_TO_METERS);

        FixtureDef rightHippoFixtureDef = new FixtureDef();
        rightHippoFixtureDef.shape = rightHippoShape;
        rightHippoFixtureDef.density = HIPPO_DENSITY;
        rightHippoFixtureDef.friction = HIPPO_FRICTION;
        rightHippoFixtureDef.restitution = HIPPO_RESTITUTION;
        rightHippoFixtureDef.filter.categoryBits = HIPPO_ENTITY;
        rightHippoFixtureDef.filter.maskBits = BALL_ENTITY|WORLD_ENTITY;

        rightHippo.createFixture(rightHippoFixtureDef);
        rightHippoShape.dispose();

        /*
        //  LEFT HIPPO BODY
        */
        BodyDef leftHippoBodyDef = new BodyDef();
        leftHippoBodyDef.type = BodyDef.BodyType.DynamicBody;
        leftHippoBodyDef.position.set((leftHippoSprite.getX() + leftHippoSprite.getWidth()/2) / PIXELS_TO_METERS,
                (leftHippoSprite.getY() + leftHippoSprite.getHeight()/2) / PIXELS_TO_METERS);

        leftHippo = world.createBody(leftHippoBodyDef);

        // Prevent the hippo's angular facing from changing
        leftHippo.setFixedRotation(true);

        PolygonShape leftHippoShape = new PolygonShape();
        leftHippoShape.setAsBox(leftHippoSprite.getWidth()*HIPPO_SCALE / 2 / PIXELS_TO_METERS,
                leftHippoSprite.getHeight()*HIPPO_SCALE / 2 / PIXELS_TO_METERS);

        FixtureDef leftHippoFixtureDef = new FixtureDef();
        leftHippoFixtureDef.shape = leftHippoShape;
        leftHippoFixtureDef.density = HIPPO_DENSITY;
        leftHippoFixtureDef.friction = HIPPO_FRICTION;
        leftHippoFixtureDef.restitution = HIPPO_RESTITUTION;
        leftHippoFixtureDef.filter.categoryBits = HIPPO_ENTITY;
        leftHippoFixtureDef.filter.maskBits = BALL_ENTITY|WORLD_ENTITY;

        leftHippo.createFixture(leftHippoFixtureDef);
        leftHippoShape.dispose();

        /*
        //  BALL BODY
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
        ballFixtureDef.density = BALL_DENSITY;
        ballFixtureDef.restitution = BALL_RESTITUTION;
        ballFixtureDef.filter.categoryBits = BALL_ENTITY;
        ballFixtureDef.filter.maskBits = HIPPO_ENTITY|WORLD_ENTITY;

        ball.createFixture(ballFixtureDef);
        ballShape.dispose();

        /*
        //  NET BODY
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
        netFixtureDef.restitution = NET_RESTITUTION;
        netFixtureDef.friction = 0f;
        netFixtureDef.filter.categoryBits = WORLD_ENTITY;
        netFixtureDef.filter.maskBits = BALL_ENTITY|HIPPO_ENTITY;

        net.createFixture(netFixtureDef);
        netShape.dispose();
        // End body definitions

        // Begin edge definitions
        /*
        //  BOTTOM SCREEN EDGE
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
        //  TOP SCREEN EDGE
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
        //  LEFT SCREEN EDGE
        */
        BodyDef leftBodyDef = new BodyDef();
        leftBodyDef.type = BodyDef.BodyType.StaticBody;
        leftBodyDef.position.set(0,0);
        FixtureDef leftFixtureDef = new FixtureDef();
        leftFixtureDef.filter.categoryBits = WORLD_ENTITY;

        EdgeShape leftEdgeShape = new EdgeShape();
        leftFixtureDef.shape = leftEdgeShape;
        leftFixtureDef.restitution = WALL_RESTITUTION;
        leftFixtureDef.friction = 0f;
        leftEdgeShape.set(-w/2,-h/2,-w/2,h/2);

        leftEdge = world.createBody(leftBodyDef);
        leftEdge.createFixture(leftFixtureDef);
        leftEdgeShape.dispose();

        /*
        //  RIGHT SCREEN EDGE
        */
        BodyDef rightBodyDef = new BodyDef();
        rightBodyDef.type = BodyDef.BodyType.StaticBody;
        rightBodyDef.position.set(0,0);
        FixtureDef rightFixtureDef = new FixtureDef();
        rightFixtureDef.filter.categoryBits = WORLD_ENTITY;

        EdgeShape rightEdgeShape = new EdgeShape();
        rightFixtureDef.shape = rightEdgeShape;
        rightFixtureDef.restitution = WALL_RESTITUTION;
        rightFixtureDef.friction = 0f;
        rightEdgeShape.set(w/2,h/2,w/2,-h/2);

        rightEdge = world.createBody(rightBodyDef);
        rightEdge.createFixture(rightFixtureDef);
        rightEdgeShape.dispose();
        // End edge definitions

        /*
        //  BALL AND GROUND COLLISION
        */
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                // Listen for when the ball lands on the ground
                // Since we don't know if A/B will be the ball or the edge, we must include comparisons for both ways
                if ((contact.getFixtureA().getBody() == bottomEdge && contact.getFixtureB().getBody() == ball)
                        ||
                        (contact.getFixtureA().getBody() == ball && contact.getFixtureB().getBody() == bottomEdge)) {
                    // If the ball has not landed for the current round, increment score
                    if (!hasBallLanded) {
                        if (ball.getPosition().x < 0) {
                            rightScore++;
                            winningHippo = true; // winningHippo means the right hippo has won the round
                        } else if (ball.getPosition().x > 0) {
                            leftScore++;
                            winningHippo = false; // !winningHippo means the left hippo has won the round
                        }
                        hasBallLanded = true;
                    }
                }
            }

            // Ignore the rest of this
            @Override
            public void endContact(Contact contact) {}
            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {}
            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {}
        });

        Gdx.input.setInputProcessor(this);

        debugRenderer = new Box2DDebugRenderer();
        font = new BitmapFont();
        font.setColor(Color.BLACK);
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Setup complete; initialize the game with right hippo on serve
        pause();
        reset(rightHippo);
    }

    public void renderBackground() {
        backgroundSprite.draw(batch);
    }

    @Override
    public void render(float delta) {
        camera.update();

        // Advance the game state if it's running, freeze the game state if it's paused
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

        // The rest of the code must stay outside of the switch structure,
        // or else massive jittering occurs with the sprites while the game is paused

        // Match the rightHippo sprite to the coordinates of its body
        rightHippoSprite.setPosition(
                (rightHippo.getPosition().x * PIXELS_TO_METERS) - rightHippoSprite.getWidth()/2,
                (rightHippo.getPosition().y * PIXELS_TO_METERS) - rightHippoSprite.getHeight()/2);
        rightHippoSprite.setRotation((float)Math.toDegrees(rightHippo.getAngle()));

        // Match the leftHippo sprite to the coordinates of its body
        leftHippoSprite.setPosition(
                (leftHippo.getPosition().x * PIXELS_TO_METERS) - leftHippoSprite.getWidth()/2,
                (leftHippo.getPosition().y * PIXELS_TO_METERS) - leftHippoSprite.getHeight()/2);
        leftHippoSprite.setRotation((float)Math.toDegrees(leftHippo.getAngle()));

        // Match the ball sprite to the coordinates of its body
        ballSprite.setPosition(
                (ball.getPosition().x * PIXELS_TO_METERS) - ballSprite.getWidth() / 2,
                (ball.getPosition().y * PIXELS_TO_METERS) - ballSprite.getHeight() / 2);
        ballSprite.setRotation((float) Math.toDegrees(ball.getAngle()));

        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        debugMatrix = batch.getProjectionMatrix().cpy().scale(PIXELS_TO_METERS,	PIXELS_TO_METERS, 0);
        batch.begin();

        renderBackground();

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

        // If the ball has landed, prepare to start next round
        if (hasBallLanded) {
            if (winningHippo)
                font.draw(batch, "Right player scores!", 0f, 0f);
            else
                font.draw(batch, "Left player scores!", 0f, 0f);

            // Wait one second before pausing and resetting the game state
            timeSinceLanding += delta;
            if (timeSinceLanding >= TIME_TO_NEXT_ROUND/2) {
                pause();
                if (winningHippo)
                    reset(rightHippo);
                else
                    reset(leftHippo);
            }
        }

        // If we are preparing to start the next round
        if (isNextRoundStarting) {
            // Display round count at center of the screen
            font.draw(batch, "Round " + roundCount, 0f, 0f);

            // Countdown from 1 second to resume the game state
            timeUntilStart -= delta;
            if (timeUntilStart <= 0) {
                isNextRoundStarting = false;
                resume();
            }
        }
        else if (this.state == State.PAUSE)
            font.draw(batch, "Paused", 0f, 0f);

        // Display the left player's score count at bottom left corner
        font.draw(batch,
                "Left player score: " + leftScore,
                -Gdx.graphics.getWidth() / 2 + 2.5f, -Gdx.graphics.getHeight() / 2 + 20);

        // Display the right player's score count at bottom right corner
        font.draw(batch,
                "Right player score: " + rightScore,
                Gdx.graphics.getWidth() / 2 - 145, -Gdx.graphics.getHeight() / 2 + 20);

        batch.end();

        if (drawDebug)
            debugRenderer.render(world, debugMatrix);
    }

    @Override
    public boolean keyDown(int keycode) {
        // Only acknowledge key usage when player movement is enabled
        if (isMovementAllowed) {
            // When the user presses an arrow key, apply an initial force and enable additional
            // force to be added during the render loop
            if (keycode == Input.Keys.RIGHT) {
                rightHippo.applyForceToCenter(HORIZONTAL_VELOCITY, 0f, true);
                if (isRightHippoFlipped) {
                    rightHippoSprite.flip(true, false);
                    isRightHippoFlipped = false;
                }
                rightHeld = true;
            }
            if (keycode == Input.Keys.LEFT) {
                rightHippo.applyForceToCenter(-HORIZONTAL_VELOCITY, 0f, true);
                if (!isRightHippoFlipped) {
                    rightHippoSprite.flip(true, false);
                    isRightHippoFlipped = true;
                }
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
                if (isLeftHippoFlipped) {
                    leftHippoSprite.flip(true, false);
                    isLeftHippoFlipped = false;
                }
                dHeld = true;
            }
            if (keycode == Input.Keys.A) {
                leftHippo.applyForceToCenter(-HORIZONTAL_VELOCITY, 0f, true);
                if (!isLeftHippoFlipped) {
                    leftHippoSprite.flip(true, false);
                    isLeftHippoFlipped = true;
                }
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

        // Allow user to change ball restitution using comma and period keys
        if(keycode == Input.Keys.COMMA) {
            ball.getFixtureList().first().setRestitution(ball.getFixtureList().first().getRestitution() - 0.1f);
        }
        if(keycode == Input.Keys.PERIOD) {
            ball.getFixtureList().first().setRestitution(ball.getFixtureList().first().getRestitution() + 0.1f);
        }
        if(keycode == Input.Keys.ESCAPE) {
            // Do not allow the players to pause or resume the game while it's transitioning between rounds
            if (state == State.RUN && !hasBallLanded && !isNextRoundStarting)
                pause();
            else if (state == State.PAUSE && !hasBallLanded && !isNextRoundStarting)
                resume();
        }
        if(keycode == Input.Keys.ENTER) {
            drawDebug = !drawDebug;
            if (!drawDebug && !drawSprite)
                drawSprite = true;
        }
        if(keycode == Input.Keys.SPACE) {
            drawSprite = !drawSprite;
            if (!drawDebug && !drawSprite)
                drawDebug = true;
        }
        return true;
    }

    public void reset(Body whichHippo) {
        // Starting next round
        timeUntilStart = TIME_TO_NEXT_ROUND/2;
        isNextRoundStarting = true;
        roundCount++;

        // Move hippos back to their starting positions
        rightHippo.setLinearVelocity(0f, 0f);
        rightHippo.setAngularVelocity(0f);
        rightHippo.setTransform(rightHippoSprite.getWidth()*4f/PIXELS_TO_METERS,-GameScreen.SCREEN_HEIGHT/PIXELS_TO_METERS/2 + rightHippoSprite.getHeight()/PIXELS_TO_METERS + 0.26f, 0f);

        leftHippo.setLinearVelocity(0f, 0f);
        leftHippo.setAngularVelocity(0f);
        leftHippo.setTransform(-leftHippoSprite.getWidth()*4f/PIXELS_TO_METERS,-GameScreen.SCREEN_HEIGHT/PIXELS_TO_METERS/2 + leftHippoSprite.getHeight()/PIXELS_TO_METERS + 0.26f, 0f);

        // If the game is paused, reset key states to avoid weird bugs
        upHeld = false;
        rightHeld = false;
        leftHeld = false;

        wHeld = false;
        dHeld = false;
        aHeld = false;

        // Reset ball back to non-landed state and place it above the specified hippo
        hasBallLanded = false;
        timeSinceLanding = 0f;
        ball.setLinearVelocity(0f, 0f);
        ball.setAngularVelocity(1f);
        ball.setTransform(whichHippo.getPosition().x, whichHippo.getPosition().y*5f/PIXELS_TO_METERS,0f);
    }

    @Override
    public void pause() {
        this.state = State.PAUSE;
        // Disable player movement inputs while game is paused
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
        hippoRed.dispose();
        ballImg.dispose();
        netImg.dispose();
        world.dispose();
    }

    // Unused Screen functions
    @Override
    public void resize(int width, int height) {}
    @Override
    public void show() {}
    @Override
    public void hide() {}

    // Unused InputProcessor functions
    @Override
    public boolean keyTyped(char character) { return false; }
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override
    public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override
    public boolean scrolled(int amount) { return false; }
}