import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;

public class HostGameMenu extends MenuWithBackground {
	
	ChatBox prevChat, chat;
	@Override
	public void setVisible(boolean v) {
		if (v == false) {
			chat.clear();
			getGame().removeLayer(chat);
			getGame().setChatBox(prevChat);
		}
        super.setVisible(v);
	}
	
	public void createLog() {
		// clear log
		Global.Log.clear();
	
		// create new chat
		prevChat = getGame().getChatBox();
        chat = new ChatBox(
            new Font("Consolas", Font.PLAIN, 12),
            10, 37,
            padding + 10, padding + 15,
            ChatBox.LEFT, ChatBox.UP
        );
        
        chat.truncater.stop();
        
        getGame().setChatBox(chat);
        getGame().addLayer(chat);
	}
	
	public HostGameMenu(Menu previous) {
	
        super(previous); // MenuWithBackground
        
		items.add(hostingLabel);
		items.add(nameLabel);
		try {
			nameLabel.text2 += InetAddress.getLocalHost().getHostName().toUpperCase();
		}
		catch (UnknownHostException ex) {
			nameLabel.text2 += "<connect to a network first>";
		}
		items.add(ipLabel);
		try {
			ipLabel.text2 += InetAddress.getLocalHost().getHostAddress();
		}
		catch (UnknownHostException ex) {
			ipLabel.text2 += "<connect to a network first>";
		}
        items.add(playerNameLabel);
        items.add(playersLabel);
        items.add(timeLabel);
        items.add(gameTypeLabel);
        // items.add(killCapLabel);
        items.add(lifeLabel);
        
		new FixedTimer(new FixedTask() {
			public void run() {
				if (getGame().isDisposed() || Game.activeGame().isDisposed()) {
					stop();
					return;
				}
				String s = Global.Log.poll();
				if (s == null) return;
				getGame().log(s);
			}
		}).start();
	}
	
	public int padding = 60;
		
	private MenuLabel hostingLabel = new MenuLabel(Global.WIDTH / 2, Global.HEIGHT /2 - 50,
				"Hosting", Color.WHITE, Global.FONT, 80, Menu.CENTER) {
		
		private int dotTimer = 1000;
		private long lastDraw = System.currentTimeMillis();
		private String dotString = "";
		private int dotNum = 1;
		
		private String getDots() {
			if (dotString.length() != dotNum) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < dotNum; ++i)
					sb.append(".");
				dotString = sb.toString();
			}
			return dotString;
		}
		@Override
		public void draw(Graphics2D g) {
			super.draw(g);
			float dotX = bounds.x + bounds.width * (1 - offset);
			long time = System.currentTimeMillis();
			if (time - lastDraw > dotTimer) {
				dotNum = (dotNum) % 5 + 1;
				lastDraw = time;
			}
			g.drawString(getDots(), dotX, bounds.y + font.getSize() / 2);
		}
	};

	private MenuLabel2 nameLabel = new MenuLabel2(hostingLabel.bounds.x, hostingLabel.bounds.y + hostingLabel.font.getSize() + 10, "Your computer name:  ", "", Color.WHITE, "Consolas", 10, 22, Menu.CENTER);
	private MenuLabel2 ipLabel = new MenuLabel2(nameLabel.bounds.x, nameLabel.bounds.y + nameLabel.bounds.height + 10, "Your IP address:  ", "", Color.WHITE, "Consolas", 10, 22, Menu.CENTER);
	private MenuLabel2 playerNameLabel = new MenuLabel2(ipLabel.bounds.x, ipLabel.bounds.y + ipLabel.bounds.height + 10, "Your player name:  ", HostPopup.getPlayerName(), Color.WHITE, "Consolas", 10, 22, Menu.CENTER);
	private MenuLabel2 playersLabel = new MenuLabel2(playerNameLabel.bounds.x, playerNameLabel.bounds.y + playerNameLabel.bounds.height + 10, "# of players:  ", "" + HostPopup.getPlayers(), Color.WHITE, "Consolas", 10, 22, Menu.CENTER);
	private MenuLabel2 lifeLabel = new MenuLabel2(playersLabel.bounds.x, playersLabel.bounds.y + playersLabel.bounds.height + 10, "Life:  ", "" + HostPopup.getLife(), Color.WHITE, "Consolas", 10, 22, Menu.CENTER);
	private MenuLabel2 timeLabel = new MenuLabel2(lifeLabel.bounds.x, lifeLabel.bounds.y + lifeLabel.bounds.height + 10, "Time limit:  ", HostPopup.getTimeString(), Color.WHITE, "Consolas", 10, 22, Menu.CENTER);
	private MenuLabel2 gameTypeLabel = new MenuLabel2(timeLabel.bounds.x, timeLabel.bounds.y + timeLabel.bounds.height + 10, "Game type:  ", HostPopup.getGameTypeString(), Color.WHITE, "Consolas", 10, 22, Menu.CENTER);
	// private MenuLabel2 killCapLabel = new MenuLabel2(gameTypeLabel.bounds.x, gameTypeLabel.bounds.y + gameTypeLabel.bounds.height + 10, "Kill cap:  ", "" + HostPopup.getKillCap(), Color.WHITE, "Consolas", 10, 22, Menu.CENTER);
}