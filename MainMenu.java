import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import javax.imageio.*;
import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

public class MainMenu extends Menu {

	public MainMenu() {
		// play bgm first
		
        int x = 685;
		int y = 129;
        int w = 276;
        int h = 43;
		int off = 74;
		
		final ImageMenuItem instructionsScreen = new ImageMenuItem("instructionsScreen.png", off, off, Global.WIDTH - off * 2 - w - 20, Global.HEIGHT - off * 2);
		// instructionsScreen.setVisible(false);
		
		final ImageMenuItem powerupsScreen = new ImageMenuItem("powerupsScreen.png", instructionsScreen.bounds.x, instructionsScreen.bounds.y, instructionsScreen.bounds.width, instructionsScreen.bounds.height);
		powerupsScreen.setVisible(false);
		

		items.add(new ImageMenuItem("logo1.png", x, 76, w, 100));
		items.add(new HoverableImage("hostGame.png", x, y += off, w, h) {
            public void onClick() {
				createHostGame();
            }
			
        });
		items.add(new HoverableImage("joinGame.png", x, y += off, w, h) {
            public void onClick() {
                createClientGame();
            }
        });
		items.add(new HoverableImage("instructions.png", x, y += off, w, h) {
			public void onClick() {
				instructionsScreen.setVisible(!instructionsScreen.isVisible());
				powerupsScreen.setVisible(false);
			}
		});
		items.add(new HoverableImage("powerups.png", x, y += off, w, h) {
			public void onClick() {
				instructionsScreen.setVisible(false);
				powerupsScreen.setVisible(!powerupsScreen.isVisible());
			}
		});
		items.add(new HoverableImage("quit.png", x, y += off, w, h) {
            public void onClick() {
				if (JOptionPane.showConfirmDialog(getGame().getFrame(), "Are you sure you want to quit?", "Quit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
                System.exit(0);
            }
        });
		
		items.add(instructionsScreen);
		items.add(powerupsScreen);
		
	}
	
	void createClientGame() {
	
		JFrame frame = getGame().getFrame();
		
		int res = ClientPopup.displayDialog(frame);
		if (res != 1) {
			return;
		}
		
		MainMenu.this.setVisible(false);
		
		ClientGameMenu client = new ClientGameMenu(MainMenu.this);
		client.getGame().addMenu(client);
		client.createLog();
		
		Game original = getGame();
		ClientGame clientGame = null;
		
		// turn on ambient bgm while joining
		Sounds.shoot.enabled = false;
		Sounds.bgm.loop(Clip.LOOP_CONTINUOUSLY);
		
		Global.debugging = true;
		
		try {
			do {
			
				String name = ClientPopup.getPlayerName();
				String host = ClientPopup.getHost();
				
				client.nameLabel.text2 = name;
				client.ipLabel.text2 = host;
			
				try {
					clientGame = new ClientGame(name, host);
					break;
				}
				catch (ClientNameException ex) { // name exists
					JOptionPane.showMessageDialog(frame, "The name [" + name + "] already exists in the server.\nEnter a new name.", "Error - Name already exists", JOptionPane.ERROR_MESSAGE);
					int result = ClientPopup.displayDialog(frame);
					if (result != 1)
						throw new IOException(ex);
				}
				catch (UnknownHostException ex) {
					JOptionPane.showMessageDialog(frame, "Unknown host [" + host + "].\nMake sure you are connected to the same network.", "Error - Unknown host", JOptionPane.ERROR_MESSAGE);
					int result = ClientPopup.displayDialog(frame);
					if (result != 1)
						throw ex;
				}
		
			} while (true);
		}
		catch (IOException ex) { // was not able to connect
			System.out.println("EX");
			client.onClose();
			original.removeLayer(client);
			Game.setActiveGame(original);
			return;
		}
		catch (NullPointerException ex) {
			System.out.println("Something went null: " + ex);
		}
		finally {
			// reset sounds
			Sounds.bgm.stop();
			Sounds.shoot.enabled = true;
		}
		
		// success
		
		frame.remove(original);
		original.dispose();
		
		frame.add(clientGame);
		
		clientGame.getPlayerShip().getControls().setContainer(frame);
		
		FullScreenToggle.addToggleToGame(frame, clientGame, KeyEvent.VK_F11);
		
		// force frame to resize components
        frame.setVisible(true);
		
		clientGame.startTheGame();
		
	}
	
	void createHostGame() {
		
		
		JFrame frame = getGame().getFrame();
		
		// update settings first
		int res;
		do {
			if (HostPopup.playerNameDialog(frame) == null)
				return;
			res = HostPopup.hostSettings(frame);
			if (res == JOptionPane.CLOSED_OPTION)
				return;
		} while (res != 1);
		
		int numPlayers = HostPopup.getPlayers();
		
		// display host menu for loading
		
		MainMenu.this.setVisible(false);
		
		HostGameMenu host = new HostGameMenu(MainMenu.this);
		host.getGame().addMenu(host);
		host.createLog();
								
		Game original = getGame();
		ServerGame serverGame = null;
		
		// turn on ambient bgm while hosting
		Sounds.shoot.enabled = false;
		Sounds.bgm.loop(Clip.LOOP_CONTINUOUSLY);
		
		try {
			serverGame = new ServerGame(numPlayers, HostPopup.getPlayerName());
		}
		catch (IOException ex) {
			host.onClose();
			original.removeLayer(host);
			Game.setActiveGame(original);
			return; // not success
		}
		finally {
			// reset sounds
			Sounds.bgm.stop();
			Sounds.shoot.enabled = true;
		}
		
		// success
		
		frame.remove(original);
		original.dispose();
		
		frame.add(serverGame);
		
		serverGame.getPlayerShip().getControls().setContainer(frame);
		FullScreenToggle.addToggleToGame(frame, serverGame, KeyEvent.VK_F11);
        
		// force frame to resize components
        frame.setVisible(true);
        
		serverGame.startTheGame();
                
	}
	
}