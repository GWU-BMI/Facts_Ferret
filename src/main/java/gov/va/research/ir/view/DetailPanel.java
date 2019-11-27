/*
 *  Copyright 2011 United States Department of Veterans Affairs
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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.html.HTMLDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import gov.va.research.ir.SearchUtils;
import gov.va.research.ir.ThreadUtils;
import gov.va.research.ir.model.Command;
import gov.va.research.ir.model.SearchResult;
import gov.va.research.ir.model.SearchTerm;
import gov.va.research.ir.model.StopWords;
import net.miginfocom.swing.MigLayout;



import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.IntervalCategoryDataset;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
/**
 * @author vhaislreddd
 *
 */
public class DetailPanel extends JPanel {

	private static final long serialVersionUID = 3186910177114206794L;
	private static final boolean OBFUSCATE = false;
	private static final ImageIcon MALE_ICON = new ImageIcon(
			ClassLoader.getSystemResource("img/male.png"));
	private static final ImageIcon FEMALE_ICON = new ImageIcon(
			ClassLoader.getSystemResource("img/female.png"));
	private static final Pattern MALE_PATTERN = Pattern.compile("M|MALE",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern FEMALE_PATTERN = Pattern.compile("F|FEMALE",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern AFFIRMATIVE_PATTERN = Pattern.compile(
			"Y|YES|1|T|TRUE", Pattern.CASE_INSENSITIVE);
	private static final Pattern NEGATIVE_PATTERN = Pattern.compile("N|NO|0|F|FALSE",
			Pattern.CASE_INSENSITIVE);
	public static final int PAGESIZE = 30;
	private static final Pattern SOLO_CR_PATTERN = Pattern.compile("(?<!\n)\r(?!\n)");
	private static final String LS = System.getProperty("line.separator");
	private static final Comparator<Point> POINT_COMPARATOR = new Comparator<Point>() {
		@Override
		public int compare(Point o1, Point o2) {
			return (o1.x != o2.x ? o1.x - o2.x : o1.y - o2.y);
		}
	};
	private static final Font LABEL_FONT = UIManager.getFont("Label.font");
	private static final String LABEL_BODY_RULE = "body { font-family: " + LABEL_FONT.getFamily() + "; font-size: " + LABEL_FONT.getSize() + "pt; }";

	private int page = 0;
	private boolean showAllDocs = false;

	public DetailPanel() {
		initialize();
	}

	public void initialize() {
		setLayout(new BorderLayout());
		setBackground(Color.WHITE);

	}





	public JPanel buildEnChoicePanel(final JButton button) {
		JPanel docChoicePanel = new JPanel(new FlowLayout());
		JRadioButton matchingDocsBtn = new JRadioButton("Matching Encounters");
		matchingDocsBtn.setSelected(!this.showAllDocs);
		JRadioButton allDocsBtn = new JRadioButton("All Encounters");
		allDocsBtn.setSelected(this.showAllDocs);
		ButtonGroup docsBtnGrp = new ButtonGroup();
		docsBtnGrp.add(matchingDocsBtn);
		docsBtnGrp.add(allDocsBtn);
		docChoicePanel.add(matchingDocsBtn);
		docChoicePanel.add(allDocsBtn);

		matchingDocsBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				page--;
				showAllDocs = false;
				button.doClick();
			}
		});
		allDocsBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				page--;
				showAllDocs = true;
				button.doClick();
			}
		});

		return docChoicePanel;
	}








	public void reset() {
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				removeAll();
				page = 0;
				showAllDocs = false;
			}
		});
	}

	/**
	 * Adds a new result set to the detail panel
	 *
	 * @param patients
	 *            The patients to add to the detail panel.
	 * @param patientEncountersMap
	 *            A map from the patient ID to the list of Encounters belonging
	 *            to the patient.
	 * @param searchTerms
	 *            The terms used in the search that produced this result set.
	 * @param l
	 *            An <code>ActionListener</code> to handle action events (tree
	 *            expansion, etc.)
	 * @param moreAvailable
	 *            A flat go indicate whether there are more results available.
	 */
	public void addResults(
			final List<SearchResult.Patient> patients, final Map<String, List<SearchResult.Encounter>> patientEncountersMap,
			final List<SearchTerm> searchTerms, final ActionListener l,
			final boolean moreAvailable, final StopWords stopWords) {
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				removeAll();
				DefaultMutableTreeNode root = new DefaultMutableTreeNode();
				for (SearchResult.Patient patient : patients) {
					DefaultMutableTreeNode patientNode = new DefaultMutableTreeNode(patient);
					root.add(patientNode);
					for (SearchResult.Encounter encounter : patientEncountersMap.get(patient.id)) {
						MutableTreeNode EncounterNode = new DefaultMutableTreeNode(encounter);
						patientNode.add(EncounterNode);
					}
				}


				final JTree patientTree = new JTree(root);
				patientTree.setRootVisible(false);
				patientTree.setShowsRootHandles(true);
				patientTree.setCellRenderer(new PatientAndEncounterCellRenderer(searchTerms));
				final JScrollPane patientScrollPane = new JScrollPane(patientTree);
				final JPanel nextPanel = new JPanel(new BorderLayout());
				JButton nextButton = new JButton("Next >");
				nextButton.addActionListener(l);
				nextButton.setActionCommand(Command.NEXTPAGE.toString());
				nextButton.setEnabled(moreAvailable);
				nextPanel.add(nextButton, BorderLayout.WEST);

				final JPanel patientPanel = new JPanel(new BorderLayout());
				patientPanel.add(buildEnChoicePanel(nextButton), BorderLayout.NORTH);
				patientPanel.add(patientScrollPane, BorderLayout.CENTER);
				patientPanel.add(nextPanel, BorderLayout.SOUTH);

				JPanel patientDetailPanel = new JPanel(new MigLayout(
						"insets 10"));
				patientDetailPanel.setBackground(Color.WHITE);


//				final JTextPane patientDemographicsLabel = new JTextPane();
//				patientDemographicsLabel.setEditable(false);
//				patientDemographicsLabel.setContentType("text/html");
//				((HTMLDocument)patientDemographicsLabel.getDocument()).getStyleSheet().addRule(LABEL_BODY_RULE);
//				patientDetailPanel.add(patientDemographicsLabel);





				JPanel linkPanel = new JPanel( new MigLayout("insets 0, gap 0 1"));
				patientDetailPanel.add(linkPanel, "gapleft 5, gaptop 5");

				linkPanel.setBackground(Color.WHITE);


				final JButton icd9Button = new JButton("Comorbid Conditions");
				icd9Button.setActionCommand(Command.DISPLAYDIAGNOSES.toString());
				icd9Button.addActionListener(l);
				icd9Button.setVisible(false);
				linkPanel.add(icd9Button, "gaptop 0, gapright 0, gapbottom 0 ");


				final JButton drugButton = new JButton("Medications");
				drugButton.setActionCommand(Command.DISPLAYDRUGS.toString());
				drugButton.addActionListener(l);
				drugButton.setVisible(false);
				linkPanel.add(drugButton, "gaptop 0, gapleft 0, gapbottom 0 ");



				final JButton cptButton = new JButton("Lab results");
				cptButton.setActionCommand(Command.DISPLAYLABRESULTS.toString());
				cptButton.addActionListener(l);
				cptButton.setVisible(false);
				linkPanel.add(cptButton, "gaptop 0, gapleft 0, gapbottom 0 , wrap");




				final JLinkButton En_Condition = new JLinkButton(" Encounter Comorbid Conditions");
				En_Condition.setActionCommand(Command.Encounters_Condition.toString());   //Change
				En_Condition.addActionListener(l);

				En_Condition.setVisible(false);
				linkPanel.add(En_Condition, "gaptop 0, gapbottom 0, wrap");


				final JLinkButton En_Medication = new JLinkButton(" Encounter Medications");
				En_Medication.setActionCommand(Command.Encounters_Medication.toString());   //Change
				En_Medication.addActionListener(l);
				En_Medication.setVisible(false);
				linkPanel.add(En_Medication, "gaptop 0, gapbottom 0, wrap");


				final JLinkButton EN_Result = new JLinkButton(" Encounter Lab Results");
				EN_Result.setActionCommand(Command.Encounters_Lab.toString());   //Change
				EN_Result.addActionListener(l);
				EN_Result.setVisible(false);
				linkPanel.add(EN_Result, "gaptop 0, gapbottom 0, wrap");




				JPanel docTextPanel = new JPanel(new BorderLayout());
				docTextPanel.setBackground(Color.WHITE);

				final JTextArea docTextArea = new JTextArea();
				docTextArea.setBorder(new EmptyBorder(10, 10, 10, 10));
				docTextArea.setEditable(false);
				docTextArea.setVisible(false);
				docTextPanel.add(docTextArea, BorderLayout.CENTER);
				final JScrollPane docScrollPane = new JScrollPane(docTextPanel);
				docScrollPane.setBorder(null);

				JPanel docPanel = new JPanel(new BorderLayout());
				docPanel.setBackground(Color.YELLOW);

				final JTextPane documentIdLabel = new JTextPane();
				documentIdLabel.setEditable(false);
				documentIdLabel.setContentType("text/html");
				((HTMLDocument)documentIdLabel.getDocument()).getStyleSheet().addRule(LABEL_BODY_RULE);
				documentIdLabel.setBorder(new EmptyBorder(1, 10, 0, 10));
				docPanel.add(documentIdLabel, BorderLayout.NORTH);
				docPanel.add(docScrollPane, BorderLayout.CENTER);

				JPanel patientDocPanel = new JPanel(new BorderLayout());
				patientDocPanel.setBackground(Color.RED);
				patientDocPanel.add(patientDetailPanel, BorderLayout.NORTH);
				patientDocPanel.add(docPanel, BorderLayout.CENTER);

				JSplitPane splitPane = new JSplitPane(
						JSplitPane.HORIZONTAL_SPLIT, patientPanel, patientDocPanel);
				splitPane.setDividerLocation(400);

				add(splitPane);
				validate();



				patientTree
						.addTreeSelectionListener(new TreeSelectionListener() {
							public void valueChanged(TreeSelectionEvent e) {
								TreePath newLeadSelectionPath = e
										.getNewLeadSelectionPath();
								if (newLeadSelectionPath != null) {
									Object[] selectedPath = newLeadSelectionPath
											.getPath();
									DefaultMutableTreeNode patientNode = (DefaultMutableTreeNode) selectedPath[1];
									SearchResult.Patient p = (SearchResult.Patient) patientNode
											.getUserObject();
									SearchResult.Encounter d = null;
									if (selectedPath.length > 2) {
										DefaultMutableTreeNode EncounterNode = (DefaultMutableTreeNode) selectedPath[2];
										d = (SearchResult.Encounter) EncounterNode.getUserObject();
									}






									if (p != null  ) {
//										patientDemographicsLabel.setText("<html><b>PatientICN:</b> "
//												+ (OBFUSCATE && p.id != null ? p.id.replaceAll("\\S", "X") : p.id)
//												+ ", <b>Gender:</b> "
//												+ p.gender
//												+ ", <b>Age:</b> "
//												+ p.age
//												+ ", <b>Deceased:</b> "
//												+ p.deceased
//												+ ", <b>County:</b> "
//												+ (OBFUSCATE && p.county != null ? p.county
//												.replaceAll("\\S", "X")
//												: p.county)
//												+ ", <b>State:</b> " + p.state);
										String patientIdStr = p.id.toString();


										icd9Button.setName(patientIdStr);
										icd9Button.setVisible(true);
										drugButton.setName(patientIdStr);
										drugButton.setVisible(true);
										cptButton.setName(patientIdStr);
										cptButton.setVisible(true);
										En_Condition.setVisible(false);

										En_Medication.setVisible(false);
										EN_Result.setVisible(false);

									}



									if (d != null) {

										icd9Button.setVisible(false);
										drugButton.setVisible(false);
										cptButton.setVisible(false);

										String enc_id= d.id.toString();
										En_Condition.setVisible(true);
										En_Condition.setName(enc_id);

										En_Medication.setVisible(true);
										En_Medication.setName(enc_id);

										EN_Result.setVisible(true);
										EN_Result.setName(enc_id);



										SearchUtils su = new SearchUtils();
//										documentIdLabel.setText("<html><u><b>TIUDocumentSID:</b> " + (OBFUSCATE ? "XXXXXXXXXX" : d.id) + "</u></html>");
//										String docText =  d.text == null ? "" : SOLO_CR_PATTERN.matcher(d.text).replaceAll(LS);
//										List<Point> matches = su.findMatches(
//												docText, searchTerms, stopWords);
//										if (OBFUSCATE) {
//											docText = obfuscate(docText, matches);
//										}
//										docTextArea.setText(docText);
//										Highlighter h = docTextArea
//												.getHighlighter();
//										if (matches != null
//												&& matches.size() > 0) {
//											try {
//												for (Point m : matches) {
//													h.addHighlight(
//															m.x,
//															m.y + 1,
//															ViewUtils.HIGHLIGHT_PAINTER);
//												}
//											} catch (BadLocationException e1) {
//												throw new RuntimeException(e1);
//											}
//										}
//										// Scroll back to top of document
//										docTextArea.select(0, 0);
//										docTextArea.setVisible(true);
									}






									else {
										documentIdLabel.setText("");
										docTextArea.setText("");
										docTextArea.setVisible(false);
									}
									validate();
								} else {
//									patientDemographicsLabel.setText("");
									docTextArea.setText("");
									docTextArea.setVisible(false);
								}
							}
						});



			}
		});
	}

	private String obfuscate(final String text, final List<Point> matches) {
		Collections.sort(matches, POINT_COMPARATOR);
		String obfuscatedText = null;
		if (matches == null || matches.size() == 0) {
			obfuscatedText = text.replaceAll("\\S", "X");
		} else {
			StringBuilder docTextSB = new StringBuilder();
			int lastEndIdx = 0;
			for (Point m : matches) {
				if (lastEndIdx < m.x) {
					docTextSB.append(text.substring(lastEndIdx, m.x).replaceAll(
							"\\S", "X"));
					docTextSB.append(text.substring(m.x, m.y + 1));
					lastEndIdx = m.y + 1;
				}
			}
			docTextSB.append(text.substring(lastEndIdx).replaceAll("\\S", "X"));
			obfuscatedText = docTextSB.toString();
		}
		return obfuscatedText;
	}

	class PatientAndEncounterCellRenderer extends DefaultTreeCellRenderer {

		private static final long serialVersionUID = -392800622003166700L;

		public PatientAndEncounterCellRenderer(final List<SearchTerm> searchTerms) {
			super();
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			if (value instanceof DefaultMutableTreeNode) {
				DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
				Object userObject = treeNode.getUserObject();
				if (userObject instanceof SearchResult.Patient
						|| userObject instanceof SearchResult.Encounter) {


					if (userObject instanceof SearchResult.Patient) {

						SearchResult.Patient p = (SearchResult.Patient) userObject;



						if (p.id != null) {
							setIcon(MALE_ICON);
						}



						if (p.gender != null
								&& MALE_PATTERN.matcher(p.gender).matches()) {
							setIcon(MALE_ICON);
						} else if (p.gender != null
								&& FEMALE_PATTERN.matcher(p.gender).matches()) {
							setIcon(FEMALE_ICON);
						}
//						if (p.deceased != null
//								&& AFFIRMATIVE_PATTERN.matcher(p.deceased)
//								.matches()) {
//							setTextNonSelectionColor(Color.GRAY);
//							setTextSelectionColor(Color.WHITE);
//						} else if (p.deceased != null
//								&& NEGATIVE_PATTERN.matcher(p.deceased)
//								.matches()) {
//							setTextNonSelectionColor(Color.BLACK);
//							setTextSelectionColor(Color.WHITE);
//						} else {
//							setTextNonSelectionColor(Color.blue);
//							setTextSelectionColor(Color.BLUE);
//						}
						//setText(p.id);
						StringBuilder text = new StringBuilder();
						text=text.append(p.race);
						text=text.append("    born in: ");
						text=text.append(p.year) ;
						setText(text.toString());


					}

					else if (userObject instanceof SearchResult.Encounter) {
						SearchResult.Encounter d = (SearchResult.Encounter) userObject;
						setTextNonSelectionColor(Color.BLACK);
						setTextSelectionColor(Color.WHITE);
//						StringBuilder text = new StringBuilder(
//								d.date == null ? "null" : d.date.toString()
//										+ " : ");

						StringBuilder text = new StringBuilder();
						text.append(d);
						setText(text.toString());
						setIcon(null);
					}
					if (sel) {
						setForeground(getTextSelectionColor());
					} else {
						setForeground(getTextNonSelectionColor());
					}
					setEnabled(true);
					selected = sel;
					setComponentOrientation(tree.getComponentOrientation());
				} else {
					super.getTreeCellRendererComponent(tree, value, sel,
							expanded, leaf, row, hasFocus);
				}
			}
			return this;
		}
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public boolean getShowAllDocs() {
		return this.showAllDocs;
	}

	public void cancel() {
	}
}
