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

public class VolleyBall extends ApplicationAdapter implements InputProcessor {
	SpriteBatch batch;
	Sprite hippoSprite, ballSprite;
	Texture hippoImg, ballImg;
	World world;
	Body hippo, ball;
	Body bottomEdge, topEdge, leftEdge, rightEdge;
	Box2DDebugRenderer debugRenderer;
	Matrix4 debugMatrix;
	OrthographicCamera camera;
	BitmapFont font;

	float torque = 0.0f;
	boolean drawSprite = true, drawDebug = true;
	boolean upHeld = false, downHeld = false, rightHeld = false, leftHeld = false;

	final float PIXELS_TO_METERS = 100f;

	@Override
	public void create() {
		batch = new SpriteBatch();
		hippoImg = new Texture("Hippo.png");
		ballImg = new Texture("volleyball.png");
		hippoSprite = new Sprite(hippoImg);
		ballSprite = new Sprite(ballImg);

		hippoSprite.setPosition(-hippoSprite.getWidth()/2,-hippoSprite.getHeight()/2);
		ballSprite.setPosition(0,0);

		world = new World(new Vector2(0, -10f),true);

		// Creating the hippo body
		BodyDef hippoBodyDef = new BodyDef();
		hippoBodyDef.type = BodyDef.BodyType.DynamicBody;
		hippoBodyDef.position.set((hippoSprite.getX() + hippoSprite.getWidth()/2) / PIXELS_TO_METERS,
				(hippoSprite.getY() + hippoSprite.getHeight()/2) / PIXELS_TO_METERS);

		hippo = world.createBody(hippoBodyDef);

		PolygonShape shape = new PolygonShape();
		shape.setAsBox(hippoSprite.getWidth()/2 / PIXELS_TO_METERS,
				hippoSprite.getHeight()/2 / PIXELS_TO_METERS);

		FixtureDef hippoFixtureDef = new FixtureDef();
		hippoFixtureDef.shape = shape;
		hippoFixtureDef.density = 1.0f;
		hippoFixtureDef.restitution = 0.0f; // No bounce on collision with walls or floor

		hippo.createFixture(hippoFixtureDef);
		shape.dispose();

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

		// Prevent the hippo's angular facing from changing
		hippo.setTransform(hippo.getPosition(), 0f);

		// Check if any of the keys are being held, then apply force accordingly
		if(rightHeld)
			hippo.applyForceToCenter(10f,0f,true);
		if(leftHeld)
			hippo.applyForceToCenter(-10f,0f,true);
		if(upHeld)
			hippo.applyForceToCenter(0f,3f,true);
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
			hippo.applyForceToCenter(10f, 0f, true);
			rightHeld = true;
		}
		if(keycode == Input.Keys.LEFT) {
			hippo.applyForceToCenter(-10f, 0f,true);
			leftHeld = true;
		}
		if(keycode == Input.Keys.UP) {
			// Only allow jumping if the hippo is actually touching the ground
			if (hippoSprite.getY() < -273) {
				hippo.applyForceToCenter(0f, 150f, true);
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