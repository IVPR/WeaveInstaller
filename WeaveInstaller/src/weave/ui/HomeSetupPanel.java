/*
    Weave (Web-based Analysis and Visualization Environment)
    Copyright (C) 2008-2011 University of Massachusetts Lowell

    This file is a part of Weave.

    Weave is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License, Version 3,
    as published by the Free Software Foundation.

    Weave is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Weave.  If not, see <http://www.gnu.org/licenses/>.
*/

package weave.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;

import weave.Revisions;
import weave.Settings;
import weave.async.AsyncCallback;
import weave.async.AsyncObserver;
import weave.async.AsyncTask;
import weave.configs.IConfig;
import weave.inc.SetupPanel;
import weave.managers.ConfigManager;
import weave.managers.IconManager;
import weave.reflect.Reflectable;
import weave.utils.BugReportUtils;
import weave.utils.DownloadUtils;
import weave.utils.FileUtils;
import weave.utils.ImageUtils;
import weave.utils.LaunchUtils;
import weave.utils.RemoteUtils;
import weave.utils.TimeUtils;
import weave.utils.TraceUtils;
import weave.utils.TransferUtils;
import weave.utils.UpdateUtils;
import weave.utils.ZipUtils;

@SuppressWarnings("serial")
public class HomeSetupPanel extends SetupPanel 
{
	private boolean refreshProgramatically = false;
	public JTabbedPane tabbedPane;
	public JPanel tab1, tab2, tab3, tab4, tab5;

	
	// ============== Tab 1 ============== //
	public JButton  installButton, refreshButton, 
					deployButton, deleteButton, 
					pruneButton, adminButton;
	public JLabel	downloadLabel;
	public JProgressBar progressbar;
	public WeaveStats weaveStats;
	public RevisionTable revisionTable;
	
	
	// ============== Tab 2 ============== //
	
	
	// ============== Tab 3 ============== //
	public JScrollPane settingsScrollPane;
	public TitledBorder settingsServerUpdatesTitle, settingsWeaveUpdatesTitle, settingsMaintenanceTitle, settingsProtoExtTitle;
	public JCheckBox settingsUpdatesAutoInstallCheckbox, settingsUpdatesCheckNewCheckbox;
	public JComboBox<String> settingsUpdatesCheckNewCombobox;
	public JCheckBox settingsMaintenanceDeleteLogsCheckbox, settingsMaintenanceDebugCheckbox;
	public JTextField settingsMaintenanceDeleteLogsTextfield;
	public JCheckBox settingsExtCheckbox, settingsProtocolCheckbox;
	
	
	// ============== Tab 4 ============== //
	public String faqURL = "http://ivpr.oicweave.org/faq.php?" + Calendar.getInstance().getTimeInMillis();
	public JEditorPane troubleshootHTML;
	public JScrollPane troubleshootScrollPane;

	// ============== Tab 5 ============== //
	public JLabel aboutImage, aboutTitle, aboutVersion;
	public JEditorPane aboutHTML;
	
	public HomeSetupPanel()
	{
		maxPanels = 1;
		
		setLayout(null);
		setBounds(0, 0, SetupPanel.RIGHT_PANEL_WIDTH, SetupPanel.RIGHT_PANEL_HEIGHT);

		JPanel panel = null;
		for (int i = 0; i < maxPanels; i++) {
			switch (i) {
				case 0: panel = createHomeSetupPanel(); 	break;
			}
			panels.add(panel);
			add(panel);
		}
		hidePanels();
		
		setVisible(true);

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				refreshProgramatically = true;
				refreshButton.doClick();
			}
		}, 1000);
		
		globalHashMap.put("HomeSetupPanel", HomeSetupPanel.this);
	}

	public JPanel createHomeSetupPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(null);
		panel.setBounds(0, 0, this.getWidth(), this.getHeight());
		panel.setBackground(new Color(0xFFFFFF));
		
		tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		tabbedPane.setBounds(0, 0, panel.getWidth(), panel.getHeight());

		tabbedPane.addTab("Weave", (tab1 = createTab1(tabbedPane)));
