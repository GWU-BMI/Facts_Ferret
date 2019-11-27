/*
 *  Copyright 2013 United States Department of Veterans Affairs,
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

import gov.va.research.ir.model.DocSampleType;
import gov.va.research.ir.model.SampleOutputType;
import gov.va.research.ir.model.SaveFile;
import gov.va.research.ir.model.SaveType;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.EnumSet;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author vhaislreddd
 *
 */
public class SaveDialog extends JDialog implements ActionListener, ChangeListener {

	private static final long serialVersionUID = -6051237943823911891L;
	private static final Logger LOG = LoggerFactory.getLogger(SaveDialog.class);
	private ButtonGroup saveTypeButtonGroup;
	private SaveFile saveFile;
	private DetailsPanel detailsPanel;

	public SaveDialog(final JFrame parent) {
		super(parent, true);
		init();
	}

	public SaveFile getSaveFile() {
		return saveFile;
	}

	private void init() {
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle("Options for Saving");
		setLayout(new BorderLayout());
		JPanel choicePanel = buildChoicePanel();
		JPanel buttonPanel = buildButtonPanel();
		add(choicePanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);
		pack();
		detailsPanel.setEnabled(false);
		int parentLeft = getParent().getX();
		int parentTop = getParent().getY();
		int parentWidth = getParent().getWidth();
		int parentHeight = getParent().getHeight();
		setLocation(parentLeft + ((parentWidth - getWidth()) / 2), (parentTop + ((parentHeight - getHeight())/ 2)));
	}

	private JPanel buildChoicePanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		JRadioButton patientSIDsButton = new JRadioButton("PatientSIDs (csv)");
		patientSIDsButton.setActionCommand(SaveType.PATIENT_SIDS.name());
		JRadioButton patientAndDocumentSIDsButton = new JRadioButton("PatientSIDs and TIUDocumentSIDs (csv)");
		patientAndDocumentSIDsButton.setActionCommand(SaveType.PATIENT_AND_DOCUMENT_SIDS.name());
		JRadioButton detailsButton = new JRadioButton("Detailed Results (pdf)");
		detailsButton.setActionCommand(SaveType.DETAILS.name());
		detailsButton.addChangeListener(this);
		saveTypeButtonGroup = new ButtonGroup();
		saveTypeButtonGroup.add(patientSIDsButton);
		saveTypeButtonGroup.add(patientAndDocumentSIDsButton);
		saveTypeButtonGroup.add(detailsButton);
		saveTypeButtonGroup.setSelected(patientSIDsButton.getModel(), true);

		panel.add(new JLabel("Save Type:"));
		panel.add(patientSIDsButton);
		panel.add(patientAndDocumentSIDsButton);
		panel.add(detailsButton);

		detailsPanel = new DetailsPanel();
		panel.add(detailsPanel);

