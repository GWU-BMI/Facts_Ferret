/*
 *  Copyright 2012 United States Department of Veterans Affairs
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
import gov.va.research.ir.model.Command;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import net.miginfocom.swing.MigLayout;

/**
 * @author vhaislreddd
 *
 */
public class OverviewPanel extends JPanel {

	private static final String MATCHING_DOCUMENTS_LABEL_TEXT = "Matching Documents:";
	private static final String MATCHING_PATIENTS_LABEL_TEXT = "Matching Patients:";
	private static final long serialVersionUID = -7274182659658124236L;
	private JLabel numMatchingPatients;
	private JLabel numMatchingDocuments;
	private JLabel matchingPatientsLabel;
	private JLabel matchingDocumentsLabel;
	private JButton saveButton;
	private ActionListener actionListener;

	public OverviewPanel(final ActionListener al) throws IOException {
		this.actionListener = al;
		initialize();
	}

	private void initialize() throws IOException {
		removeAll();
		setBackground(Color.WHITE);
		setLayout(new MigLayout("insets 0","342[right][left]25[right][left]25[left]"));
		add((matchingPatientsLabel = new JLabel(MATCHING_PATIENTS_LABEL_TEXT)));
		add((numMatchingPatients = new JLabel()));
		add((matchingDocumentsLabel = new JLabel(MATCHING_DOCUMENTS_LABEL_TEXT)));
		add((numMatchingDocuments = new JLabel()));
		add((saveButton = new JButton("Save"))/*, "wrap"*/);
		matchingPatientsLabel.setVisible(false);
		numMatchingPatients.setVisible(false);
		matchingDocumentsLabel.setVisible(false);
		numMatchingPatients.setVisible(false);

		saveButton.setActionCommand(Command.SAVE.toString());
		saveButton.addActionListener(actionListener);
		saveButton.setEnabled(false);
		saveButton.setVisible(false);
	}

	public void setNumMatchingPatients(final int numPatients) {
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				matchingPatientsLabel.setVisible(true);
				numMatchingPatients.setText(String.valueOf(numPatients));
				numMatchingPatients.setVisible(true);
			}
		});
	}

	public void setNumMatchingDocuments(final int numDocuments) {
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				if (numDocuments == -1) {
					matchingDocumentsLabel.setVisible(false);
					numMatchingDocuments.setVisible(false);
				} else {
					matchingDocumentsLabel.setVisible(true);
					numMatchingDocuments.setText(String.valueOf(numDocuments));
					numMatchingDocuments.setVisible(true);
				}
			}
		});
	}

	public void setSaveEnabled(final boolean saveEnabled) {
		saveButton.setVisible(saveEnabled);
		saveButton.setEnabled(saveEnabled);
	}

	public void reset() {
		matchingPatientsLabel.setVisible(false);
		numMatchingPatients.setText("");
		numMatchingPatients.setVisible(false);

		matchingDocumentsLabel.setVisible(false);
		numMatchingDocuments.setText("");
		numMatchingDocuments.setVisible(false);

		saveButton.setEnabled(false);
		saveButton.setVisible(false);
	}

	public void dispose() throws SQLException {
	}

}
