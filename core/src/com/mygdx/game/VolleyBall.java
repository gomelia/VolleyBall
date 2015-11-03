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
import com.badlogic.gdx.physics.box2d.*;

public class VolleyBall extends ApplicationAdapter implements InputProcessor {
	SpriteBatch batch;
	Sprite sprite, ballSprite;
	Texture img, ballImg;
	World world;
	Body body;
	Body ball;
	Body bodyBottomEdge;
	Body bodyTopEdge;
	Body bodyLeftSide;
	Body bodyRightSide;
	Box2DDebugRenderer debugRenderer;
	Matrix4 debugMatrix;
	OrthographicCamera camera;
	BitmapFont font;
	boolean upHeld = false, downHeld = false, rightHeld = false, leftHeld = false;

	float torque = 0.0f;
	boolean drawSprite = true;

	final float PIXELS_TO_METERS = 100f;

	@Override
	public void create() {

		batch = new SpriteBatch();
		img = new Texture("Hippo.png");
		ballImg = new Texture("volleyball.png");
		sprite = new Sprite(img);
		ballSprite = new Sprite(ballImg);

		//sprite.scale(1.0f);

		sprite.setPosition(-sprite.getWidth()/2,-sprite.getHeight()/2);
		ballSprite.setPosition(0,0);

		world = new World(new Vector2(0, -10f),true);

		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyDef.BodyType.DynamicBody;
		bodyDef.position.set((sprite.getX() + sprite.getWidth()/2) / PIXELS_TO_METERS,
				(sprite.getY() + sprite.getHeight()/2) / PIXELS_TO_METERS);

		body = world.createBody(bodyDef);

		PolygonShape shape = new PolygonShape();
		shape.setAsBox(sprite.getWidth()/2 / PIXELS_TO_METERS,
				sprite.getHeight()/2 / PIXELS_TO_METERS);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = shape;
		fixtureDef.density = 1.0f;
		fixtureDef.restitution = 0.0f;

		body.createFixture(fixtureDef);
		shape.dispose();

		// Ball
		BodyDef ballDef = new BodyDef();
		ballDef.type = BodyDef.BodyType.DynamicBody;
		ballDef.position.set((ballSprite.getX() + ballSprite.getWidth()/2) / PIXELS_TO_METERS,
				(ballSprite.getY() + ballSprite.getHeight()/2) / PIXELS_TO_METERS);

		ball = world.createBody(ballDef);

		CircleShape ballShape = new CircleShape();
		ballShape.setRadius((ballSprite.getWidth() / 2) / PIXELS_TO_METERS);

		FixtureDef ballFixtureDef = new FixtureDef();
		ballFixtureDef.shape = ballShape;
		ballFixtureDef.density = 0.3f;
		ballFixtureDef.restitution = 0.75f;

		ball.createFixture(ballFixtureDef);
		ballShape.dispose();

		// Bottom edge
		BodyDef bodyDef2 = new BodyDef();
		bodyDef2.type = BodyDef.BodyType.StaticBody;
		float w = Gdx.graphics.getWidth()/PIXELS_TO_METERS;
		// Set the height to just 50 pixels above the bottom of the screen so we can see the edge in the
		// debug renderer
		float h = Gdx.graphics.getHeight()/PIXELS_TO_METERS- 50/PIXELS_TO_METERS;
		//bodyDef2.position.set(0, h-10/PIXELS_TO_METERS);
		bodyDef2.position.set(0,0);
		FixtureDef fixtureDef2 = new FixtureDef();

		EdgeShape edgeShape = new EdgeShape();
		fixtureDef2.shape = edgeShape;
		edgeShape.set(-w/2,-h/2,w/2,-h/2);

		bodyBottomEdge = world.createBody(bodyDef2);
		bodyBottomEdge.createFixture(fixtureDef2);
		edgeShape.dispose();

		// We don't want the 50 pixel padding for the other edges, so set height to screen height
		h = Gdx.graphics.getHeight()/PIXELS_TO_METERS;

		// Top edge
		BodyDef bodyDef3 = new BodyDef();
		bodyDef3.type = BodyDef.BodyType.StaticBody;
		bodyDef3.position.set(0,0);
		FixtureDef fixtureDef3 = new FixtureDef();

		EdgeShape edgeShape2 = new EdgeShape();
		fixtureDef3.shape = edgeShape2;
		edgeShape.set(w/2,h/2,-w/2,h/2);

		bodyTopEdge = world.createBody(bodyDef3);
		bodyTopEdge.createFixture(fixtureDef3);
		edgeShape2.dispose();

		// Left edge
		BodyDef bodyDef4 = new BodyDef();
		bodyDef4.type = BodyDef.BodyType.StaticBody;
		bodyDef4.position.set(0,0);
		FixtureDef fixtureDef4 = new FixtureDef();

		EdgeShape edgeShape3 = new EdgeShape();
		fixtureDef4.shape = edgeShape3;
		edgeShape.set(-w/2,-h/2,-w/2,h/2);

		bodyLeftSide = world.createBody(bodyDef4);
		bodyLeftSide.createFixture(fixtureDef4);
		edgeShape3.dispose();

		// Right edge
		BodyDef bodyDef5 = new BodyDef();
		bodyDef5.type = BodyDef.BodyType.StaticBody;
		bodyDef5.position.set(0,0);
		FixtureDef fixtureDef5 = new FixtureDef();

		EdgeShape edgeShape4 = new EdgeShape();
		fixtureDef5.shape = edgeShape4;
		edgeShape.set(w/2,h/2,w/2,-h/2);

		bodyRightSide = world.createBody(bodyDef5);
		bodyRightSide.createFixture(fixtureDef5);
		edgeShape4.dispose();

		Gdx.input.setInputProcessor(this);

		debugRenderer = new Box2DDebugRenderer();
		font = new BitmapFont();
		font.setColor(Color.BLACK);
		camera = new OrthographicCamera(Gdx.graphics.getWidth(),Gdx.graphics.
				getHeight());
	}

