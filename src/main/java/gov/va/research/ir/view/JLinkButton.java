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

import java.awt.Color;
import java.awt.Cursor;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * @author vhaislreddd
 *
 */
public class JLinkButton extends JButton {

	private static final long serialVersionUID = 970550886103983005L;

	/**
	 *
	 */
	public JLinkButton() {
		super();
		init();
	}

	/**
	 * @param icon
	 */
	public JLinkButton(Icon icon) {
		super(icon);
		init();
	}

	/**
	 * @param text
	 */
	public JLinkButton(String text) {
		super(text);
		init();
	}

	/**
	 * @param a
	 */
	public JLinkButton(Action a) {
		super(a);
		init();
	}

	/**
	 * @param text
	 * @param icon
	 */
	public JLinkButton(String text, Icon icon) {
		super(text, icon);
		init();
	}

	private void init() {
		setBorder(new EmptyBorder(0,0,0,0));
		setText("<html><u><font color=\"blue\">" + getText() + "</font></u></html>");
//		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setHorizontalAlignment(SwingConstants.LEFT);
		setBorderPainted(false);
		setContentAreaFilled(false);
		setOpaque(false);
		setBackground(Color.WHITE);
	}

}
