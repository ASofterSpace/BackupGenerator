/**
 * Unlicensed code created by A Softer Space, 2021
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator.gui;

import com.asofterspace.backupGenerator.BackupCtrl;
import com.asofterspace.backupGenerator.BackupGenerator;
import com.asofterspace.backupGenerator.integrityCheck.IntegrityCheckCtrl;
import com.asofterspace.backupGenerator.output.OutputUtils;
import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.gui.Arrangement;
import com.asofterspace.toolbox.gui.GuiUtils;
import com.asofterspace.toolbox.gui.MainWindow;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.Utils;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.io.IOException;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;


public class GUI extends MainWindow {

	private final static String CONFIG_KEY_WIDTH = "mainFrameWidth";
	private final static String CONFIG_KEY_HEIGHT = "mainFrameHeight";
	private final static String CONFIG_KEY_LEFT = "mainFrameLeft";
	private final static String CONFIG_KEY_TOP = "mainFrameTop";

	private final static String CUR_DIR_BASE_TEXT = "Current Directory (shown here if turned on in settings)";

	private BackupCtrl backupCtrl;

	private IntegrityCheckCtrl integrityCheckCtrl;

	private JMenuItem close;

	private JLabel currentDirectoryLabel;

	private ConfigFile configuration;


	public GUI(BackupCtrl backupCtrl, IntegrityCheckCtrl integrityCheckCtrl, ConfigFile config) {

		this.backupCtrl = backupCtrl;

		this.integrityCheckCtrl = integrityCheckCtrl;

		this.configuration = config;

		// enable anti-aliasing for swing
		System.setProperty("swing.aatext", "true");
		// enable anti-aliasing for awt
		System.setProperty("awt.useSystemAAFontSettings", "on");
	}

	@Override
	public void run() {

		super.create();

		refreshTitleBar();

		createMenu(mainFrame);

		createMainPanel(mainFrame);

		// do not call super.show, as we are doing things a little bit
		// differently around here (including restoring from previous
		// position...)
		// super.show();

		final Integer lastWidth = configuration.getInteger(CONFIG_KEY_WIDTH, -1);
		final Integer lastHeight = configuration.getInteger(CONFIG_KEY_HEIGHT, -1);
		final Integer lastLeft = configuration.getInteger(CONFIG_KEY_LEFT, -1);
		final Integer lastTop = configuration.getInteger(CONFIG_KEY_TOP, -1);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Stage everything to be shown
				mainFrame.pack();

				// now display the whole jazz
				mainFrame.setVisible(true);

				if ((lastWidth < 1) || (lastHeight < 1)) {
					GuiUtils.maximizeWindow(mainFrame);
				} else {
					mainFrame.setSize(lastWidth, lastHeight);

					mainFrame.setPreferredSize(new Dimension(lastWidth, lastHeight));

					mainFrame.setLocation(new Point(lastLeft, lastTop));
				}

				mainFrame.addComponentListener(new ComponentAdapter() {
					public void componentResized(ComponentEvent componentEvent) {
						configuration.set(CONFIG_KEY_WIDTH, mainFrame.getWidth());
						configuration.set(CONFIG_KEY_HEIGHT, mainFrame.getHeight());
					}

					public void componentMoved(ComponentEvent componentEvent) {
						configuration.set(CONFIG_KEY_LEFT, mainFrame.getLocation().x);
						configuration.set(CONFIG_KEY_TOP, mainFrame.getLocation().y);
					}
				});

				BackupGenerator.setGuiVisible(true);
			}
		});
	}

	private JMenuBar createMenu(JFrame parent) {

		JMenuBar menu = new JMenuBar();

		JMenu file = new JMenu("Backup");
		menu.add(file);

		JMenuItem pause = new JMenuItem("Pause");
		pause.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				backupCtrl.pause();
				integrityCheckCtrl.pause();
			}
		});
		file.add(pause);

		JMenuItem resume = new JMenuItem("Resume");
		resume.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				backupCtrl.resume();
				integrityCheckCtrl.resume();
			}
		});
		file.add(resume);

		file.addSeparator();

		JMenuItem cancel = new JMenuItem("Cancel");
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				backupCtrl.cancel();
				integrityCheckCtrl.cancel();
			}
		});
		file.add(cancel);

		file.addSeparator();

		close = new JMenuItem("Exit");
		close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
		close.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		file.add(close);

		JMenu settings = new JMenu("Settings");
		menu.add(settings);

		JCheckBoxMenuItem toggleShowingCurDir = new JCheckBoxMenuItem("Show Current Directory");
		toggleShowingCurDir.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean newVal = !OutputUtils.getPrintDirectories();
				OutputUtils.setPrintDirectories(newVal);
				if (!newVal) {
					currentDirectoryLabel.setText(CUR_DIR_BASE_TEXT);
				}
				toggleShowingCurDir.setState(newVal);
			}
		});
		settings.add(toggleShowingCurDir);

		JCheckBoxMenuItem toggleReportChangingActions = new JCheckBoxMenuItem("Report Output for Every Write Action");
		toggleReportChangingActions.setState(backupCtrl.getReportChangingActions());
		toggleReportChangingActions.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean newVal = !backupCtrl.getReportChangingActions();
				backupCtrl.setReportChangingActions(newVal);
				toggleReportChangingActions.setState(newVal);
			}
		});
		settings.add(toggleReportChangingActions);

		JCheckBoxMenuItem toggleReportAllActions = new JCheckBoxMenuItem("Report Output for Every Write and Check Action");
		toggleReportAllActions.setState(backupCtrl.getReportAllActions());
		toggleReportAllActions.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean newVal = !backupCtrl.getReportAllActions();
				backupCtrl.setReportAllActions(newVal);
				toggleReportAllActions.setState(newVal);
			}
		});
		settings.add(toggleReportAllActions);

		JCheckBoxMenuItem toggleDebug = new JCheckBoxMenuItem("Debug Mode (Forward Output to System.out)");
		toggleDebug.setState(false);
		toggleDebug.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean newVal = !OutputUtils.getDebugMode();
				OutputUtils.setDebugMode(newVal);
				toggleDebug.setState(newVal);
			}
		});
		settings.add(toggleDebug);

		JMenu windowMenu = new JMenu("Window");
		menu.add(windowMenu);

		JMenuItem curItem = new JMenuItem("Copy Content");
		curItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GuiUtils.copyToClipboard(OutputUtils.getErrorLogContent());
			}
		});
		windowMenu.add(curItem);

		curItem = new JMenuItem("Clear Content");
		curItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				OutputUtils.clearErrorLog();
			}
		});
		windowMenu.add(curItem);

		JMenu huh = new JMenu("?");

		JMenuItem openConfigPath = new JMenuItem("Open Config Path");
		openConfigPath.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Desktop.getDesktop().open(configuration.getParentDirectory().getJavaFile());
				} catch (IOException ex) {
					// do nothing
				}
			}
		});
		huh.add(openConfigPath);

		JMenuItem about = new JMenuItem("About");
		about.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String aboutMessage = "This is the " + Utils.getProgramTitle() + ".\n" +
					"Version: " + Utils.getVersionNumber() + " (" + Utils.getVersionDate() + ")\n" +
					"Brought to you by: A Softer Space";
				JOptionPane.showMessageDialog(mainFrame, aboutMessage, "About", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		huh.add(about);
		menu.add(huh);

		parent.setJMenuBar(menu);

		return menu;
	}

	private JPanel createMainPanel(JFrame parent) {

		JPanel mainPanel = new JPanel();
		mainPanel.setPreferredSize(new Dimension(800, 500));
		GridBagLayout mainPanelLayout = new GridBagLayout();
		mainPanel.setLayout(mainPanelLayout);

		JLabel targetLabel = new JLabel("(target will appear here)");
		mainPanel.add(targetLabel, new Arrangement(0, 0, 1.0, 0.05));
		OutputUtils.setTargetLabel(targetLabel);

		currentDirectoryLabel = new JLabel(CUR_DIR_BASE_TEXT);
		mainPanel.add(currentDirectoryLabel, new Arrangement(0, 1, 1.0, 0.05));
		OutputUtils.setCurrentDirectoryLabel(currentDirectoryLabel);

		JLabel outputLabel = new JLabel("(output will appear here)");
		mainPanel.add(outputLabel, new Arrangement(0, 2, 1.0, 0.05));
		OutputUtils.setOutputLabel(outputLabel);

		JTextArea errorMemo = new JTextArea("");
		JScrollPane errorScroller = new JScrollPane(errorMemo);
		mainPanel.add(errorScroller, new Arrangement(0, 3, 1.0, 1.0));
		OutputUtils.setErrorMemo(errorMemo);

		parent.add(mainPanel, BorderLayout.CENTER);

		return mainPanel;
	}

	private void refreshTitleBar() {
		mainFrame.setTitle(Utils.getProgramTitle());
	}

}