	private float elapsed = 0;
	@Override
	public void render() {
		camera.update();
		// Step the physics simulation forward at a rate of 60hz
		world.step(1f/60f, 6, 2);

		//body.applyTorque(torque,true);
		body.setTransform(body.getPosition(), 0f);

		if(rightHeld)
			body.applyForceToCenter(10f,0f,true);
		if(leftHeld)
			body.applyForceToCenter(-10f,0f,true);
		if(upHeld)
			body.applyForceToCenter(0f,3f,true);
		if(downHeld)
			body.applyForceToCenter(0f, -5f, true);

		sprite.setPosition((body.getPosition().x * PIXELS_TO_METERS) - sprite.
						getWidth()/2 ,
				(body.getPosition().y * PIXELS_TO_METERS) -sprite.getHeight()/2 )
		;
		sprite.setRotation((float)Math.toDegrees(body.getAngle()));

		ballSprite.setPosition((ball.getPosition().x * PIXELS_TO_METERS) - ballSprite.
						getWidth()/2 ,
				(ball.getPosition().y * PIXELS_TO_METERS) -ballSprite.getHeight()/2 )
		;
		ballSprite.setRotation((float)Math.toDegrees(ball.getAngle()));

		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.setProjectionMatrix(camera.combined);
		debugMatrix = batch.getProjectionMatrix().cpy().scale(PIXELS_TO_METERS,
				PIXELS_TO_METERS, 0);
		batch.begin();

		batch.draw(sprite, sprite.getX(), sprite.getY(),sprite.getOriginX(),
				sprite.getOriginY(),
				sprite.getWidth(),sprite.getHeight(),sprite.getScaleX(),sprite.
						getScaleY(),sprite.getRotation());
		batch.draw(ballSprite, ballSprite.getX(), ballSprite.getY(),ballSprite.getOriginX(),
				ballSprite.getOriginY(),
				ballSprite.getWidth(),ballSprite.getHeight(),ballSprite.getScaleX(),ballSprite.
						getScaleY(),ballSprite.getRotation());

		//System.out.println(sprite.getY());
		/*
		font.draw(batch,
				"Restitution: " + body.getFixtureList().first().getRestitution(),
				-Gdx.graphics.getWidth()/2,
				Gdx.graphics.getHeight()/2 );
		*/
		batch.end();

		debugRenderer.render(world, debugMatrix);
	}

	@Override
	public void dispose() {
		img.dispose();
		world.dispose();
	}

	@Override
	public boolean keyDown(int keycode) {
		if(keycode == Input.Keys.RIGHT) {
			body.applyForceToCenter(10f, 0f, true);
			rightHeld = true;
		}
		if(keycode == Input.Keys.LEFT) {
			body.applyForceToCenter(-10f, 0f,true);
			leftHeld = true;
		}
		if(keycode == Input.Keys.UP) {
			if (sprite.getY() < -273) {
				body.applyForceToCenter(0f, 150f, true);
				upHeld = true;
			}
		}
		if(keycode == Input.Keys.DOWN) {
			body.applyForceToCenter(0f, 5f, true);
			downHeld = true;
		}

		return true;
	}

	@Override
	public boolean keyUp(int keycode) {
		if(keycode == Input.Keys.RIGHT)
			rightHeld = false;
		if(keycode == Input.Keys.LEFT)
			leftHeld = false;
		if(keycode == Input.Keys.UP)
			upHeld = false;
		if(keycode == Input.Keys.DOWN)
			downHeld = false;

		/*
		// On brackets ( [ ] ) apply torque, either clock or counterclockwise
		if(keycode == Input.Keys.RIGHT_BRACKET)
			torque += 0.1f;
		if(keycode == Input.Keys.LEFT_BRACKET)
			torque -= 0.1f;

		// Remove the torque using backslash /
		if(keycode == Input.Keys.BACKSLASH)
			torque = 0.0f;
		*/

		// If user hits spacebar, reset everything back to normal
		if(keycode == Input.Keys.SPACE|| keycode == Input.Keys.NUM_2) {
			body.setLinearVelocity(0f, 0f);
			body.setAngularVelocity(0f);
			torque = 0f;
			sprite.setPosition(0f,0f);
			body.setTransform(0f,0f,0f);

			ball.setLinearVelocity(0f, 0f);
			ball.setAngularVelocity(0f);
			ballSprite.setPosition(0f,0f);
			ball.setTransform(0f,0f,0f);
		}

		if(keycode == Input.Keys.COMMA) {
			body.getFixtureList().first().setRestitution(body.getFixtureList().first().getRestitution()-0.1f);
		}
		if(keycode == Input.Keys.PERIOD) {
			body.getFixtureList().first().setRestitution(body.getFixtureList().first().getRestitution()+0.1f);
		}
		if(keycode == Input.Keys.ESCAPE || keycode == Input.Keys.NUM_1)
			drawSprite = !drawSprite;

		return true;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}


	// On touch we apply force from the direction of the users touch.
	// This could result in the object "spinning"
	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		ball.applyForce(1f,1f,screenX,screenY,true);
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