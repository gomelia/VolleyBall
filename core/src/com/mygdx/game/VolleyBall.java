package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.PolygonShape;

public class VolleyBall extends ApplicationAdapter implements InputProcessor {
	SpriteBatch batch;
	Sprite hippoSprite, ballSprite, netSprite;
	Texture hippoImg, ballImg, netImg;
	World world;
	Body hippo, ball, net;
	Body bottomEdge, topEdge, leftEdge, rightEdge;
	Box2DDebugRenderer debugRenderer;
	Matrix4 debugMatrix;
	OrthographicCamera camera;
	BitmapFont font;

	float torque = 0.0f;
	float jumpHeight = 0.0f;
	boolean drawSprite = true, drawDebug = true;
	boolean upHeld = false, downHeld = false, rightHeld = false, leftHeld = false;

    // Hippo calibrations
	final float HORIZONTAL_VELOCITY = 12.5f;
	final float MAX_HORIZONTAL_VELOCITY = 6.25f;
	final float JUMP_VELOCITY = 250f;
	final float JUMP_HOLD_VELOCITY = 25f;
	final float MAX_JUMP_HEIGHT = 350f;

	// Other calibrations
	final float WALL_RESTITUTION = 0.5f;
	final float WALL_SCALE = 0.75f;
	final float PIXELS_TO_METERS = 100f;

