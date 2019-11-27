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

import gov.va.research.ir.SearchUtils;
import gov.va.research.ir.ThreadUtils;
import gov.va.research.ir.model.Command;
import gov.va.research.ir.model.County;
import gov.va.research.ir.model.Field;
import gov.va.research.ir.model.SearchTerm;
import gov.va.research.ir.model.SearchWorker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.xpath.XPathExpressionException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.geotools.feature.SchemaException;
import org.hibernate.metamodel.source.binder.JpaCallbackClass;
import org.jfree.chart.plot.CategoryPlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

import static java.awt.event.ActionEvent.ACTION_LAST;

/**
 * @author vhaislreddd
 */
public class SummaryPanel extends JPanel implements PropertyChangeListener, PdfExportable {

	private static final long serialVersionUID = -969368587786186572L;
	private static final Logger LOG = LoggerFactory.getLogger(SummaryPanel.class);
	private URL loadingIconURL;
	private static final int TOP_CUTOFF = 25;
	private static final boolean BLOCK_PHI = false;

	private JTabbedPane tabbedPane;
	private OverviewPanel overviewPanel;
	// private AbstractMapPanel mapPanel;
	private HistogramPanel agePanel;
	private CategoryChartPanel racePanel;
	//	private HistogramPanel maritalstatusPanel;
	private CategoryChartPanel deceasedPanel;
	private CategoryChartPanel genderPanel;
	private CategoryListPanel drugPanel;
	private CategoryListPanel icd9Panel;
	// private CategoryListPanel doctypePanel;
	// private CategoryListPanel cptPanel;
	private DetailPanel detailPanel;
	private ExecutorService executor;
	private boolean cancelled = false;
	private ActionListener actionListener;

	public SummaryPanel(final ActionListener al) throws IOException {
		super();
		this.addPropertyChangeListener(this);
		this.actionListener = al;
		initialize();
	}

