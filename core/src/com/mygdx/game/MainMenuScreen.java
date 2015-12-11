package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MainMenuScreen implements Screen {

    final VolleyBall game;
    GameScreen gameScreen = null;
    Sound gameMusic;
    Texture background;
    Sprite backgroundSprite;

    OrthographicCamera camera;



    public MainMenuScreen(final VolleyBall game) {
        this.game = game;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 1200, 900);
        gameMusic = Gdx.audio.newSound(Gdx.files.internal("Sumo_music.mp3"));
        gameMusic.play();
        background = new Texture("mainscreen.jpg");
        backgroundSprite = new Sprite(background);
        backgroundSprite.setBounds(0f, 0f, 1200f, 900f);
        //backgroundSprite.setCenter(0, 0);

    }

    @Override
    public void render(float delta) {
       // Gdx.gl.glClearColor(0, 0, 0.2f, 1);
        //Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        backgroundSprite.draw(game.batch);
        game.font.draw(game.batch, "Welcome to Hippo VolleyBall!!! ", 100, 150);
        game.font.draw(game.batch, "Tap anywhere to begin!", 100, 100);
        game.batch.end();

        if (gameScreen == null)
            gameScreen = new GameScreen(game, this);

        if (Gdx.input.isTouched()) {
            game.setScreen(gameScreen);
            if (gameScreen.leftWin || gameScreen.rightWin)
                gameScreen.startNewGame();
            dispose();
        }


    }

    @Override
    public void resize(int width, int height) {}
    @Override
    public void show() {}
    @Override
    public void hide() {}
    @Override
    public void pause() {}
    @Override
    public void resume() {}
    @Override
    public void dispose() {}
}