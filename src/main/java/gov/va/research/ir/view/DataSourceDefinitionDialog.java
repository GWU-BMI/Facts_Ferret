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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

/**
 * @author vhaislreddd
 *
 */
public class DataSourceDefinitionDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = -1892179547128350572L;
	private String status = null;

	/**
	 * @param owner
	 */
	public DataSourceDefinitionDialog(Frame owner) {
		super(owner);
		this.setSize(400, 200);
		this.setBackground(Color.WHITE);
		this.setLayout(new MigLayout());
		this.setPreferredSize(new Dimension(400, 200));
		this.setTitle("Define New Subset");
		this.add(new JLabel("Database Name"), "cell 0 0");
		this.add(new JTextField(30), "cell 1 0");
		this.add(new JLabel("Database Schema"), "cell 0 1");
		this.add(new JTextField(30), "cell 1 1");
		this.add(new JLabel("Document Table"), "cell 0 2");
		this.add(new JTextField(30), "cell 1 2");

		JButton okBtn = new JButton("Add");
		okBtn.setActionCommand("OK");
		okBtn.addActionListener(this);
		this.add(okBtn , "cell 1 5, split");
		JButton cancelBtn = new JButton("Cancel");
		cancelBtn.setActionCommand("CANCEL");
		cancelBtn.addActionListener(this);
		this.add(cancelBtn);
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		status = e.getActionCommand();
		this.setVisible(false);
	}

	public String getStatus() {
		return status;
	}


}
