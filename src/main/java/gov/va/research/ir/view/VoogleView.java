/*
 *  Copyright 2011 United States Department of Veterans Affairs,
 *		Health Services Research & Development Service
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package gov.va.research.ir.view;

import gov.va.research.ir.ThreadUtils;
import gov.va.research.ir.model.CodeNameCount;
import gov.va.research.ir.model.Command;
import gov.va.research.ir.model.Field;
import gov.va.research.ir.model.NameCount;
import gov.va.research.ir.model.SaveFile;
import gov.va.research.ir.model.SearchTerm;
import gov.va.research.ir.model.SearchWorker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.pdfbox.exceptions.COSVisitorException;

/**
 * @author doug
 *
 */
public class VoogleView implements SearchResultDisplayer<SearchPanel.SearchRow> {

	private static final Logger LOG = LoggerFactory.getLogger(VoogleView.class);
	private static final int APP_PREFERRED_WIDTH = 1024;
	private static final Pattern HTML_TAG_PATTERN = Pattern.compile("\\<.*?>");
	private static final String QUESTION_IMAGE_FILENAME = "/img/questionImage.png";

	public JFrame frame;
	public JSplitPane mainPanel;
	private Dimension screenSize;
	private SearchPanel searchPanel;
	private SummaryPanel summaryPanel;
	private JButton searchButton;
	private JButton helpButton;
	private JLabel statusField;
	private JButton queryRecommendationButton;
	private JProgressBar progressBar;
	private String error;
	private JComboBox<String> dataSetComboBox;
	private ActionListener actionListener;
	private long searchStartTime;
	private SwingWorker<?, ?> savingMessageWorker;

