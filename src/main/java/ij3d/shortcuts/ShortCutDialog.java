
package ij3d.shortcuts;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

@SuppressWarnings("serial")
public class ShortCutDialog extends JDialog {

	public ShortCutDialog(final ShortCuts shortcuts) {
		final ShortCutTable table = new ShortCutTable(shortcuts);
		getContentPane().add(new JScrollPane(table));

		final JPanel buttons = new JPanel(new FlowLayout());
		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				shortcuts.reload();
				dispose();
			}
		});
		buttons.add(cancel);
		final JButton ok = new JButton("Ok");
		ok.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				shortcuts.save();
				dispose();
			}
		});
		buttons.add(ok);
		getContentPane().add(buttons, BorderLayout.SOUTH);

		pack();
		setVisible(true);
	}
}
