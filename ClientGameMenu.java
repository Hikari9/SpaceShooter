import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;

public class ClientGameMenu extends MenuWithBackground {
	
	
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
		prevChat = getGame().getChatBox();
        chat = new ChatBox(
            new Font("Consolas", Font.PLAIN, 12),
            10, 37,
            padding + 10, padding + 15,
            ChatBox.LEFT, ChatBox.UP
        );
        
        // chat.truncater.stop();
        
        getGame().setChatBox(chat);
        getGame().addLayer(chat);
	}
	
	public ClientGameMenu(Menu previous) {
		super(previous);
		items.add(joiningLabel);
		nameLabel.text2 = ClientPopup.getPlayerName();
		ipLabel.text2 = ClientPopup.getHost();
		items.add(nameLabel);
		items.add(ipLabel);
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
	
	@Override
	public void onClose() {
		Sounds.bgm.stop();
		Sounds.shoot.enabled = true;
		super.onClose();
	}

	private MenuLabel joiningLabel = new MenuLabel(Global.WIDTH / 2, Global.HEIGHT /2 - 50,
				"Joining", Color.WHITE, Global.FONT, 80, Menu.CENTER) {
		
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
	
	
	protected MenuLabel2 nameLabel = new MenuLabel2(joiningLabel.bounds.x, joiningLabel.bounds.y + joiningLabel.font.getSize() + 10, "Your player name: ", "", Color.WHITE, "Consolas", 10, 22, Menu.CENTER);
	protected MenuLabel2 ipLabel = new MenuLabel2(nameLabel.bounds.x, nameLabel.bounds.y + nameLabel.bounds.height + 10, "Connecting to: " , "", Color.WHITE, "Consolas", 10, 22, Menu.CENTER);
	
}