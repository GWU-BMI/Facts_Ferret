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

import gov.va.research.ir.ThreadUtils;
import gov.va.research.ir.model.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.*;
import javax.swing.border.LineBorder;

/**
 * @author vhaislreddd
 *
 */
public class SearchPanel extends JPanel {

	private static final long serialVersionUID = -3083286557555175361L;
	private JButton addRowButton;
	private List<DisplayRow> rows;
	private Field[] SEARCHABLE_FIELDS_ARR = Field.SEARCHABLE_FIELDS.toArray(new Field[Field.SEARCHABLE_FIELDS.size()]);
	private static final Dimension ROW_BUTTON_DIM = new Dimension(25, 25);
	private static final Dimension BOOL_DIM = new Dimension(75, 25);
	private static final Dimension FIELD_CHOOSER_DIM = new Dimension(110, 25);
	private static final Dimension QUALIFIER_DIM = new Dimension(125, 25);
	private static final Dimension VALUE_DIM = new Dimension(200, 25);
	private URL rmRowImgURL;
	private ImageIcon rmRowIcon;
	private int maxGridY = 0;
	private JLabel qualifierLabel;
	private ActionListener  al;
	//add 08/31/2017 by Huijuan for an additional button for drug
	//private JButton selectButton;
	/**
	 *
	 */
	public SearchPanel(ActionListener al) {
		super();
		this.al = al;
		initialize();
	}

