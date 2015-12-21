package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Align;

public class InstructionScreen implements Screen {

    final VolleyBall game;
    Texture background;
    Sprite backgroundSprite;
    CharSequence helpMessage;

    public InstructionScreen(final VolleyBall game) {
        create();
        this.game = game;
    }

    public void create() {
        background = new Texture("mainscreen.jpg");
        backgroundSprite = new Sprite(background);
        backgroundSprite.setBounds(0f, 0f, 950f, 768f);
        helpMessage = "Welcome to Hippo Volleyball! \n" +
                "Click anywhere to return to main menu. \n\n\n\n" +
                "This game is designed to be played by two players only. \n\n" +
                "The red hippo starts on the left side of the screen and is controlled with the WASD keys. \n" +
                "The blue hippo starts on the right side of the screen and is controlled with the arrow keys. \n" +
                "In Hippo Volleyball, you don't abide by the rules set by puny humans. You are allowed to hit \n" +
                "the ball any number of times in a row, and you can jump over the net to your opponent's side \n" +
                "to launch an all-out offense. \n\n" +
                "The first hippo to reach " + VolleyBall.scoreToWin + " points wins the game! \n" +
                "Click anywhere to return to main menu.";
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.begin();
        backgroundSprite.draw(game.batch);
        // Before changing the font scale, grab the previous scale values
        float scaleX = game.font.getData().scaleX;
        float scaleY = game.font.getData().scaleY;
        // Set the scale of the font to double the default
        game.font.getData().setScale(1.5f);
        // First print the message in pure black; this will provide a drop shadow effect that improves readability
        game.font.setColor(0f, 0f, 0f, 255f);
        game.font.draw(game.batch, helpMessage, 0, VolleyBall.SCREEN_HEIGHT - 102.5f, VolleyBall.SCREEN_WIDTH, Align.center, false);
        // Print the message again in bright green
        game.font.setColor(0f, 100f, 0f, 255f);
        game.font.draw(game.batch, helpMessage, 0, VolleyBall.SCREEN_HEIGHT - 100, VolleyBall.SCREEN_WIDTH, Align.center, false);
        // Set the game font scale back to its previous values
        game.font.getData().setScale(scaleX, scaleY);
        game.batch.end();

        if (Gdx.input.isTouched()) {
            game.setScreen(new MainMenuScreen(game));
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