	public VoogleView(final ActionListener al) {
		this.actionListener = al;
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 *
	 * @throws IOException
	 */
	private void initialize() {
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				setLookAndFeel();
				URL logoURL = this.getClass().getResource(/*"/logo.gif"*/"/img/Facts-Ferret.png");  //
				ImageIcon logo = new ImageIcon(logoURL);
				JLabel logoLabel = new JLabel(logo);
				logoLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		//		logoLabel.setAlignmentY(JLabel.TOP_ALIGNMENT);


				frame = new JFrame();
				frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				screenSize = frame.getToolkit().getScreenSize();
				int appHeight = Math.round(screenSize.height * 0.9f);
				frame.setBounds(Math
								.round((screenSize.width - APP_PREFERRED_WIDTH) / 2),
						Math.round((screenSize.height - appHeight) / 2),
						APP_PREFERRED_WIDTH, appHeight);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setBackground(Color.WHITE);

				searchPanel = new SearchPanel(actionListener);
				searchPanel.addHierarchyListener(new HierarchyListener() {

					@Override
					public void hierarchyChanged(HierarchyEvent he) {
						System.out.println("SearchPanel hierarchy event: " + he.toString());
					}

				});

				JPanel superSearchPanel = new JPanel();
				superSearchPanel.setLayout(new GridBagLayout());
				superSearchPanel.setBackground(Color.WHITE);
				GridBagConstraints c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = 0;
				c.gridwidth = 2;
				c.gridheight = 1;
				c.anchor = GridBagConstraints.PAGE_START;
				superSearchPanel.add(logoLabel);
				c.gridx = 0;
				c.gridy = 1;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.anchor = GridBagConstraints.PAGE_START;
				superSearchPanel.add(searchPanel, c);

				searchButton = new JButton("Search");
				searchButton.setActionCommand(Command.SEARCH.toString());
				searchButton.addActionListener(actionListener);
				c.gridx = 1;
				c.gridy = 1;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.anchor = GridBagConstraints.LINE_START;
				superSearchPanel.add(searchButton, c);

				URL helpImageURL = this.getClass().getResource(QUESTION_IMAGE_FILENAME);
				ImageIcon helpIcon = null;
				try {
					BufferedImage helpImage = ImageIO.read(helpImageURL);
					helpIcon = new ImageIcon(helpImage.getScaledInstance(16, 16, Image.SCALE_SMOOTH));
				} catch (IOException e) {
					throw new RuntimeException("Failed to load image: " + QUESTION_IMAGE_FILENAME, e);
				}

				helpButton = new JIconButton(helpIcon);
				helpButton.setActionCommand(Command.HELP.toString());
				helpButton.addActionListener(actionListener);
				c.gridx = 2;
				c.gridy = 1;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.anchor = GridBagConstraints.LINE_START;
				superSearchPanel.add(helpButton, c);

				JPanel optionsPanel = new JPanel();
				optionsPanel.setBackground(Color.white);
				//optionsPanel.add(new JLabel("DataSet:"));
				optionsPanel.setVisible(true);
//				dataSetComboBox = new JComboBox<String>();
//				dataSetComboBox.addItem("initializing...");
//				dataSetComboBox.setEnabled(false);
				//optionsPanel.add(dataSetComboBox);

				JSeparator verticalSeparator = new JSeparator(SwingConstants.VERTICAL);
				verticalSeparator.setPreferredSize(new Dimension(0, 20));

		     	verticalSeparator.setVisible(true);

				optionsPanel.add(verticalSeparator, "align right");

				queryRecommendationButton = new JButton("Query Recommendation");
				queryRecommendationButton.setActionCommand(Command.QUERYRECOMMENDATION.toString());
				queryRecommendationButton.setVisible(false);

				queryRecommendationButton.addActionListener(actionListener);
				optionsPanel.add(queryRecommendationButton);

				superSearchPanel.add((optionsPanel));
				c.gridx = 0;
				c.gridy = 5;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.anchor = GridBagConstraints.LINE_START;
				superSearchPanel.add(optionsPanel, c);
				superSearchPanel.setBackground(Color.white);

				JPanel statusPanel = new JPanel(new BorderLayout());
				statusPanel.setBackground(Color.WHITE);
				statusPanel.setBorder(null);
				statusField = new JLabel();
				statusField.setVisible(false);
				statusField.setPreferredSize(new Dimension(0, 19));
				statusPanel.add(statusField, BorderLayout.WEST);
				progressBar = new JProgressBar();
				progressBar.setVisible(false);
				statusPanel.add(progressBar, BorderLayout.CENTER);
				JLinkButton feedbackButton = new JLinkButton("Feedback");
				feedbackButton.setActionCommand(Command.FEEDBACK.toString());
				feedbackButton.addActionListener(actionListener);
				statusPanel.add(feedbackButton, BorderLayout.EAST);

				try {
					summaryPanel = new SummaryPanel(actionListener);
				} catch (IOException e) {
					setError(e.getMessage());
					e.printStackTrace();
				}
				summaryPanel.add(statusPanel, BorderLayout.SOUTH);
				mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(superSearchPanel), summaryPanel);
				mainPanel.setBackground(Color.WHITE);
				mainPanel.setDividerLocation((int)(screenSize.height * 0.3));

				frame.getContentPane().add(mainPanel);
				frame.getRootPane().setDefaultButton(searchButton);
			}
		});
	}

	private static void setLookAndFeel() {
		try {
			boolean lafset = false;
			if (!lafset) {
				UIManager.setLookAndFeel(UIManager
						.getSystemLookAndFeelClassName());
			}
		} catch (Exception e) {
			System.err.println("Failed to set look-and-feel");
		}
	}

	public List<SearchTerm> getSearchTerms() {
		List<SearchTerm> searchTerms = new ArrayList<SearchTerm>();
		for (SearchPanel.SearchRow row : getRows()) {
			SearchTerm st = row.toSearchTerm();
			if (st.term != null && st.term.length() > 0) {
				searchTerms.add(row.toSearchTerm());
			}
		}
		return searchTerms;
	}

	public JPanel getSearchPanel() {
		return this.searchPanel;
	}

	public SummaryPanel getSummaryPanel() {
		return summaryPanel;
	}
	public void addRow(final Field field, final String text) {
		searchPanel.addRow(field, text);
		frame.validate();
	}

	public List<SearchPanel.SearchRow> getRows() {
		return searchPanel.getRows();
	}

	public void clearResults() {
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				if (summaryPanel != null) {
					summaryPanel.reset();
					summaryPanel.validate();
					summaryPanel.repaint();
				}
			}
		});
	}

	public void chooseDrug(List<NameCount> chooses,final SearchPanel.SearchRow row) {
		/*
        String[] choices = {
                "oxymorphone",
                "HYDROmorphone-ropivacaine",
                "guaifenesin-hydromorphone",
                "HYDROmorphone",
                "morphine",
                "hydromorphone",
                "bupivacaine-hydromorphone",
                "apomorphine",
                "morphine liposomal",
                "bupivacaine-HYDROmorphone",
                "morphine-naltrexone"
        };*/

		JFrame frame;
		frame = new JFrame();
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setBackground(Color.WHITE);

		final Collection<JCheckBox> queryDrugs = new ArrayList<JCheckBox>(
				chooses.size());

		final JDialog queryDrugDialog = new JDialog(
				frame, "Please choose one or more drugs to search from database", true);
		queryDrugDialog.setLayout(new BorderLayout());
		JPanel queryRecommendationPanel = new JPanel();
		queryRecommendationPanel.setLayout(new BoxLayout(queryRecommendationPanel, BoxLayout.PAGE_AXIS));

		for (NameCount choice : chooses) {
			JCheckBox cb = new JCheckBox(choice.name);
			queryDrugs.add(cb);
			queryRecommendationPanel.add(cb, "wrap");
		}
		queryDrugDialog.add(new JScrollPane(
				queryRecommendationPanel), BorderLayout.CENTER);
		//final Collection<JCheckBox> queryRecommendations = new ArrayList<JCheckBox>(
		//		choices.length);
//		List<SearchTerm> searchTerms= getSearchTerms();
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String chosenDrugs = "";
				if ("OK".equals(e.getActionCommand())) {
					for (JCheckBox checkBox : queryDrugs) {
						if (checkBox != null  && checkBox.isSelected()) {
							//searchPanel.addRow(Field.DOCUMENT_TEXT, checkBox.getText());
							frame.validate();
							//chosendrugs.add(checkBox.getText());
							if (chosenDrugs.equals(""))
								chosenDrugs= chosenDrugs + checkBox.getText();
							else
								chosenDrugs= chosenDrugs+"|"+checkBox.getText();
							//addRow(Field.DOCUMENT_TEXT,
							//checkBox.getText());
						}
					}
					System.out.println(chosenDrugs);
					row.valueField.setText(chosenDrugs);
                    /*
                    for ( int i=0; i<searchTerms.size(); i++) {
                        SearchTerm searchTerm= searchTerms.get(i);
                        if ( searchTerm.field == Field.DRUGS ) {
                            try {
                                SearchTerm st= new SearchTerm(chosenDrugs, searchTerm.field, searchTerm.boolOp, searchTerm.qualifier);
                                searchTerms.remove(i);
                                searchTerms.add(i,st);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                                //view.error("A data set must be selected");
                                return;
                            }
                        }
                    }*/

				}
				queryDrugDialog.pack();
				queryDrugDialog.setVisible(false);
				queryDrugDialog.dispose();
				Container parent = queryDrugDialog
						.getParent();
				if (parent != null) {
					parent.remove(queryDrugDialog);
				}
			}
		};
		JButton btnOK = new JButton("OK");
		btnOK.setActionCommand("OK");
		btnOK.addActionListener(al);
		JButton btnCancel = new JButton("Cancel");
		btnCancel.setActionCommand("CANCEL");
		btnCancel.addActionListener(al);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel,
				BoxLayout.LINE_AXIS));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(btnOK);
		buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		buttonPanel.add(btnCancel);
		buttonPanel.add(Box.createRigidArea(new Dimension(15, 0)));
		queryDrugDialog.add(buttonPanel,
				BorderLayout.SOUTH);
		queryDrugDialog.setBounds(
				frame.getX() + 20,
				frame.getY() + 20,
				queryDrugDialog.getPreferredSize().width + 30,
				queryDrugDialog.getPreferredSize().height + 30);
		// display dialog
		queryDrugDialog.setVisible(true);
	}


	public void chooseDiagnosis(List<CodeNameCount> chooses,final SearchPanel.SearchRow row) {
		JFrame frame;
		frame = new JFrame();
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setBackground(Color.WHITE);

		final Collection<JCheckBox> queryDiagnosis = new ArrayList<JCheckBox>(
				chooses.size());

		final JDialog queryDiagnosisDialog = new JDialog(
				frame, "Please choose one or more diagnosis to search from database", true);
		queryDiagnosisDialog.setLayout(new BorderLayout());
		JPanel queryRecommendationPanel = new JPanel();
		queryRecommendationPanel.setLayout(new BoxLayout(queryRecommendationPanel, BoxLayout.PAGE_AXIS));

		for (CodeNameCount choice : chooses) {
			JCheckBox cb = new JCheckBox(choice.name);
			queryDiagnosis.add(cb);
			queryRecommendationPanel.add(cb, "wrap");
		}
		queryDiagnosisDialog.add(new JScrollPane(
				queryRecommendationPanel), BorderLayout.CENTER);
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String chosenDiagnosis = "";
				if ("OK".equals(e.getActionCommand())) {
					for (JCheckBox checkBox : queryDiagnosis) {
						if (checkBox != null
								&& checkBox.isSelected()) {
							frame.validate();
							if (chosenDiagnosis.equals(""))
								chosenDiagnosis= chosenDiagnosis + checkBox.getText();
							else
								chosenDiagnosis= chosenDiagnosis+"|"+checkBox.getText();
						}
					}
					System.out.println(chosenDiagnosis);
					row.valueField.setText(chosenDiagnosis);
				}
				queryDiagnosisDialog.pack();
				queryDiagnosisDialog.setVisible(false);
				queryDiagnosisDialog.dispose();
				Container parent = queryDiagnosisDialog
						.getParent();
				if (parent != null) {
					parent.remove(queryDiagnosisDialog);
				}
			}
		};
		JButton btnOK = new JButton("OK");
		btnOK.setActionCommand("OK");
		btnOK.addActionListener(al);
		JButton btnCancel = new JButton("Cancel");
		btnCancel.setActionCommand("CANCEL");
		btnCancel.addActionListener(al);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel,
				BoxLayout.LINE_AXIS));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(btnOK);
		buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		buttonPanel.add(btnCancel);
		buttonPanel.add(Box.createRigidArea(new Dimension(15, 0)));
		queryDiagnosisDialog.add(buttonPanel,
				BorderLayout.SOUTH);
		queryDiagnosisDialog.setBounds(
				frame.getX() + 20,
				frame.getY() + 20,
				queryDiagnosisDialog.getPreferredSize().width + 30,
				queryDiagnosisDialog.getPreferredSize().height + 30);
		// display dialog
		queryDiagnosisDialog.setVisible(true);
	}

	public void chooseICD9Code(List<CodeNameCount> chooses,final SearchPanel.SearchRow row) {
		JFrame frame;
		frame = new JFrame();
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setBackground(Color.WHITE);

		final Collection<JCheckBox> queryDiagnosis = new ArrayList<JCheckBox>(
				chooses.size());

		final JDialog queryDiagnosisDialog = new JDialog(
				frame, "Please choose one or more ICD9 code to search from database", true);
		queryDiagnosisDialog.setLayout(new BorderLayout());
		JPanel queryRecommendationPanel = new JPanel();
		queryRecommendationPanel.setLayout(new BoxLayout(queryRecommendationPanel, BoxLayout.PAGE_AXIS));

		for (CodeNameCount choice : chooses) {
			JCheckBox cb = new JCheckBox(choice.code);
			queryDiagnosis.add(cb);
			queryRecommendationPanel.add(cb, "wrap");
		}
		queryDiagnosisDialog.add(new JScrollPane(
				queryRecommendationPanel), BorderLayout.CENTER);
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String chosenDiagnosis = "";
				if ("OK".equals(e.getActionCommand())) {
					for (JCheckBox checkBox : queryDiagnosis) {
						if (checkBox != null
								&& checkBox.isSelected()) {
							frame.validate();
							if (chosenDiagnosis.equals(""))
								chosenDiagnosis= chosenDiagnosis + checkBox.getText();
							else
								chosenDiagnosis= chosenDiagnosis+"|"+checkBox.getText();
						}
					}
					System.out.println(chosenDiagnosis);
					row.valueField.setText(chosenDiagnosis);
				}
				queryDiagnosisDialog.pack();
				queryDiagnosisDialog.setVisible(false);
				queryDiagnosisDialog.dispose();
				Container parent = queryDiagnosisDialog
						.getParent();
				if (parent != null) {
					parent.remove(queryDiagnosisDialog);
				}
			}
		};

		JButton btnOK = new JButton("OK");
		btnOK.setActionCommand("OK");
		btnOK.addActionListener(al);
		JButton btnCancel = new JButton("Cancel");
		btnCancel.setActionCommand("CANCEL");
		btnCancel.addActionListener(al);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel,
				BoxLayout.LINE_AXIS));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(btnOK);
		buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		buttonPanel.add(btnCancel);
		buttonPanel.add(Box.createRigidArea(new Dimension(15, 0)));
		queryDiagnosisDialog.add(buttonPanel,
				BorderLayout.SOUTH);
		queryDiagnosisDialog.setBounds(
				frame.getX() + 20,
				frame.getY() + 20,
				queryDiagnosisDialog.getPreferredSize().width + 30,
				queryDiagnosisDialog.getPreferredSize().height + 30);
		// display dialog
		queryDiagnosisDialog.setVisible(true);
	}

	public void chooseLab(List<CodeNameCount> chooses,final SearchPanel.SearchRow row) {
		JFrame frame;
		frame = new JFrame();
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setBackground(Color.WHITE);

		final Collection<JCheckBox> queryDiagnosis = new ArrayList<JCheckBox>(
				chooses.size());

		final JDialog queryDiagnosisDialog = new JDialog(
				frame, "Please choose one or more Lab names to search from database", true);
		queryDiagnosisDialog.setLayout(new BorderLayout());
		JPanel queryRecommendationPanel = new JPanel();
		queryRecommendationPanel.setLayout(new BoxLayout(queryRecommendationPanel, BoxLayout.PAGE_AXIS));

		for (CodeNameCount choice : chooses) {
			JCheckBox cb = new JCheckBox(choice.name);
			queryDiagnosis.add(cb);
			queryRecommendationPanel.add(cb, "wrap");
		}
		queryDiagnosisDialog.add(new JScrollPane(
				queryRecommendationPanel), BorderLayout.CENTER);
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String chosenDiagnosis = "";
				if ("OK".equals(e.getActionCommand())) {
					for (JCheckBox checkBox : queryDiagnosis) {
						if (checkBox != null
								&& checkBox.isSelected()) {
							frame.validate();
							if (chosenDiagnosis.equals(""))
								chosenDiagnosis= chosenDiagnosis + checkBox.getText();
							else
								chosenDiagnosis= chosenDiagnosis+"|"+checkBox.getText();
						}
					}
					System.out.println(chosenDiagnosis);
					row.qualifierField.setText(chosenDiagnosis);
				}
				queryDiagnosisDialog.pack();
				queryDiagnosisDialog.setVisible(false);
				queryDiagnosisDialog.dispose();
				Container parent = queryDiagnosisDialog
						.getParent();
				if (parent != null) {
					parent.remove(queryDiagnosisDialog);
				}
			}
		};
		JButton btnOK = new JButton("OK");
		btnOK.setActionCommand("OK");
		btnOK.addActionListener(al);
		JButton btnCancel = new JButton("Cancel");
		btnCancel.setActionCommand("CANCEL");
		btnCancel.addActionListener(al);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel,
				BoxLayout.LINE_AXIS));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(btnOK);
		buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		buttonPanel.add(btnCancel);
		buttonPanel.add(Box.createRigidArea(new Dimension(15, 0)));
		queryDiagnosisDialog.add(buttonPanel,
				BorderLayout.SOUTH);
		queryDiagnosisDialog.setBounds(
				frame.getX() + 20,
				frame.getY() + 20,
				queryDiagnosisDialog.getPreferredSize().width + 30,
				queryDiagnosisDialog.getPreferredSize().height + 30);
		// display dialog
		queryDiagnosisDialog.setVisible(true);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see gov.va.research.ir.view.SearchResultDisplayer#displayICD9s(int,
	 * javax.swing.JComponent)
	 */
	public void displayDialog(final String message, final String title) {
		// remove html from title
		final String fixedtitle = HTML_TAG_PATTERN.matcher(title)
				.replaceAll("");
		JComponent body = null;
		String delimiterPattern = null;
		if (message.trim().startsWith("<html>")
				|| message.trim().startsWith("<HTML>")) {
			body = new JLabel(message);
			delimiterPattern = "</tr>|<br>";
		} else {
			// wrap message
			JTextArea ta = new JTextArea(message);
			ta.setLineWrap(true);
			ta.setWrapStyleWord(true);
			ta.setEditable(false);
			body = ta;
			delimiterPattern = "[\\r?\\n]";
		}
		int width = frame.getWidth();
		String[] lines = message.split(delimiterPattern);
		int maxLineLength = 0;
		for (String line : lines) {
			if (maxLineLength < line.length()) {
				maxLineLength = line.length();
			}
		}
		if (width > maxLineLength * 8) {
			width = maxLineLength * 8;
		}
		body.setBackground(Color.WHITE);
		final JScrollPane scrollPane = new JScrollPane(body);
		if (lines.length > 30 || width > 800) {
			scrollPane.setPreferredSize(new Dimension(800, 600));
		}
		ThreadUtils.runOnEDT(new Runnable() {
			@Override
			public void run() {
				JOptionPane.showMessageDialog(null, scrollPane, fixedtitle,
						JOptionPane.PLAIN_MESSAGE);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * gov.va.research.ir.view.SearchResultDisplayer#displayDialog(java.util
	 * .List, java.lang.String)
	 */
	public void displayDialog(List<?> list, String title) {
		StringBuilder sb = new StringBuilder("<html><table>");
		if (list != null) {
			for (Object i : list) {
				if (i instanceof CodeNameCount) {
					CodeNameCount cnc = (CodeNameCount) i;
					sb.append("<tr><td>[" + cnc.code + "]</td><td>" + cnc.name
							+ "</td><td>" + cnc.count + "</td></tr>");
				} else if (i instanceof NameCount) {
					NameCount nc = (NameCount) i;
					sb.append("<tr><td>" + nc.name + "</td><td>" + nc.count
							+ "</td></tr>");
				} else {
					sb.append("<tr>" + i.toString() + "</tr>");
				}
			}
		}
		sb.append("</table></html>");
		displayDialog(sb.toString(), title);
	}

	public void searchBegun() {
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				clearError();
				searchButton.setText("Cancel");
				searchButton.setActionCommand(Command.CANCEL.toString());
				searchButton.revalidate();
				searchButton.repaint();
				setStatus("Searching ...");
				progressBar.setValue(0);
				progressBar.setIndeterminate(true);
				progressBar.setVisible(true);
				statusField.setVisible(true);
				queryRecommendationButton.setEnabled(true);
				dataSetComboBox.setEnabled(false);
				searchStartTime = System.currentTimeMillis();
			}
		});
	}

	public void cancelSearch() {
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				summaryPanel.cancel();
				searchButton.setText("Search");
				searchButton.setActionCommand(Command.SEARCH.toString());
				searchButton.revalidate();
				searchButton.repaint();
				setStatus("<html>Search Canceled</html>");
				progressBar.setIndeterminate(false);
				progressBar.setVisible(false);
				queryRecommendationButton.setEnabled(true);
				dataSetComboBox.setEnabled(true);
			}
		});
	}

	public void searchComplete(final SearchWorker search,
							   final List<SearchTerm> searchTerms) {
		if (!"Search".equals(searchButton.getActionCommand())) {
			final long searchCompleteTime = System.currentTimeMillis()
					- searchStartTime;
			ThreadUtils.runOnEDT(new Runnable() {
				public void run() {
					setStatus("<html>Initial search complete ("
							+ ViewUtils.formatHMS(searchCompleteTime)
							+ "), aggregating data ...</html>");
				}
			});
			try {
				new SwingWorker<Object, Object>() {
					@Override
					protected Object doInBackground() throws Exception {
						try {
							summaryPanel.displaySummary(search, searchTerms,
									search.hasSensitiveData());
							final long aggregationCompleteTime = (System
									.currentTimeMillis() - searchStartTime)
									- searchCompleteTime;
							ThreadUtils.runOnEDT(new Runnable() {
								@Override
								public void run() {
									searchButton.setText("Search");
									searchButton
											.setActionCommand(Command.SEARCH
													.toString());
									searchButton.revalidate();
									searchButton.repaint();
									setStatus("<html>Search complete ("
											+ ViewUtils
											.formatHMS(searchCompleteTime)
											+ "), summarization complete ("
											+ ViewUtils
											.formatHMS(aggregationCompleteTime)
											+ ")</html>");
									progressBar.setIndeterminate(false);
									progressBar.setVisible(false);
									queryRecommendationButton.setEnabled(true);
									dataSetComboBox.setEnabled(true);
								}
							});
						} catch (CancellationException e) {
							error("Search Canceled");
							LOG.error("Search Canceled");
							e.printStackTrace();
							cancelSearch();
						} catch (Exception e) {
							error(e.getMessage());
							LOG.error(e.getMessage());
							e.printStackTrace();
							cancelSearch();
						}
						return null;
					}
				}.execute();
			} catch (Exception e) {
				error(e.getMessage());
				LOG.error(e.getMessage());
			}
		}
	}

	public String getDataSetName() {
		String selectedItem = null;
		if (dataSetComboBox.getSelectedItem() != null) {
			selectedItem = dataSetComboBox.getSelectedItem().toString();
		}
		return selectedItem;
	}

	public void setStatus(final String string) {
		if (this.error == null) {
			this.statusField.setText(string);
		}
		this.statusField.validate();
		this.statusField.repaint();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see voogle.view.SearchResultDisplayer#setVisible(boolean)
	 */
	public void setVisible(final boolean visible) {
		this.frame.setVisible(visible);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * gov.va.research.ir.view.SearchResultDisplayer#addWindowListener(java.
	 * awt.event.WindowListener)
	 */
	public void addWindowListener(WindowListener l) {
		this.frame.addWindowListener(l);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * gov.va.research.ir.view.SearchResultDisplayer#error(java.lang.Throwable)
	 */
	public void error(final String message) {
		setError(message);
	}

	private void setError(final String errorMessage) {
		this.error = errorMessage;
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		int maxlinesize = 50;
		int lines = (errorMessage.length() / maxlinesize) + 1;
		for (int i = 0;  i < lines; i++) {
			int begIdx = i * maxlinesize;
			int endIdx = Math.min(errorMessage.length(), begIdx + maxlinesize);
			pw.println(errorMessage.substring(begIdx, endIdx));
		}
		final String errStr = sw.toString();
		ThreadUtils.runOnEDT(new Runnable() {
			@Override
			public void run() {
				JOptionPane.showMessageDialog(frame, errStr, "Error",
						JOptionPane.ERROR_MESSAGE);
				progressBar.setVisible(false);
			}
		});
	}

	private void clearError() {
		this.error = null;
	}

	public void dispose() {
		try {
			summaryPanel.dispose();
		} catch (SQLException e) {
			e.printStackTrace();
			LOG.error(e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see gov.va.research.ir.view.SearchResultDisplayer#getFileForSaving()
	 */
	public SaveFile getFileForSaving() {
		SaveDialog saveDialog = new SaveDialog(this.frame);
		saveDialog.setVisible(true);
		return saveDialog.getSaveFile();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * gov.va.research.ir.view.SearchResultDisplayer#doQueryRecommendation()
	 */



	public void doQueryRecommendation(final Collection<String> terms) {
		final Collection<JCheckBox> queryRecommendations = new ArrayList<JCheckBox>(terms.size());
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				if (terms.size() == 0) {

					JOptionPane.showMessageDialog(null,
							"No Recommended Terms were Found",
							"Additional Recommended Terms",
							JOptionPane.INFORMATION_MESSAGE);
				} else {
					// build dialog
					final JDialog queryRecommendationDialog = new JDialog(frame, "Additional Recommended Terms", true);
					queryRecommendationDialog.setLayout(new BorderLayout());
					JPanel queryRecommendationPanel = new JPanel();
					queryRecommendationPanel.setLayout(new BoxLayout(queryRecommendationPanel, BoxLayout.PAGE_AXIS));
					for (String term : terms) {
						JCheckBox cb = new JCheckBox(term);
						queryRecommendations.add(cb);
						queryRecommendationPanel.add(cb, "wrap");
					}
					queryRecommendationDialog.add(new JScrollPane(
							queryRecommendationPanel), BorderLayout.CENTER);
					ActionListener al = new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							if ("OK".equals(e.getActionCommand())) {
								for (JCheckBox checkBox : queryRecommendations) {
									if (checkBox != null
											&& checkBox.isSelected()) {
										addRow(Field.DOCUMENT_TEXT,
												checkBox.getText());
									}
								}
							}
							queryRecommendationDialog.pack();
							 queryRecommendationDialog.setVisible(true);
							queryRecommendationDialog.dispose();
							Container parent = queryRecommendationDialog
									.getParent();
							if (parent != null) {
								parent.remove(queryRecommendationDialog);
							}
						}
					};
					JButton btnOK = new JButton("OK");
					btnOK.setActionCommand("OK");
					btnOK.addActionListener(al);
					JButton btnCancel = new JButton("Cancel");
					btnCancel.setActionCommand("CANCEL");
					btnCancel.addActionListener(al);

					JPanel buttonPanel = new JPanel();
					buttonPanel.setLayout(new BoxLayout(buttonPanel,
							BoxLayout.LINE_AXIS));
					buttonPanel.add(Box.createHorizontalGlue());
					buttonPanel.add(btnOK);
					buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));
					buttonPanel.add(btnCancel);
					buttonPanel.add(Box.createRigidArea(new Dimension(15, 0)));
					queryRecommendationDialog.add(buttonPanel,
							BorderLayout.SOUTH);
					queryRecommendationDialog.setBounds(
							frame.getX() + 20,
							frame.getY() + 20,
							queryRecommendationDialog.getPreferredSize().width + 30,
							queryRecommendationDialog.getPreferredSize().height + 30);
					// display dialog
					queryRecommendationDialog.setVisible(true);
				}
			}
		});
	}






	public void setState(final ViewState state) {
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				switch (state) {
					case WAIT:
						if (mainPanel != null) {
							mainPanel.setCursor(Cursor
									.getPredefinedCursor(Cursor.WAIT_CURSOR));
						}
						break;
					case NORMAL:
						if (mainPanel != null) {
							mainPanel.setCursor(Cursor.getDefaultCursor());
						}
						break;
					default:
						LOG.warn("Unknown ViewState: "
								+ (state == null ? "null" : state.toString()));
						break;
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * gov.va.research.ir.view.SearchResultDisplayer#setDataSetNames(java.util
	 * .Collection)
	 */
	@Override
	public void setDataSetNames(final Collection<String> dataSetNames) {
		final List<String> dsNameList = new ArrayList<String>(dataSetNames);
		Collections.sort(dsNameList);
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				if (dataSetComboBox == null) {
					dataSetComboBox = new JComboBox<String>();
				} else {
					dataSetComboBox.removeAllItems();
				}
				for (String ds : dsNameList) {
					dataSetComboBox.addItem(ds);
				}
				dataSetComboBox.setEnabled(true);
				dataSetComboBox.validate();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see gov.va.research.ir.view.SearchResultDisplayer#saveBegun()
	 */
	@Override
	public void saveBegun() {
		savingMessageWorker = new SwingWorker<Object, Object>() {
			@Override
			protected Object doInBackground() throws Exception {
				JDialog savingDlg = new JDialog(frame, true);
				savingDlg.add(new JLabel("Saving..."), BorderLayout.CENTER);
				JProgressBar pb = new JProgressBar();
				pb.setIndeterminate(true);
				savingDlg.add(pb, BorderLayout.SOUTH);
				savingDlg.pack();
				savingDlg.setLocation(frame.getLocation().x + ((frame.getWidth() - savingDlg.getWidth()) / 2), frame.getLocation().y + ((frame.getHeight() - savingDlg.getHeight()) / 2));
				savingDlg.setVisible(true);
				while (!this.isCancelled()) {
					Thread.sleep(1000);
				}
				savingDlg.setVisible(false);
				return null;
			}
		};
		savingMessageWorker.execute();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see gov.va.research.ir.view.SearchResultDisplayer#saveComplete()
	 */
	@Override
	public void saveComplete() {
		if (savingMessageWorker != null) {
			savingMessageWorker.cancel(true);
		}
	}

	/* (non-Javadoc)
	 * @see gov.va.research.ir.view.SearchResultDisplayer#displayHelp()
	 */
	@Override
	public void displayHelp() {
		JOptionPane.showMessageDialog(frame, "<html>Search Rules:<br><ul>" +
				"<li>All search criteria are case insensitive.</li>" +
				"<li>Search criteria for field types may use the * wildcard.</li>" +
				"<li>Numeric ranges may be specified using two numbers separated by a hyphen (e.g. 5-10).</li>" +
				"<li>Numeric ranges may also be specified by using &gt; and &lt; symbols (e.g. &gt;3).</li>" +
				"<li>The <em>Age</em> field matches age at the time of the visit.</li>" +
				"</ul></html>", "Search Help", JOptionPane.INFORMATION_MESSAGE);
	}

	/* (non-Javadoc)
	 * @see gov.va.research.ir.view.SearchResultDisplayer#saveSummary(java.io.File)
	 */
	@Override
	public void saveSummary(final File file, final SearchWorker search) throws IOException, SQLException {
		PDDocument pdf = new PDDocument();
		PDPage pdPage = new PDPage(PDPage.PAGE_SIZE_LETTER);
		pdf.addPage(pdPage);
		PDFont font = PDType1Font.COURIER;
		int fontHeight = Math.round((font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000) * 12);
		PDPageContentStream contentStream = new PDPageContentStream(pdf, pdPage);
		contentStream.beginText();
		contentStream.setFont(font, 12);
		contentStream.appendRawCommands(fontHeight + " TL\n");
		contentStream.moveTextPositionByAmount(fontHeight, PDPage.PAGE_SIZE_LETTER.getHeight() - (fontHeight * 2));
		contentStream.drawString(new Date().toString());
		contentStream.appendRawCommands("T*\n");
		contentStream.appendRawCommands("T*\n");
		contentStream.drawString("Data Set : " + getDataSetName());
		contentStream.appendRawCommands("T*\n");
		for (SearchTerm st : getSearchTerms()) {
			contentStream.appendRawCommands("T*\n");
			contentStream.drawString(st.toString());
		}
		contentStream.appendRawCommands("T*\n");
		contentStream.appendRawCommands("T*\n");
		contentStream.drawString("Matching Patients: " + search.getPatientResultCount());
		contentStream.appendRawCommands("T*\n");
		contentStream.drawString("Matching Documents: " + search.getDocumentResultCount());
		contentStream.endText();
		contentStream.close();
		summaryPanel.addPdfPages(pdf);
		try {
			pdf.save(file);
		} catch (COSVisitorException e) {
			throw new IOException(e);
		} finally {
			pdf.close();
		}
	}

	/* (non-Javadoc)
	 * @see gov.va.research.ir.view.SearchResultDisplayer#gatherFeedback()
	 */
	@Override
	public void gatherFeedback() throws IOException {
		JPanel p = new JPanel(new BorderLayout());
		p.add(new JLabel("Please leave us feedback on FactsFerret"), BorderLayout.NORTH);
		JTextArea ta = new JTextArea(5, 40);
		ta.setLineWrap(true);
		ta.setWrapStyleWord(true);
		JScrollPane sp = new JScrollPane(ta);
		p.add(sp, BorderLayout.CENTER);
		JOptionPane.showMessageDialog(mainPanel, p, null, JOptionPane.QUESTION_MESSAGE);
		String feedback = ta.getText();
		if (feedback != null && feedback.trim().length() > 0) {
			try (
					PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("feedback.txt", true)));
			) {
				pw.printf("%1$tF %1$tR\n%2$s\n", new Date(), feedback);
				pw.flush();
				pw.close();
			}
		}
	}

}