	private void initialize() throws IOException {
		cancelled = false;
		this.setLayout(new BorderLayout());
		final JPanel mainPanel = this;
		loadingIconURL = SummaryPanel.class.getResource("/img/loading-icon.gif");

		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				setBackground(Color.WHITE);
				try {
					overviewPanel = new OverviewPanel(actionListener);
				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}
				mainPanel.add(overviewPanel, BorderLayout.NORTH);

				tabbedPane = new JTabbedPane();
				tabbedPane.setBackground(Color.WHITE);
				mainPanel.add(tabbedPane, BorderLayout.CENTER);

				//			try {
				//				mapPanel = new MapPanel2();
				//			} catch (Exception e) {
				//				throw new RuntimeException(e);
				//			}
				//			tabbedPane.addTab("Map", mapPanel);

				agePanel = new HistogramPanel("  ", "Age Range", "Number of Patients");

				agePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
				tabbedPane.addTab("Age", new JScrollPane(agePanel));


				genderPanel = new CategoryChartPanel("Gender Distribution");
				genderPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
				tabbedPane.addTab("Gender", new JScrollPane(genderPanel));


				racePanel = new CategoryChartPanel("Race Distribution"); //Seyed
				racePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
				tabbedPane.addTab("Race", new JScrollPane(racePanel));

/*
				maritalstatusPanel = new HistogramPanel("Histogram of Marital Status", "Marital Status", "Number of Patients"); //Seyed
				maritalstatusPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
				tabbedPane.addTab("Marital Status", new JScrollPane(maritalstatusPanel));
*/

				deceasedPanel = new CategoryChartPanel("Deceased Distribution");
				deceasedPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
				tabbedPane.addTab("Deceased", new JScrollPane(deceasedPanel));



				drugPanel = new CategoryListPanel("Top 25 Medications" ); //  + TOP_CUTOFF + " Prescription");
				drugPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
				tabbedPane.addTab("Medications", new JScrollPane(drugPanel));




//				ActionEvent ae = new ActionEvent(drugPanel, ActionEvent.ACTION_LAST, Command.TopMedications.toString());
//				actionListener.actionPerformed(ae);
				tabbedPane.addChangeListener(new ChangeListener() {    //For running Medication on click
					public void stateChanged(ChangeEvent ce){

						JTabbedPane sourceTabbedPane = (JTabbedPane) ce.getSource();
						if (sourceTabbedPane.getTitleAt(sourceTabbedPane.getSelectedIndex()) == "Medications" ){
							System.err.println("Medication tab selected");
							actionListener.actionPerformed(new ActionEvent( drugPanel,ActionEvent.ACTION_LAST, Command.TopMedications.toString()));
						}
					}
				});







//				ActionEvent ae = new ActionEvent(detailPanel, ActionEvent.ACTION_LAST, Command.NEXTPAGE.toString());
//				actionListener.actionPerformed(ae);
//                tabbedPane.addChangeListener(new ChangeListener() {
//                	public void stateChanged(ChangeEvent ce) {
//                		if (tabbedPane.getSelectedComponent() instanceof DetailPanel) {
//                			System.err.println("Detail tab selected");
//                			actionListener.actionPerformed(new ActionEvent(detailPanel, ActionEvent.ACTION_LAST, Command.GETDIAGNOSES.toString()));
//						}
//					}
//				});


				icd9Panel = new CategoryListPanel("Top 25 Comorbid Conditions");// + TOP_CUTOFF + " ICD Codes");
				icd9Panel.setBorder(new EmptyBorder(10, 10, 10, 10));
				tabbedPane.addTab("Comorbid Conditions", new JScrollPane(icd9Panel));







				tabbedPane.addChangeListener(new ChangeListener() {    //For running Medication on click
					public void stateChanged(ChangeEvent ce){

						JTabbedPane sourceTabbedPane = (JTabbedPane) ce.getSource();
						if (sourceTabbedPane.getTitleAt(sourceTabbedPane.getSelectedIndex()) == "Comorbid Conditions" ){
							System.err.println("Comorbid Conditions tab selected");
							actionListener.actionPerformed(new ActionEvent( icd9Panel,ActionEvent.ACTION_LAST, Command.TopDiagnosis.toString()));
						}
					}
				});






/*
                cptPanel = new CategoryListPanel("Top " + TOP_CUTOFF + " CPT Codes");
                cptPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
                tabbedPane.addTab("Procedure", new JScrollPane(cptPanel));

                doctypePanel = new CategoryListPanel("Top " + TOP_CUTOFF + " Document Types");
                doctypePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
                tabbedPane.addTab("Doc. Type", new JScrollPane(doctypePanel));
 */
                detailPanel = new DetailPanel();

                tabbedPane.addTab("Details", detailPanel);







				// setEnabledTab(mapPanel, false);
				setEnabledTab(agePanel, false);
				setEnabledTab(racePanel, false);
				//	setEnabledTab(maritalstatusPanel, false);
				setEnabledTab(deceasedPanel, false);
				setEnabledTab(genderPanel, false);
				setEnabledTab(drugPanel, false);
				setEnabledTab(icd9Panel, false);
				// setEnabledTab(doctypePanel, false);
				// setEnabledTab(cptPanel, false);
				 setEnabledTab(detailPanel, false);
			}
		});
	}

	public void reset() {
		cancelled = false;
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {

				tabbedPane.setSelectedIndex(0);

				for (int i = 0; i < tabbedPane.getTabCount(); i++) {
					tabbedPane.setEnabledAt(i, false);
				}

				overviewPanel.reset();
				//    mapPanel.reset();
				agePanel.reset();

				//		maritalstatusPanel.reset();
				deceasedPanel.reset();
				genderPanel.reset();
				racePanel.reset();
				drugPanel.reset();
				icd9Panel.reset();
				//     doctypePanel.reset();
				//     cptPanel.reset();
				     detailPanel.reset();
			}
		});
	}

	public void setNumMatchingPatients(int count) {
		overviewPanel.setNumMatchingPatients(count);
	}
	/**
	 * @param search The search worker (for accessing results)
	 * @param searchTerms The search terms
	 * @throws URISyntaxException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws SchemaException
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IllegalAccessException
	 * @throws XPathExpressionException
	 */
	public synchronized void displaySummary(final SearchWorker search,
											final List<SearchTerm> searchTerms, final boolean hasSensitiveData)
			throws InterruptedException, ExecutionException, IllegalAccessException, SQLException, XPathExpressionException, IOException {
		overviewPanel.setSaveEnabled(false);
		// If the data source has sensitive data and the patient count is less than 10, do not display
		final int patientCount = search.getPatientResultCount();
		if (BLOCK_PHI && hasSensitiveData && patientCount < 10) {
			throw new IllegalAccessException("Less than 10 matching patients");
		}
		overviewPanel.setNumMatchingPatients(patientCount);
		//	firePropertyChange("panelUpdate", null, mapPanel);

		// Set up collection of summarization tasks
		List<Runnable> summarizers = new ArrayList<Runnable>();

	/*	if (SearchUtils.containsDocumentFields(searchTerms)) {
			// Add task for counting the matching documents
			summarizers.add(new Runnable() {
				public void run() {
					try {
						final int documentCount = search
								.getDocumentResultCount();
						overviewPanel.setNumMatchingDocuments(documentCount);
						firePropertyChange("panelUpdate", null, mapPanel);
					} catch (SQLException e) {
						throw new RuntimeException(e);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			});
		} else {
			overviewPanel.setNumMatchingDocuments(-1);
		}
*/
		// Add task for plotting the map
/*
        summarizers.add(new Runnable() {
            public void run() {
                setLoadingTab(mapPanel, true);
                // update map
                try {
                    Map<Coordinate, Integer> countySubtotalsMap = search.getGeographicDistribution();
                    mapPanel.updateMap(countySubtotalsMap);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    setLoadingTab(mapPanel, false);
                }
                firePropertyChange("panelUpdate", null, mapPanel);
            }
        });


        */
		// Add task for finding the age distribution

		summarizers.add(new Runnable() {
			public void run() {
				setLoadingTab(agePanel, true);
				try {
					agePanel.addValues(search.getAgeDistribution(), 5);
					firePropertyChange("panelUpdate", null, agePanel);
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					setLoadingTab(agePanel, false);
				}
			}
		});





		// Add race distribution

		summarizers.add(new Runnable() {
			public void run() {
				setLoadingTab(racePanel, true);
				try {
					racePanel.addValues(search.getRaceDistribution());
					firePropertyChange("panelUpdate", null, racePanel);
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					setLoadingTab(racePanel, false);
				}
			}
		});



		// Add Medication distribution

		summarizers.add(new Runnable() {
			public void run() {
				setLoadingTab(drugPanel, true);
				try {
//					drugPanel.addValues(search.getMedicationDistribution());
					firePropertyChange("panelUpdate", null, drugPanel);
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					setLoadingTab(drugPanel, false);
				}
			}
		});





		// Add task for finding the gender distribution

		summarizers.add(new Runnable() {
			public void run() {
				setLoadingTab(genderPanel, true);
				try {
					genderPanel.addValues(search.getGenderDistribution());
					firePropertyChange("panelUpdate", null, genderPanel);
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					setLoadingTab(genderPanel, false);
				}
			}
		});



		// Add task for finding the deceased distribution

		summarizers.add(new Runnable() {
			public void run() {
				setLoadingTab(deceasedPanel, true);
				try {
					deceasedPanel.addValues(search.getDeceasedDistribution());
					firePropertyChange("panelUpdate", null, deceasedPanel);
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					setLoadingTab(deceasedPanel, false);
				}
			}
		});


		// Add task for getting the patient details

		summarizers.add(new Runnable() {
			public void run() {
				setLoadingTab(detailPanel, true);
				try {
					ActionEvent ae = new ActionEvent(detailPanel, ACTION_LAST, Command.NEXTPAGE.toString());
					actionListener.actionPerformed(ae);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});





 //For using in details
//		summarizers.add(new Runnable() {
//			public void run() {
//				try {
//					search.getMatchingDiagnoses();
//				} catch (Exception e) {
//					throw new RuntimeException(e);
//				}
//			}
//		});







		// Add task for finding the diagnosis distribution

		summarizers.add(new Runnable() {
			public void run() {
				setLoadingTab(icd9Panel, true);
				try {
					//icd9Panel.addValues(search.getDxDistribution(TOP_CUTOFF));
					firePropertyChange("panelUpdate", null, icd9Panel);
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					setLoadingTab(icd9Panel, false);
				}
			}

		});


/*

        // Add task for finding the procedure distribution

        summarizers.add(new Runnable() {
            public void run() {
                setLoadingTab(cptPanel, true);
                try {
                    cptPanel.addValues(search.getProcedureDistribution(TOP_CUTOFF));
                    firePropertyChange("panelUpdate", null, cptPanel);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    setLoadingTab(cptPanel, false);
                }
            }
        });
*/
		// Add task for finding the document type distribution

/*		summarizers.add(new Runnable() {
			public void run() {
				setLoadingTab(doctypePanel, true);
				try {
					doctypePanel.addValues(search.getDocumentTypeDistribution(TOP_CUTOFF));
					firePropertyChange("panelUpdate", null, doctypePanel);
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					setLoadingTab(doctypePanel, false);
				}
			}
		});

*/



		// Set up a thread pool to execute the individual summarizations
		executor = Executors.newFixedThreadPool(summarizers.size());
		// Set up a collection of futures to access the results
		List<Future<?>> futures = new ArrayList<Future<?>>(summarizers.size());
		// Execute all of the summarizers
		for (Runnable r : summarizers) {
			futures.add(executor.submit(r));
		}
		// Do an orderly shutdown of the thread pool
		executor.shutdown();
		while (!executor.isTerminated()) {
			if (cancelled) {
				for (Future<?> f : futures) {
					f.cancel(true);
				}
			}
			Thread.sleep(100);
		}
		// Do a get() on each future in order to re-throw any exceptions from the summarizersca
		for (Future<?> f : futures) {
			f.get();
		}
		// Wait for tabs with worker threads to finish
		Thread.sleep(1000);
		int numLoading = 0;
		do {
			numLoading = 0;
			for (int i = 0; i < tabbedPane.getTabCount(); i++) {
				if (tabbedPane.getIconAt(i) != null) {
					numLoading++;
				}
			}
			if (numLoading != 0) {
				Thread.sleep(1000);
			}
		} while (numLoading != 0);
//		mapPanel.updatePanel();
		overviewPanel.setSaveEnabled(true);
	}

	public void dispose() throws SQLException {
//		mapPanel.dispose();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.
	 * PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		if ("panelUpdate".equals(evt.getPropertyName())
				&& evt.getNewValue() != null
				&& evt.getNewValue() instanceof JPanel) {
			final JPanel panel = (JPanel) evt.getNewValue();
			ThreadUtils.runOnEDT(new Runnable() {
				public void run() {
					setEnabledTab(panel, true);
					panel.validate();
					validate();
				}
			});
		} else {
			LOG.info("Unhandled property change event: " + evt.toString());
		}
	}

	public void setEnabledTab(final JComponent c, final boolean enabled) {
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				for (int i = 0; i < tabbedPane.getTabCount(); i++) {
					Component tabComponent = tabbedPane.getComponentAt(i);
					if (tabComponent.equals(c)) {
						tabbedPane.setEnabledAt(i, enabled);
						break;
					} else if (tabComponent instanceof JScrollPane) {
						for (Component scrollComponent : ((JScrollPane) tabComponent)
								.getViewport().getComponents()) {
							if (scrollComponent.equals(c)) {
								tabbedPane.setEnabledAt(i, enabled);
								break;
							}
						}
					}
				}
			}
		});
	}

	public void setLoadingTab(final JComponent c, final boolean loading) {
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				// find component tab index
				int tabIdx = -1;
				for (int i = 0; i < tabbedPane.getTabCount(); i++) {
					Component tabComponent = tabbedPane.getComponentAt(i);
					if (tabComponent.equals(c)) {
						tabIdx = i;
						break;
					} else if (tabComponent instanceof JScrollPane) {
						for (Component scrollComponent : ((JScrollPane) tabComponent)
								.getViewport().getComponents()) {
							if (scrollComponent.equals(c)) {
								tabIdx = i;
								break;
							}
						}
					}
				}
				if (tabIdx != -1) {
					if (loading) {
						try {
							AnimatedIcon ai =  new AnimatedIcon(tabbedPane, loadingIconURL);
							tabbedPane.setIconAt(tabIdx, ai);
							tabbedPane.setDisabledIconAt(tabIdx, ai);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					} else {
						Icon icon = tabbedPane.getIconAt(tabIdx);
						if (icon instanceof AnimatedIcon) {
							((AnimatedIcon)icon).setAnimating(false);
						}
						tabbedPane.setIconAt(tabIdx, null);
						tabbedPane.setDisabledIconAt(tabIdx, null);
					}
				}
			}
		});
	}

	public void cancel() {
		cancelled = true;
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	/* (non-Javadoc)
	 * @see gov.va.research.ir.view.PdfExportable#buildPdfPage(org.apache.pdfbox.pdmodel.PDDocument)
	 */
	@Override
	public List<PDPage> addPdfPages(PDDocument pdDocument) throws IOException {
		List<PDPage> pages = new ArrayList<>();
		//	pages.addAll(mapPanel.addPdfPages(pdDocument));
		pages.addAll(agePanel.addPdfPages(pdDocument));
		pages.addAll(deceasedPanel.addPdfPages(pdDocument));
		pages.addAll(genderPanel.addPdfPages(pdDocument));
		pages.addAll(racePanel.addPdfPages(pdDocument));
		pages.addAll(drugPanel.addPdfPages(pdDocument));
		//	pages.addAll(icd9Panel.addPdfPages(pdDocument));
		//	pages.addAll(cptPanel.addPdfPages(pdDocument));
		//	pages.addAll(doctypePanel.addPdfPages(pdDocument));
		return pages;
	}
}
