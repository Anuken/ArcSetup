package io.anuke.arc.setup.desktop;

import io.anuke.arc.ApplicationCore;
import io.anuke.arc.backends.lwjgl3.Lwjgl3Application;
import io.anuke.arc.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.anuke.arc.setup.UI;

public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setTitle("uCore Project Setup");
		config.setWindowedMode(800, 600);
		config.setDecorated(false);
		config.setResizable(false);
		new Lwjgl3Application(new ApplicationCore(){
			@Override
			public void setup(){
				add(new UI());
			}
		}, config);
	}
}