	private void initialize() {
		setLayout(new GridBagLayout());
		setBackground(Color.WHITE);

		JLabel fieldLabel = new JLabel("Field:");
		qualifierLabel = new JLabel("Name:");
		qualifierLabel.setVisible(false);
		JLabel valueLabel = new JLabel("Value:");

		//add by Huijuan for an additional button for drug


		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		add(fieldLabel, c);
		c.gridx = 2;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		add(qualifierLabel, c);
		c.gridx = 3;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		add(valueLabel, c);

		URL addRowImgURL = this.getClass().getResource("/img/newrow-16x16.png");
		addRowButton = new JButton(new ImageIcon(addRowImgURL));
		addRowButton.setActionCommand(Command.ADDROW.toString());
		addRowButton.setSize(ROW_BUTTON_DIM);
		addRowButton.setPreferredSize(ROW_BUTTON_DIM);
		addRowButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				addRow();
			}
		});

		rmRowImgURL = this.getClass().getResource("/img/rmrow-16x16.png");
		rmRowIcon = new ImageIcon(rmRowImgURL);

		rows = new ArrayList<DisplayRow>();

		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				addRow();
			}
		});
	}

	public void addRow() {
		addRow(new DisplayRow(this,this.al));
	}

	public void addRow(final Field field, final String text) {
		addRow(new DisplayRow(field, text, this,this.al));
	}

	private void addRow(final DisplayRow dr) {
		ThreadUtils.runOnEDT(new Runnable() {
			@Override
			public void run() {
				remove(addRowButton);
				maxGridY++;
				GridBagConstraints c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = maxGridY;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.anchor = GridBagConstraints.FIRST_LINE_START;
				add(dr.searchRow.boolBox, c);
				dr.searchRow.boolBox.setSelectedIndex(1);
				c.gridx = 1;
				c.gridy = maxGridY;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.anchor = GridBagConstraints.FIRST_LINE_START;
				dr.searchRow.comboBox.setPreferredSize(new Dimension(150, 25));
				add(dr.searchRow.comboBox, c);
				c.gridx = 2;
				c.gridy = maxGridY;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.anchor = GridBagConstraints.FIRST_LINE_START;
				add(dr.searchRow.qualifierField, c);
				c.gridx = 3;
				c.gridy = maxGridY;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.anchor = GridBagConstraints.FIRST_LINE_START;
				add(dr.searchRow.valueField, c);
				//added by Huijuan 08/31/2017
				c.gridx = 4;
				c.gridy = maxGridY;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.anchor = GridBagConstraints.FIRST_LINE_START;
				add(dr.searchRow.selectDrugButton, c);

				//added by Huijuan 08/31/2017
				c.gridx = 4;
				c.gridy = maxGridY;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.anchor = GridBagConstraints.FIRST_LINE_START;
				add(dr.searchRow.selectDiagnosisButton, c);

				//added by Huijuan 09/08/2017
				c.gridx = 4;
				c.gridy = maxGridY;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.anchor = GridBagConstraints.FIRST_LINE_START;
				add(dr.searchRow.selectICD9codeButton, c);

				//added by Huijuan 09/08/2017
				c.gridx = 4;
				c.gridy = maxGridY;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.anchor = GridBagConstraints.FIRST_LINE_START;
				add(dr.searchRow.selectLabButton, c);

				c.gridx = 5;
				c.gridy = maxGridY;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.anchor = GridBagConstraints.FIRST_LINE_START;
				add(dr.rmRowButton, c);
				rows.add(dr);
				boolean firstrow = rows.size() == 1;
				dr.setBoolBoxRmRowBtnVisible(!firstrow);
				c.gridx = (firstrow ? 5 : 6);
				c.gridy = maxGridY;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.anchor = GridBagConstraints.FIRST_LINE_START;
				add(addRowButton, c);
				revalidate();
				repaint();
				if (dr.parent instanceof SearchPanel) {
					SearchPanel sp = (SearchPanel)dr.parent;
					sp.revalidate();
					sp.repaint();
				}
			}
		});
	}

	public List<SearchRow> getRows() {
		List<SearchRow> srs = new ArrayList<SearchRow>(rows.size());
		for (DisplayRow dr : rows) {
			srs.add(dr.searchRow);
		}
		return srs;
	}

	private Field getField(final String fieldName) {
		for (Field f : Field.values()) {
			if (f.toString().equals(fieldName)) {
				return f;
			}
		}
		return null;
	}

	class DisplayRow {
		final SearchRow searchRow;
		private JPanel parent;
		private JButton rmRowButton;

		public DisplayRow(final JPanel parent,ActionListener al) {
			this.parent = parent;
			this.searchRow = new SearchRow(parent);
			this.rmRowButton = new JButton(rmRowIcon);
			this.rmRowButton.setActionCommand(Command.REMOVEROW.toString());
			this.rmRowButton.setPreferredSize(ROW_BUTTON_DIM);

			this.searchRow.qualifierField.setVisible(false);
			this.searchRow.selectDrugButton.setVisible(false);
			this.searchRow.selectDiagnosisButton.setVisible(false);
			this.searchRow.selectICD9codeButton.setVisible(false);
			this.searchRow.selectLabButton.setVisible(false);
			this.searchRow.comboBox.setMaximumRowCount(SEARCHABLE_FIELDS_ARR.length);
			this.searchRow.comboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent ae) {
					Field newField = (Field)((JComboBox<Field>)ae.getSource()).getSelectedItem();
					searchRow.setQualifierFieldVisible(newField == Field.LAB);
					searchRow.setSelectButtonVisible(newField == Field.MEDICATION);   //-drug
					searchRow.setSelectDiagButtonVisible(newField == Field.DIAGNOSIS);  //ICD9DESCRIPTION
					searchRow.setSelectICD9CODEButtonVisible(newField==Field.ICD9CODE);
					searchRow.setSelectLabButtonVisible(newField==Field.LAB);
				}
			});
			//added by Huijuan, 08/31/2017, whenever value of of the drug is changed, show them