//		tabbedPane.addTab("Plugins", (tab2 = createTab2(tabbedPane)));
		tabbedPane.addTab("Settings", (tab3 = createTab3(tabbedPane)));
		tabbedPane.addTab("Troubleshoot", (tab4 = createTab4(tabbedPane)));
		tabbedPane.addTab("About", (tab5 = createTab5(tabbedPane)));
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event)
			{
				JPanel selectedPanel = (JPanel) tabbedPane.getSelectedComponent();
				if( selectedPanel == tab4 )
				{
					try {
						faqURL = "http://ivpr.oicweave.org/faq.php?" + Calendar.getInstance().getTimeInMillis();
//						System.out.println("page updated to " + faqURL);
						troubleshootHTML.setPage(faqURL);
						
						// Remove all link listeners
						for( HyperlinkListener h : troubleshootHTML.getHyperlinkListeners() )
							troubleshootHTML.removeHyperlinkListener(h);
						// Add new link listener
						troubleshootHTML.addHyperlinkListener(new HyperlinkListener() {
							@Override
							public void hyperlinkUpdate(HyperlinkEvent e) {
								if( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED )
								{
									try {
										LaunchUtils.browse(e.getURL().toURI());
									} catch (IOException ex) {
										TraceUtils.trace(TraceUtils.STDERR, ex);
										BugReportUtils.showBugReportDialog(ex);
									} catch (InterruptedException ex) {
										TraceUtils.trace(TraceUtils.STDERR, ex);
										BugReportUtils.showBugReportDialog(ex);
									} catch (URISyntaxException ex) {
										TraceUtils.trace(TraceUtils.STDERR, ex);
										BugReportUtils.showBugReportDialog(ex);
									}
								}
							}
						});
					} catch (IOException e) {
						TraceUtils.trace(TraceUtils.STDERR, e);
						BugReportUtils.showBugReportDialog(e);
					}
				}
			}
		});
		panel.add(tabbedPane);

