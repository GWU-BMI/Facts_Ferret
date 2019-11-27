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

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

/**
 * @author vhaislreddd
 *
 */
public class JIconButton extends JButton {

	private static final long serialVersionUID = 970550886103983005L;

	/**
	 *
	 */
	public JIconButton() {
		super();
		init();
	}

	/**
	 * @param icon
	 */
	public JIconButton(Icon icon) {
		super(icon);
		init();
	}

	/**
	 * @param text
	 */
	public JIconButton(String text) {
		super(text);
		init();
	}

	/**
	 * @param a
	 */
	public JIconButton(Action a) {
		super(a);
		init();
	}

	/**
	 * @param text
	 * @param icon
	 */
	public JIconButton(String text, Icon icon) {
		super(text, icon);
		init();
	}

	private void init() {
		setBorder(new EmptyBorder(0,0,0,0));
		setBorderPainted(false);
		setContentAreaFilled(false);
		setOpaque(false);
		setBackground(Color.WHITE);
	}

}