		panel.setBorder(new EmptyBorder(10, 10, 10, 10));
		return panel;
	}

	private class DetailsPanel extends JPanel implements ChangeListener {

		private static final long serialVersionUID = 3997259685326101465L;
		private ButtonGroup buttonGroupDocSampleType;
		private ButtonGroup buttonGroupDocsOrSnippets;
		private JPanel docSampleTypePanel;
		private JPanel docsOrSnippetsPanel;
		private JSpinner numWordsBeforeSpinnerField;
		private JSpinner numWordsAfterSpinnerField;
		private JSpinner numSnippetsPerPatientSpinnerField;
		private JLabel snippetBeforeLabel;
		private JLabel snippetAfterLabel;
		private JLabel snippetsPerPatientLabel;
		private JSpinner numPatientsSpinnerField;
		private JLabel patientsLabel;

		public DetailsPanel() {
			super();

			docSampleTypePanel = buildDocSampleTypePanel();
			docsOrSnippetsPanel = buildDocsOrSnippetsPanel();
			setEnabledPatients(false);
			setEnabledDocumentsOrSnippets(false);

			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(docSampleTypePanel);
			add(docsOrSnippetsPanel);
			setBorder(new TitledBorder("Save Sample Documents"));
		}

		JPanel buildSnippetParamPanel(JRadioButton snippetsButton) {
			numWordsBeforeSpinnerField = new JSpinner(new SpinnerNumberModel(20, 0, 100, 1));
			numWordsBeforeSpinnerField.setAlignmentX(LEFT_ALIGNMENT);
			numWordsBeforeSpinnerField.setMaximumSize(new Dimension(40, 100));
			snippetBeforeLabel = new JLabel("words before");
			snippetBeforeLabel.setAlignmentX(LEFT_ALIGNMENT);
			numWordsAfterSpinnerField = new JSpinner(new SpinnerNumberModel(20, 0, 100, 1));
			numWordsAfterSpinnerField.setAlignmentX(LEFT_ALIGNMENT);
			numWordsAfterSpinnerField.setMaximumSize(new Dimension(40, 100));
			snippetAfterLabel = new JLabel("words after");
			snippetAfterLabel.setAlignmentX(LEFT_ALIGNMENT);
			numSnippetsPerPatientSpinnerField = new JSpinner(new SpinnerNumberModel(5, 0, 50, 1));
			numSnippetsPerPatientSpinnerField.setAlignmentX(LEFT_ALIGNMENT);
			numSnippetsPerPatientSpinnerField.setMaximumSize(new Dimension(40, 100));
			snippetsPerPatientLabel = new JLabel("snippets per patient");
			snippetsPerPatientLabel.setAlignmentX(LEFT_ALIGNMENT);

			JPanel beforePanel = buildSnippetDetailSubpanel(numWordsBeforeSpinnerField, snippetBeforeLabel);
			JPanel afterPanel = buildSnippetDetailSubpanel(numWordsAfterSpinnerField, snippetAfterLabel);
			JPanel perPatientPanel = buildSnippetDetailSubpanel(numSnippetsPerPatientSpinnerField, snippetsPerPatientLabel);

			JPanel snippetDetailPanel = new JPanel();
			snippetDetailPanel.setLayout(new BoxLayout(snippetDetailPanel, BoxLayout.Y_AXIS));
			snippetDetailPanel.add(beforePanel);
			snippetDetailPanel.add(afterPanel);
			snippetDetailPanel.add(perPatientPanel);
			snippetDetailPanel.setAlignmentX(LEFT_ALIGNMENT);

			JPanel snippetPanel = new JPanel();
			snippetPanel.setLayout(new BoxLayout(snippetPanel, BoxLayout.X_AXIS));
			snippetPanel.add(snippetsButton);
			snippetPanel.add(snippetDetailPanel);
			snippetPanel.setAlignmentX(LEFT_ALIGNMENT);

			return snippetPanel;
		}

		JPanel buildSnippetDetailSubpanel(final JSpinner spinner, final JLabel label) {
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			panel.add(spinner);
			panel.add(label);
			panel.add(Box.createHorizontalGlue());
			panel.setAlignmentX(LEFT_ALIGNMENT);
			return panel;
		}

		JPanel buildDocsOrSnippetsPanel() {
			JRadioButton fullDocsButton = new JRadioButton("Full Documents");
			fullDocsButton.setActionCommand(SampleOutputType.DOCUMENTS.name());
			fullDocsButton.setSelected(true);
			fullDocsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
			JRadioButton snippetsButton = new JRadioButton("Snippets");
			snippetsButton.setActionCommand(SampleOutputType.SNIPPETS.name());
			snippetsButton.setSelected(false);
			snippetsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
			JRadioButton bothButton = new JRadioButton("Both");
			bothButton.setActionCommand("All");
			bothButton.setSelected(false);
			bothButton.setAlignmentX(Component.LEFT_ALIGNMENT);

			fullDocsButton.addChangeListener(this);
			snippetsButton.addChangeListener(this);

			buttonGroupDocsOrSnippets = new ButtonGroup();
			buttonGroupDocsOrSnippets.add(fullDocsButton);
			buttonGroupDocsOrSnippets.add(snippetsButton);
			buttonGroupDocsOrSnippets.add(bothButton);
			buttonGroupDocsOrSnippets.setSelected(fullDocsButton.getModel(), true);

			JPanel docsOrSnippetsPanel = new JPanel();
			docsOrSnippetsPanel.setLayout(new BoxLayout(docsOrSnippetsPanel, BoxLayout.Y_AXIS));
			docsOrSnippetsPanel.add(fullDocsButton);
			JPanel snippetParamsPanel = buildSnippetParamPanel(snippetsButton);
			docsOrSnippetsPanel.add(snippetParamsPanel);
			docsOrSnippetsPanel.add(bothButton);
			docsOrSnippetsPanel.setBorder(new TitledBorder("Documents or Snippets"));

			return docsOrSnippetsPanel;
		}

		JPanel buildDocSampleTypePanel() {
			JRadioButton noneButton = new JRadioButton("None");
			noneButton.setActionCommand(DocSampleType.NONE.name());
			noneButton.setSelected(true);
			noneButton.addChangeListener(this);
			noneButton.setAlignmentX(LEFT_ALIGNMENT);

			JRadioButton allButton = new JRadioButton("All");
			allButton.setActionCommand(DocSampleType.ALL.name());
			allButton.addChangeListener(this);
			allButton.setSelected(false);
			allButton.setAlignmentX(LEFT_ALIGNMENT);

			JRadioButton topRankPatientButton = new JRadioButton("Top Ranked Patients");
			topRankPatientButton.setActionCommand(DocSampleType.TOPRANKPATIENT.name());
			topRankPatientButton.addChangeListener(this);
			topRankPatientButton.setSelected(false);
			topRankPatientButton.setAlignmentX(LEFT_ALIGNMENT);

			JRadioButton randomButton = new JRadioButton("Random Patients");
			randomButton.setActionCommand(DocSampleType.RANDOM.name());
			randomButton.addChangeListener(this);
			randomButton.setSelected(false);
			randomButton.setAlignmentX(LEFT_ALIGNMENT);

			numPatientsSpinnerField = new JSpinner(new SpinnerNumberModel(100, 1, 100000, 25));
			numPatientsSpinnerField.setMaximumSize(new Dimension(40, 100));
			numPatientsSpinnerField.setAlignmentX(LEFT_ALIGNMENT);
			javax.swing.JComponent editor = numPatientsSpinnerField.getEditor();

			if (editor instanceof javax.swing.JSpinner.NumberEditor) {
				final javax.swing.JSpinner.NumberEditor ne = (javax.swing.JSpinner.NumberEditor)editor;
				ne.getTextField().addFocusListener(new java.awt.event.FocusListener() {
					@Override
					public void focusGained(FocusEvent e) {
						String modelVal = numPatientsSpinnerField.getValue().toString();
						String editorVal = ne.getTextField().getText();
						if (!modelVal.equals(editorVal)) {
							ne.getTextField().setText(numPatientsSpinnerField.getValue().toString());
						}
					}
					@Override
					public void focusLost(FocusEvent e) {
						String modelVal = numPatientsSpinnerField.getValue().toString();
						String editorVal = ne.getTextField().getText();
						if (!modelVal.equals(editorVal)) {
							try {
								Integer editorValue = Integer.valueOf(ne.getTextField().getText());
								numPatientsSpinnerField.setValue(editorValue);
							} catch (NumberFormatException nfe) {
								JOptionPane.showMessageDialog(e.getComponent(), "Number of patients must be an integer", "Patient Number Error", JOptionPane.ERROR_MESSAGE);
								ne.getTextField().setText(numPatientsSpinnerField.getValue().toString());
							}
						}
					}
				});
			}

			patientsLabel = new JLabel("patients");
			patientsLabel.setAlignmentX(LEFT_ALIGNMENT);

			buttonGroupDocSampleType = new ButtonGroup();
			buttonGroupDocSampleType.add(noneButton);
			buttonGroupDocSampleType.add(allButton);
			buttonGroupDocSampleType.add(topRankPatientButton);
			buttonGroupDocSampleType.add(randomButton);
			buttonGroupDocSampleType.setSelected(noneButton.getModel(), true);

			JPanel patientsButtonPanel = new JPanel();
			patientsButtonPanel.setLayout(new BoxLayout(patientsButtonPanel, BoxLayout.Y_AXIS));
			patientsButtonPanel.add(topRankPatientButton);
			patientsButtonPanel.add(randomButton);
			patientsButtonPanel.setAlignmentX(LEFT_ALIGNMENT);

			JPanel numPatientsSubPanel = new JPanel();
			numPatientsSubPanel.setLayout(new BoxLayout(numPatientsSubPanel, BoxLayout.X_AXIS));
			numPatientsSubPanel.add(numPatientsSpinnerField);
			numPatientsSubPanel.add(patientsLabel);
			numPatientsSubPanel.add(Box.createHorizontalGlue());
			numPatientsSubPanel.setAlignmentX(LEFT_ALIGNMENT);

			JPanel numPatientsPanel = new JPanel();
			numPatientsPanel.setLayout(new BoxLayout(numPatientsPanel, BoxLayout.Y_AXIS));
			numPatientsPanel.add(Box.createGlue());
			numPatientsPanel.add(numPatientsSubPanel);
			numPatientsPanel.add(Box.createGlue());
			numPatientsPanel.setAlignmentX(LEFT_ALIGNMENT);

			JPanel patientsPanel = new JPanel();
			patientsPanel.setLayout(new BoxLayout(patientsPanel, BoxLayout.X_AXIS));
			patientsPanel.add(patientsButtonPanel);
			patientsPanel.add(numPatientsPanel);
			patientsPanel.setAlignmentX(LEFT_ALIGNMENT);


			JPanel docSampleTypePanel = new JPanel();
			docSampleTypePanel.setLayout(new BoxLayout(docSampleTypePanel, BoxLayout.Y_AXIS));
			docSampleTypePanel.add(noneButton);
			docSampleTypePanel.add(allButton);
			docSampleTypePanel.add(patientsPanel);
			docSampleTypePanel.setAlignmentX(LEFT_ALIGNMENT);

			return docSampleTypePanel;
		}

		@Override
		public void setEnabled(boolean enabled) {
			Enumeration<AbstractButton> buttonEnum = buttonGroupDocSampleType.getElements();
			while (buttonEnum.hasMoreElements()) {
				buttonEnum.nextElement().setEnabled(enabled);
			}
			docSampleTypePanel.setEnabled(enabled);
			boolean noneButtonSelected = DocSampleType.NONE.name().equals(buttonGroupDocSampleType.getSelection().getActionCommand());
			boolean allButtonSelected = DocSampleType.ALL.name().equals(buttonGroupDocSampleType.getSelection().getActionCommand());
			if (!(enabled && (noneButtonSelected || allButtonSelected))) {
				if (noneButtonSelected) {
					setEnabledPatients(enabled);
					setEnabledDocumentsOrSnippets(enabled);
				} else if (allButtonSelected) {
					setEnabledPatients(!enabled);
					setEnabledDocumentsOrSnippets(!enabled);
				}
			}
			super.setEnabled(enabled);
		}

		/* (non-Javadoc)
		 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
		 */
		@Override
		public void stateChanged(ChangeEvent e) {
			if (e.getSource() instanceof JRadioButton) {
				JRadioButton rb = (JRadioButton)e.getSource();
				if (rb.isSelected()) {
					if (DocSampleType.NONE.name().equals(rb.getActionCommand())) {
						setEnabledPatients(false);
						setEnabledDocumentsOrSnippets(false);
					} else if (DocSampleType.ALL.name().equals(rb.getActionCommand())) {
						setEnabledPatients(false);
						setEnabledDocumentsOrSnippets(true);
					} else if (DocSampleType.TOPRANKPATIENT.name().equals(rb.getActionCommand()) || DocSampleType.RANDOM.name().equals(rb.getActionCommand())) {
						setEnabledPatients(true);
						setEnabledDocumentsOrSnippets(true);
					} else if (SampleOutputType.DOCUMENTS.name().equals(rb.getActionCommand())) {
						setEnabledSnippetParams(false);
					} else if (SampleOutputType.SNIPPETS.name().equals(rb.getActionCommand())) {
						setEnabledSnippetParams(true);
					} else {
						LOG.error("Unrecognized action command: " + rb.getActionCommand());
					}
					validate();
				}
			}
		}

		private void setEnabledSnippetParams(boolean enabled) {
			numWordsBeforeSpinnerField.setEnabled(enabled);
			snippetBeforeLabel.setEnabled(enabled);
			numWordsAfterSpinnerField.setEnabled(enabled);
			snippetAfterLabel.setEnabled(enabled);
			numSnippetsPerPatientSpinnerField.setEnabled(enabled);
			snippetsPerPatientLabel.setEnabled(enabled);
		}

		private void setEnabledDocumentsOrSnippets(boolean enabled) {
			Enumeration<AbstractButton> buttonEnum = buttonGroupDocsOrSnippets.getElements();
			while (buttonEnum.hasMoreElements()) {
				buttonEnum.nextElement().setEnabled(enabled);
			}
			docsOrSnippetsPanel.setEnabled(enabled);
			boolean snippetsButtonSelected = SampleOutputType.SNIPPETS.name().equals(buttonGroupDocsOrSnippets.getSelection().getActionCommand());
			setEnabledSnippetParams(enabled && snippetsButtonSelected);
		}

		private void setEnabledPatients(boolean enabled) {
			numPatientsSpinnerField.setEnabled(enabled);
			patientsLabel.setEnabled(enabled);
		}
	}

	private enum ActionCommand {
		SAVE, CANCEL;

		public String getLabel() {
			switch (this) {
				case SAVE: return "Save";
				case CANCEL: return "Cancel";
				default: return null;
			}
		}
	}

	private JPanel buildButtonPanel() {
		JButton saveButton = new JButton(ActionCommand.SAVE.getLabel());
		saveButton.setActionCommand(ActionCommand.SAVE.name());
		saveButton.addActionListener(this);
		JButton cancelButton = new JButton(ActionCommand.CANCEL.getLabel());
		cancelButton.setActionCommand(ActionCommand.CANCEL.name());
		cancelButton.addActionListener(this);
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.add(cancelButton);
		panel.add(saveButton);
		return panel;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		String command = e.getActionCommand();
		if (source instanceof JButton) {
			if (ActionCommand.SAVE.name().equals(command)) {
				JFileChooser fileChooser = new JFileChooser();
				SaveType saveType = SaveType.valueOf(saveTypeButtonGroup.getSelection().getActionCommand());
				DocSampleType docSampleType = DocSampleType.valueOf(detailsPanel.buttonGroupDocSampleType.getSelection().getActionCommand());
				EnumSet<SampleOutputType> sampleOutputTypes = null;
				if ("All".equalsIgnoreCase(detailsPanel.buttonGroupDocsOrSnippets.getSelection().getActionCommand())) {
					sampleOutputTypes = EnumSet.allOf(SampleOutputType.class);
				} else {
					sampleOutputTypes = EnumSet.of(SampleOutputType.valueOf(detailsPanel.buttonGroupDocsOrSnippets.getSelection().getActionCommand()));
				}
				int numWordsBefore = ((SpinnerNumberModel)detailsPanel.numWordsBeforeSpinnerField.getModel()).getNumber().intValue();
				int numWordsAfter = ((SpinnerNumberModel)detailsPanel.numWordsAfterSpinnerField.getModel()).getNumber().intValue();
				int numSnippetsPerPatient = ((SpinnerNumberModel)detailsPanel.numSnippetsPerPatientSpinnerField.getModel()).getNumber().intValue();
				int numPatients = ((SpinnerNumberModel)detailsPanel.numPatientsSpinnerField.getModel()).getNumber().intValue();
				if (saveType == null) {
					JOptionPane
							.showConfirmDialog(this.getParent(),
									"Save Type must be selected before selecting save file");
				} else {
					fileChooser.setFileFilter(saveType.getFileFilter());
					if (JFileChooser.APPROVE_OPTION == fileChooser.showSaveDialog(this)) {
						File selectedFile = fileChooser.getSelectedFile();
						// Automatically add file extension if it is missing
						if (fileChooser.getFileFilter() instanceof FileNameExtensionFilter) {
							String[] extensions = ((FileNameExtensionFilter)fileChooser.getFileFilter()).getExtensions();
							if (extensions != null && extensions.length > 0) {
								boolean hasExt = false;
								for (String ext : extensions) {
									if (selectedFile.getAbsolutePath().endsWith("." + ext)) {
										hasExt = true;
										break;
									}
								}
								if (!hasExt) {
									selectedFile = new File(selectedFile.getAbsolutePath() + "."
											+ extensions[0]);
								}
							}
						}
						if (selectedFile.exists()
								&& JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(
								this,
								"The file already exists and will be overwritten.",
								"Overwrite File", JOptionPane.OK_CANCEL_OPTION)) {
							this.saveFile = null;
						} else {
							this.saveFile = new SaveFile(selectedFile, saveType, docSampleType, sampleOutputTypes, numPatients, numWordsBefore, numWordsAfter, numSnippetsPerPatient);
						}
					}
					this.setVisible(false);
					this.dispose();
				}
			} else if (ActionCommand.CANCEL.name().equals(command)) {
				this.saveFile = null;
				this.setVisible(false);
				this.dispose();
			}
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() instanceof JRadioButton) {
			JRadioButton rb = (JRadioButton)e.getSource();
			if (SaveType.DETAILS.name().equals(rb.getActionCommand())) {
				this.detailsPanel.setEnabled(rb.isSelected());
			}
			validate();
		}
	}
}