	@Override
	public void create() {
		batch = new SpriteBatch();
		hippoImg = new Texture("Hippo.png");
		ballImg = new Texture("volleyball.png");
		netImg = new Texture("Net.png");
		hippoSprite = new Sprite(hippoImg);
		ballSprite = new Sprite(ballImg);
		netSprite = new Sprite(netImg);

		hippoSprite.setPosition(-hippoSprite.getWidth()/2,-hippoSprite.getHeight()/2);
		ballSprite.setPosition(0,0);
		netSprite.setPosition(-netSprite.getWidth()/2,-400);
		netSprite.setScale(WALL_SCALE);

		world = new World(new Vector2(0, -15f),true);

		// Creating the hippo body
		BodyDef hippoBodyDef = new BodyDef();
		hippoBodyDef.type = BodyDef.BodyType.DynamicBody;
		hippoBodyDef.position.set((hippoSprite.getX() + hippoSprite.getWidth()/2) / PIXELS_TO_METERS,
				(hippoSprite.getY() + hippoSprite.getHeight()/2) / PIXELS_TO_METERS);

		hippo = world.createBody(hippoBodyDef);

		// Prevent the hippo's angular facing from changing
		hippo.setFixedRotation(true);

		PolygonShape hippoShape = new PolygonShape();
		hippoShape.setAsBox(hippoSprite.getWidth()/2 / PIXELS_TO_METERS,
				hippoSprite.getHeight()/2 / PIXELS_TO_METERS);

		FixtureDef hippoFixtureDef = new FixtureDef();
		hippoFixtureDef.shape = hippoShape;
		hippoFixtureDef.density = 1.0f;
		hippoFixtureDef.restitution = 0.0f; // No bounce on collision with walls or floor

		hippo.createFixture(hippoFixtureDef);
		hippoShape.dispose();

		// Creating the ball body
		BodyDef ballBodyDef = new BodyDef();
		ballBodyDef.type = BodyDef.BodyType.DynamicBody;
		ballBodyDef.position.set((ballSprite.getX() + ballSprite.getWidth()/2) / PIXELS_TO_METERS,
				(ballSprite.getY() + ballSprite.getHeight()/2) / PIXELS_TO_METERS);

		ball = world.createBody(ballBodyDef);

		CircleShape ballShape = new CircleShape();
		ballShape.setRadius((ballSprite.getWidth() / 2) / PIXELS_TO_METERS);

		FixtureDef ballFixtureDef = new FixtureDef();
		ballFixtureDef.shape = ballShape;
		ballFixtureDef.density = 0.75f; // Less density than the hippo
		ballFixtureDef.restitution = 0.75f; // Unlike the hippo, the ball should bounce off surfaces

		ball.createFixture(ballFixtureDef);
		ballShape.dispose();

		// Creating the volleyball net body

		BodyDef netBodyDef = new BodyDef();
		netBodyDef.type = BodyDef.BodyType.StaticBody;
		netBodyDef.position.set((netSprite.getX() + netSprite.getWidth()/2) / PIXELS_TO_METERS,
				(netSprite.getY() + netSprite.getHeight()/2) / PIXELS_TO_METERS);

		net = world.createBody(netBodyDef);

		PolygonShape netShape = new PolygonShape();
		netShape.setAsBox(netSprite.getWidth()*WALL_SCALE / 2 / PIXELS_TO_METERS,
				netSprite.getHeight()*WALL_SCALE / 2 / PIXELS_TO_METERS);

		FixtureDef netFixtureDef = new FixtureDef();
		netFixtureDef.shape = netShape;
		netFixtureDef.density = 0.75f; // Less density than the hippo
		netFixtureDef.restitution = 0.75f; // Unlike the hippo, the ball should bounce off surfaces

		net.createFixture(netFixtureDef);
		netShape.dispose();


		// Defining the bottom edge of the screen
		BodyDef bottomBodyDef = new BodyDef();
		bottomBodyDef.type = BodyDef.BodyType.StaticBody;
		float w = Gdx.graphics.getWidth()/PIXELS_TO_METERS;
		// Set the height to just 50 pixels above the bottom of the screen so we can see the edge in the
		// debug renderer
		float h = Gdx.graphics.getHeight()/PIXELS_TO_METERS- 50/PIXELS_TO_METERS;
		//bottomBodyDef.position.set(0, h-10/PIXELS_TO_METERS);
		bottomBodyDef.position.set(0,0);
		FixtureDef bottomFixtureDef = new FixtureDef();

		EdgeShape bottomEdgeShape = new EdgeShape();
		bottomFixtureDef.shape = bottomEdgeShape;
		bottomEdgeShape.set(-w/2,-h/2,w/2,-h/2);

		bottomEdge = world.createBody(bottomBodyDef);
		bottomEdge.createFixture(bottomFixtureDef);
		bottomEdgeShape.dispose();

		// We don't want the 50 pixel padding for the other edges, so set height to screen height
		h = Gdx.graphics.getHeight()/PIXELS_TO_METERS;

		// Top screen edge definition
		BodyDef topBodyDef = new BodyDef();
		topBodyDef.type = BodyDef.BodyType.StaticBody;
		topBodyDef.position.set(0,0);
		FixtureDef topFixtureDef = new FixtureDef();

		EdgeShape topEdgeShape = new EdgeShape();
		topFixtureDef.shape = topEdgeShape;
		topEdgeShape.set(w/2,h/2,-w/2,h/2);

		topEdge = world.createBody(topBodyDef);
		topEdge.createFixture(topFixtureDef);
		topEdgeShape.dispose();

		// Left screen edge definition
		BodyDef leftBodyDef = new BodyDef();
		leftBodyDef.type = BodyDef.BodyType.StaticBody;
		leftBodyDef.position.set(0,0);
		FixtureDef leftFixtureDef = new FixtureDef();

		EdgeShape leftEdgeShape = new EdgeShape();
		leftFixtureDef.shape = leftEdgeShape;
		leftFixtureDef.restitution = WALL_RESTITUTION;
		leftEdgeShape.set(-w/2,-h/2,-w/2,h/2);

		leftEdge = world.createBody(leftBodyDef);
		leftEdge.createFixture(leftFixtureDef);
		leftEdgeShape.dispose();

		// Right screen edge definition
		BodyDef rightBodyDef = new BodyDef();
		rightBodyDef.type = BodyDef.BodyType.StaticBody;
		rightBodyDef.position.set(0,0);
		FixtureDef rightFixtureDef = new FixtureDef();

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
	}

