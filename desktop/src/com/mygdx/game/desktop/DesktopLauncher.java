package com.mygdx.game.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.mygdx.game.GameScreen;
import com.mygdx.game.VolleyBall;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = VolleyBall.SCREEN_WIDTH;
		config.height = VolleyBall.SCREEN_HEIGHT;
		new LwjglApplication(new VolleyBall(), config);
	}
}