//			this.searchRow.valueField.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent ae) {
//					Field newField = (Field)((JComboBox<Field>)ae.getSource()).getSelectedItem();
//					switch (newField) {
//						case DRUGS:
//							searchRow.setSelectButtonVisible(true);
//							break;
//						default:
//							searchRow.setSelectButtonVisible(false);
//							break;
//					}
//				}
//			});

			this.searchRow.selectDrugButton.addActionListener(al);
			this.searchRow.selectDiagnosisButton.addActionListener(al);
			this.searchRow.selectICD9codeButton.addActionListener(al);
			this.searchRow.selectLabButton.addActionListener(al);

			this.rmRowButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent ae) {
					Object source = ae.getSource();
					if (source instanceof JButton) {
						final JButton btn = (JButton)source;
						DisplayRow cdr = null;
						for (int r = rows.size() - 1; r >= 0; r--) {
							cdr = rows.get(r);
							if (btn == cdr.rmRowButton) {
								break;
							}
						}
						if (cdr == null) {
							throw new RuntimeException("Failed to locate row to remove");
						}
						final DisplayRow dr = cdr;
						final SearchPanel sp = (SearchPanel)btn.getParent();
						rows.remove(dr);
						final int removedGridY = ((GridBagLayout)sp.getLayout()).getConstraints(dr.rmRowButton).gridy;
						final int addRowButtonGridY = ((GridBagLayout)sp.getLayout()).getConstraints(addRowButton).gridy;
						final int maxRowGridY = ((GridBagLayout)sp.getLayout()).getConstraints(rows.get(rows.size() - 1).rmRowButton).gridy;
						ThreadUtils.runOnEDT(new Runnable() {
							@Override
							public void run() {
								sp.remove(dr.searchRow.boolBox);
								sp.remove(dr.searchRow.comboBox);
								sp.remove(dr.searchRow.qualifierField);
								sp.remove(dr.searchRow.valueField);
								sp.remove(dr.searchRow.selectDrugButton);
								sp.remove(dr.searchRow.selectDiagnosisButton);
								sp.remove(dr.searchRow.selectICD9codeButton);
								sp.remove(dr.searchRow.selectLabButton);
								sp.remove(dr.rmRowButton);
								if (removedGridY == addRowButtonGridY) {
									sp.remove(addRowButton);
									GridBagConstraints c = new GridBagConstraints();
									c.gridx = maxRowGridY <= 1 ? 4 : 5;
									c.gridy = maxRowGridY;
									c.gridwidth = 1;
									c.gridheight = 1;
									c.anchor = GridBagConstraints.FIRST_LINE_START;
									sp.add(addRowButton, c);
								}
								sp.revalidate();
								sp.repaint();
								System.out.println("qualifier pref width=" + dr.searchRow.qualifierField.getPreferredSize().width + ", height=" + dr.searchRow.qualifierField.getPreferredSize().height);
								System.out.println("qualifier width=" + dr.searchRow.qualifierField.getSize().width + ", height=" + dr.searchRow.qualifierField.getSize().height);
								System.out.println("value pref width=" + dr.searchRow.valueField.getPreferredSize().width + ", height=" + dr.searchRow.valueField.getPreferredSize().height);
								System.out.println("value width=" + dr.searchRow.valueField.getSize().width + ", height=" + dr.searchRow.valueField.getSize().height);
							};
						});
					}
				}
			});
		}

		public DisplayRow(final Field field, final String text, final JPanel parent,ActionListener al) {
			this(parent,al);
			searchRow.valueField.setText(text);
			searchRow.comboBox.setSelectedItem(field);
		}

		public void remove() {
			parent.remove(searchRow.qualifierField);
			parent.remove(searchRow.comboBox);
			parent.remove(searchRow.boolBox);
			parent.remove(rmRowButton);
			parent.remove(addRowButton);
			parent.add(rmRowButton);
			parent.add(addRowButton);
			parent.validate();
			parent.repaint();
		}

		public void setBoolBoxRmRowBtnVisible(final boolean visible) {
			searchRow.setBoolBoxVisible(visible);
			rmRowButton.setVisible(visible);
			rmRowButton.setEnabled(visible);
		}
	}

	public static class QRButton extends JButton {
		private JTextField textField;

		public QRButton(String str) {
			super(str);
		}
		public QRButton(String str, JTextField tf) {
			super(str);
			this.textField = tf;
		}

		public JTextField getTextField() {
			return textField;
		}
		public void setTextField(final JTextField tf) {
			this.textField = tf;
		}
	}

	public class SearchRow {
		public static final String AND = "And";
		public static final String OR = "Or";
//		public static final String NOT = "Not";

		final public JTextField qualifierField;
		final public JTextField valueField;
		final JComboBox<Field> comboBox;
		final JComboBox<String> boolBox;
		final Container parent;

		//Huijuan
		final public QRButton selectDrugButton;
		final public QRButton selectDiagnosisButton;
		final public QRButton selectICD9codeButton;
		final public QRButton selectLabButton;

		SearchRow(Container parent) {
			this.parent = parent;
			this.qualifierField = new JTextField(15);
			this.qualifierField.setPreferredSize(QUALIFIER_DIM);
			this.comboBox = new JComboBox<>(SEARCHABLE_FIELDS_ARR);
			this.comboBox.setPreferredSize(FIELD_CHOOSER_DIM);
			this.boolBox = new JComboBox<String>(new String[] { AND, OR/*, NOT*/ });
			this.boolBox.setPreferredSize(BOOL_DIM);
			this.valueField = new JTextField(25);
			this.valueField.setPreferredSize(VALUE_DIM);

			this.selectDrugButton = new QRButton("Add similar medications", this.valueField);
			selectDrugButton.setActionCommand(Command.SELECTDRUGS.toString());

			this.selectDiagnosisButton = new QRButton("Add similar diagnosis", this.valueField);
			selectDiagnosisButton.setActionCommand(Command.SELECTDIAGNOSIS.toString());

			this.selectICD9codeButton = new QRButton("Add similar ICD9 codes", this.valueField);
			selectICD9codeButton.setActionCommand(Command.SELECTICD9CODE.toString());

			this.selectLabButton = new QRButton("select lab", this.valueField);
			selectLabButton.setActionCommand(Command.SELECTLAB.toString());

		}

		public void setQualifierFieldVisible(final boolean visible) {
			ThreadUtils.runOnEDT(new Runnable() {
				@Override
				public void run() {
					qualifierLabel.setVisible(visible);
					qualifierLabel.revalidate();
					qualifierLabel.repaint();
					qualifierField.setVisible(visible);
					qualifierField.revalidate();
					qualifierField.repaint();
					valueField.revalidate();
					valueField.repaint();
					if (parent != null) {
						parent.revalidate();
						parent.repaint();
					}
				}
			});
		}

		//by Huijuan 08/31/2017
		public void setSelectButtonVisible(final boolean visible) {
			ThreadUtils.runOnEDT(new Runnable() {
				@Override
				public void run() {
					selectDrugButton.setVisible(visible);
					selectDrugButton.revalidate();
					selectDrugButton.repaint();
					valueField.revalidate();
					valueField.repaint();
					if (parent != null) {
						parent.revalidate();
						parent.repaint();
					}
				}
			});
		}

		//by Huijuan 09/07/2017
		public void setSelectDiagButtonVisible(final boolean visible) {
			ThreadUtils.runOnEDT(new Runnable() {
				@Override
				public void run() {
					selectDiagnosisButton.setVisible(visible);
					selectDiagnosisButton.revalidate();
					selectDiagnosisButton.repaint();
					valueField.revalidate();
					valueField.repaint();
					if (parent != null) {
						parent.revalidate();
						parent.repaint();
					}
				}
			});
		}

		//by Huijuan 09/07/2017
		public void setSelectICD9CODEButtonVisible(final boolean visible) {
			ThreadUtils.runOnEDT(new Runnable() {
				@Override
				public void run() {
					selectICD9codeButton.setVisible(visible);
					selectICD9codeButton.revalidate();
					selectICD9codeButton.repaint();
					valueField.revalidate();
					valueField.repaint();
					if (parent != null) {
						parent.revalidate();
						parent.repaint();
					}
				}
			});
		}

		//by Huijuan 09/07/2017
		public void setSelectLabButtonVisible(final boolean visible) {
			ThreadUtils.runOnEDT(new Runnable() {
				@Override
				public void run() {
					selectLabButton.setVisible(visible);
					selectLabButton.revalidate();
					selectLabButton.repaint();
					valueField.revalidate();
					valueField.repaint();
					if (parent != null) {
						parent.revalidate();
						parent.repaint();
					}
				}
			});
		}

		public String getQualifier() {
			return this.qualifierField.getText();
		}
		public String getSearchField() {
			return this.comboBox.getSelectedItem().toString();
		}
		public SearchTerm toSearchTerm() {
			return new SearchTerm(this.valueField.getText(), getField(this.comboBox.getSelectedItem().toString()),
					BoolOp.valueOf(boolBox.getSelectedItem().toString().toUpperCase()), this.qualifierField.getText());
		}
		public void setBoolBoxVisible(final boolean visible) {
			this.boolBox.setVisible(false);
			this.boolBox.setEnabled(true);
		}
	}

}
