package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MainMenuScreen implements Screen {

    final VolleyBall game;
    GameScreen gameScreen = null;
    InstructionScreen helpScreen = null;
    MainMenuScreen mainMenuScreen = null;
    Texture background;
    Sprite backgroundSprite;

    Skin skin;
    Stage stage;
    Viewport vport;

    public MainMenuScreen(final VolleyBall game) {
        create();
        this.game = game;
        mainMenuScreen = this;
    }

    public void create() {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        background = new Texture("mainscreen.jpg");
        backgroundSprite = new Sprite(background);
        backgroundSprite.setBounds(0f, 0f, 1200f, 900f);

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
        textButtonStyle.checked = skin.newDrawable("white", Color.BLUE);
        textButtonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY);

        textButtonStyle.font = skin.getFont("default");

        skin.add("default", textButtonStyle);

        // Create a button with the "default" TextButtonStyle. A 3rd parameter can be used to specify a name other than "default".
        final TextButton playButton = new TextButton("PLAY", textButtonStyle);
        playButton.setPosition(200, 200);
        stage.addActor(playButton);
        //stage.addActor(textButton);
        //stage.addActor(textButton);
        playButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                playButton.setText("Starting new game");
                if (gameScreen == null)
                    gameScreen = new GameScreen(game);
                game.setScreen(gameScreen);
                gameScreen.startNewGame();
                dispose();
            }
        });

        final TextButton helpButton = new TextButton("HELP", textButtonStyle);
        helpButton.setPosition(400, 200);
        stage.addActor(helpButton);
        helpButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                if (helpScreen == null)
                    helpScreen = new InstructionScreen(game);
                game.setScreen(helpScreen);
                dispose();
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        //Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
        game.batch.begin();
        backgroundSprite.draw(game.batch);
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