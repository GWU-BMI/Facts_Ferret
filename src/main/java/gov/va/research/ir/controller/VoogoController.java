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
package gov.va.research.ir.controller;

import apple.laf.JRSUIConstants;
import gov.va.research.ir.ThreadUtils;
import gov.va.research.ir.model.CodeNameCount;
import gov.va.research.ir.model.Command;
import gov.va.research.ir.model.Context;
import gov.va.research.ir.model.DAO;
import gov.va.research.ir.model.Field;
import gov.va.research.ir.model.NameCount;
import gov.va.research.ir.model.QueryExpander;
import gov.va.research.ir.model.SaveFile;
import gov.va.research.ir.model.SearchResult;
import gov.va.research.ir.model.SearchTerm;
import gov.va.research.ir.model.SearchWorker;
import gov.va.research.ir.model.StopWords;
import gov.va.research.ir.view.*;
import gov.va.vinci.nlp.qeUtils.domain.TermWeight;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.Serializable;
import java.sql.*;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

import org.apache.commons.lang.ObjectUtils;
import org.apache.http.util.Args;
import org.apache.poi.hssf.util.HSSFColor;
import org.eclipse.emf.common.util.EMap;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.labels.*;
import org.jfree.chart.renderer.category.*;

import org.jfree.util.PublicCloneable;
import org.jfree.util.ShapeUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.jfree.data.category.IntervalCategoryDataset;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.data.time.SimpleTimePeriod;
import java.awt.color.*;

import org.jfree.chart.plot.CategoryPlot;

import java.awt.Color;
import java.text.DateFormat;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Day;
import org.jfree.date.SerialDate;

import org.jfree.ui.RefineryUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;

//import org.jfree.chart.labels.CategoryToolTipGenerator;
//import org.jfree.chart.labels.StandardCategoryToolTipGenerator;

import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Day;
import org.jfree.date.MonthConstants;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;


import javax.swing.JPanel;

import static javax.swing.text.StyleConstants.Size;
import static org.jfree.chart.labels.IntervalCategoryToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT_STRING;

/**
 * @author vhaislreddd

 */



import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;



public class VoogoController implements ActionListener, PropertyChangeListener, WindowListener {



    private DataSource dataSource;



    private static final Logger LOG = LoggerFactory.getLogger(VoogoController.class);
    private SearchResultDisplayer<SearchPanel.SearchRow> view;
    private SearchWorker search;

    private Map<String,DAO> daoMap;
    private boolean initialized;
    private Thread detailPanelPatientThread;
    private ScheduledExecutorService searchTimeoutExecutorService;
    private ScheduledFuture<?> searchTimeoutFuture;
    private QueryExpander queryExpander;

