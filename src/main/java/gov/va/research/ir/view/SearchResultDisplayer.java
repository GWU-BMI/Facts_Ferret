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
package gov.va.research.ir.view;

import gov.va.research.ir.model.SaveFile;
import gov.va.research.ir.model.SearchTerm;
import gov.va.research.ir.model.SearchWorker;

import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;


/**
 * @author doug
 *
 */
public interface SearchResultDisplayer<T> {

	public void clearResults();
	public void searchBegun();
	public void searchComplete(final SearchWorker search, final List<SearchTerm> searchTerms);
	public void cancelSearch();
	public List<T> getRows();
	public void setVisible(boolean visible);
	public void addWindowListener(WindowListener l);
	public void displayDialog(final String message, final String title);
	public void setState(final ViewState state);
	public void error(String message);
	public void setStatus(String status);
	public void dispose();
	public SaveFile getFileForSaving();
	public void doQueryRecommendation(final Collection<String> terms);
	public void displayDialog(List<?> list, String title);
	public void setDataSetNames(Collection<String> dataSetNames);
	public String getDataSetName();
	public void saveBegun();
	public void saveComplete();
	public List<SearchTerm> getSearchTerms();
	void displayHelp();
	public void saveSummary(File file, SearchWorker search) throws IOException, SQLException;
	public void gatherFeedback() throws IOException;

	public default SummaryPanel getSummaryPanel() {
		return null;
	}
}
