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
    MainMenuScreen mainMenuScreen;

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
    Sound wavSound1, wavSound2, wavSound3, wavSound4, wavSound5;
    MathUtils rand;
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
    boolean scoringHippo; // Which hippo will serve next round? False = leftHippo, true = rightHippo
    boolean isNextRoundStarting; // Do not allow players to pause/resume the game while this is true
    boolean rightWin = false, leftWin = false; // Which hippo has won the game?
    float timeSinceLanding = 0f; // Timer until game state is paused and round transitions (players cannot pause/resume during this time)
    float timeUntilStart; // Timer until the new round starts and game state is resumed (players cannot pause/resume during this time)

    // Variables for game rendering
    boolean drawSprite = true, drawDebug = true;

    // Hippo calibrations
    final float HIPPO_SCALE = 1.25f; // Decides the size of the hippo; density and velocity variables are scaled by this value
    final float HIPPO_DENSITY = 0f; // Do not use density for hippos; libgdx will assign a mass value which will remain constant when scaling the hippo size up or down
    final float HIPPO_RESTITUTION = 0.0f; // No bounce on collision with the ground
    final float HIPPO_FRICTION = 50f; // Very little sliding on the ground
    final float HORIZONTAL_VELOCITY = 100f; // Velocity when pressing or holding Left/Right or A/D
    final float MAX_HORIZONTAL_VELOCITY = 6.75f; // Max horizontal speed
    final float JUMP_VELOCITY = 225f; // Initial velocity when pressing Up or W
    final float JUMP_HOLD_VELOCITY = 75f; // Incremental velocity when holding Up or W
    final float MAX_JUMP_HEIGHT = 600f; // Constraint to prevent hippos from perpetually floating up

    // Other calibrations
    final float BALL_DENSITY = 1.5f; // Less density than the hippos
    final float BALL_RESTITUTION = 0.7f; // Unlike the hippos, the ball will bounce off everything
    final float WALL_RESTITUTION = 0.5f; // Hippos and ball lose half of their velocity upon collision with walls
    final float NET_RESTITUTION = 0.95f; // Hippos and ball will retain most of their velocity upon collision with the net
    final float NET_SCALE = 0.75f;
    final float TIME_TO_NEXT_ROUND = 2f; // Wait 2 seconds after scoring to start next round
    final float PIXELS_TO_METERS = 100f;

    // Entity definitions
    final short HIPPO_ENTITY = 0x1; // Hippos will not collide with each other
    final short BALL_ENTITY = 0x1 << 1; // The ball collides with hippos and the world
    final short WORLD_ENTITY = 0x1 << 2;

    public GameScreen(final VolleyBall game, MainMenuScreen mainMenuScreen) {
        this.game = game;
        this.mainMenuScreen = mainMenuScreen;

        batch = new SpriteBatch();
        hippoImg = new Texture("Hippo.png");
        ballImg = new Texture("volleyball.png");
        netImg = new Texture("Net.png");
        leftHippoSprite = new Sprite(hippoImg); // Faces towards the right by default
        leftHippoSprite.setScale(HIPPO_SCALE);
        rightHippoSprite = new Sprite(hippoImg);
        rightHippoSprite.flip(true,false); // Flip horizontally so the right hippo will face towards the left by default
        rightHippoSprite.setScale(HIPPO_SCALE);
        ballSprite = new Sprite(ballImg);
        netSprite = new Sprite(netImg);
        background = new Texture("anime_style_background___beach_by_azuki_sato-d2zhqix.jpg");
        backgroundSprite = new Sprite(background);
        backgroundSprite.setBounds(0f, 0f, 1200f, 900f);
        backgroundSprite.setCenter(0, 0);

        wavSound1 = Gdx.audio.newSound(Gdx.files.internal("bgrunt.wav"));
        wavSound2 = Gdx.audio.newSound(Gdx.files.internal("w1.wav"));
        wavSound3 = Gdx.audio.newSound(Gdx.files.internal("w2.wav"));
        wavSound4 = Gdx.audio.newSound(Gdx.files.internal("chew_roar.mp3"));

        netSprite.setPosition(-netSprite.getWidth()/2,-525);
        netSprite.setScale(NET_SCALE);

        world = new World(new Vector2(0, -15f),true);

        /*
        // BEGIN BODY DEFINITIONS
        */
        rightHippo = createHippo(rightHippoSprite);
        leftHippo = createHippo(leftHippoSprite);
        ball = createBall(ballSprite);
        net = createNet(netSprite);
        // End body definitions

        /*
        // BEGIN EDGE DEFINITIONS
        */
        // Set the height to just 50 pixels above the bottom of the screen so we have a place to
        // display the score counters
        float w = Gdx.graphics.getWidth()/PIXELS_TO_METERS;
        float h = Gdx.graphics.getHeight()/PIXELS_TO_METERS- 50/PIXELS_TO_METERS;

        bottomEdge = createScreenEdge(-w/2, -h/2, w/2, -h/2);

        // We don't want the 50 pixel padding for the other edges, so set height to screen height
        h = Gdx.graphics.getHeight()/PIXELS_TO_METERS;

        topEdge = createScreenEdge(w/2, h/2, -w/2, h/2);
        leftEdge = createScreenEdge(-w/2,-h/2,-w/2,h/2);
        rightEdge = createScreenEdge(w/2, h/2, w/2, -h/2);
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
                            scoringHippo = true; // scoringHippo means the right hippo has won the round
                        } else if (ball.getPosition().x > 0) {
                            leftScore++;
                            scoringHippo = false; // !scoringHippo means the left hippo has won the round
                        }
                        // If one of the players has scored enough points to win, and they have a sufficient margin lead
                        if (rightScore >= VolleyBall.scoreToWin && rightScore - leftScore >= VolleyBall.scoreMargin) {
                            rightWin = true;
                        }
                        else if (leftScore >= VolleyBall.scoreToWin && leftScore - rightScore >= VolleyBall.scoreMargin) {
                            leftWin = true;
                        }

                        // Ball has landed; game will take action in the render loop
                        hasBallLanded = true;
                    }
                }

                /*
                // Hippo contact with ball plays a sound
                */
                if ((contact.getFixtureA().getBody() == rightHippo && contact.getFixtureB().getBody() == ball)
                        ||
                        (contact.getFixtureA().getBody() == ball && contact.getFixtureB().getBody() == rightHippo)
                        ||
                        (contact.getFixtureA().getBody() == leftHippo && contact.getFixtureB().getBody() == ball)
                        ||
                        (contact.getFixtureA().getBody() == ball && contact.getFixtureB().getBody() == leftHippo)) {
                    int rnum = rand.random(0, 3);
                    if(rnum == 0)
                    {
                        wavSound1.play();
                    }
                    else if(rnum == 1)
                    {
                        wavSound2.play();
                    }
                    else if(rnum == 2)
                    {
                        wavSound3.play();
                    }
                    else if(rnum == 3)
                    {
                        wavSound4.play();
                    }

                    //wavSound5.play();
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
        startNewGame();
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
            case FINISHED:
                break;
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

        // Always draw the background first, and do not allow it to be disabled by user
        backgroundSprite.draw(batch);

        // Draw all other graphics if enabled by user
        if (drawSprite) {
            rightHippoSprite.draw(batch);
            leftHippoSprite.draw(batch);
            ballSprite.draw(batch);
            netSprite.draw(batch);
        }

        // If the ball has landed, prepare to start next round or end the game
        if (hasBallLanded) {
            // If the right hippo has won the game
            if (rightWin) {
                font.draw(batch, "Right player wins the game! (Click anywhere to exit)", 0f, 0f);
                this.state = State.FINISHED;
            }
            // If the left hippo has won the game
            else if (leftWin) {
                font.draw(batch, "Left player wins the game! (Click anywhere to exit)", 0f, 0f);
                this.state = State.FINISHED;
            }
            // Neither hippo has won the game yet
            else {
                if (scoringHippo)
                    font.draw(batch, "Right player scores!", 0f, 0f);
                else
                    font.draw(batch, "Left player scores!", 0f, 0f);

                // Wait one second before pausing and resetting the game state
                timeSinceLanding += delta;
                if (timeSinceLanding >= TIME_TO_NEXT_ROUND / 2) {
                    pause();
                    if (scoringHippo)
                        startNewRound(rightHippo);
                    else
                        startNewRound(leftHippo);
                }
            }
        }

        // If the game is finished, wait for the user to click to return to main menu
        if (Gdx.input.isTouched()) {
            if (this.state == State.FINISHED) {
                hasBallLanded = false; // Exiting game screen, prevent the above code from executing upon return
                game.setScreen(mainMenuScreen);
                // Do not use dispose()
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

    public Body createHippo (Sprite hippoSprite) {
        BodyDef hippoBodyDef = new BodyDef();
        hippoBodyDef.type = BodyDef.BodyType.DynamicBody;
        hippoBodyDef.position.set((hippoSprite.getX() + hippoSprite.getWidth()/2) / PIXELS_TO_METERS,
                (hippoSprite.getY() + hippoSprite.getHeight()/2) / PIXELS_TO_METERS);

        Body hippoBody = world.createBody(hippoBodyDef);

        // Prevent the hippo's angular facing from changing
        hippoBody.setFixedRotation(true);

        PolygonShape hippoShape = new PolygonShape();
        hippoShape.setAsBox(hippoSprite.getWidth()*HIPPO_SCALE / 2 / PIXELS_TO_METERS,
                hippoSprite.getHeight()*HIPPO_SCALE / 2 / PIXELS_TO_METERS);

        FixtureDef hippoFixtureDef = new FixtureDef();
        hippoFixtureDef.shape = hippoShape;
        hippoFixtureDef.density = HIPPO_DENSITY;
        hippoFixtureDef.friction = HIPPO_FRICTION;
        hippoFixtureDef.restitution = HIPPO_RESTITUTION;
        hippoFixtureDef.filter.categoryBits = HIPPO_ENTITY;
        hippoFixtureDef.filter.maskBits = BALL_ENTITY|WORLD_ENTITY;

        hippoBody.createFixture(hippoFixtureDef);
        hippoShape.dispose();

        return hippoBody;
    }

    public Body createBall (Sprite ballSprite) {
        BodyDef ballBodyDef = new BodyDef();
        ballBodyDef.type = BodyDef.BodyType.DynamicBody;
        ballBodyDef.position.set((ballSprite.getX() + ballSprite.getWidth()/2) / PIXELS_TO_METERS,
                (ballSprite.getY() + ballSprite.getHeight()/2) / PIXELS_TO_METERS);

        Body ballBody = world.createBody(ballBodyDef);

        CircleShape ballShape = new CircleShape();
        ballShape.setRadius((ballSprite.getWidth() / 2) / PIXELS_TO_METERS);

        FixtureDef ballFixtureDef = new FixtureDef();
        ballFixtureDef.shape = ballShape;
        ballFixtureDef.density = BALL_DENSITY;
        ballFixtureDef.restitution = BALL_RESTITUTION;
        ballFixtureDef.filter.categoryBits = BALL_ENTITY;
        ballFixtureDef.filter.maskBits = HIPPO_ENTITY|WORLD_ENTITY;

        ballBody.createFixture(ballFixtureDef);
        ballShape.dispose();

        return ballBody;
    }

    public Body createNet (Sprite netSprite) {
        BodyDef netBodyDef = new BodyDef();
        netBodyDef.type = BodyDef.BodyType.StaticBody;
        netBodyDef.position.set((netSprite.getX() + netSprite.getWidth()/2) / PIXELS_TO_METERS,
                (netSprite.getY() + netSprite.getHeight()/2) / PIXELS_TO_METERS);

        Body netBody = world.createBody(netBodyDef);

        PolygonShape netShape = new PolygonShape();
        netShape.setAsBox(netSprite.getWidth()*NET_SCALE / 2 / PIXELS_TO_METERS,
                netSprite.getHeight()*NET_SCALE / 2 / PIXELS_TO_METERS);

        FixtureDef netFixtureDef = new FixtureDef();
        netFixtureDef.shape = netShape;
        netFixtureDef.restitution = NET_RESTITUTION;
        netFixtureDef.friction = 0f;
        netFixtureDef.filter.categoryBits = WORLD_ENTITY;
        netFixtureDef.filter.maskBits = BALL_ENTITY|HIPPO_ENTITY;

        netBody.createFixture(netFixtureDef);
        netShape.dispose();

        return netBody;
    }

    public Body createScreenEdge (float v1X, float v1Y, float v2X, float v2Y) {
        BodyDef edgeBodyDef = new BodyDef();
        edgeBodyDef.type = BodyDef.BodyType.StaticBody;
        edgeBodyDef.position.set(0,0);
        FixtureDef edgeFixtureDef = new FixtureDef();
        edgeFixtureDef.filter.categoryBits = WORLD_ENTITY;

        EdgeShape edgeShape = new EdgeShape();
        edgeFixtureDef.shape = edgeShape;
        edgeShape.set(v1X, v1Y, v2X, v2Y);

        Body screenEdgeBody = world.createBody(edgeBodyDef);
        screenEdgeBody.createFixture(edgeFixtureDef);
        edgeShape.dispose();

        return screenEdgeBody;
    }

    public void startNewRound(Body whichHippo) {
        // Starting next round
        timeUntilStart = TIME_TO_NEXT_ROUND/2;
        isNextRoundStarting = true;
        roundCount++;

        // Move hippos back to their starting positions
        rightHippo.setLinearVelocity(0f, 0f);
        rightHippo.setAngularVelocity(0f);
        rightHippo.setTransform(rightHippoSprite.getWidth()*4f/PIXELS_TO_METERS,-VolleyBall.SCREEN_HEIGHT/PIXELS_TO_METERS/2 + rightHippoSprite.getHeight()/PIXELS_TO_METERS - 0.2f, 0f);

        leftHippo.setLinearVelocity(0f, 0f);
        leftHippo.setAngularVelocity(0f);
        leftHippo.setTransform(-leftHippoSprite.getWidth()*4f/PIXELS_TO_METERS,-VolleyBall.SCREEN_HEIGHT/PIXELS_TO_METERS/2 + leftHippoSprite.getHeight()/PIXELS_TO_METERS - 0.2f, 0f);

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

    public void startNewGame() {
        pause();
        leftScore = 0;
        rightScore = 0;
        leftWin = false;
        rightWin = false;
        roundCount = 0;
        startNewRound(rightHippo);
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