package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;

public class SettingsScreen implements Screen {

    final VolleyBall game;
    Texture background;
    Sprite backgroundSprite;

    Skin skin;
    Stage stage;

    public SettingsScreen (final VolleyBall game) {
        create();
        this.game = game;
    }

    public void create() {
        background = new Texture("mainscreen.jpg");
        backgroundSprite = new Sprite(background);
        backgroundSprite.setBounds(0f, 0f, 950f, 768f);
        backgroundSprite.setAlpha(200f);

        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // A skin can be loaded via JSON or defined programmatically, either is fine. Using a skin is optional but strongly
        // recommended solely for the convenience of getting a texture, region, etc as a drawable, tinted drawable, etc.
        skin = new Skin();
        // Generate a 1x1 white texture and store it in the skin named "white".
        Pixmap pixmap = new Pixmap(100, 100, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.GREEN);
        pixmap.fill();

        skin.add("white", new Texture(pixmap));

        // Store the default libgdx font under the name "default".
        BitmapFont bfont = new BitmapFont();
        bfont.getData().setScale(1, 1);
        skin.add("default", bfont);

        // Configure a TextButtonStyle and name it "default". Skin resources are stored by type, so this doesn't overwrite the font.
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);
        textButtonStyle.down = skin.newDrawable("white", Color.DARK_GRAY);
        //textButtonStyle.checked = skin.newDrawable("white", Color.BLUE);
        textButtonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY);

        textButtonStyle.font = skin.getFont("default");

        skin.add("default", textButtonStyle);

        final TextButton scoreTotalDecreaseButton = new TextButton("<", textButtonStyle);
        scoreTotalDecreaseButton.setBounds(0f,0f, 50f, 100f);
        scoreTotalDecreaseButton.setPosition(VolleyBall.SCREEN_WIDTH/2 - 100f, VolleyBall.SCREEN_HEIGHT/2, Align.center);
        stage.addActor(scoreTotalDecreaseButton);
        scoreTotalDecreaseButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                game.decreaseScore();
            }
        });

        final TextButton scoreTotalIncreaseButton = new TextButton(">", textButtonStyle);
        scoreTotalIncreaseButton.setBounds(0f,0f, 50f, 100f);
        scoreTotalIncreaseButton.setPosition(VolleyBall.SCREEN_WIDTH/2 + 100f, VolleyBall.SCREEN_HEIGHT/2, Align.center);
        stage.addActor(scoreTotalIncreaseButton);
        scoreTotalIncreaseButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                game.increaseScore();
            }
        });

        final TextButton toMainMenu = new TextButton("RETURN TO MAIN MENU", textButtonStyle);
        toMainMenu.setBounds(0f,0f, 200f, 50f);
        toMainMenu.setPosition(VolleyBall.SCREEN_WIDTH/2, VolleyBall.SCREEN_HEIGHT/2 - 200f, Align.center);
        stage.addActor(toMainMenu);
        toMainMenu.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });
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
        game.font.getData().setScale(2f);
        // First print the message in pure black; this will provide a drop shadow effect that improves readability
        game.font.setColor(0f, 0f, 0f, 255f);
        game.font.draw(game.batch, "Score\n" +
                "to win:\n" + VolleyBall.scoreToWin, 0f, VolleyBall.SCREEN_HEIGHT/2 + 50f, VolleyBall.SCREEN_WIDTH, Align.center, false);
        // Print the message again in bright green
        game.font.setColor(0f, 100f, 0f, 255f);
        game.font.draw(game.batch, "Score\n" +
                "to win:\n" + VolleyBall.scoreToWin, 0f, VolleyBall.SCREEN_HEIGHT/2 + 50f, VolleyBall.SCREEN_WIDTH, Align.center, false);
        // Set the game font scale back to its previous values
        game.font.getData().setScale(scaleX, scaleY);

        game.batch.end();

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
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
    public void dispose() {
        background.dispose();
        stage.dispose();
        skin.dispose();
    }
}