//		tabbedPane.setEnabledAt(1, false);
		switchToTab(tab1);
		
		return panel;
	}


	@Reflectable
	public Boolean switchToTab(String name)
	{
		return switchToTab(tabbedPane.indexOfTab(name));
	}
	@Reflectable
	public Boolean switchToTab(Component c)
	{
		return switchToTab(tabbedPane.indexOfComponent(c));
	}
	@Reflectable
	public Boolean switchToTab(int index)
	{
		try {
			tabbedPane.setSelectedIndex(index);
		} catch (IndexOutOfBoundsException e) {
			TraceUtils.trace(TraceUtils.STDERR, e);
			return false;
		}
		return true;
	}
	
	
	public JPanel createTab(JComponent parent)
	{
		JPanel panel = new JPanel(null);
		panel.setBounds(0, 0, parent.getWidth(), parent.getHeight());
		panel.setBackground(Color.WHITE);
		
		return panel;
	}
	
	public JPanel createTab1(JComponent parent)
	{
		JPanel panel = createTab(parent);

		refreshButton = new JButton("Refresh");
		refreshButton.setBounds(330, 10, 100, 30);
		refreshButton.setToolTipText("Check for a new version of " + Settings.PROJECT_NAME);
		refreshButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent a) 
			{
				try {
					refreshInterface();
				} catch (InterruptedException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
				} catch (MalformedURLException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
				}
			}
		});
		
		
		installButton = new JButton("Install");
		installButton.setBounds(330, 45, 100, 30);
		installButton.setToolTipText("Download the latest version of "+ Settings.PROJECT_NAME +" and install it.");
		installButton.setEnabled(false);
		installButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent a)
			{
				try {
					setButtonsEnabled(false);
					downloadBinaries();
				} catch (InterruptedException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
				} catch (MalformedURLException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
				} catch (IOException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
				}
			}
		});
		
		deployButton = new JButton("Deploy");
		deployButton.setBounds(330, 140, 100, 30);
		deployButton.setToolTipText("Install Weave from a backup revision, selected on the left in the table.");
		deployButton.setVisible(true);
		deployButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent a) 
			{
				int index = revisionTable.getTable().getSelectedRow();
				if( index < 0 )
					return;
				
				extractBinaries(Revisions.getRevisionsList().get(index));
			}
		});
		
		
		deleteButton = new JButton("Delete");
		deleteButton.setBounds(330, 175, 100, 30);
		deleteButton.setToolTipText("Delete an individual revision, selected on the left in the table.");
		deleteButton.setVisible(true);
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent a) 
			{
				int index = revisionTable.getTable().getSelectedRow();
				if( index < 0 )
					return;
				
				if( JOptionPane.showConfirmDialog(
						null, 
						"Deleting revisions cannot be undone.\n\nAre you sure you want to continue?", 
						"Warning", 
						JOptionPane.YES_NO_OPTION, 
						JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION )
					return;
				
				File selectedFile = Revisions.getRevisionsList().get(index);
				FileUtils.recursiveDelete(selectedFile);

				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						refreshProgramatically = true;
						try {
							refreshInterface();
						} catch (InterruptedException e) {
							TraceUtils.trace(TraceUtils.STDERR, e);
						} catch (MalformedURLException e) {
							TraceUtils.trace(TraceUtils.STDERR, e);
						}
					}
				}, 1000);
			}
		});
		
		
		pruneButton = new JButton("Clean");
		pruneButton.setBounds(330, 210, 100, 30);
		pruneButton.setToolTipText("Auto-delete older revisions to free up space on your hard drive.");
		pruneButton.setVisible(true);
		pruneButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if( JOptionPane.showConfirmDialog(
						null, 
						"Auto-cleaned revisions will be deleted\nand cannot be undone.\n\nAre you sure you want to continue?",
						"Warning",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION )
					return;
				
				Revisions.pruneRevisions();
				
				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						refreshProgramatically = true;
						try {
							refreshInterface();
						} catch (InterruptedException e) {
							TraceUtils.trace(TraceUtils.STDERR, e);
						} catch (MalformedURLException e) {
							TraceUtils.trace(TraceUtils.STDERR, e);
						}
					}
				}, 1000);
			}
		});
		
		
		adminButton = new JButton("Launch Admin Console");
		adminButton.setBounds(10, 285, 300, 30);
		adminButton.setToolTipText("Open up the Admin Console");
		adminButton.setVisible(true);
		adminButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent a) 
			{
				try {
					LaunchUtils.openAdminConsole();
				} catch (IOException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
				} catch (URISyntaxException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
				} catch (InterruptedException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
				}
			}
		});
		

		weaveStats = new WeaveStats();
		weaveStats.setBounds(10, 10, 300, 75);
		weaveStats.setVisible(true);
		
		progressbar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
		progressbar.setBounds(10, 85, 420, 15);
		progressbar.setIndeterminate(true);
		progressbar.setVisible(false);
		
		downloadLabel = new JLabel();
		downloadLabel.setBounds(10, 105, 420, 25);
		downloadLabel.setFont(new Font(Settings.FONT, Font.PLAIN, 12));
		downloadLabel.setText("");
		downloadLabel.setVisible(false);
		
		revisionTable = new RevisionTable();
		revisionTable.setBounds(10, 140, 300, 135);
		revisionTable.setVisible(true);
		
		
		panel.add(weaveStats);
		panel.add(revisionTable);
		panel.add(progressbar);
		panel.add(downloadLabel);
		panel.add(refreshButton);
		panel.add(installButton);
		panel.add(deployButton);
		panel.add(deleteButton);
		panel.add(pruneButton);
		panel.add(adminButton);
		
		return panel;
	}
	public JPanel createTab2(JComponent parent)
	{
		JPanel panel = createTab(parent);
		return panel;
	}
	public JPanel createTab3(JComponent parent)
	{
		JPanel panel = createTab(parent);
		JPanel innerPanel = new JPanel();
		JPanel serverUpdateBox = new JPanel();
		JPanel weaveUpdateBox = new JPanel();
		JPanel maintenanceBox = new JPanel();
		JPanel protoextBox = new JPanel();
		
		innerPanel.setLayout(null);
		innerPanel.setSize(panel.getWidth() - 40, 800);
		innerPanel.setPreferredSize(new Dimension(parent.getWidth() - 40, 800));
		innerPanel.setBackground(Color.WHITE);
		

		////////////////////////////////////////////////////////////////////////////////////////////////
		// Weave Server Assistant Updates
		////////////////////////////////////////////////////////////////////////////////////////////////
		settingsServerUpdatesTitle = BorderFactory.createTitledBorder(null, "Server Assistant Updates", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font(Settings.FONT, Font.BOLD, 14), Color.BLUE);
		serverUpdateBox.setBounds(10, 10, innerPanel.getWidth() - 40, 150);
		serverUpdateBox.setLayout(null);
		serverUpdateBox.setBackground(Color.WHITE);
		serverUpdateBox.setBorder(settingsServerUpdatesTitle);
		
		settingsUpdatesAutoInstallCheckbox = new JCheckBox("Automatically install updates on startup");
		settingsUpdatesAutoInstallCheckbox.setBounds(10, 20, 300, 30);
		settingsUpdatesAutoInstallCheckbox.setBackground(Color.WHITE);
		settingsUpdatesAutoInstallCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
			}
		});
		serverUpdateBox.add(settingsUpdatesAutoInstallCheckbox);
		
		settingsUpdatesCheckNewCheckbox = new JCheckBox("Check for new updates");
		settingsUpdatesCheckNewCheckbox.setBounds(10, 50, 170, 30);
		settingsUpdatesCheckNewCheckbox.setBackground(Color.WHITE);
		settingsUpdatesCheckNewCheckbox.setEnabled(true);
		settingsUpdatesCheckNewCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				settingsUpdatesCheckNewCombobox.setEnabled(settingsUpdatesCheckNewCheckbox.isEnabled());
			}
		});
		serverUpdateBox.add(settingsUpdatesCheckNewCheckbox);
		
		settingsUpdatesCheckNewCombobox = new JComboBox<String>();
		settingsUpdatesCheckNewCombobox.setBounds(180, 50, 150, 30);
		settingsUpdatesCheckNewCombobox.setBackground(Color.WHITE);
		settingsUpdatesCheckNewCombobox.setEnabled(settingsUpdatesCheckNewCheckbox.isEnabled());
		settingsUpdatesCheckNewCombobox.addItem("Every hour");
		settingsUpdatesCheckNewCombobox.addItem("Every day");
		settingsUpdatesCheckNewCombobox.addItem("Every week");
		settingsUpdatesCheckNewCombobox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
			}
		});
		serverUpdateBox.add(settingsUpdatesCheckNewCombobox);
		

		////////////////////////////////////////////////////////////////////////////////////////////////
		// Weave Updates
		////////////////////////////////////////////////////////////////////////////////////////////////
		settingsWeaveUpdatesTitle = BorderFactory.createTitledBorder(null, "Weave Updates", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font(Settings.FONT, Font.BOLD, 14), Color.BLUE);
		weaveUpdateBox.setBounds(10, 170, innerPanel.getWidth() - 40, 150);
		weaveUpdateBox.setLayout(null);
		weaveUpdateBox.setBackground(Color.WHITE);
		weaveUpdateBox.setBorder(settingsWeaveUpdatesTitle);
		
		

		////////////////////////////////////////////////////////////////////////////////////////////////
		// Maintenance
		////////////////////////////////////////////////////////////////////////////////////////////////
		settingsMaintenanceTitle = BorderFactory.createTitledBorder(null, "Maintenance", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font(Settings.FONT, Font.BOLD, 14), Color.BLUE);
		maintenanceBox.setBounds(10, 330, innerPanel.getWidth() - 40, 150);
		maintenanceBox.setLayout(null);
		maintenanceBox.setBackground(Color.WHITE);
		maintenanceBox.setBorder(settingsMaintenanceTitle);
		
		settingsMaintenanceDeleteLogsCheckbox = new JCheckBox("Delete log files older than                 days");
		settingsMaintenanceDeleteLogsCheckbox.setBounds(10, 22, 300, 25);
		settingsMaintenanceDeleteLogsCheckbox.setBackground(Color.WHITE);
		settingsMaintenanceDeleteLogsCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
			}
		});
		maintenanceBox.add(settingsMaintenanceDeleteLogsCheckbox);
		
		settingsMaintenanceDeleteLogsTextfield = new JTextField();
		settingsMaintenanceDeleteLogsTextfield.setBounds(190, 20, 30, 30);
		settingsMaintenanceDeleteLogsTextfield.setBackground(Color.GRAY);
		maintenanceBox.add(settingsMaintenanceDeleteLogsTextfield);
		
		maintenanceBox.setComponentZOrder(settingsMaintenanceDeleteLogsTextfield, 0);
		maintenanceBox.setComponentZOrder(settingsMaintenanceDeleteLogsCheckbox, 1);

		

		////////////////////////////////////////////////////////////////////////////////////////////////
		// Protocols & Extensions
		////////////////////////////////////////////////////////////////////////////////////////////////
		
		settingsProtoExtTitle = BorderFactory.createTitledBorder(null, "Protocol & Extension", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font(Settings.FONT, Font.BOLD, 14), Color.GRAY);
		protoextBox.setBounds(10, 490, innerPanel.getWidth() - 40, 150);
		protoextBox.setLayout(null);
		protoextBox.setBackground(Color.WHITE);
		protoextBox.setBorder(settingsProtoExtTitle);
		
		settingsExtCheckbox = new JCheckBox("Enable Weave Extesion");
		settingsExtCheckbox.setBounds(10, 20, 300, 30);
		settingsExtCheckbox.setBackground(Color.WHITE);
		settingsExtCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Settings.enableWeaveExtension(settingsExtCheckbox.isSelected());
				} catch (IllegalArgumentException e1) {
					TraceUtils.trace(TraceUtils.STDERR, e1);
				} catch (IllegalAccessException e1) {
					TraceUtils.trace(TraceUtils.STDERR, e1);
				} catch (InvocationTargetException e1) {
					TraceUtils.trace(TraceUtils.STDERR, e1);
				}
			}
		});
		protoextBox.add(settingsExtCheckbox);
		
		settingsProtocolCheckbox = new JCheckBox("Enable Weave Protocol");
		settingsProtocolCheckbox.setBounds(10, 50, 300, 30);
		settingsProtocolCheckbox.setBackground(Color.WHITE);
		settingsProtocolCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Settings.enableWeaveProtocol(settingsProtocolCheckbox.isSelected());
				} catch (IllegalArgumentException e1) {
					TraceUtils.trace(TraceUtils.STDERR, e1);
				} catch (IllegalAccessException e1) {
					TraceUtils.trace(TraceUtils.STDERR, e1);
				} catch (InvocationTargetException e1) {
					TraceUtils.trace(TraceUtils.STDERR, e1);
				}
			}
		});
		protoextBox.add(settingsProtocolCheckbox);
		
		
		innerPanel.add(serverUpdateBox);
		innerPanel.add(weaveUpdateBox);
		innerPanel.add(maintenanceBox);
		innerPanel.add(protoextBox);
		
		
		settingsScrollPane = new JScrollPane();
		settingsScrollPane.setBounds(0, 0, parent.getWidth() - 10, parent.getHeight() - 30);
		settingsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		settingsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		settingsScrollPane.setViewportView(innerPanel);
		settingsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		settingsScrollPane.setVisible(true);
		
		panel.add(settingsScrollPane);
		
		return panel;
	}
	public JPanel createTab4(JComponent parent)
	{
		JPanel panel = createTab(parent);

		try {
			troubleshootHTML = new JEditorPane();
			troubleshootHTML.setPage(faqURL);
			troubleshootHTML.setBounds(0, 0, panel.getWidth() - 20, panel.getHeight() - 20);
			troubleshootHTML.setBackground(Color.WHITE);
			troubleshootHTML.setEditable(false);
			troubleshootHTML.setVisible(true);
			
			troubleshootScrollPane = new JScrollPane(troubleshootHTML, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			troubleshootScrollPane.setBounds(0, 0, parent.getWidth() - 10, parent.getHeight() - 30);
			troubleshootScrollPane.getVerticalScrollBar().setUnitIncrement(16);
			troubleshootScrollPane.setVisible(true);
		} catch (IOException e) {
			TraceUtils.trace(TraceUtils.STDERR, e);
			BugReportUtils.showBugReportDialog(e);
		}

		panel.add(troubleshootScrollPane);
		
		return panel;
	}
	public JPanel createTab5(JComponent parent)
	{
		JPanel panel = createTab(parent);
		
		try {
			aboutImage = new JLabel("");
			aboutImage.setBounds(20, 20, 100, 100);
			aboutImage.setIcon(new ImageIcon(ImageUtils.scale(ImageIO.read(IconManager.ICON_TRAY_LOGO_LARGE), aboutImage.getWidth(), ImageUtils.SCALE_WIDTH)));
			
			aboutTitle = new JLabel(Settings.SERVER_NAME);
			aboutTitle.setBounds(150, 30, 300, 30);
			aboutTitle.setFont(new Font(Settings.FONT, Font.BOLD, 18));
			
			aboutVersion = new JLabel(Settings.SERVER_VER);
			aboutVersion.setBounds(150, 60, 300, 30);
			aboutVersion.setFont(new Font(Settings.FONT, Font.PLAIN, 13));
			
			aboutHTML = new JEditorPane();
			aboutHTML.setBounds(20, 130, 400, 200);
			aboutHTML.setBackground(Color.WHITE);
			aboutHTML.setEditable(false);
			aboutHTML.setContentType("text/html");
			aboutHTML.setFont(new Font(Settings.FONT, Font.PLAIN, 10));
			aboutHTML.setText(	"Weave is a <b>We</b>b-based <b>A</b>nalysis and <b>V</b>isualization <b>E</b>nvironment designed to " +
								"enable visualization of any available  data by anyone for any purpose.<br><br><br><br>" +
								"(c) Institute for Visualization and Perception Research<br>" +
								"Visit: <a href='" + Settings.OICWEAVE_URL + "'>" + Settings.OICWEAVE_URL + "</a><br>");
			String htmlStyle = "body { 	font-family: " + aboutHTML.getFont().getFamily() + "; " +
										"font-size: " + aboutHTML.getFont().getSize() + "px; }" +
								"b { font-size: " + (aboutHTML.getFont().getSize() + 2) + "px; }";
			((HTMLDocument)aboutHTML.getDocument()).getStyleSheet().addRule(htmlStyle);
			aboutHTML.addHyperlinkListener(new HyperlinkListener() {
				@Override
				public void hyperlinkUpdate(HyperlinkEvent e) {
					if( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED )
					{
						try {
							LaunchUtils.browse(e.getURL().toURI());
						} catch (IOException ex) {
							TraceUtils.trace(TraceUtils.STDERR, ex);
							BugReportUtils.showBugReportDialog(ex);
						} catch (InterruptedException ex) {
							TraceUtils.trace(TraceUtils.STDERR, ex);
							BugReportUtils.showBugReportDialog(ex);
						} catch (URISyntaxException ex) {
							TraceUtils.trace(TraceUtils.STDERR, ex);
							BugReportUtils.showBugReportDialog(ex);
						}
					}
				}
			});
			
		} catch (IOException e) {
			TraceUtils.trace(TraceUtils.STDERR, e);
		}
		
		panel.add(aboutImage);
		panel.add(aboutTitle);
		panel.add(aboutVersion);
		panel.add(aboutHTML);
		
		return panel;
	}
	
	private void downloadBinaries() throws InterruptedException, IOException
	{
		// Get the install URL to the zip file
		final URL url;
		final String urlStr = RemoteUtils.getConfigEntry(RemoteUtils.WEAVE_BINARIES_URL);
		if( urlStr == null ) {
			JOptionPane.showConfirmDialog(null, 
					"A connection to the internet could not be established.\n\n" +
					"Please connect to the internet and try again.", 
					"No Connection", 
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
			setButtonsEnabled(true);
			pruneButton.setEnabled(Revisions.getNumberOfRevisions() > Settings.recommendPrune);
			return;
		}
		url = new URL(urlStr);

		// Get the zip file's file name
		String fileName = UpdateUtils.getWeaveUpdateFileName();
		if( fileName == null ) {
			JOptionPane.showConfirmDialog(null,
					"There was an error generating the update package filename.\n\n" +
					"Please try again later or if the problem persists,\n" +
					"report this issue as a bug for the developers.", 
					"Error", 
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
			setButtonsEnabled(true);
			pruneButton.setEnabled(Revisions.getNumberOfRevisions() > Settings.recommendPrune);
			return;
		}
		
		// Get the active servlet container
		IConfig actvContainer = ConfigManager.getConfigManager().getActiveContainer();
		if( actvContainer == null ) {
			JOptionPane.showMessageDialog(null, 
					"There is no active servlet selected.\n\n" + 
					"Please configure a servlet to use, then try again.", "Error", JOptionPane.ERROR_MESSAGE);
			setButtonsEnabled(true);
			pruneButton.setEnabled(Revisions.getNumberOfRevisions() > Settings.recommendPrune);
			return;
		}

		// Get the active servlet container's webapps directory
		File cfgWebapps = actvContainer.getWebappsDirectory();
		if( cfgWebapps == null || !cfgWebapps.exists() ) {
			JOptionPane.showMessageDialog(null, 
					"Webapps folder for " + actvContainer.getConfigName() + " is not set.", "Error", JOptionPane.ERROR_MESSAGE);
			setButtonsEnabled(true);
			pruneButton.setEnabled(Revisions.getNumberOfRevisions() > Settings.recommendPrune);
			return;
		}
		
		final File zipFile = new File(Settings.REVISIONS_DIRECTORY, fileName);
		
		final AsyncObserver observer = new AsyncObserver() {
			@Override
			public void onUpdate() {
				if( info.max == -1 ) {
					// Unknown max size - progress unavailable
					progressbar.setIndeterminate(true);
					downloadLabel.setText( 
							String.format("Downloading update.... %s @ %s",
								FileUtils.sizeify(info.cur), 
								DownloadUtils.speedify(info.speed)) );
				} else {
					// Known max size
					progressbar.setIndeterminate(false);
					progressbar.setValue( info.percent );
					if( info.time > 3600 )
						downloadLabel.setText(
								String.format("Downloading - %d%% - %s - %s (%s)", 
									info.percent, 
									"Calculating ETA...",
									FileUtils.sizeify(info.cur),
									DownloadUtils.speedify(info.speed)) );
					else if( info.time < 60 )
						downloadLabel.setText(
								String.format("Downloading - %d%% - %s - %s (%s)", 
									info.percent, 
									TimeUtils.format("%s s remaining", info.time),
									FileUtils.sizeify(info.cur),
									DownloadUtils.speedify(info.speed)) );
					else
						downloadLabel.setText(
								String.format("Downloading - %d%% - %s - %s (%s)",
									info.percent, 
									TimeUtils.format("%m:%ss remaining", info.time),
									FileUtils.sizeify(info.cur),
									DownloadUtils.speedify(info.speed)) );
				}
			}
		};
		AsyncCallback callback = new AsyncCallback() {
			@Override
			public void run(Object o) {
				int returnCode = (Integer) o;

				Settings.transferCancelled = false;
				Settings.downloadLocked = false;

				switch( returnCode )
				{
					case TransferUtils.COMPLETE:
						TraceUtils.put(TraceUtils.STDOUT, "DONE");
						downloadLabel.setText("Download Complete....");
						downloadLabel.setForeground(Color.BLACK);
	
						extractBinaries(zipFile);
						break;
					case TransferUtils.CANCELLED:
						TraceUtils.put(TraceUtils.STDOUT, "CANCELLED");
						downloadLabel.setText("Cancelling Download....");
						downloadLabel.setForeground(Color.BLACK);
						break;
					case TransferUtils.FAILED:
						TraceUtils.put(TraceUtils.STDOUT, "FAILED");
						downloadLabel.setText("Download Failed....");
						downloadLabel.setForeground(Color.RED);

						try {
							Thread.sleep(2000);
							refreshProgramatically = true;
							refreshInterface();
						} catch (InterruptedException e) {
							TraceUtils.trace(TraceUtils.STDERR, e);
							BugReportUtils.showBugReportDialog(e);
						} catch (MalformedURLException e) {
							TraceUtils.trace(TraceUtils.STDERR, e);
							BugReportUtils.showBugReportDialog(e);
						}
						break;
					case TransferUtils.OFFLINE:
						break;
				}
			}
		};
		AsyncTask task = new AsyncTask() {
			@Override
			public Object doInBackground() {
				Object o = TransferUtils.FAILED;
				try {
					observer.init(url);
					o = DownloadUtils.download(urlStr, zipFile, observer, 4 * TransferUtils.MB);
				} catch (ArithmeticException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
					BugReportUtils.showBugReportDialog(e);
				} catch (IOException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
					BugReportUtils.showBugReportDialog(e);
				} catch (InterruptedException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
					BugReportUtils.showBugReportDialog(e);
				}
				return o;
			}
		};

		if( !Settings.DOWNLOADS_TMP_DIRECTORY.exists() )
			Settings.DOWNLOADS_TMP_DIRECTORY.mkdirs();

		TraceUtils.trace(TraceUtils.STDOUT, "-> Downloading update.............");
		
		downloadLabel.setVisible(true);
		progressbar.setVisible(true);
		
		downloadLabel.setText("Downloading update.....");
		progressbar.setIndeterminate(true);
		
		Thread.sleep(1000);
		
		progressbar.setValue(0);
		progressbar.setIndeterminate(false);

		Settings.downloadLocked = true;
		Settings.transferCancelled = false;
		
		task.addCallback(callback);
		task.execute();
	}
	
	private void extractBinaries(final File zipFile)
	{
		final AsyncObserver observer = new AsyncObserver() {
			@Override
			public void onUpdate() {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						progressbar.setValue( info.percent / 2 );
						downloadLabel.setText( 
								String.format(
										"Extracting update.... %d%%", 
										info.percent / 2 ) );
					}
				});
			}
		};
		AsyncCallback callback = new AsyncCallback() {
			@Override
			public void run(Object o) {
				int returnCode = (Integer) o;
				
				switch( returnCode )
				{
					case TransferUtils.COMPLETE:
						TraceUtils.put(TraceUtils.STDOUT, "DONE");
						
						String folderName = Revisions.getRevisionName(zipFile.getAbsolutePath());
						moveBinaries(new File(Settings.UNZIP_DIRECTORY, folderName));
						break;
					case TransferUtils.FAILED:
						break;
					case TransferUtils.CANCELLED:
						break;
					case TransferUtils.OFFLINE:
						break;
				}
			}
		};
		AsyncTask task = new AsyncTask() {
			@Override
			public Object doInBackground() {
				Object o = TransferUtils.FAILED;
				try {
					observer.init(zipFile);
					o = ZipUtils.extract(zipFile, Settings.UNZIP_DIRECTORY, TransferUtils.OVERWRITE | TransferUtils.MULTIPLE_FILES, observer, 8 * TransferUtils.MB);
				} catch (ArithmeticException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
					BugReportUtils.showBugReportDialog(e);
				}catch (NullPointerException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
					// No bug report
				} catch (ZipException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
					BugReportUtils.showBugReportDialog(e);
				} catch (IOException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
					BugReportUtils.showBugReportDialog(e);
				} catch (InterruptedException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
					BugReportUtils.showBugReportDialog(e);
				}
				return o;
			}
		};

		try {
			if( !Settings.UNZIP_DIRECTORY.exists() )
				Settings.UNZIP_DIRECTORY.mkdirs();
			
			progressbar.setVisible(true);
			downloadLabel.setVisible(true);
			
			progressbar.setIndeterminate(true);
			downloadLabel.setText("Preparing Extraction....");
			Thread.sleep(1000);
			
			TraceUtils.trace(TraceUtils.STDOUT, "-> Extracting update..............");
			
			Settings.canQuit = false;
			
			downloadLabel.setText("Extracting update....");
			progressbar.setIndeterminate(false);
		} catch (InterruptedException e) {
			TraceUtils.trace(TraceUtils.STDERR, e);
			BugReportUtils.showBugReportDialog(e);
		}
		
		task.addCallback(callback);
		task.execute();
	}
	
	private void moveBinaries(final File unzippedFile)
	{
		final File configWebapps = ConfigManager.getConfigManager().getActiveContainer().getWebappsDirectory();
		
		final AsyncObserver observer = new AsyncObserver() {
			@Override
			public void onUpdate() {
				progressbar.setValue( 50 + info.percent / 2 );
				downloadLabel.setText( 
						String.format(
								"Installing update.... %d%%", 
								50 + info.percent / 2 ) );
			}
		};
		AsyncCallback callback = new AsyncCallback() {
			@Override
			public void run(Object o) {
				int returnCode = (Integer) o;
				
				switch( returnCode ) {
				case TransferUtils.COMPLETE:
					TraceUtils.put(TraceUtils.STDOUT, "DONE");
					downloadLabel.setText("Install complete....");
					
					Settings.canQuit = true;
					System.gc();

					ConfigManager
						.getConfigManager()
						.getActiveContainer()
						.setInstallVersion(Revisions.getRevisionVersion(unzippedFile.getAbsolutePath()));
					ConfigManager.getConfigManager().save();
					
					try {
						Settings.cleanUp();
						Thread.sleep(1000);
						refreshProgramatically = true;
						refreshInterface();
					} catch (InterruptedException e) {
						TraceUtils.trace(TraceUtils.STDERR, e);
					} catch (MalformedURLException e) {
						TraceUtils.trace(TraceUtils.STDERR, e);
					}
					break;
				case TransferUtils.CANCELLED:
					break;
				case TransferUtils.FAILED:
					break;
				case TransferUtils.OFFLINE:
					break;
				}
			}
		};
		AsyncTask task = new AsyncTask() {
			@Override
			public Object doInBackground() {
				int status = TransferUtils.COMPLETE;
				String[] files = unzippedFile.list();
				
				try {
					observer.init(unzippedFile);

					for( String file : files )
					{
						File source = new File(unzippedFile, file);
						File destination = new File(configWebapps, file);
						status &= FileUtils.copy(source, destination, TransferUtils.MULTIPLE_FILES | TransferUtils.OVERWRITE, observer, 8 * TransferUtils.MB);
					}
				} catch (ArithmeticException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
					BugReportUtils.showBugReportDialog(e);
				} catch (FileNotFoundException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
					BugReportUtils.showBugReportDialog(e);
				} catch (IOException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
					BugReportUtils.showBugReportDialog(e);
				} catch (InterruptedException e) {
					TraceUtils.trace(TraceUtils.STDERR, e);
					BugReportUtils.showBugReportDialog(e);
				}
				return status;
			}
		};

		TraceUtils.trace(TraceUtils.STDOUT, "-> Installing update..............");

		downloadLabel.setText("Installing Update....");
		progressbar.setIndeterminate(false);
		
		task.addCallback(callback);
		task.execute();
	}
	
	
	private void refreshInterface() throws InterruptedException, MalformedURLException
	{
		TraceUtils.traceln(TraceUtils.STDOUT, "-> Refreshing User Interface......");

		Settings.canQuit = false;
		
		setButtonsEnabled(false);
		int updateAvailable = UpdateUtils.isWeaveUpdateAvailable(!refreshProgramatically);
		weaveStats.refresh(updateAvailable);
		refreshProgramatically = false;

		Settings.canQuit = true;
		
		downloadLabel.setVisible(false);
		downloadLabel.setText("");
		progressbar.setVisible(false);
		progressbar.setIndeterminate(true);
		progressbar.setString("");
		progressbar.setValue(0);
		setButtonsEnabled(true);
		installButton.setEnabled(updateAvailable == UpdateUtils.UPDATE_AVAILABLE);
		pruneButton.setEnabled(Revisions.getNumberOfRevisions() > Settings.recommendPrune);
		revisionTable.updateTableData();
	}
	
	private void setButtonsEnabled(boolean enabled)
	{
		refreshButton.setEnabled(enabled);
		installButton.setEnabled(enabled);
		deployButton.setEnabled(enabled);
		deleteButton.setEnabled(enabled);
		pruneButton.setEnabled(enabled);
	}
}
