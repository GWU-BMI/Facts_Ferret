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


package gov.va.research.ir;

import gov.va.research.ir.controller.VoogoController;
import gov.va.research.ir.view.CLIView;
import gov.va.research.ir.view.SearchPanel;
import gov.va.research.ir.view.SearchResultDisplayer;
import gov.va.research.ir.view.VoogleView;

import java.awt.EventQueue;

import org.geotools.resources.SwingUtilities;


/**
 * @author doug
 *
 */
public class FactsFerret {

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					final VoogoController controller = new VoogoController();
					if (args != null && args.length >= 2) {
						final CLIView cliView = new CLIView(controller, args[0], args[1], (args.length >= 3 ? args[2] : null));
						SwingUtilities.invokeAndWait(new Runnable() {
							@Override
							public void run() {
								SearchResultDisplayer<SearchPanel.SearchRow> view = cliView;
								controller.setView(view);
							}
						});
					} else {
						SwingUtilities.invokeAndWait(new Runnable() {
							@Override
							public void run() {
								SearchResultDisplayer<SearchPanel.SearchRow> view = new VoogleView(controller);
								controller.setView(view);
							}
						});
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

}