	private float elapsed = 0;
	@Override
	public void render() {
		camera.update();
		// Step the physics simulation forward at a rate of 60hz
		world.step(1f/60f, 6, 2);

		// Check if any of the keys are being held, then apply force accordingly
		if(rightHeld && hippo.getLinearVelocity().x <= MAX_HORIZONTAL_VELOCITY)
			hippo.applyForceToCenter(HORIZONTAL_VELOCITY,0f,true);
		if(leftHeld && hippo.getLinearVelocity().x >= -MAX_HORIZONTAL_VELOCITY)
			hippo.applyForceToCenter(-HORIZONTAL_VELOCITY,0f,true);
		if(upHeld && jumpHeight < MAX_JUMP_HEIGHT) {
			hippo.applyForceToCenter(0f, JUMP_HOLD_VELOCITY, true);
			jumpHeight += JUMP_HOLD_VELOCITY;
		}
		if(downHeld)
			hippo.applyForceToCenter(0f, -5f, true);

		hippoSprite.setPosition(
				(hippo.getPosition().x * PIXELS_TO_METERS) - hippoSprite.getWidth()/2,
				(hippo.getPosition().y * PIXELS_TO_METERS) - hippoSprite.getHeight()/2);
		hippoSprite.setRotation((float)Math.toDegrees(hippo.getAngle()));

		ballSprite.setPosition(
				(ball.getPosition().x * PIXELS_TO_METERS) - ballSprite.getWidth() / 2,
				(ball.getPosition().y * PIXELS_TO_METERS) - ballSprite.getHeight() / 2);
		ballSprite.setRotation((float) Math.toDegrees(ball.getAngle()));

		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		System.out.println(hippo.getPosition().y);

		batch.setProjectionMatrix(camera.combined);
		debugMatrix = batch.getProjectionMatrix().cpy().scale(PIXELS_TO_METERS,	PIXELS_TO_METERS, 0);
		batch.begin();

		if (drawSprite) {
			// Draw the hippo sprite
			batch.draw(hippoSprite, hippoSprite.getX(), hippoSprite.getY(),
					hippoSprite.getOriginX(), hippoSprite.getOriginY(),
					hippoSprite.getWidth(), hippoSprite.getHeight(),
					hippoSprite.getScaleX(), hippoSprite.getScaleY(),
					hippoSprite.getRotation());

			// Draw the ball sprite
			batch.draw(ballSprite, ballSprite.getX(), ballSprite.getY(),
					ballSprite.getOriginX(), ballSprite.getOriginY(),
					ballSprite.getWidth(), ballSprite.getHeight(),
					ballSprite.getScaleX(), ballSprite.getScaleY(),
					ballSprite.getRotation());

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

	@Override
	public void dispose() {
		hippoImg.dispose();
		ballImg.dispose();
		world.dispose();
	}

	@Override
	public boolean keyDown(int keycode) {
		// When the user presses an arrow key, apply an initial force and enable additional
		// force to be added during the render loop
		if(keycode == Input.Keys.RIGHT) {
			hippo.applyForceToCenter(HORIZONTAL_VELOCITY, 0f, true);
			rightHeld = true;
		}
		if(keycode == Input.Keys.LEFT) {
			hippo.applyForceToCenter(-HORIZONTAL_VELOCITY, 0f,true);
			leftHeld = true;
		}
		if(keycode == Input.Keys.UP) {
			// Only allow jumping if the hippo is actually touching the ground
			if (hippo.getLinearVelocity().y <= 1 && hippo.getLinearVelocity().y >= 0 /* && hippo.getPosition().y < -2 */ ) {
				hippo.applyForceToCenter(0f, JUMP_VELOCITY, true);
				jumpHeight = JUMP_VELOCITY;
				upHeld = true;
			}
		}
		if(keycode == Input.Keys.DOWN) {
			hippo.applyForceToCenter(0f, 5f, true);
			downHeld = true;
		}

		return true;
	}

	@Override
	public boolean keyUp(int keycode) {
		// When the user releases an arrow key, disable force during the render loop
		if(keycode == Input.Keys.RIGHT)
			rightHeld = false;
		if(keycode == Input.Keys.LEFT)
			leftHeld = false;
		if(keycode == Input.Keys.UP)
			upHeld = false;
		if(keycode == Input.Keys.DOWN)
			downHeld = false;

		// If user hits spacebar, reset everything back to normal
		if(keycode == Input.Keys.SPACE|| keycode == Input.Keys.NUM_2) {
			hippo.setLinearVelocity(0f, 0f);
			hippo.setAngularVelocity(0f);
			hippoSprite.setPosition(0f,0f);
			hippo.setTransform(0f,0f,0f);

			ball.setLinearVelocity(0f, 0f);
			ball.setAngularVelocity(0f);
			ballSprite.setPosition(0f,0f);
			ball.setTransform(0f,0f,0f);
		}

		// Allow user to change ball restitution using comma and period keys
		if(keycode == Input.Keys.COMMA) {
			ball.getFixtureList().first().setRestitution(ball.getFixtureList().first().getRestitution() - 0.1f);
		}
		if(keycode == Input.Keys.PERIOD) {
			ball.getFixtureList().first().setRestitution(ball.getFixtureList().first().getRestitution()+0.1f);
		}
		if(keycode == Input.Keys.ESCAPE) {
			drawSprite = !drawSprite;
			if (!drawDebug && !drawSprite)
				drawDebug = true;
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
		return true;
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