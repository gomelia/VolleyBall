package com.mygdx.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class VolleyBall extends Game {
	public static final int SCREEN_HEIGHT = 768;
	public static final int SCREEN_WIDTH = 950;

	public SpriteBatch batch;
	public BitmapFont font;
	public Music music;

	public static int scoreToWin = 15; // The least number of points to score before a player can win the game; 15 by default
	public static int scoreMargin = 2; // The difference between points scored by both players should be equal to or
	// greater than this in other to win the game; 2 by default

	public void create() {
		batch = new SpriteBatch();
		//Use LibGDX's default Arial font.
		font = new BitmapFont();
		// Fetching the music file incurs a significant loading time, so do it before creating the menu screen
		music = Gdx.audio.newMusic(Gdx.files.internal("Sumo_music.mp3"));
		music.setLooping(true);
		this.setScreen(new MainMenuScreen(this));
	}

	public void render() {
		super.render(); //important!
	}

	public void dispose() {
		batch.dispose();
		font.dispose();
	}
}