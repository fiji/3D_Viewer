/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2016 Fiji developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package view4d;

import java.awt.Adjustable;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.ImageProducer;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;

/**
 * This class implements the window with the controls for the 4D viewer.
 *
 * @author Benjamin Schmid
 */
public class TimelineGUI implements ActionListener, KeyListener {

	private final JPanel p;
	private final boolean visible = false;

	final String nbbFile = "icons/nobounceback.png";
	final String bbFile = "icons/bounceback.png";
	final int bbIndex = 2;
	final Image bbImage, nbbImage;

	final String playFile = "icons/play.png";
	final String pauseFile = "icons/pause.png";
	final int playIndex = 3;
	final Image playImage, pauseImage;

	private static final String[] FILES = new String[] { "icons/first.png",
		"icons/last.png", "icons/nobounceback.png", "icons/play.png",
		"icons/record.png", "icons/faster.png", "icons/slower.png" };

	private static final String[] COMMANDS = new String[] { "FIRST", "LAST",
		"NOBOUNCEBACK", "PLAY", "RECORD", "FASTER", "SLOWER" };

	private final JButton[] buttons = new JButton[FILES.length];
	private final Timeline timeline;
	private final JScrollBar scroll;
	private final JTextField tf;

	/**
	 * Initializes a new Viewer4DController; opens a new new window with the
	 * control buttons for the 4D viewer.
	 * 
	 * @param tl
	 */
	public TimelineGUI(final Timeline tl) {
		this.timeline = tl;

		bbImage = loadIcon(bbFile);
		nbbImage = loadIcon(nbbFile);
		playImage = loadIcon(playFile);
		pauseImage = loadIcon(pauseFile);

		final GridBagLayout gridbag = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();

		p = new JPanel(gridbag);
		c.gridx = c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.EAST;
		c.weightx = c.weighty = 0.0;

		for (int i = 0; i < FILES.length; i++) {
			buttons[i] = new JButton(new ImageIcon(loadIcon(FILES[i])));
			buttons[i].setBorder(null);
			buttons[i].addActionListener(this);
			buttons[i].setActionCommand(COMMANDS[i]);
			gridbag.setConstraints(buttons[i], c);
			p.add(buttons[i]);
			c.gridx++;
		}
		// set up scroll bar
		final int min = timeline.getUniverse().getStartTime();
		final int max = timeline.getUniverse().getEndTime() + 1;
		final int cur = timeline.getUniverse().getCurrentTimepoint();

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		scroll = new JScrollBar(Adjustable.HORIZONTAL, cur, 1, min, max);
		scroll.addAdjustmentListener(new AdjustmentListener() {

			@Override
			public void adjustmentValueChanged(final AdjustmentEvent e) {
				showTimepoint(scroll.getValue());
			}
		});
		gridbag.setConstraints(scroll, c);
		p.add(scroll);

		// set up text field
		tf = new JTextField(2);
		tf.setText(Integer.toString(cur));
		tf.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(final KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					int v = 0;
					try {
						v = Integer.parseInt(tf.getText());
						showTimepoint(v);
						tf.selectAll();
					}
					catch (final Exception ex) {}
				}
			}
		});
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.EAST;
		c.weightx = c.weighty = 0.0;
		c.gridx++;
		gridbag.setConstraints(tf, c);
		p.add(tf);
	}

	private void showTimepoint(final int v) {
		timeline.getUniverse().showTimepoint(v);
	}

	public JPanel getPanel() {
		return p;
	}

	public void updateTimepoint(final int val) {
		scroll.setValue(val);
		tf.setText(Integer.toString(val));
	}

	public void updateStartAndEnd(final int start, final int end) {
		scroll.setMinimum(start);
		scroll.setMaximum(end + 1);
	}

	private Image loadIcon(final String name) {
		URL url;
		Image img = null;
		try {
			url = getClass().getResource(name);
			img =
				Toolkit.getDefaultToolkit().createImage(
					(ImageProducer) url.getContent());
		}
		catch (final Exception e) {
			e.printStackTrace();
		}
		if (img == null) throw new RuntimeException("Image not found: " + name);
		return img;
	}

	/**
	 * Toggle play/pause
	 */
	public synchronized void togglePlay() {
		if (!p.isVisible()) return;
		if (buttons[playIndex].getActionCommand().equals("PLAY")) {
			buttons[playIndex].setActionCommand("PAUSE");
			buttons[playIndex].setIcon(new ImageIcon(pauseImage));
			buttons[playIndex].setBorder(null);
			buttons[playIndex].repaint();
			timeline.play();
		}
		else {
			buttons[playIndex].setActionCommand("PLAY");
			buttons[playIndex].setIcon(new ImageIcon(playImage));
			buttons[playIndex].setBorder(null);
			buttons[playIndex].repaint();
			timeline.pause();
		}
	}

	@Override
	public void keyPressed(final KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_BACK_SLASH ||
			e.getKeyCode() == KeyEvent.VK_SPACE) togglePlay();
	}

	@Override
	public void keyReleased(final KeyEvent e) {}

	@Override
	public void keyTyped(final KeyEvent e) {}

	@Override
	public void actionPerformed(final ActionEvent e) {
		for (int i = 0; i < buttons.length; i++)
			buttons[i].repaint();

		final String command = e.getActionCommand();

		if (command.equals("BOUNCEBACK")) {
			buttons[bbIndex].setActionCommand("NOBOUNCEBACK");
			buttons[bbIndex].setIcon(new ImageIcon(nbbImage));
			buttons[bbIndex].setBorder(null);
			buttons[bbIndex].repaint();
			timeline.setBounceBack(true);
		}
		else if (command.equals("NOBOUNCEBACK")) {
			buttons[bbIndex].setActionCommand("BOUNCEBACK");
			buttons[bbIndex].setIcon(new ImageIcon(bbImage));
			buttons[bbIndex].setBorder(null);
			buttons[bbIndex].repaint();
			timeline.setBounceBack(false);
		}
		else if (command.equals("PLAY")) {
			buttons[playIndex].setActionCommand("PAUSE");
			buttons[playIndex].setIcon(new ImageIcon(pauseImage));
			buttons[playIndex].setBorder(null);
			buttons[playIndex].repaint();
			timeline.play();
		}
		else if (command.equals("PAUSE")) {
			buttons[playIndex].setActionCommand("PLAY");
			buttons[playIndex].setIcon(new ImageIcon(playImage));
			buttons[playIndex].setBorder(null);
			buttons[playIndex].repaint();
			timeline.pause();
		}
		else if (command.equals("RECORD")) {
			timeline.record().show();
		}
		else if (command.equals("FIRST")) {
			timeline.first();
		}
		else if (command.equals("LAST")) {
			timeline.last();
		}
		else if (command.equals("FASTER")) {
			timeline.faster();
		}
		else if (command.equals("SLOWER")) {
			timeline.slower();
		}
	}
}
