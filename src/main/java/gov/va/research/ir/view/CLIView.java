package gov.va.research.ir.view;

import gov.va.research.ir.model.BoolOp;
import gov.va.research.ir.model.Command;
import gov.va.research.ir.model.DocSampleType;
import gov.va.research.ir.model.Field;
import gov.va.research.ir.model.SaveFile;
import gov.va.research.ir.model.SaveType;
import gov.va.research.ir.model.SearchTerm;
import gov.va.research.ir.model.SearchWorker;
import gov.va.research.ir.view.SearchPanel.SearchRow;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;


public class CLIView implements SearchResultDisplayer<SearchPanel.SearchRow> {

	public static final int TOP_CUTOFF = 25;
	public static final int NUM_RANDOM_DOCS = 100;
	private ActionListener actionListener;
	//private String query;
	private String dataSetName;
	public String outputFileName;
	private JFrame frame = new JFrame();
	private List<SearchTerm> searchTerms;

	public CLIView(final ActionListener actionListener, final String query, final String dataSetName, final String outputFileName) {
		this.actionListener = actionListener;
		// Fields separated by ;
		String[] fieldQueries = query.split(";");
		searchTerms = new ArrayList<>();
		for (String fieldQuery : fieldQueries) {
			String[] tokens = fieldQuery.split(":");
			String andOrNot = tokens[0].trim();
			String fieldName = tokens[1].trim();
			String fieldText = tokens[2].trim();
			if (fieldText.startsWith("'") && fieldText.endsWith("'")) {
				fieldText = fieldText.substring(1, fieldText.length() - 1);
			}
			tokens = fieldText.split("(?i)\\s+AND\\s+");
			Field field = Field.valueOf(fieldName);
			for (String token : tokens) {
				searchTerms.add(new SearchTerm(token, field, BoolOp.valueOf(andOrNot.toUpperCase())));
			}
		}
		this.dataSetName = dataSetName;
		this.outputFileName = outputFileName;
	}

	@Override
	public void clearResults() {
	}

	@Override
	public void searchBegun() {
	}

	@Override
	public void searchComplete(final SearchWorker search, final List<SearchTerm> searchTerms) {
		try{
			search.writeSearchSummary(new SaveFile(new File(this.outputFileName), SaveType.DETAILS, DocSampleType.TOPRANKPATIENT));
		} catch (IOException | InterruptedException
				| ExecutionException e) {
			throw new RuntimeException(e);
		}
		this.frame.setVisible(false);
		this.frame.dispose();
	}

	@Override
	public void cancelSearch() {
	}

	@Override
	public List<SearchRow> getRows() {
		return null;
	}

	@Override
	public void setVisible(boolean visible) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				frame.setVisible(true);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				actionListener.actionPerformed(new ActionEvent(actionListener, 0, Command.SEARCH.toString()));
			}
		});
	}

	@Override
	public void addWindowListener(WindowListener l) {
	}

	@Override
	public void displayDialog(String message, String title) {
	}

	@Override
	public void setState(ViewState state) {
	}

	@Override
	public void error(String message) {
	}

	@Override
	public void setStatus(String status) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public SaveFile getFileForSaving() {
		return null;
	}

	@Override
	public void doQueryRecommendation(Collection<String> terms) {
	}

	@Override
	public void displayDialog(List<?> list, String title) {
	}

	@Override
	public void setDataSetNames(Collection<String> dataSetNames) {
	}

	@Override
	public String getDataSetName() {
		return this.dataSetName;
	}

	@Override
	public void saveBegun() {
	}

	@Override
	public void saveComplete() {
	}

	/* (non-Javadoc)
	 * @see gov.va.research.ir.view.SearchResultDisplayer#getSearchTerms()
	 */
	@Override
	public List<SearchTerm> getSearchTerms() {
		return this.searchTerms;
	}

	/* (non-Javadoc)
	 * @see gov.va.research.ir.view.SearchResultDisplayer#displayHelp()
	 */
	@Override
	public void displayHelp() {
	}

	/* (non-Javadoc)
	 * @see gov.va.research.ir.view.SearchResultDisplayer#saveSummary(java.io.File)
	 */
	@Override
	public void saveSummary(File file, SearchWorker search) {
	}

	/* (non-Javadoc)
	 * @see gov.va.research.ir.view.SearchResultDisplayer#gatherFeedback()
	 */
	@Override
	public void gatherFeedback() {
	}
}