    public VoogoController() {
        daoMap = new HashMap<String,DAO>();
        List<DAO> daos = (List<DAO>)Context.applicationContext.getBean("DAOs");
        for (DAO dao : daos) { daoMap.put(dao.getDisplayName(), dao); }
        queryExpander = Context.applicationContext.getBean(QueryExpander.class);

        initialized = false;
        new Thread(new Runnable () {
            public void run() {
                try {
                    initialized = true;
                } catch (Exception e) {
                    LOG.error(e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void setView(SearchResultDisplayer<SearchPanel.SearchRow> view) {
        this.view = view;
        this.view.addWindowListener(this);
        this.view.setDataSetNames(daoMap.keySet());
        this.view.setVisible(true);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.
     * PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if ("error".equals(evt.getPropertyName())) {
            view.error(evt.getNewValue().toString());
        } else if ("state".equals(evt.getPropertyName())) {
            final Object newVal = evt.getNewValue();
            if (SearchWorker.SearchState.PENDING.equals(newVal)) {
                view.setStatus("Search Pending");
            } else if (SearchWorker.SearchState.STARTED.equals(newVal)) {
                view.setStatus("Search Started");
            } else if (SearchWorker.SearchState.EXPANDING_QUERY_OPENDB
                    .equals(newVal)) {
                view.setStatus("Expanding Query - Opening DB");
            } else if (SearchWorker.SearchState.EXPANDING_QUERY_CUIS
                    .equals(newVal)) {
                view.setStatus("Expanding Query - CUIs");
            } else if (SearchWorker.SearchState.EXPANDING_QUERY_SYNONYMS
                    .equals(newVal)) {
                view.setStatus("Expanding Query - synonyms");
            } else if (SearchWorker.SearchState.COUNTING_DOCUMENTS
                    .equals(newVal)) {
                view.setStatus("Counting Documents");
            } else if (SearchWorker.SearchState.QUERYING_DOCUMENTS
                    .equals(newVal)) {
                view.setStatus("Fetching Documents");
            } else if (SearchWorker.SearchState.QUERYING_PATIENTS
                    .equals(newVal)) {
                view.setStatus("Fetching Patients");
            } else if (SearchWorker.SearchState.DONE.equals(newVal) /* || SwingWorker.StateValue.DONE.equals(newVal) */) {
                if (searchTimeoutFuture != null) {
                    if (!searchTimeoutFuture.cancel(false)) {
                        view.displayDialog("Search cancelled, query timeout expired", "Search Cancelled");
                    }
                }
                final SearchWorker search = (SearchWorker)evt.getSource();
                view.searchComplete(search, view.getSearchTerms());
            } else if (SearchWorker.SearchState.CANCELLED.equals(newVal)) {
                view.cancelSearch();
            } else {
                LOG.warn("Unhandled state property change event: oldValue = "
                        + evt.getOldValue()
                        + ", newValue = "
                        + evt.getNewValue());
            }
        } else {
            LOG.warn("Unhandled property change event: "
                    + evt.getPropertyName() + ", oldValue = "
                    + evt.getOldValue() + ", newValue = " + evt.getNewValue());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */

    static class Job {

        private String title;
        private Date start_date;
        private Date end_date;
        private String status;

        public Job() {
        }

        public Job(String title, Date start_date, Date end_date, String status) {
            this.title = title;
            this.start_date = start_date;
            this.end_date = end_date;
            this.status = status;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Date getStart_date() {
            return start_date;
        }

        public void setStart_date(Date start_date) {
            this.start_date = start_date;
        }

        public Date getEnd_date() {
            return end_date;
        }

        public void setEnd_date(Date end_date) {
            this.end_date = end_date;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

    }


    public void actionPerformed(final ActionEvent e) {
        Command command = Command.valueOf(e.getActionCommand());
        List<SearchTerm> searchTerms = null;


        switch (command) {
            case SEARCH:
                view.clearResults();
                searchTerms = view.getSearchTerms();
                String dataSetName = view.getDataSetName();
                if (dataSetName != null) {
                    DAO dao = daoMap.get(dataSetName);
                    search = new SearchWorker(searchTerms, dao);
                    search.addPropertyChangeListener(this);
                    search.execute();
                    view.searchBegun();
                    // Create a task to kill the search if it is not finished in a reasonable amount of time
                    final int timeout = 0;//dao.getQueryTimeoutMinutes();
                    if (timeout > 0) {
                        final TimeUnit timeoutUnit = TimeUnit.MINUTES;
                        searchTimeoutExecutorService = Executors.newScheduledThreadPool(1);
                        searchTimeoutFuture = searchTimeoutExecutorService.schedule(new Runnable() {
                            @Override
                            public void run() {
                                final ActionEvent ae = new ActionEvent(e.getSource(), -1, Command.CANCEL.name());
                                EventQueue.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        LOG.info("Cancelling search because it exceeded the allowed time of " + timeout + " " + timeoutUnit);
                                        actionPerformed(ae);
                                    }
                                });
                            }
                        }, timeout, timeoutUnit);
                    }
                } else {
                    view.error("A data set must be selected");
                }
                break;


            case CANCEL:
                if (search != null) {
                    try {
                        search.cancel();
                    } catch (SQLException e1) {
                        LOG.info("Exception when canceling search", e1);
                    }
                    search.removePropertyChangeListener(this);
                    search = null;
                }
                if (detailPanelPatientThread != null) {
                    detailPanelPatientThread.interrupt();
                }
                if (searchTimeoutFuture != null && e.getID() != -1) {
                    searchTimeoutFuture.cancel(false);
                }
                view.cancelSearch();
                break;


            case SHOWMESSAGE:
                final JComponent source = (JComponent) e.getSource();
                String buttonText = null;
                if (source instanceof JButton) {
                    buttonText = ((JButton) source).getText();
                }
                view.displayDialog(source.getName(), buttonText);
                break;
            case SAVE:
                try {
                    final SaveFile saveFile = view.getFileForSaving();
                    if (saveFile != null) {
                        SwingWorker<Object, Object> sw = new SwingWorker<Object, Object>() {
                            @Override
                            protected Object doInBackground() throws Exception {
                                LOG.info("Save thread = " + Thread.currentThread().getName() + " (" + Thread.currentThread().getId() + ")");
                                try {
                                    view.saveSummary(saveFile.file, search);
                                    search.saveResults(saveFile);
                                } catch (Exception e) {
                                    LOG.error(e.getMessage());
                                    e.printStackTrace();
                                    throw new RuntimeException(e);
                                } finally {
                                    view.saveComplete();
                                }
                                return null;
                            }
                        };
                        sw.addPropertyChangeListener(this);
                        sw.execute();
                        view.saveBegun();
                    }
                } catch (Exception e1) {
                    view.error(e1.getMessage());
                    LOG.error(e1.getMessage(), e1);
                }
                break;



//            case QUERYRECOMMENDATION:
//                List<String> recommendedTerms;
//                try {
//                    view.setState(ViewState.WAIT);
//                    searchTerms = view.getSearchTerms();
//                    Set<String> docTextTerms = new HashSet<String>();
//
////                    for (SearchTerm st : searchTerms) {
////                        if (Field.DOCUMENT_TEXT.equals(st.field)) {
////                            docTextTerms.add(st.term);
////                        }
////                    }
//
//                    docTextTerms.add("hallucinations");
//                  //  fieldValueMap.get(Field.DIAGNOSIS).get(0).term
//                    recommendedTerms = new ArrayList<String>();
//                    if (docTextTerms.size() > 0) {
//                        try {
//                            while (!initialized) {
//                                Thread.sleep(100);
//                            }
//                            List<TermWeight> recommendations = queryExpander.findRelatedTerms(docTextTerms);
//                            int numAdded = 0;
//                            for (int i = recommendations.size() - 1; i >= 0 && numAdded <= 20; i--) {
//                                TermWeight rec = recommendations.get(i);
//                                if (!docTextTerms.contains(rec.getWord())) {
//                                    recommendedTerms.add(rec.getWord());
//                                    numAdded++;
//                                }
//                            }
//                        } catch (Exception e1) {
//                            view.error(e1.getMessage());
//                            LOG.error(e1.getMessage());
//                            e1.printStackTrace();
//                        }
//                    }
//                } finally {
//                    view.setState(ViewState.NORMAL);
//                }
//                view.doQueryRecommendation(recommendedTerms);
//                break;



            case SELECTDIAGNOSIS:
                String dsName = view.getDataSetName();
                DAO dao = daoMap.get(dsName);
                search = new SearchWorker(searchTerms, dao);
                search.addPropertyChangeListener(this);
                List<String> recommendedTerms2 = new ArrayList<String>();

                try {


                    String diagComponentName = ((SearchPanel.QRButton) e.getSource()).getTextField().getText();
        //            if (docTextTerms.size() > 0) {

                        try {
                            while (!initialized) {
                                Thread.sleep(100);
                            }

                           recommendedTerms2 = search.select_Diagnosis(diagComponentName);



                          } catch (Exception e1) {
                              view.error(e1.getMessage());
                               LOG.error(e1.getMessage());
                               e1.printStackTrace();
                        }
              }

//                catch (NullPointerException e1) {
//
//                }
                  finally {
                    view.setState(ViewState.NORMAL);
                }
              view.doQueryRecommendation(recommendedTerms2);

                break;




            case SELECTDRUGS:
                String dssName = view.getDataSetName();
                DAO daoa = daoMap.get(dssName);
                search = new SearchWorker(searchTerms, daoa);
                search.addPropertyChangeListener(this);
                List<String> recommendedTerms3 = new ArrayList<String>();

                try {


                    String diagComponentName = ((SearchPanel.QRButton) e.getSource()).getTextField().getText();


                    try {
                        while (!initialized) {
                            Thread.sleep(100);
                        }

                        recommendedTerms3 = search.select_Medication(diagComponentName);



                    } catch (Exception e1) {
                        view.error(e1.getMessage());
                        LOG.error(e1.getMessage());
                        e1.printStackTrace();
                    }
                }


                finally {
                    view.setState(ViewState.NORMAL);
                }
                view.doQueryRecommendation(recommendedTerms3);

                break;



            case SELECTICD9CODE:
                List<String> recommendedTerms4;
                try {
                    view.setState(ViewState.WAIT);
                    searchTerms = view.getSearchTerms();
                    Set<String> docTextTerms = new HashSet<String>();

//                    for (SearchTerm st : searchTerms) {
//                        if (Field.DOCUMENT_TEXT.equals(st.field)) {
//                            docTextTerms.add(st.term);
//                        }
//                    }

                    docTextTerms.add("hallucinations");
                    //  fieldValueMap.get(Field.DIAGNOSIS).get(0).term
                    recommendedTerms4 = new ArrayList<String>();
                    if (docTextTerms.size() > 0) {
                        try {
                            while (!initialized) {
                                Thread.sleep(100);
                            }
                            List<TermWeight> recommendations = queryExpander.findRelatedTerms(docTextTerms);
                            int numAdded = 0;
                            for (int i = recommendations.size() - 1; i >= 0 && numAdded <= 20; i--) {
                                TermWeight rec = recommendations.get(i);
                                if (!docTextTerms.contains(rec.getWord())) {
                                    recommendedTerms4.add(rec.getWord());
                                    numAdded++;
                                }
                            }
                        } catch (Exception e1) {
                            view.error(e1.getMessage());
                            LOG.error(e1.getMessage());
                            e1.printStackTrace();
                        }
                    }
                } finally {
                    view.setState(ViewState.NORMAL);
                }
                view.doQueryRecommendation(recommendedTerms4);
                break;






            case DISPLAYDIAGNOSES:
                DetailPanel detailPanel=null;
                if (e.getSource() instanceof DetailPanel) {
                    detailPanel = (DetailPanel)e.getSource();
                } else if (e.getSource() instanceof JButton) {
                    JButton nextButton = (JButton)e.getSource();
                    Component parent = nextButton.getParent();
                    while (parent != null && (!(parent instanceof DetailPanel))) {
                        parent = parent.getParent();
                    }
                    if (parent instanceof DetailPanel) {
                        detailPanel = (DetailPanel)parent;
                    }
                } else {
                    LOG.error("Unhandled source for NEXTPAGE event: " + e.getSource());
                }


                final DetailPanel ffDetailPanel = detailPanel;

                List<SearchResult.PDiag> diagCncs;
                List<SearchResult.PDiag> DIJ;

                if (!ffDetailPanel.getShowAllDocs()) {
                    try {
                        view.setState(ViewState.WAIT);
                        String diagComponentName = ((Component) e.getSource()).getName();
                        diagCncs = null;

                        try {

                            diagCncs = search.getDiagnoses(diagComponentName);

                        } catch (SQLException e1) {
                            view.error(e1.getMessage());
                            LOG.error(e1.getMessage());
                            e1.printStackTrace();
                        }
                    } finally {
                        view.setState(ViewState.NORMAL);
                    }

                } else
                {
                    try {
                        view.setState(ViewState.WAIT);
                        String diagComponentName = ((Component) e.getSource()).getName();
                        diagCncs = null;

                        try {

                            diagCncs = search.getDiagnoses2(diagComponentName);

                        } catch (SQLException e1) {
                            view.error(e1.getMessage());
                            LOG.error(e1.getMessage());
                            e1.printStackTrace();
                        }
                    } finally {
                        view.setState(ViewState.NORMAL);
                    }

                }


                DIJ= diagCncs;
                int dbj = DIJ.size();


                class DiagGa extends JFrame  {

                    private static final long serialVersionUID = 1L;

                    public DiagGa(String title) {
                        super(title);

                        IntervalCategoryDataset dataset =  getCategoryDataset();
                        JFreeChart chart = ChartFactory.createGanttChart(
                                "Comorbid Conditions ",
                                "Conditions",
                                "Timeline",
                                dataset, false, true, true
                        );

                        ChartPanel panel = new ChartPanel(chart);

                        setContentPane(panel);

                        CategoryPlot plot = chart.getCategoryPlot();
                        chart.setBackgroundPaint(new Color(234, 234, 234, 75));
                        plot.setBackgroundPaint(new Color(255, 255, 0, 82));

                        GanttRenderer renderer2 = new GanttRenderer();

                        renderer2.setBaseToolTipGenerator(new IntervalCategoryToolTipGenerator("{1}  ",DateFormat.getDateInstance()));

                        renderer2.setMaximumBarWidth(0.05);

                        plot.setRenderer(renderer2);
                    }

                    HashMap < String, java.sql.Timestamp> con_enc_int = new HashMap<>();
                    HashMap < String, java.sql.Timestamp> con_enc_fin = new HashMap<>();
                    HashMap < String, List> con_enc_id = new HashMap<>();

                    List list_id = new ArrayList<>();

                    private IntervalCategoryDataset getCategoryDataset() {

                        TaskSeries series1 = new TaskSeries("");





                        for ( int i=0 ; i<dbj; i++) {

                               if (con_enc_int.containsKey(DIJ.get(i).Di) ) {

                                           if (con_enc_int.get(DIJ.get(i).Di).compareTo( DIJ.get(i).date_adm )  > 0 ) {
                                               con_enc_int.put(DIJ.get(i).Di, DIJ.get(i).date_adm);
                                          }

                                          if  (con_enc_fin.get(DIJ.get(i).Di).compareTo( DIJ.get(i).date_dis )  < 0 ) {
                                               con_enc_fin.put(DIJ.get(i).Di, DIJ.get(i).date_dis);
                                             }

                                          List w=  con_enc_id.get(DIJ.get(i).Di);
                                          w.add(i);
                                          con_enc_id.put(DIJ.get(i).Di, w );

                            }

                            else {

                                con_enc_int.put(DIJ.get(i).Di, DIJ.get(i).date_adm); con_enc_fin.put(DIJ.get(i).Di, DIJ.get(i).date_dis);

                                List vr = new ArrayList<>();

                                vr.add(i);
                                con_enc_id.put(DIJ.get(i).Di, vr);



                            }

                            SearchResult.PDiag pd = DIJ.get(i);

                        }



                         for (String key : con_enc_int.keySet()) {


                             List<Integer> u = con_enc_id.get(key);

                             Task t = new Task ( key , con_enc_int.get(key)  , con_enc_fin.get(key) );

                             for ( int j =0;  j<u.size() ; j++  )  {

                                 Task sub_t = new Task (DIJ.get(u.get(j)).Di, DIJ.get( u.get(j)).date_adm, DIJ.get( u.get(j)).date_dis );
                                 t.addSubtask(sub_t);
                                                                    }

                             series1.add(t);

                        }

                       // System.out.println(con_enc_int  +  "  aval ");  System.out.println(con_enc_fin +  "  dovom " );   System.out.println(con_enc_id +  "  id ");
                        TaskSeriesCollection dataset = new TaskSeriesCollection();
                        dataset.add(series1);

                        return dataset;
                    }


                    public  void main() {
                        SwingUtilities.invokeLater(() -> {
                            DiagGa diagga = new DiagGa("Comorbid Conditions");
                            //	diagga.setSize(1000, 800);
                            diagga.setLocationRelativeTo(null);
                            diagga.setVisible(true);
                            diagga.pack();

                        });
                    }
                }


                if (dbj >0) { DiagGa GatDiag = new DiagGa(null);	 GatDiag.main();} else {view.displayDialog(" No record was found!" , "Lab Results");}

                break;







            case DISPLAYDRUGS:

                DetailPanel D_detailPanel=null;
                if (e.getSource() instanceof DetailPanel) {
                    D_detailPanel = (DetailPanel)e.getSource();
                } else if (e.getSource() instanceof JButton) {
                    JButton nextButton = (JButton)e.getSource();
                    Component parent = nextButton.getParent();
                    while (parent != null && (!(parent instanceof DetailPanel))) {
                        parent = parent.getParent();
                    }
                    if (parent instanceof DetailPanel) {
                        D_detailPanel = (DetailPanel)parent;
                    }
                } else {
                    LOG.error("Unhandled source for NEXTPAGE event: " + e.getSource());
                }


                final DetailPanel D_DetailPanel = D_detailPanel;

                List<SearchResult.PMedication> drugNcs;
                List<SearchResult.PMedication> JJ;



                if (!D_DetailPanel.getShowAllDocs()) {

                    try {
                        view.setState(ViewState.WAIT);
                        String drugComponentName = ((Component) e.getSource()).getName();
                        drugNcs = null;
                        try {
                            drugNcs = search.getDrugs(drugComponentName);
                        } catch (SQLException e1) {
                            view.error(e1.getMessage());
                            LOG.error(e1.getMessage());
                            e1.printStackTrace();
                        }
                    } finally {
                        view.setState(ViewState.NORMAL);
                    }
                } else {
                    try {
                        view.setState(ViewState.WAIT);
                        String drugComponentName = ((Component) e.getSource()).getName();
                        drugNcs = null;
                        try {
                            drugNcs = search.getDrugs2(drugComponentName);
                        } catch (SQLException e1) {
                            view.error(e1.getMessage());
                            LOG.error(e1.getMessage());
                            e1.printStackTrace();
                        }
                    } finally {
                        view.setState(ViewState.NORMAL);
                    }
                }


                int b = drugNcs.size();
                JJ= drugNcs;

                if (b >0) {

                    class DrugGa extends JFrame {

                        private static final long serialVersionUID = 1L;


                        public DrugGa(String title) {
                            super(title);

                            IntervalCategoryDataset dataset3 =  getCategoryDataset();
                            JFreeChart chart = ChartFactory.createGanttChart(
                                    "Medications",
                                    "Medications",
                                    "Timeline",
                                    dataset3, false, true, true
                            );

                            ChartPanel panel = new ChartPanel(chart);
                            setContentPane(panel);

                            CategoryPlot plot = chart.getCategoryPlot();
                            chart.setBackgroundPaint(new Color(234, 234, 234, 107));
                            plot.setBackgroundPaint(new Color(10, 32, 247, 20));

                            GanttRenderer renderer2 = new GanttRenderer();



                            renderer2.setBaseToolTipGenerator(new IntervalCategoryToolTipGenerator("{1} ",DateFormat.getDateInstance()));

                            renderer2.setMaximumBarWidth(0.05);

                            plot.setRenderer(renderer2);

                        }


                        HashMap < String, java.sql.Date> drug_enc_int = new HashMap<>();
                        HashMap < String, java.sql.Date> drug_enc_fin = new HashMap<>();
                        HashMap < String, List> drug_enc_id = new HashMap<>();

                        List drug_list_id = new ArrayList<>();

                        public IntervalCategoryDataset getCategoryDataset() {
                            TaskSeries series1 = new TaskSeries("");

                            for (int i = 0; i < b; i++) {

                                if (drug_enc_int.containsKey(JJ.get(i).PatMed) ) {

                                    if (drug_enc_int.get(JJ.get(i).PatMed).compareTo( JJ.get(i).med_started_dt_tm )  > 0 ) {
                                        drug_enc_int.put(JJ.get(i).PatMed, JJ.get(i).med_started_dt_tm);
                                    }

                                    if  (drug_enc_fin.get(JJ.get(i).PatMed).compareTo( JJ.get(i).med_stopped_dt_tm )  < 0 ) {
                                        drug_enc_fin.put(JJ.get(i).PatMed, JJ.get(i).med_stopped_dt_tm);
                                    }

                                    List w=  drug_enc_id.get(JJ.get(i).PatMed);
                                    w.add(i);
                                    drug_enc_id.put(JJ.get(i).PatMed, w );

                                }

                                else {

                                    if (JJ.get(i).Timing ==1) {
                                        drug_enc_int.put(JJ.get(i).PatMed, JJ.get(i).med_started_dt_tm);
                                        drug_enc_fin.put(JJ.get(i).PatMed, JJ.get(i).med_stopped_dt_tm);
                                    }
                                    else {
                                        drug_enc_int.put(JJ.get(i).PatMed, JJ.get(i).med_started_dt_tm);
                                        drug_enc_fin.put(JJ.get(i).PatMed, JJ.get(i).med_started_dt_tm);
                                        }

                                    List dr = new ArrayList<>();

                                    dr.add(i);
                                    drug_enc_id.put(JJ.get(i).PatMed, dr);



                                }

                              //  SearchResult.PDiag pd = DIJ.get(i);

                            }


                            for (String key : drug_enc_int.keySet()) {


                                List<Integer> u = drug_enc_id.get(key);


                                Task t = new Task ( key , drug_enc_int.get(key)  , drug_enc_fin.get(key) );



                                for ( int j =0;  j<u.size() ; j++  )  {

                                    if (JJ.get(u.get(j)).Timing ==1 ) {
                                        Task sub_t = new Task(JJ.get(u.get(j)).PatMed, JJ.get(u.get(j)).med_started_dt_tm, JJ.get(u.get(j)).med_stopped_dt_tm);
                                        t.addSubtask(sub_t);  }
                                    else {
                                        Task sub_t = new Task(JJ.get(u.get(j)).PatMed, JJ.get(u.get(j)).med_started_dt_tm, JJ.get(u.get(j)).med_started_dt_tm);
                                        t.addSubtask(sub_t);
                                    }


                                }

                                series1.add(t);

                            }


                            TaskSeriesCollection dataset = new TaskSeriesCollection();
                            dataset.add(series1);



//                                SearchResult.PMedication pm = JJ.get(i);
//                                if (pm.Timing==1) {
//                                    Task t = new Task(pm.PatMed, pm.med_started_dt_tm, pm.med_stopped_dt_tm);
//                                    series1.add(t);
//                                }
//                                else {Task t = new Task(pm.PatMed, pm.med_started_dt_tm, pm.med_started_dt_tm);
//                                    series1.add(t);}
//                            }
//
//                            TaskSeriesCollection dataset = new TaskSeriesCollection();
//
//
//                            dataset.add(series1);

                            return dataset;

                        }


                        public void main() {
                            SwingUtilities.invokeLater(() -> {
                                DrugGa drugga = new DrugGa("Medications");
                                //	diagga.setSize(1000, 800);
                                drugga.setLocationRelativeTo(null);
                                drugga.setVisible(true);
                                drugga.pack();


                            });
                        }

                    }
                    DrugGa GatDrug = new DrugGa(null); GatDrug.main();
                } else {view.displayDialog(" No record was found!" , "Medications");}


                break;






            case Encounters_Medication:

                DetailPanel M_detailPanel=null;
                if (e.getSource() instanceof DetailPanel) {
                    M_detailPanel = (DetailPanel)e.getSource();
                } else if (e.getSource() instanceof JButton) {
                    JButton nextButton = (JButton)e.getSource();
                    Component parent = nextButton.getParent();
                    while (parent != null && (!(parent instanceof DetailPanel))) {
                        parent = parent.getParent();
                    }
                    if (parent instanceof DetailPanel) {
                        M_detailPanel = (DetailPanel)parent;
                    }
                } else {
                    LOG.error("Unhandled source for NEXTPAGE event: " + e.getSource());
                }


                final DetailPanel M_DetailPanel = M_detailPanel;

                List<SearchResult.PMedication> E_drugNcs;
                List<SearchResult.PMedication> E_JJ;

                if (!M_DetailPanel.getShowAllDocs()) {

                    try {
                        view.setState(ViewState.WAIT);
                        String drugComponentName = ((Component) e.getSource()).getName();
                        E_drugNcs = null;
                        try {
                            E_drugNcs = search.matching_encounter_drugs(drugComponentName);
                        } catch (SQLException e1) {
                            view.error(e1.getMessage());
                            LOG.error(e1.getMessage());
                            e1.printStackTrace();
                        }
                    } finally {
                        view.setState(ViewState.NORMAL);
                    }
                } else {
                    try {
                        view.setState(ViewState.WAIT);
                        String drugComponentName = ((Component) e.getSource()).getName();
                        E_drugNcs = null;
                        try {
                            E_drugNcs = search.all_encounter_drugs(drugComponentName);
                        } catch (SQLException e1) {
                            view.error(e1.getMessage());
                            LOG.error(e1.getMessage());
                            e1.printStackTrace();
                        }
                    } finally {
                        view.setState(ViewState.NORMAL);
                    }
                }


                int E_b = E_drugNcs.size();
                E_JJ= E_drugNcs;

                System.out.println(E_b);

                if (E_b >0) {

                    class E_DrugGa extends JFrame {

                        private static final long serialVersionUID = 1L;


                        public E_DrugGa(String title) {
                            super(title);

                            IntervalCategoryDataset dataset4 =  getCategoryDataset();
                            JFreeChart chart = ChartFactory.createGanttChart(
                                    "Medications of the Encounter",
                                    "Medications",
                                    "Timeline",
                                    dataset4, false, true, true
                            );

                            ChartPanel panel = new ChartPanel(chart);
                            setContentPane(panel);

                            CategoryPlot plot = chart.getCategoryPlot();
                            chart.setBackgroundPaint(new Color(234, 234, 234, 107));
                            plot.setBackgroundPaint(new Color(10, 32, 254, 0));

                            GanttRenderer renderer2 = new GanttRenderer();



                            renderer2.setBaseToolTipGenerator(new IntervalCategoryToolTipGenerator("{1} |  {2}  -  {4}",DateFormat.getDateInstance()));

                            renderer2.setMaximumBarWidth(0.05);

                            plot.setRenderer(renderer2);

                        }


                        public IntervalCategoryDataset getCategoryDataset() {
                            TaskSeries series1 = new TaskSeries("");

                            for (int i = 0; i < E_b; i++) {
                                SearchResult.PMedication E_pm = E_JJ.get(i);
                                if (E_pm.Timing==1) {
                                    Task t = new Task(E_pm.PatMed, E_pm.med_started_dt_tm, E_pm.med_stopped_dt_tm);
                                    series1.add(t);
                                }
                                else {Task t = new Task(E_pm.PatMed, E_pm.med_started_dt_tm, E_pm.med_started_dt_tm);
                                    series1.add(t);}
                            }

                            TaskSeriesCollection dataset4 = new TaskSeriesCollection();


                            dataset4.add(series1);

                            return dataset4;

                        }


                        public void main() {
                            SwingUtilities.invokeLater(() -> {
                                E_DrugGa E_drugga = new E_DrugGa("Medications of the Encounter");
                                //	diagga.setSize(1000, 800);
                                E_drugga.setLocationRelativeTo(null);
                                E_drugga.setVisible(true);
                                E_drugga.pack();


                            });
                        }

                    }
                    E_DrugGa E_GatDrug = new E_DrugGa(null); E_GatDrug.main();

                } else {view.displayDialog(" No record was found!" , "Medications of the Encounter");}


                break;














            case DISPLAYLABRESULTS:

                DetailPanel LR_detailPanel=null;  // detailPanel
                if (e.getSource() instanceof DetailPanel) {
                    LR_detailPanel = (DetailPanel)e.getSource();
                } else if (e.getSource() instanceof JButton) {
                    JButton nextButton = (JButton)e.getSource();
                    Component parent = nextButton.getParent();
                    while (parent != null && (!(parent instanceof DetailPanel))) {
                        parent = parent.getParent();
                    }
                    if (parent instanceof DetailPanel) {
                        LR_detailPanel = (DetailPanel)parent;
                    }
                } else {
                    LOG.error("Unhandled source for NEXTPAGE event: " + e.getSource());
                }


                final DetailPanel LRDetailPanel = LR_detailPanel;
                List<SearchResult.PLResult> procCncs;
                List<SearchResult.PLResult> MJ;

                if (!LRDetailPanel.getShowAllDocs()) {


                    try {
                        view.setState(ViewState.WAIT);
                        String procComponentName = ((Component) e.getSource()).getName();
                        procCncs = null;
                        try {
                            procCncs = search.getLabResult(procComponentName);
                        } catch (SQLException e1) {
                            view.error(e1.getMessage());
                            LOG.error(e1.getMessage());
                            e1.printStackTrace();
                        }
                    } finally {
                        view.setState(ViewState.NORMAL);
                    }
                }


                else {

                    try {
                        view.setState(ViewState.WAIT);
                        String procComponentName = ((Component) e.getSource()).getName();
                        procCncs = null;
                        try {
                            procCncs = search.getLabResult_ALL_Matches(procComponentName);
                        } catch (SQLException e1) {
                            view.error(e1.getMessage());
                            LOG.error(e1.getMessage());
                            e1.printStackTrace();
                        }
                    } finally {
                        view.setState(ViewState.NORMAL);
                    }

                }

                MJ = procCncs;
                int bl= MJ.size();

                if( bl >0) {
                    class Lab_Result extends JFrame {

                        public Lab_Result(String title) {
                            super(title);
                            JPanel chartPanel = mypanel();
                            //	chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
                            setContentPane(chartPanel);
                        }



                        HashMap<Integer, Integer> col_map = new HashMap<>();
                        LinkedHashMap<java.sql.Date, Integer> color_Liver = new LinkedHashMap<>();
                        LinkedHashMap<java.sql.Date, Integer> color_Thyroid = new LinkedHashMap<>();
                        LinkedHashMap<java.sql.Date, Integer> color_Lipids = new LinkedHashMap<>();
                        LinkedHashMap<java.sql.Date, Integer> color_Urine = new LinkedHashMap<>();
                        LinkedHashMap<java.sql.Date, Integer> color_Basic = new LinkedHashMap<>();
                        LinkedHashMap<java.sql.Date, Integer> color_Cbc = new LinkedHashMap<>();


                        private DefaultCategoryDataset  createDataset() {
                            DefaultCategoryDataset dataset = new DefaultCategoryDataset();


                            HashMap<java.sql.Date, StringBuilder> Liver_Map = new HashMap<>();
                            HashMap<java.sql.Date, StringBuilder> Lipids_Map = new HashMap<>();
                            HashMap<java.sql.Date, StringBuilder> Urin_Map = new HashMap<>();
                            HashMap<java.sql.Date, StringBuilder> Basic7_Map = new HashMap<>();
                            HashMap<java.sql.Date, StringBuilder> CBC_Map = new HashMap<>();
                            HashMap<java.sql.Date, StringBuilder> Thyroid_Map = new HashMap<>();

                            int Liver =0, Lipids =0, Urine=0, Basic =0, Cbc=0,Thyroid=0  ;
                            int series_number=0;


                            for (int j=0; j<bl; j++) {





                                SearchResult.PLResult LG = MJ.get(j);

                                if (MJ.get(j).Group.equals("Bilirubin Test")  || MJ.get(j).Group.equals("ALT Test")  || MJ.get(j).Group.equals("Alk Phos Test")||
                                MJ.get(j).Group.equals("GGT Test")
                                        || MJ.get(j).Group.equals("Albumin Test")  ||MJ.get(j).Group.equals("Protein Test")||
                                MJ.get(j).PLR.contains("AST") || MJ.get(j).PLR.contains("ACT") ) {
                                    Liver =1;
                                    if (Liver_Map.containsKey(MJ.get(j).lab_drawn_dt_tm)) {
                                        Liver_Map.put(MJ.get(j).lab_drawn_dt_tm, Liver_Map.get(MJ.get(j).lab_drawn_dt_tm).append("<br>"+  LG.PLR + " &nbsp "+ ":"+ " &nbsp " + LG.numeric_result ));

                                        if (MJ.get(j).Status==1) { color_Liver.put(MJ.get(j).lab_drawn_dt_tm, 1);}
                                        else if ( MJ.get(j).Status==0 && color_Liver.get(MJ.get(j).lab_drawn_dt_tm) != 1) { color_Liver.put(MJ.get(j).lab_drawn_dt_tm, 0);}
                                        else if ( color_Liver.get(MJ.get(j).lab_drawn_dt_tm) == 2 && MJ.get(j).Status==2) { color_Liver.put(MJ.get(j).lab_drawn_dt_tm,2);}


                                    } else {

                                        String Mo =  LG.PLR + " &nbsp "+ ":"+ " &nbsp " + LG.numeric_result ;

                                        Liver_Map.put(MJ.get(j).lab_drawn_dt_tm, new StringBuilder(Mo));

                                        color_Liver.put(MJ.get(j).lab_drawn_dt_tm, MJ.get(j).Status);

                                    }
                                }



                                else if (MJ.get(j).Group.equals("Thyroid Test")) {
                                    Thyroid=1;
                                    if (Thyroid_Map.containsKey(MJ.get(j).lab_drawn_dt_tm)) {
                                        Thyroid_Map.put(MJ.get(j).lab_drawn_dt_tm, Thyroid_Map.get(MJ.get(j).lab_drawn_dt_tm).append("<br>"  + LG.PLR + " &nbsp "+ ":"+ " &nbsp " + LG.numeric_result ));

                                        if (MJ.get(j).Status==1) { color_Thyroid.put(MJ.get(j).lab_drawn_dt_tm, 1);}
                                        else if ( MJ.get(j).Status==0 && color_Thyroid.get(MJ.get(j).lab_drawn_dt_tm) != 1) { color_Thyroid.put(MJ.get(j).lab_drawn_dt_tm, 0);}
                                        else if ( color_Thyroid.get(MJ.get(j).lab_drawn_dt_tm) == 2 && MJ.get(j).Status==2) { color_Thyroid.put(MJ.get(j).lab_drawn_dt_tm,2);}


                                    } else {

                                        String TT =  LG.PLR + " &nbsp "+ ":"+ " &nbsp " + LG.numeric_result ;

                                        Thyroid_Map.put(MJ.get(j).lab_drawn_dt_tm, new StringBuilder(TT));
                                        color_Thyroid.put(MJ.get(j).lab_drawn_dt_tm, MJ.get(j).Status);

                                    }

                                }



                                else if (MJ.get(j).Group.equals("Cholesterol Test") || MJ.get(j).Group.equals("Lipids Test") || MJ.get(j).Group.equals("Triglyceride Test") ) {
                                    Lipids=1;
                                    if (Lipids_Map.containsKey(MJ.get(j).lab_drawn_dt_tm)) {
                                        Lipids_Map.put(MJ.get(j).lab_drawn_dt_tm, Lipids_Map.get(MJ.get(j).lab_drawn_dt_tm).append("<br>" + LG.PLR + " &nbsp "+ ":"+ " &nbsp " + LG.numeric_result ));
                                        if (MJ.get(j).Status==1) { color_Lipids.put(MJ.get(j).lab_drawn_dt_tm, 1);}
                                        else if ( MJ.get(j).Status==0  && color_Lipids.get(MJ.get(j).lab_drawn_dt_tm) != 1) { color_Lipids.put(MJ.get(j).lab_drawn_dt_tm, 0);}
                                        else if ( color_Lipids.get(MJ.get(j).lab_drawn_dt_tm) == 2 && MJ.get(j).Status==2) { color_Lipids.put(MJ.get(j).lab_drawn_dt_tm,2);}

                                    } else {

                                        String TT =  LG.PLR + " &nbsp "+ ":"+ " &nbsp " + LG.numeric_result;

                                        Lipids_Map.put(MJ.get(j).lab_drawn_dt_tm, new StringBuilder(TT));
                                        color_Lipids.put(MJ.get(j).lab_drawn_dt_tm, MJ.get(j).Status);

                                    }

                                }


                                else if (MJ.get(j).Group.equals("Urinalysis Test")) {
                                    Urine=1;
                                    if (Urin_Map.containsKey(MJ.get(j).lab_drawn_dt_tm)) {
                                        Urin_Map.put(MJ.get(j).lab_drawn_dt_tm, Urin_Map.get(MJ.get(j).lab_drawn_dt_tm).append("<br>" + LG.PLR + " &nbsp "+ ":"+ " &nbsp " + LG.numeric_result ));

                                        if (MJ.get(j).Status==1) { color_Urine.put(MJ.get(j).lab_drawn_dt_tm, 1);}
                                        else if ( MJ.get(j).Status==0 && color_Urine.get(MJ.get(j).lab_drawn_dt_tm) != 1) { color_Urine.put(MJ.get(j).lab_drawn_dt_tm, 0);}
                                        else if ( color_Urine.get(MJ.get(j).lab_drawn_dt_tm) == 2 && MJ.get(j).Status==2) { color_Urine.put(MJ.get(j).lab_drawn_dt_tm,2);}


                                    } else {

                                        String UT =  LG.PLR + " &nbsp "+ ":"+ " &nbsp " + LG.numeric_result ;

                                        Urin_Map.put(MJ.get(j).lab_drawn_dt_tm, new StringBuilder(UT));
                                        color_Urine.put(MJ.get(j).lab_drawn_dt_tm, MJ.get(j).Status);

                                    }
                                }


                                else if ( MJ.get(j).Group.equals("CBC Test") || MJ.get(j).Group.equals("Diff, CBC") ||MJ.get(j).PLR.contains("Hct")  ||  MJ.get(j).PLR.contains("CBC") || MJ.get(j).PLR.contains("Hemoglobin")
                                        || MJ.get(j).PLR.contains("RDW") || MJ.get(j).PLR.contains("Mono") || MJ.get(j).PLR.contains("Lymph") || MJ.get(j).PLR.contains("Eosinophil")
                                        || MJ.get(j).PLR.contains("Baso") || MJ.get(j).PLR.contains("MCV") || MJ.get(j).PLR.contains("MCHC") || MJ.get(j).PLR.contains("Platelet")
                                        || MJ.get(j).PLR.contains("Hgb") || MJ.get(j).PLR.contains("RBC")  ||  MJ.get(j).PLR.contains("WBC")

                                )
                                {
                                    Cbc=1;
                                    if (CBC_Map.containsKey(MJ.get(j).lab_drawn_dt_tm)) {
                                        CBC_Map.put(MJ.get(j).lab_drawn_dt_tm, CBC_Map.get(MJ.get(j).lab_drawn_dt_tm).append("<br>" +  LG.PLR + " &nbsp "+ ":"+ " &nbsp " + LG.numeric_result ));

                                        if (MJ.get(j).Status==1) { color_Cbc.put(MJ.get(j).lab_drawn_dt_tm, 1);}
                                        else if ( MJ.get(j).Status==0 && color_Cbc.get(MJ.get(j).lab_drawn_dt_tm) != 1) { color_Cbc.put(MJ.get(j).lab_drawn_dt_tm, 0);}
                                        else if ( color_Cbc.get(MJ.get(j).lab_drawn_dt_tm) == 2 && MJ.get(j).Status==2) { color_Cbc.put(MJ.get(j).lab_drawn_dt_tm,2);}


                                    }
                                    else {

                                        String CbT =  LG.PLR + " &nbsp "+ ":"+ " &nbsp " + LG.numeric_result ;


                                        CBC_Map.put(MJ.get(j).lab_drawn_dt_tm, new StringBuilder(CbT));
                                        color_Cbc.put(MJ.get(j).lab_drawn_dt_tm, MJ.get(j).Status);

                                    }
                                }



                                else if (MJ.get(j).Group.equals("Sodium Test") || MJ.get(j).Group.equals("Potassium Test") || MJ.get(j).Group.equals("Chloride Test")
                                        || MJ.get(j).Group.equals("Creatinine Test") || MJ.get(j).Group.equals("Glucose Test") || MJ.get(j).Group.equals("BUN Test")
                                        || MJ.get(j).PLR.contains("CO2") ||MJ.get(j).PLR.contains("Anion Gap") ){
                                    Basic=1;
                                    if (Basic7_Map.containsKey(MJ.get(j).lab_drawn_dt_tm)) {
                                        Basic7_Map.put(MJ.get(j).lab_drawn_dt_tm, Basic7_Map.get(MJ.get(j).lab_drawn_dt_tm).append("<br>" +  LG.PLR + " &nbsp "+ ":"+ " &nbsp " + LG.numeric_result ));

                                        if (MJ.get(j).Status==1) { color_Basic.put(MJ.get(j).lab_drawn_dt_tm, 1);}
                                        else if ( MJ.get(j).Status==0 && color_Basic.get(MJ.get(j).lab_drawn_dt_tm) != 1) { color_Basic.put(MJ.get(j).lab_drawn_dt_tm, 0);}
                                        else if ( color_Basic.get(MJ.get(j).lab_drawn_dt_tm) == 2 && MJ.get(j).Status==2) { color_Basic.put(MJ.get(j).lab_drawn_dt_tm,2);}



                                    } else {

                                        String C7 = LG.PLR + " &nbsp "+ ":"+ " &nbsp " + LG.numeric_result ;
                                        Basic7_Map.put(MJ.get(j).lab_drawn_dt_tm, new StringBuilder(C7));
                                        color_Basic.put(MJ.get(j).lab_drawn_dt_tm, MJ.get(j).Status);

                                    }
                                }


                                else {

                                    if (!col_map.containsValue(0)) {
                                        series_number = 0;
                                        dataset.setValue(new Long(new Day(MJ.get(j).lab_drawn_dt_tm).getMiddleMillisecond()),
                                                MJ.get(j).PLR + " &nbsp " + ":" + " &nbsp " + MJ.get(j).numeric_result , MJ.get(j).PLR);

                                    }


                                    else if (MJ.get(j).PLR.equals(MJ.get(j - 1).PLR) && MJ.get(j).lab_drawn_dt_tm.equals(MJ.get(j - 1).lab_drawn_dt_tm))  {
                                        series_number++;
                                        dataset.setValue(new Long(new Day(MJ.get(j).lab_drawn_dt_tm).getMiddleMillisecond()),
                                                MJ.get(j-1).PLR + " &nbsp " + ":" + " &nbsp " + MJ.get(j-1).numeric_result +
                                                        "<br>" + MJ.get(j).PLR + " &nbsp " + ":" + " &nbsp " + MJ.get(j).numeric_result
                                                , MJ.get(j).PLR); }

                                    else {
                                        series_number++;
                                        dataset.setValue(new Long(new Day(MJ.get(j).lab_drawn_dt_tm).getMiddleMillisecond()),
                                                MJ.get(j).PLR + " &nbsp " + ":" + " &nbsp " + MJ.get(j).numeric_result
                                                , MJ.get(j).PLR);
                                    }
                                    col_map.put(j,series_number);



                                }

                            }




                            if(Liver>0){
                                for (java.sql.Date key : Liver_Map.keySet()) {

                                    java.sql.Date a = key;
                                    Day aa = new Day(a);
                                    String b = Liver_Map.get(key).toString();
                                    dataset.setValue(new Long(aa.getMiddleMillisecond()), b, "Liver");

                                }
                            }



                            if (Thyroid >0) {
                                for (java.sql.Date key : Thyroid_Map.keySet()) {

                                    java.sql.Date Ta = key;
                                    Day Taa = new Day(Ta);
                                    String Tb = Thyroid_Map.get(key).toString();
                                    dataset.setValue(new Long(Taa.getMiddleMillisecond()), Tb, "Thyroid Test");
                                }
                            }


                            if (Lipids >0) {
                                for (java.sql.Date key : Lipids_Map.keySet()) {

                                    java.sql.Date Ca = key;
                                    Day Caa = new Day(Ca);
                                    String Cb = Lipids_Map.get(key).toString();
                                    dataset.setValue(new Long(Caa.getMiddleMillisecond()), Cb, "LIPIDS");
                                }
                            }


                            if (Urine >0) {
                                for (java.sql.Date key : Urin_Map.keySet()) {

                                    java.sql.Date Ua = key;
                                    Day Uaa = new Day(Ua);
                                    String Ub = Urin_Map.get(key).toString();
                                    dataset.setValue(new Long(Uaa.getMiddleMillisecond()), Ub, "Urine Analysis");
                                }
                            }



                            if (Basic >0) {
                                for (java.sql.Date key : Basic7_Map.keySet()) {

                                    java.sql.Date C7_b = key;
                                    Day c_a7 = new Day(C7_b);
                                    String cc7_b = Basic7_Map.get(key).toString();
                                    dataset.setValue(new Long(c_a7.getMiddleMillisecond()), cc7_b, "Basic 7");
                                }
                            }



                            if (Cbc >0) {
                                for (java.sql.Date key : CBC_Map.keySet()) {


                                    Day c_a = new Day(key);
                                    String cc_b = CBC_Map.get(key).toString();
                                    dataset.setValue(new Long(c_a.getMiddleMillisecond()), cc_b, "CBC Tests");
                                }
                            }

                            return dataset;


                        }


                        private  JFreeChart createChart(DefaultCategoryDataset dataset) {

                            JFreeChart chart = ChartFactory.createBarChart(
                                    "",      // title
                                    "Lab Results",
                                    "Drawn Date",
                                    dataset,
                                    PlotOrientation.HORIZONTAL,
                                    false,
                                    true,
                                    false
                            );

                            chart.setBackgroundPaint(new Color(0, 134, 236, 22));

                            CategoryPlot plot = chart.getCategoryPlot();

                            plot.setBackgroundPaint(Color.WHITE);

                            CategoryItemRenderer renderer = new LineAndShapeRenderer(false, true);


                            int mk = bl;
                            SearchResult.PLResult CS = null;



                            if (col_map.size() > 0) {


                                for (Integer key : col_map.keySet()) {

                                    if (MJ.get(key).Status == 1) {
                                        renderer.setSeriesShape(col_map.get(key), ShapeUtilities.createDownTriangle(5.01f));
                                        renderer.setSeriesPaint(col_map.get(key), new Color(255, 68, 0, 219));
                                    } else if (MJ.get(key).Status == 2) {
                                        renderer.setSeriesShape(col_map.get(key), ShapeUtilities.createUpTriangle(6.01f));
                                        renderer.setSeriesPaint(col_map.get(key), new Color(12, 216, 0, 255));
                                    } else {
                                        renderer.setSeriesShape(col_map.get(key), ShapeUtilities.createDiamond(6.39f));
                                        renderer.setSeriesPaint(col_map.get(key), new Color(92, 92, 92, 151));
                                    }


                                }
                            }


//                            for ( int i = col_map.size(); i<mk; i++) {
//                                renderer.setSeriesShape(i, ShapeUtilities.createDiamond(6.39f));
//                                renderer.setSeriesPaint(i, new Color(92, 92, 92, 151));
//
//                            }


                            int Liver_size = color_Liver.size();
                            if(Liver_size>0) {
                                int indxL=0;
                                List<java.sql.Date> IndxLiv= new ArrayList(color_Liver.keySet());


                                for (java.sql.Date key : color_Liver.keySet()) {
                                    indxL = IndxLiv.indexOf(key);
                                    if (color_Liver.get(key).equals(1)) {
                                        renderer.setSeriesShape(col_map.size()+indxL, ShapeUtilities.createDownTriangle(5.01f));
                                        renderer.setSeriesPaint(col_map.size() +indxL, new Color(255, 68, 0, 219));
                                    } else if (color_Liver.get(key).equals(0)) {
                                        renderer.setSeriesShape(col_map.size()+indxL, ShapeUtilities.createDiamond(6.39f));
                                        renderer.setSeriesPaint(col_map.size()+indxL, new Color(92, 92, 92, 151));
                                    } else if (color_Liver.get(key).equals(2)) {
                                        renderer.setSeriesShape(col_map.size()+indxL, ShapeUtilities.createUpTriangle(6.01f));
                                        renderer.setSeriesPaint(col_map.size()+indxL, new Color(12, 216, 0, 255));
                                    }
                                }

                            }



                            int Thyroid_size = color_Thyroid.size();
                            int indxTh=0;

                            if(Thyroid_size>0) {

                                List<java.sql.Date> IndxThy= new ArrayList(color_Thyroid.keySet());

                                for (java.sql.Date key : color_Thyroid.keySet()) {
                                    indxTh = IndxThy.indexOf(key);
                                    if (color_Thyroid.get(key).equals(1)) {
                                        renderer.setSeriesShape(col_map.size()+indxTh+Liver_size, ShapeUtilities.createDownTriangle(5.01f));
                                        renderer.setSeriesPaint(col_map.size() +indxTh+Liver_size, new Color(255, 68, 0, 219));
                                    } else if (color_Thyroid.get(key).equals(0)) {
                                        renderer.setSeriesShape(col_map.size()+indxTh +Liver_size, ShapeUtilities.createDiamond(6.39f));
                                        renderer.setSeriesPaint(col_map.size()+indxTh +Liver_size, new Color(92, 92, 92, 151));
                                    } else if (color_Thyroid.get(key).equals(2)) {
                                        renderer.setSeriesShape(col_map.size()+indxTh+Liver_size, ShapeUtilities.createUpTriangle(6.01f));
                                        renderer.setSeriesPaint(col_map.size()+indxTh +Liver_size, new Color(12, 216, 0, 255));
                                    }
                                }
                            }



                            int Lipids_size = color_Lipids.size();
                            int indxLip=0;
                            if(Lipids_size>0) {

                                List<java.sql.Date> IndxLipids= new ArrayList(color_Lipids.keySet());

                                for (java.sql.Date key : color_Lipids.keySet()) {
                                    indxLip = IndxLipids.indexOf(key);
                                    if (color_Lipids.get(key).equals(1)) {
                                        renderer.setSeriesShape(col_map.size()+indxLip+Liver_size+Thyroid_size, ShapeUtilities.createDownTriangle(5.01f));
                                        renderer.setSeriesPaint(col_map.size() +indxLip+Liver_size+Thyroid_size, new Color(255, 68, 0, 219));
                                    } else if (color_Lipids.get(key).equals(0)) {
                                        renderer.setSeriesShape(col_map.size()+indxLip +Liver_size+Thyroid_size, ShapeUtilities.createDiamond(6.39f));
                                        renderer.setSeriesPaint(col_map.size()+indxLip +Liver_size+Thyroid_size, new Color(92, 92, 92, 151));
                                    } else if (color_Lipids.get(key).equals(2)) {
                                        renderer.setSeriesShape(col_map.size()+indxLip+Liver_size+Thyroid_size, ShapeUtilities.createUpTriangle(6.01f));
                                        renderer.setSeriesPaint(col_map.size()+indxLip +Liver_size+Thyroid_size, new Color(12, 216, 0, 255));
                                    }
                                }
                            }


                            int indxUr=0;
                            int Urine_size= color_Urine.size();
                            if(Urine_size>0) {
                                List<java.sql.Date> IndxUrine= new ArrayList(color_Urine.keySet());

                                for (java.sql.Date key : color_Urine.keySet()) {
                                    indxUr = IndxUrine.indexOf(key);
                                    if (color_Urine.get(key).equals(1)) {
                                        renderer.setSeriesShape(col_map.size()+indxUr+Liver_size+Thyroid_size+Lipids_size, ShapeUtilities.createDownTriangle(5.01f));
                                        renderer.setSeriesPaint(col_map.size() +indxUr+Liver_size+Thyroid_size+Lipids_size, new Color(255, 68, 0, 219));
                                    } else if (color_Urine.get(key).equals(0)) {
                                        renderer.setSeriesShape(col_map.size()+indxUr +Liver_size+Thyroid_size+Lipids_size, ShapeUtilities.createDiamond(6.39f));
                                        renderer.setSeriesPaint(col_map.size()+indxUr +Liver_size+Thyroid_size+Lipids_size, new Color(92, 92, 92, 151));
                                    } else if (color_Urine.get(key).equals(2)) {
                                        renderer.setSeriesShape(col_map.size()+indxUr+Liver_size+Thyroid_size+Lipids_size, ShapeUtilities.createUpTriangle(6.01f));
                                        renderer.setSeriesPaint(col_map.size()+indxUr +Liver_size+Thyroid_size+Lipids_size, new Color(12, 216, 0, 255));
                                    }
                                }

                            }

                            int indxBsc=0;
                            int Basic_size= color_Basic.size();
                            if(color_Basic.size()>0) {
                                List<java.sql.Date> IndxBasic= new ArrayList(color_Basic.keySet());

                                for (java.sql.Date key : color_Basic.keySet()) {
                                    indxBsc = IndxBasic.indexOf(key);
                                    if (color_Basic.get(key).equals(1)) {
                                        renderer.setSeriesShape(col_map.size()+indxBsc+Liver_size+Thyroid_size+Lipids_size+Urine_size, ShapeUtilities.createDownTriangle(5.01f));
                                        renderer.setSeriesPaint(col_map.size() +indxBsc+Liver_size+Thyroid_size+Lipids_size+Urine_size, new Color(255, 68, 0, 219));
                                    } else if (color_Basic.get(key).equals(0)) {
                                        renderer.setSeriesShape(col_map.size()+indxBsc +Liver_size+Thyroid_size+Lipids_size+Urine_size, ShapeUtilities.createDiamond(6.39f));
                                        renderer.setSeriesPaint(col_map.size()+indxBsc +Liver_size+Thyroid_size+Lipids_size+Urine_size, new Color(92, 92, 92, 151));
                                    } else if (color_Basic.get(key).equals(2)) {
                                        renderer.setSeriesShape(col_map.size()+indxBsc+Liver_size+Thyroid_size+Lipids_size+Urine_size, ShapeUtilities.createUpTriangle(6.01f));
                                        renderer.setSeriesPaint(col_map.size()+indxBsc +Liver_size+Thyroid_size+Lipids_size+Urine_size, new Color(12, 216, 0, 255));
                                    }
                                }

                            }

                            int indxcbc=0;
                            int CBC_size= color_Cbc.size();
                            if(CBC_size>0) {
                                List<java.sql.Date> IndxCBC= new ArrayList(color_Cbc.keySet());

                                for (java.sql.Date key : color_Cbc.keySet()) {
                                    indxcbc = IndxCBC.indexOf(key);
                                    if (color_Cbc.get(key).equals(1)) {
                                        renderer.setSeriesShape(col_map.size()+indxcbc+Liver_size+Thyroid_size+Lipids_size+Urine_size +Basic_size, ShapeUtilities.createDownTriangle(5.01f));
                                        renderer.setSeriesPaint(col_map.size() +indxcbc+Liver_size+Thyroid_size+Lipids_size+Urine_size+Basic_size, new Color(255, 68, 0, 219));
                                    } else if (color_Cbc.get(key).equals(0)) {
                                        renderer.setSeriesShape(col_map.size()+indxcbc +Liver_size+Thyroid_size+Lipids_size+Urine_size+Basic_size, ShapeUtilities.createDiamond(6.39f));
                                        renderer.setSeriesPaint(col_map.size()+indxcbc +Liver_size+Thyroid_size+Lipids_size+Urine_size+Basic_size, new Color(92, 92, 92, 151));
                                    } else if (color_Cbc.get(key).equals(2)) {
                                        renderer.setSeriesShape(col_map.size()+indxcbc+Liver_size+Thyroid_size+Lipids_size+Urine_size+Basic_size, ShapeUtilities.createUpTriangle(6.01f));
                                        renderer.setSeriesPaint(col_map.size()+indxcbc +Liver_size+Thyroid_size+Lipids_size+Urine_size+Basic_size, new Color(12, 216, 0, 255));
                                    }
                                }

                            }



                            //	plot.setRangePannable(true);

                            plot.getDomainAxis().setMaximumCategoryLabelWidthRatio(10.0f);

                            plot.getDomainAxis().setUpperMargin(0.01);
                            plot.getDomainAxis().setLowerMargin(0.009);
                            plot.setRangeAxis(new DateAxis("Drawn date"));

                            CategoryAxis axis1 = plot.getDomainAxis();
                            //axis1.set
                            axis1.setLabelFont(new Font("SansSerif", 1, 13));


                            ValueAxis axis2 = plot.getRangeAxis();
                            axis2.setLabelFont(new Font("SansSerif", 1, 13));


                            CategoryToolTipGenerator toolTipGenerator = new StandardCategoryToolTipGenerator(
                                    "<html> <body style=\"background-color:white;\">" + "<p> <font color = \"#202020\" face = " + " WildWest " + " size = "+" 4 "+" >" +
                                            "{0}" +"</font></p>"+ "<br>" + "<br>" + "{2}" + " </body> </html>", DateFormat.getDateInstance());

                            toolTipGenerator.generateToolTip(createDataset(), 1, 0);


                            renderer.setBaseToolTipGenerator(toolTipGenerator);


                            plot.setRenderer(renderer);


                            //	ChartUtilities.applyCurrentTheme(chart);


                            return chart;
                        }



                        public  JPanel mypanel() {
                            JFreeChart chart = createChart(createDataset());
                            return new ChartPanel(chart);

                        }

                    }


                    Lab_Result Lr = new Lab_Result("Lab results");
                    Lr.pack();
                    RefineryUtilities.centerFrameOnScreen(Lr);
                    Lr.setVisible(true);

                } else {view.displayDialog(" No record was found!" , "Lab Results");}


                break;






            case Encounters_Lab:

                DetailPanel ME_detailPanel=null;
                if (e.getSource() instanceof DetailPanel) {
                    ME_detailPanel = (DetailPanel)e.getSource();
                } else if (e.getSource() instanceof JButton) {
                    JButton nextButton = (JButton)e.getSource();
                    Component parent = nextButton.getParent();
                    while (parent != null && (!(parent instanceof DetailPanel))) {
                        parent = parent.getParent();
                    }
                    if (parent instanceof DetailPanel) {
                        ME_detailPanel = (DetailPanel)parent;
                    }
                } else {
                    LOG.error("Unhandled source for NEXTPAGE event: " + e.getSource());
                }


                final DetailPanel ME_DetailPanel = ME_detailPanel;

                List<SearchResult.PLResult> E_labNcs;
                List<SearchResult.PLResult> E_E;

                if (!ME_DetailPanel.getShowAllDocs()) {

                    try {
                        view.setState(ViewState.WAIT);
                        String drugComponentName = ((Component) e.getSource()).getName();
                        E_labNcs = null;
                        try {
                            E_labNcs = search.encounter_lab(drugComponentName);
                        } catch (SQLException e1) {
                            view.error(e1.getMessage());
                            LOG.error(e1.getMessage());
                            e1.printStackTrace();
                        }
                    } finally {
                        view.setState(ViewState.NORMAL);
                    }
                } else {
                    try {
                        view.setState(ViewState.WAIT);
                        String drugComponentName = ((Component) e.getSource()).getName();
                        E_labNcs = null;
                        try {
                            E_labNcs = search.all_encounter_lab(drugComponentName);
                        } catch (SQLException e1) {
                            view.error(e1.getMessage());
                            LOG.error(e1.getMessage());
                            e1.printStackTrace();
                        }
                    } finally {
                        view.setState(ViewState.NORMAL);
                    }
                }


                int E_L = E_labNcs.size();
                E_E= E_labNcs;



                if (E_L >0) {


                    class E_Lab_Result extends JFrame {

                        public E_Lab_Result(String title) {
                            super(title);
                            JPanel chartPanel = mypanel();
                            //	chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
                            setContentPane(chartPanel);
                        }


                        HashMap<Integer, Integer> color_map = new HashMap<>();
                        LinkedHashMap<java.sql.Date, Integer> color_Liver = new LinkedHashMap<>();
                        LinkedHashMap<java.sql.Date, Integer> color_Thyroid = new LinkedHashMap<>();
                        LinkedHashMap<java.sql.Date, Integer> color_Lipids = new LinkedHashMap<>();
                        LinkedHashMap<java.sql.Date, Integer> color_Urine = new LinkedHashMap<>();
                        LinkedHashMap<java.sql.Date, Integer> color_Basic = new LinkedHashMap<>();
                        LinkedHashMap<java.sql.Date, Integer> color_Cbc = new LinkedHashMap<>();



                        private DefaultCategoryDataset  createDataset() {
                            DefaultCategoryDataset dataset5 = new DefaultCategoryDataset();

                            List<SearchResult.MyList> values1 = new ArrayList<SearchResult.MyList>();


                            List<SearchResult.MyList> sj = new ArrayList<SearchResult.MyList>();

                            HashMap<java.sql.Date, StringBuilder> Liver_Map_E = new HashMap<>();
                            HashMap<java.sql.Date, StringBuilder> Lipids_Map_E = new HashMap<>();
                            HashMap<java.sql.Date, StringBuilder> Urin_Map_E = new HashMap<>();
                            HashMap<java.sql.Date, StringBuilder> Basic7_Map_E = new HashMap<>();
                            HashMap<java.sql.Date, StringBuilder> CBC_Map_E = new HashMap<>();
                            HashMap<java.sql.Date, StringBuilder> Thyroid_Map_E = new HashMap<>();

                            int Liver_E = 0, Lipids_E = 0, Urine_E = 0, Basic_E = 0, Cbc_E = 0, Thyroid_E = 0;

                            int series_num=0;

                            for (int j = 0; j < E_L; j++) {

                                System.out.print(j + "   "  + E_E.get(j).PLR +  "++++++");

                                if (E_E.get(j).Group.equals("Bilirubin Test") || E_E.get(j).PLR.contains("AST")
                                        || E_E.get(j).Group.equals("Alk Phos Test") || E_E.get(j).PLR.contains("ACT")
                                        || E_E.get(j).Group.equals("Protein Test") ||
                                             E_E.get(j).Group.equals("ALT Test") || E_E.get(j).Group.equals("GGT Test")
                                        || E_E.get(j).Group.equals("Albumin Test")) {
                                    Liver_E = 1;

                                    if (Liver_Map_E.containsKey(E_E.get(j).lab_drawn_dt_tm)) {
                                        Liver_Map_E.put(E_E.get(j).lab_drawn_dt_tm, Liver_Map_E.get(E_E.get(j).lab_drawn_dt_tm).append("<br>" + E_E.get(j).PLR + " &nbsp " + ":" + " &nbsp " + E_E.get(j).numeric_result));

                                        if (E_E.get(j).Status==1) { color_Liver.put(E_E.get(j).lab_drawn_dt_tm, 1);}
                                         else if ( E_E.get(j).Status==0 && color_Liver.get(E_E.get(j).lab_drawn_dt_tm) != 1 ) { color_Liver.put(E_E.get(j).lab_drawn_dt_tm, 0);}
                                         else if ( color_Liver.get(E_E.get(j).lab_drawn_dt_tm) == 2 && E_E.get(j).Status==2) { color_Liver.put(E_E.get(j).lab_drawn_dt_tm,2);}

                                    } else {

                                        String Mo = E_E.get(j).PLR + " &nbsp " + ":" + " &nbsp " + E_E.get(j).numeric_result;

                                        Liver_Map_E.put(E_E.get(j).lab_drawn_dt_tm, new StringBuilder(Mo));
                                        color_Liver.put(E_E.get(j).lab_drawn_dt_tm, E_E.get(j).Status);

                                    }
                                }

                                else if (E_E.get(j).Group.equals("Thyroid Test")) {

                                    Thyroid_E = 1;
                                    if (Thyroid_Map_E.containsKey(E_E.get(j).lab_drawn_dt_tm)) {
                                        Thyroid_Map_E.put(E_E.get(j).lab_drawn_dt_tm, Thyroid_Map_E.get(E_E.get(j).lab_drawn_dt_tm).append("<br>" + E_E.get(j).PLR + " &nbsp " + ":" + " &nbsp " + E_E.get(j).numeric_result));

                                        if (E_E.get(j).Status==1) { color_Thyroid.put(E_E.get(j).lab_drawn_dt_tm, 1);}
                                        else if ( E_E.get(j).Status==0 && color_Thyroid.get(E_E.get(j).lab_drawn_dt_tm) != 1 ) { color_Thyroid.put(E_E.get(j).lab_drawn_dt_tm, 0);}
                                        else if ( color_Thyroid.get(E_E.get(j).lab_drawn_dt_tm) == 2 && E_E.get(j).Status==2) { color_Thyroid.put(E_E.get(j).lab_drawn_dt_tm,2);}

                                    } else {

                                        String TT = E_E.get(j).PLR + " &nbsp " + ":" + " &nbsp " + E_E.get(j).numeric_result;

                                        Thyroid_Map_E.put(E_E.get(j).lab_drawn_dt_tm, new StringBuilder(TT));
                                        color_Thyroid.put(E_E.get(j).lab_drawn_dt_tm, E_E.get(j).Status);
                                    }

                                }


                                else if (E_E.get(j).Group.equals("Cholesterol Test") || E_E.get(j).Group.equals("Lipids Test") || E_E.get(j).Group.equals("Triglyceride Test") ) {

                                    Lipids_E = 1;
                                    if (Lipids_Map_E.containsKey(E_E.get(j).lab_drawn_dt_tm)) {
                                        Lipids_Map_E.put(E_E.get(j).lab_drawn_dt_tm, Lipids_Map_E.get(E_E.get(j).lab_drawn_dt_tm).append("<br>" + E_E.get(j).PLR + " &nbsp " + ":" + " &nbsp " + E_E.get(j).numeric_result));

                                        if (E_E.get(j).Status==1) { color_Lipids.put(E_E.get(j).lab_drawn_dt_tm, 1);}
                                        else if ( E_E.get(j).Status==0 && color_Lipids.get(E_E.get(j).lab_drawn_dt_tm) != 1) { color_Lipids.put(E_E.get(j).lab_drawn_dt_tm, 0);}
                                        else if ( color_Lipids.get(E_E.get(j).lab_drawn_dt_tm) == 2 && E_E.get(j).Status==2) { color_Lipids.put(E_E.get(j).lab_drawn_dt_tm,2);}

                                    } else {

                                        String TT = E_E.get(j).PLR + " &nbsp " + ":" + " &nbsp " + E_E.get(j).numeric_result;

                                        Lipids_Map_E.put(E_E.get(j).lab_drawn_dt_tm, new StringBuilder(TT));
                                        color_Lipids.put(E_E.get(j).lab_drawn_dt_tm, E_E.get(j).Status);
                                    }

                                }

                                else if (E_E.get(j).Group.equals("Urinalysis Test")) {
                                    Urine_E = 1;
                                    if (Urin_Map_E.containsKey(E_E.get(j).lab_drawn_dt_tm)) {
                                        Urin_Map_E.put(E_E.get(j).lab_drawn_dt_tm, Urin_Map_E.get(E_E.get(j).lab_drawn_dt_tm).append("<br>" + E_E.get(j).PLR + " &nbsp " + ":" + " &nbsp " + E_E.get(j).numeric_result));

                                        if (E_E.get(j).Status==1) { color_Urine.put(E_E.get(j).lab_drawn_dt_tm, 1);}
                                        else if ( E_E.get(j).Status==0  && color_Urine.get(E_E.get(j).lab_drawn_dt_tm) != 1 ) { color_Urine.put(E_E.get(j).lab_drawn_dt_tm, 0);}
                                        else if ( color_Urine.get(E_E.get(j).lab_drawn_dt_tm) == 2 && E_E.get(j).Status==2) { color_Urine.put(E_E.get(j).lab_drawn_dt_tm,2);}


                                    } else {

                                        String UT = E_E.get(j).PLR + " &nbsp " + ":" + " &nbsp " + E_E.get(j).numeric_result;

                                        Urin_Map_E.put(E_E.get(j).lab_drawn_dt_tm, new StringBuilder(UT));

                                        color_Urine.put(E_E.get(j).lab_drawn_dt_tm, E_E.get(j).Status);
                                    }
                                }


                                else if (E_E.get(j).Group.equals("CBC Test") || E_E.get(j).Group.equals("Diff, CBC") || E_E.get(j).PLR.contains("Hct") || E_E.get(j).PLR.contains("CBC") || E_E.get(j).PLR.contains("Hemoglobin")
                                        || E_E.get(j).PLR.contains("RDW") || E_E.get(j).PLR.contains("Mono") || E_E.get(j).PLR.contains("Lymph") || E_E.get(j).PLR.contains("Eosinophil")
                                        || E_E.get(j).PLR.contains("Baso") || E_E.get(j).PLR.contains("MCV") || E_E.get(j).PLR.contains("MCHC") || E_E.get(j).PLR.contains("Platelet")
                                        || E_E.get(j).PLR.contains("Hgb") || E_E.get(j).PLR.contains("RBC") || E_E.get(j).PLR.contains("WBC")

                                ) {

                                    Cbc_E = 1;
                                    if (CBC_Map_E.containsKey(E_E.get(j).lab_drawn_dt_tm)) {
                                        CBC_Map_E.put(E_E.get(j).lab_drawn_dt_tm, CBC_Map_E.get(E_E.get(j).lab_drawn_dt_tm).append("<br>" + E_E.get(j).PLR + " &nbsp " + ":" + " &nbsp " + E_E.get(j).numeric_result));

                                        if (E_E.get(j).Status==1) { color_Cbc.put(E_E.get(j).lab_drawn_dt_tm, 1);}
                                        else if ( E_E.get(j).Status==0 && color_Cbc.get(E_E.get(j).lab_drawn_dt_tm) != 1 ) { color_Cbc.put(E_E.get(j).lab_drawn_dt_tm, 0);}
                                        else if ( color_Cbc.get(E_E.get(j).lab_drawn_dt_tm) == 2 && E_E.get(j).Status==2) { color_Cbc.put(E_E.get(j).lab_drawn_dt_tm,2);}


                                    } else {

                                        String CbT = E_E.get(j).PLR + " &nbsp " + ":" + " &nbsp " + E_E.get(j).numeric_result;

                                        CBC_Map_E.put(E_E.get(j).lab_drawn_dt_tm, new StringBuilder(CbT));

                                        color_Cbc.put(E_E.get(j).lab_drawn_dt_tm, E_E.get(j).Status);

                                    }

                                } else if (E_E.get(j).Group.equals("Sodium Test") || E_E.get(j).Group.equals("Potassium Test") || E_E.get(j).Group.equals("Chloride Test")
                                        || E_E.get(j).Group.equals("Creatinine Test") || E_E.get(j).Group.equals("Glucose Test") || E_E.get(j).Group.equals("BUN Test")
                                        || E_E.get(j).PLR.contains("CO2") || E_E.get(j).PLR.contains("Anion Gap")) {
                                    Basic_E = 1;


                                    if (Basic7_Map_E.containsKey(E_E.get(j).lab_drawn_dt_tm)) {
                                        Basic7_Map_E.put(E_E.get(j).lab_drawn_dt_tm, Basic7_Map_E.get(E_E.get(j).lab_drawn_dt_tm).append("<br>" + E_E.get(j).PLR + " &nbsp " + ":" + " &nbsp " + E_E.get(j).numeric_result));

                                        if (E_E.get(j).Status==1) { color_Basic.put(E_E.get(j).lab_drawn_dt_tm, 1);}
                                        else if ( E_E.get(j).Status==0 && color_Basic.get(E_E.get(j).lab_drawn_dt_tm) != 1 ) { color_Basic.put(E_E.get(j).lab_drawn_dt_tm, 0);}
                                        else if ( color_Basic.get(E_E.get(j).lab_drawn_dt_tm) == 2 && E_E.get(j).Status==2) { color_Basic.put(E_E.get(j).lab_drawn_dt_tm,2);}

                                    } else {

                                        String C7 = E_E.get(j).PLR + " &nbsp " + ":" + " &nbsp " + E_E.get(j).numeric_result;
                                        Basic7_Map_E.put(E_E.get(j).lab_drawn_dt_tm, new StringBuilder(C7));

                                        color_Basic.put(E_E.get(j).lab_drawn_dt_tm, E_E.get(j).Status);

                                    }
                                }


                                else {


                                    if (!color_map.containsValue(0)) {
                                        series_num = 0;
                                        dataset5.setValue(new Long(new Day(E_E.get(j).lab_drawn_dt_tm).getMiddleMillisecond()),
                                                E_E.get(j).PLR + " &nbsp " + ":" + " &nbsp " + E_E.get(j).numeric_result , E_E.get(j).PLR);
                                    }


                                    else if (E_E.get(j).PLR.equals(E_E.get(j - 1).PLR) && E_E.get(j).lab_drawn_dt_tm.equals(E_E.get(j - 1).lab_drawn_dt_tm))  {

                                        System.err.println(" " + E_E.get(j).PLR + "~ " + j + " ~" + E_E.get(j-1).PLR);

                                        series_num++;
                                        dataset5.setValue(new Long(new Day(E_E.get(j).lab_drawn_dt_tm).getMiddleMillisecond()),
                                                E_E.get(j).PLR + " &nbsp " + ":" + " &nbsp " + E_E.get(j-1).numeric_result +
                                                        "<br>" + E_E.get(j).PLR + " &nbsp " + ":" + " &nbsp " + E_E.get(j).numeric_result, E_E.get(j).PLR); }


                                    else {
                                        series_num++;
                                        dataset5.setValue(new Long(new Day(E_E.get(j).lab_drawn_dt_tm).getMiddleMillisecond()),
                                                E_E.get(j).PLR + " &nbsp " + ":" + " &nbsp " + E_E.get(j).numeric_result + " <!-- " + j + " --> " , E_E.get(j).PLR);
                                    }
                                    color_map.put(j,series_num);

                                }

                            }



                            if(Liver_E>0){
                                for (java.sql.Date key : Liver_Map_E.keySet()) {

                                    java.sql.Date a = key;
                                    Day aa = new Day(a);

                                    dataset5.setValue(new Long(aa.getMiddleMillisecond()), Liver_Map_E.get(key).toString(), "Liver");

                                }
                            }



                            if (Thyroid_E >0) {
                                for (java.sql.Date key : Thyroid_Map_E.keySet()) {

                                    Day Taa = new Day(key);
                                    dataset5.setValue(new Long(Taa.getMiddleMillisecond()), Thyroid_Map_E.get(key).toString(), "Thyroid Test");
                                }
                            }


                            if (Lipids_E >0) {
                                for (java.sql.Date key : Lipids_Map_E.keySet()) {
                                    Day Caa = new Day(key);
                                    dataset5.setValue(new Long(Caa.getMiddleMillisecond()), Lipids_Map_E.get(key).toString(), "LIPIDS");
                                }
                            }



                            if (Urine_E >0) {
                                for (java.sql.Date key : Urin_Map_E.keySet()) {
                                    Day Uaa = new Day(key);
                                    dataset5.setValue(new Long(Uaa.getMiddleMillisecond()), Urin_Map_E.get(key).toString(), "Urine Analysis");
                                }
                            }



                            if (Basic_E >0) {
                                for (java.sql.Date key : Basic7_Map_E.keySet()) {

                                    Day c_a7 = new Day(key);
                                    dataset5.setValue(new Long(c_a7.getMiddleMillisecond()), Basic7_Map_E.get(key).toString(), "Basic 7");
                                }
                            }


                            if (Cbc_E >0) {
                                for (java.sql.Date key : CBC_Map_E.keySet()) {
                                    Day c_a = new Day(key);
                                    dataset5.setValue(new Long(c_a.getMiddleMillisecond()), CBC_Map_E.get(key).toString(), "CBC Tests");
                                }
                            }

                            return dataset5;
                        }




                        private  JFreeChart createChart(DefaultCategoryDataset dataset5) {

                            JFreeChart chart = ChartFactory.createBarChart(
                                    "",      // title
                                    "Lab Results",
                                    "Drawn Date",
                                    dataset5,
                                    PlotOrientation.HORIZONTAL,
                                    false,
                                    true,
                                    false
                            );

                            chart.setBackgroundPaint(new Color(92, 255, 196, 13));

                            CategoryPlot plot = chart.getCategoryPlot();

                            plot.setBackgroundPaint(Color.WHITE);

                            CategoryItemRenderer renderer = new LineAndShapeRenderer(false,true);

                            int EE_L = E_L;

                            if (color_map.size() > 0) {


                                for (Integer key : color_map.keySet()) {

                                    if (E_E.get(key).Status == 1) {
                                        renderer.setSeriesShape(color_map.get(key), ShapeUtilities.createDownTriangle(5.01f));
                                        renderer.setSeriesPaint(color_map.get(key), new Color(255, 68, 0, 219));
                                    } else if (E_E.get(key).Status == 2) {
                                        renderer.setSeriesShape(color_map.get(key), ShapeUtilities.createUpTriangle(6.01f));
                                        renderer.setSeriesPaint(color_map.get(key), new Color(12, 216, 0, 255));
                                    } else {
                                        renderer.setSeriesShape(color_map.get(key), ShapeUtilities.createDiamond(6.39f));
                                        renderer.setSeriesPaint(color_map.get(key), new Color(92, 92, 92, 151));
                                    }

                                }
                            }



                            int Liver_size = color_Liver.size();
                            if(Liver_size>0) {
                                int indxL=0;
                                List<java.sql.Date> IndxLiv= new ArrayList(color_Liver.keySet());


                                for (java.sql.Date key : color_Liver.keySet()) {
                                    indxL = IndxLiv.indexOf(key);
                                    if (color_Liver.get(key).equals(1)) {
                                        renderer.setSeriesShape(color_map.size()+indxL, ShapeUtilities.createDownTriangle(5.01f));
                                        renderer.setSeriesPaint(color_map.size() +indxL, new Color(255, 68, 0, 219));
                                    } else if (color_Liver.get(key).equals(0)) {
                                        renderer.setSeriesShape(color_map.size()+indxL, ShapeUtilities.createDiamond(6.39f));
                                        renderer.setSeriesPaint(color_map.size()+indxL, new Color(92, 92, 92, 151));
                                    } else if (color_Liver.get(key).equals(2)) {
                                        renderer.setSeriesShape(color_map.size()+indxL, ShapeUtilities.createUpTriangle(6.01f));
                                        renderer.setSeriesPaint(color_map.size()+indxL, new Color(12, 216, 0, 255));
                                    }
                                }

                            }

                            int Thyroid_size = color_Thyroid.size();
                            int indxTh=0;

                            if(Thyroid_size>0) {

                                List<java.sql.Date> IndxThy= new ArrayList(color_Thyroid.keySet());

                                for (java.sql.Date key : color_Thyroid.keySet()) {
                                    indxTh = IndxThy.indexOf(key);
                                    if (color_Thyroid.get(key).equals(1)) {
                                        renderer.setSeriesShape(color_map.size()+indxTh+Liver_size, ShapeUtilities.createDownTriangle(5.01f));
                                        renderer.setSeriesPaint(color_map.size() +indxTh+Liver_size, new Color(255, 68, 0, 219));
                                    } else if (color_Thyroid.get(key).equals(0)) {
                                        renderer.setSeriesShape(color_map.size()+indxTh +Liver_size, ShapeUtilities.createDiamond(6.39f));
                                        renderer.setSeriesPaint(color_map.size()+indxTh +Liver_size, new Color(92, 92, 92, 151));
                                    } else if (color_Thyroid.get(key).equals(2)) {
                                        renderer.setSeriesShape(color_map.size()+indxTh+Liver_size, ShapeUtilities.createUpTriangle(6.01f));
                                        renderer.setSeriesPaint(color_map.size()+indxTh +Liver_size, new Color(12, 216, 0, 255));
                                    }
                                }
                            }


                            int Lipids_size = color_Lipids.size();
                            int indxLip=0;
                            if(Lipids_size>0) {

                                List<java.sql.Date> IndxLipids= new ArrayList(color_Lipids.keySet());

                                for (java.sql.Date key : color_Lipids.keySet()) {
                                    indxLip = IndxLipids.indexOf(key);
                                    if (color_Lipids.get(key).equals(1)) {
                                        renderer.setSeriesShape(color_map.size()+indxLip+Liver_size+Thyroid_size, ShapeUtilities.createDownTriangle(5.01f));
                                        renderer.setSeriesPaint(color_map.size() +indxLip+Liver_size+Thyroid_size, new Color(255, 68, 0, 219));
                                    } else if (color_Lipids.get(key).equals(0)) {
                                        renderer.setSeriesShape(color_map.size()+indxLip +Liver_size+Thyroid_size, ShapeUtilities.createDiamond(6.39f));
                                        renderer.setSeriesPaint(color_map.size()+indxLip +Liver_size+Thyroid_size, new Color(92, 92, 92, 151));
                                    } else if (color_Lipids.get(key).equals(2)) {
                                        renderer.setSeriesShape(color_map.size()+indxLip+Liver_size+Thyroid_size, ShapeUtilities.createUpTriangle(6.01f));
                                        renderer.setSeriesPaint(color_map.size()+indxLip +Liver_size+Thyroid_size, new Color(12, 216, 0, 255));
                                    }
                                }
                            }


                            int indxUr=0;
                            int Urine_size= color_Urine.size();
                            if(Urine_size>0) {
                                List<java.sql.Date> IndxUrine= new ArrayList(color_Urine.keySet());

                                for (java.sql.Date key : color_Urine.keySet()) {
                                    indxUr = IndxUrine.indexOf(key);
                                    if (color_Urine.get(key).equals(1)) {
                                        renderer.setSeriesShape(color_map.size()+indxUr+Liver_size+Thyroid_size+Lipids_size, ShapeUtilities.createDownTriangle(5.01f));
                                        renderer.setSeriesPaint(color_map.size() +indxUr+Liver_size+Thyroid_size+Lipids_size, new Color(255, 68, 0, 219));
                                    } else if (color_Urine.get(key).equals(0)) {
                                        renderer.setSeriesShape(color_map.size()+indxUr +Liver_size+Thyroid_size+Lipids_size, ShapeUtilities.createDiamond(6.39f));
                                        renderer.setSeriesPaint(color_map.size()+indxUr +Liver_size+Thyroid_size+Lipids_size, new Color(92, 92, 92, 151));
                                    } else if (color_Urine.get(key).equals(2)) {
                                        renderer.setSeriesShape(color_map.size()+indxUr+Liver_size+Thyroid_size+Lipids_size, ShapeUtilities.createUpTriangle(6.01f));
                                        renderer.setSeriesPaint(color_map.size()+indxUr +Liver_size+Thyroid_size+Lipids_size, new Color(12, 216, 0, 255));
                                    }
                                }

                            }



                            int indxBsc=0;
                            int Basic_size= color_Basic.size();
                            if(Basic_size>0) {
                                List<java.sql.Date> IndxBasic= new ArrayList(color_Basic.keySet());

                                for (java.sql.Date key : color_Basic.keySet()) {
                                    indxBsc = IndxBasic.indexOf(key);
                                    if (color_Basic.get(key).equals(1)) {
                                        renderer.setSeriesShape(color_map.size()+indxBsc+Liver_size+Thyroid_size+Lipids_size+Urine_size, ShapeUtilities.createDownTriangle(5.01f));
                                        renderer.setSeriesPaint(color_map.size() +indxBsc+Liver_size+Thyroid_size+Lipids_size+Urine_size, new Color(255, 68, 0, 219));
                                    } else if (color_Basic.get(key).equals(0)) {
                                        renderer.setSeriesShape(color_map.size()+indxBsc +Liver_size+Thyroid_size+Lipids_size+Urine_size, ShapeUtilities.createDiamond(6.39f));
                                        renderer.setSeriesPaint(color_map.size()+indxBsc +Liver_size+Thyroid_size+Lipids_size+Urine_size, new Color(92, 92, 92, 151));
                                    } else if (color_Basic.get(key).equals(2)) {
                                        renderer.setSeriesShape(color_map.size()+indxBsc+Liver_size+Thyroid_size+Lipids_size+Urine_size, ShapeUtilities.createUpTriangle(6.01f));
                                        renderer.setSeriesPaint(color_map.size()+indxBsc +Liver_size+Thyroid_size+Lipids_size+Urine_size, new Color(12, 216, 0, 255));
                                    }
                                }

                            }

                            int indxcbc=0;
                            int CBC_size= color_Cbc.size();
                            if(CBC_size>0) {
                                List<java.sql.Date> IndxCBC= new ArrayList(color_Cbc.keySet());

                                for (java.sql.Date key : color_Cbc.keySet()) {
                                    indxcbc = IndxCBC.indexOf(key);
                                    if (color_Cbc.get(key).equals(1)) {
                                        renderer.setSeriesShape(color_map.size()+indxcbc+Liver_size+Thyroid_size+Lipids_size+Urine_size +Basic_size, ShapeUtilities.createDownTriangle(5.01f));
                                        renderer.setSeriesPaint(color_map.size() +indxcbc+Liver_size+Thyroid_size+Lipids_size+Urine_size+Basic_size, new Color(255, 68, 0, 219));
                                    } else if (color_Cbc.get(key).equals(0)) {
                                        renderer.setSeriesShape(color_map.size()+indxcbc +Liver_size+Thyroid_size+Lipids_size+Urine_size+Basic_size, ShapeUtilities.createDiamond(6.39f));
                                        renderer.setSeriesPaint(color_map.size()+indxcbc +Liver_size+Thyroid_size+Lipids_size+Urine_size+Basic_size, new Color(92, 92, 92, 151));
                                    } else if (color_Cbc.get(key).equals(2)) {
                                        renderer.setSeriesShape(color_map.size()+indxcbc+Liver_size+Thyroid_size+Lipids_size+Urine_size+Basic_size, ShapeUtilities.createUpTriangle(6.01f));
                                        renderer.setSeriesPaint(color_map.size()+indxcbc +Liver_size+Thyroid_size+Lipids_size+Urine_size+Basic_size, new Color(12, 216, 0, 255));
                                    }
                                }

                            }





                            //	plot.setRangePannable(true);

                            plot.getDomainAxis().setMaximumCategoryLabelWidthRatio(10.0f);

                            plot.getDomainAxis().setUpperMargin(0.01);
                            plot.getDomainAxis().setLowerMargin(0.009);
                            plot.setRangeAxis(new DateAxis("Drawn date"));

                            CategoryAxis axis1 = plot.getDomainAxis();
                            //axis1.set
                            axis1.setLabelFont(new Font("SansSerif", 1 ,13));


                            ValueAxis axis2 = plot.getRangeAxis();
                            axis2.setLabelFont(new Font("SansSerif", 1 ,13));


                            CategoryToolTipGenerator toolTipGenerator = new StandardCategoryToolTipGenerator(
                                    "<html> <body style=\"background-color:white;\">" + "<p> <font color = \"#202020\" face = " + " WildWest " + " size = "+" 4 "+" >" +
                                            "{0}" +"</font></p>"+ "<br>" + "<br>" + "{2}" + " </body> </html>", DateFormat.getDateInstance());


                            renderer.setBaseToolTipGenerator(toolTipGenerator);


                            plot.setRenderer(renderer);



                            //	ChartUtilities.applyCurrentTheme(chart);


                            return chart;
                        }



                        public  JPanel mypanel() {
                            JFreeChart chart = createChart(createDataset());
                            return new ChartPanel(chart);


                        }


                    }


                    E_Lab_Result E_Lr = new E_Lab_Result("Lab Results for the Encounter");
                    E_Lr.pack();
                    RefineryUtilities.centerFrameOnScreen(E_Lr);
                    E_Lr.setVisible(true);


                }
                else {view.displayDialog(" No record was found!" , "Lab Results for the Encounter ");}


                break;






            case Encounters_Condition:

                DetailPanel C_detailPanel=null;
                if (e.getSource() instanceof DetailPanel) {
                    C_detailPanel = (DetailPanel)e.getSource();
                } else if (e.getSource() instanceof JButton) {
                    JButton nextButton = (JButton)e.getSource();
                    Component parent = nextButton.getParent();
                    while (parent != null && (!(parent instanceof DetailPanel))) {
                        parent = parent.getParent();
                    }
                    if (parent instanceof DetailPanel) {
                        C_detailPanel = (DetailPanel)parent;
                    }
                } else {
                    LOG.error("Unhandled source for NEXTPAGE event: " + e.getSource());
                }


                final DetailPanel C_DetailPanel = C_detailPanel;

                List<SearchResult.PDiag> diagCncs_E;
                List<SearchResult.PDiag> E_C;

                if (!C_DetailPanel.getShowAllDocs()) {
                    try {
                        view.setState(ViewState.WAIT);
                        String diagComponentName = ((Component) e.getSource()).getName();
                        diagCncs_E = null;

                        try {

                            diagCncs_E = search.matching_encounter_condition(diagComponentName);

                        } catch (SQLException e1) {
                            view.error(e1.getMessage());
                            LOG.error(e1.getMessage());
                            e1.printStackTrace();
                        }
                    } finally {
                        view.setState(ViewState.NORMAL);
                    }
                } else {
                    try {
                        view.setState(ViewState.WAIT);
                        String diagComponentName = ((Component) e.getSource()).getName();
                        diagCncs_E = null;

                        try {

                            diagCncs_E = search.all_encounter_condition(diagComponentName);

                        } catch (SQLException e1) {
                            view.error(e1.getMessage());
                            LOG.error(e1.getMessage());
                            e1.printStackTrace();
                        }
                    } finally {
                        view.setState(ViewState.NORMAL);
                    }

                }

                E_C= diagCncs_E;
                int E_dbj = E_C.size();

                if (E_dbj >0) {
                    class E_DiagGa extends JFrame  {

                        private static final long serialVersionUID = 1L;

                        public E_DiagGa(String title) {
                            super(title);

                            IntervalCategoryDataset dataset =  getCategoryDataset();
                            JFreeChart chart = ChartFactory.createGanttChart(

                                    "Comorbid Conditions of the Encounter ",
                                    "Conditions",
                                    "Timeline",
                                    dataset, false, true, true
                            );

                            ChartPanel panel = new ChartPanel(chart);

                            setContentPane(panel);

                            CategoryPlot plot = chart.getCategoryPlot();
                            chart.setBackgroundPaint(new Color(234, 234, 234, 75));
                            plot.setBackgroundPaint(new Color(246, 255, 246, 82));

                            GanttRenderer renderer2 = new GanttRenderer();

                            renderer2.setBaseToolTipGenerator(new IntervalCategoryToolTipGenerator("{1} |  {2}  -  {4}",DateFormat.getDateInstance()));

                            renderer2.setMaximumBarWidth(0.05);

                            plot.setRenderer(renderer2);
                        }

                        private IntervalCategoryDataset getCategoryDataset() {
                            TaskSeries series1 = new TaskSeries("");

                            for ( int i=0 ; i<E_dbj; i++) {
                                SearchResult.PDiag pd = E_C.get(i);
                                Task t = new Task(pd.Di ,pd.date_adm,pd.date_dis);
                                series1.add(t);
                            }

                            TaskSeriesCollection dataset = new TaskSeriesCollection();
                            dataset.add(series1);

                            return dataset;
                        }


                        public  void main() {
                            SwingUtilities.invokeLater(() -> {
                                E_DiagGa E_diagga = new E_DiagGa("Comorbid Conditions of the Encounter");
                                //	diagga.setSize(1000, 800);
                                E_diagga.setLocationRelativeTo(null);
                                E_diagga.setVisible(true);
                                E_diagga.pack();

                            });
                        }
                    }

                    E_DiagGa E_GatDiag = new E_DiagGa(null);	 E_GatDiag.main();





                }
                else {view.displayDialog(" No record was found!" , "Comorbid Conditions of the Encounter");}

                break;




            case TopMedications:

                try {
                    CategoryListPanel drugPanel = (CategoryListPanel)e.getSource();
                    drugPanel.addValues(search.getMedicationDistribution());
                } catch (SQLException  | IOException se) {
                    LOG.error(se.getMessage());
                    view.displayDialog(" An error was found ", "Top Medications");

                }
                break;



            case TopDiagnosis:

                try {
                    CategoryListPanel icd9Panel = (CategoryListPanel)e.getSource();
                    icd9Panel.addValues(search.getDxDistribution(25));
                } catch (SQLException  | IOException | InterruptedException se) {
                    LOG.error(se.getMessage());
                    view.displayDialog(" An error was found ", "Top Comorbid Conditions");

                }
                break;









//			case GETDIAGNOSES:
//				try {
//					search.getMatchingDiagnoses();
//				} catch (SQLException se) {
//					LOG.error(se.getMessage());
//					view.displayDialog(" Exeption when getting matching diagnoses", "Matching Diagnoses");
//				}
//				break;




            case NEXTPAGE:
                //DetailPanel detailPanel = null;
                detailPanel = null;
                if (e.getSource() instanceof DetailPanel) {
                    detailPanel = (DetailPanel)e.getSource();
                } else if (e.getSource() instanceof JButton) {
                    JButton nextButton = (JButton)e.getSource();
                    Component parent = nextButton.getParent();
                    while (parent != null && (!(parent instanceof DetailPanel))) {
                        parent = parent.getParent();
                    }
                    if (parent instanceof DetailPanel) {
                        detailPanel = (DetailPanel)parent;
                    }
                } else {
                    LOG.error("Unhandled source for NEXTPAGE event: " + e.getSource());
                }
                if (detailPanel != null) {
                    // Get the summaryPanel
                    SummaryPanel summaryPanel = null;
                    Container parent = detailPanel.getParent();
                    if ((!(parent instanceof SummaryPanel)) && parent != null) {
                        parent = parent.getParent();
                    }
                    if (parent instanceof SummaryPanel) {
                        summaryPanel = (SummaryPanel)parent;
                    } else {
                        LOG.error("SummaryPanel not found");
                    }
                    final SummaryPanel fSummaryPanel = summaryPanel;
                    //		final DetailPanel fDetailPanel = detailPanel;
                    final DetailPanel fDetailPanel = detailPanel;
                    final ActionListener fAL = this;






                    detailPanelPatientThread = new Thread(new Runnable() {



                        // @Override
                        public void run() {

//							if (fDetailPanel.getShowAllDocs()) {
//								try {
//									search.getAllEncounters();
//								} catch (Exception e) {
//									throw new RuntimeException(e);
//								}
//							}


                            int page = fDetailPanel.getPage();
                            List<SearchResult.Patient> patients;
                            Map<String, List<SearchResult.Encounter>> docMap;
                            try {




                                ThreadUtils.runOnEDT(new Runnable() {
                                    @Override
                                    public void run() {
                                        fDetailPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                                    }
                                });

                                if (fDetailPanel.getShowAllDocs()) {
                                    try {
                                        search.getAllEncounters();
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                patients = search.getPatients(++page, DetailPanel.PAGESIZE);

                                if (patients != null) {
                                    docMap = new HashMap<String, List<SearchResult.Encounter>>(patients.size());
                                    List<SearchTerm> searchTerms = view.getSearchTerms();
                                    for (SearchResult.Patient patient : patients) {
                                        if (Thread.currentThread().isInterrupted()) {
                                            return;
                                        }
                                        if (!fDetailPanel.getShowAllDocs()) {
                                            docMap.put(patient.id, search.getMatchingEncounters(patient.id,  //encounters // write a new method for getting matching encounter instead of getMatchingDocuments
                                                    searchTerms));


                                        } else {
                                            docMap.put(patient.id,  search.getEncounters( patient.id) );
                                        }
                                    }
                                    StopWords stopWords = null;
                                    fDetailPanel.addResults(patients, docMap, searchTerms, fAL,
                                            true /*patients.size() == DetailPanel.PAGESIZE*/, stopWords);
                                    fDetailPanel.setPage(page);
                                }
                            } catch (Exception e1) {
                                view.error(e1.getMessage());
                                LOG.error(e1.getMessage());
                                e1.printStackTrace();
                            } finally {
                                ThreadUtils.runOnEDT(new Runnable() {
                                    @Override
                                    public void run() {
                                        fDetailPanel.setCursor(Cursor.getDefaultCursor());
                                        if (fSummaryPanel != null) {
                                            fSummaryPanel.setLoadingTab(
                                                    fDetailPanel, false);
                                            fSummaryPanel.setEnabledTab(
                                                    fDetailPanel, true);
                                        }
                                        fDetailPanel.validate();
                                        fDetailPanel.repaint();
                                    }
                                });
                            }
                        }
                    });





                    detailPanelPatientThread.setName("DetailPanelPatientThread");
                    detailPanelPatientThread.start();
                }
                break;






            case HELP:

                view.displayHelp();
                break;



            case FEEDBACK:
                try {
                    view.gatherFeedback();
                } catch (IOException e1) {
                    LOG.error(e1.getMessage());
                    view.error(e1.getMessage());
                }
                break;
            default:
                LOG.info("Unhandled command: " + command.toString());
                break;
        }

    }

    /* (non-Javadoc)
     * @see java.awt.event.WindowListener#windowOpened(java.awt.event.WindowEvent)
     */
    public void windowOpened(WindowEvent e) {
    }

    /* (non-Javadoc)
     * @see java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
     */
    public void windowClosing(WindowEvent e) {
        try {
            this.dispose();
        } catch (Exception e1) {
            LOG.error(e1.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see java.awt.event.WindowListener#windowClosed(java.awt.event.WindowEvent)
     */
    public void windowClosed(WindowEvent e) {
    }

    /* (non-Javadoc)
     * @see java.awt.event.WindowListener#windowIconified(java.awt.event.WindowEvent)
     */
    public void windowIconified(WindowEvent e) {
    }

    /* (non-Javadoc)
     * @see java.awt.event.WindowListener#windowDeiconified(java.awt.event.WindowEvent)
     */
    public void windowDeiconified(WindowEvent e) {
    }

    /* (non-Javadoc)
     * @see java.awt.event.WindowListener#windowActivated(java.awt.event.WindowEvent)
     */
    public void windowActivated(WindowEvent e) {
    }

    /* (non-Javadoc)
     * @see java.awt.event.WindowListener#windowDeactivated(java.awt.event.WindowEvent)
     */
    public void windowDeactivated(WindowEvent e) {
    }

    public void dispose() throws SQLException {
        view.dispose();
    }

}
