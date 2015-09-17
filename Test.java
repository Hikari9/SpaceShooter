import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class Test {
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


		final Game game = new Game();


		game.startTheGame();

		frame.add(game);

		frame.pack();
		frame.setVisible(true);

		frame.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				Ship turret = Global.spawnRandomTurret();
				turret.startSpawningBullets();
				game.turrets.clear();
				game.addShip(turret);
			}
		});
	}
}