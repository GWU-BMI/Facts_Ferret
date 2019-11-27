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
package gov.va.research.ir.model;

import gov.va.research.ir.model.SearchResult.Encounter;
import gov.va.research.ir.model.SearchResult.Patient;
import gov.va.research.ir.model.SearchResult.PDiag;
import gov.va.research.ir.model.SearchResult.PMedication;
import gov.va.research.ir.model.SearchResult.PLResult;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import gov.va.vinci.nlp.qeUtils.domain.TermWeight;
import org.apache.lucene.index.CorruptIndexException;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author vhaislreddd
 *
 */
public interface DAO  {

	public void setup() throws SQLException;
	/**
	 * @return the timeout in minutes for queries.
	 */
	public int getQueryTimeoutMinutes();

	/**
	 * @param minutes the number of minutes to allow a query to run before auto-canceling it.
	 */
	public void setQueryTimeoutMinutes(int minutes);

	/**
	 * Provides a name for the DAO that is suitable for display.
	 * @return A name for the DAO.
	 */
	public String getDisplayName();

	/**
	 * Sets a name for the DAO that is suitable for display.
	 * @param displayName The name for the DAO.
	 */
	public void setDisplayName(final String displayName);

	/**
	 * Checks if the data source contains sensitive data.
	 * @return <code>true</code> if the data may be sensitive, <code>false</code> otherwise.
	 */
	public boolean hasSensitiveData();

	/**
	 * Tests to see if the underlying data source is valid.
	 * @return <code>true</code> if the data source can be connected to, <code>false</code> otherwise.
	 */
	public boolean isValid() throws SQLException;


	public default void prepareQueryMatchingPatients(final List<SearchTerm> searchTerms) throws Exception {

	}
	/**
	 * Finds patients matching the search terms and saves them into a temporary table for future queries.
	 * @param searchTerms The terms to use for filtering the patients.
	 * @return The name of the temporary table where the results are stored.
	 * @throws Exception
	 */
	public default String queryMatchingPatients(final List<SearchTerm> searchTerms)
			throws Exception {
		return "";
	}






	public default String queryMatchingPatients(final List<SearchTerm> searchTerms,String year,boolean isFirst)
			throws Exception {
		return "";
	}
	public default void postQueryMatchingPatients(final List<SearchTerm> searchTerms) throws Exception {

	}
	/**
	 * Gets the number of patients that matched the previous call to <code>queryMatchingPatients</code>
	 * @return The number of matching patients.
	 * @throws SQLException
	 * @throws IOException
	 */
	public int getMatchingPatientCount()
			throws SQLException, IOException;

	/**
	 * Gets the number of documents that matched the previous call to <code>queryMatchingPatients</code>
	 * @return The number of matching documents.
	 * @throws SQLException
	 * @throws IOException
	 */
	public int getMatchingDocumentCount()
			throws SQLException, IOException;

	/**
	 * Gets the number of documents belonging to each document type, ordered by the document count, that matched the previous call to <code>queryMatchingPatients</code>.
	 * @param topCutoff The number of document types and their counts to return.
	 * @return A map of document types and their document counts, in descending order.
	 * @throws SQLException
	 * @throws IOException
	 */
	public Map<String, Integer> getDocumentTypeDistribution(final int topCutoff) throws SQLException, IOException;

	/**
	 * Gets the ages and patient count for each age that matched the previous call to <code>queryMatchingPatients</code>.
	 * @return A map of ages to the patient count for each age.
	 * @throws SQLException
	 * @throws IOException
	 */
	public Map<Integer, Integer> getAgeDistribution()
			throws SQLException, IOException;

	/**
	 * Gets the deceased status and patient count for patients matching the previous call to <code>queryMatchingPatients</code>.
	 * @return A map of patient counts for each deceased status.
	 * @throws SQLException
	 * @throws IOException
	 */
	public Map<String, Integer> getDeceasedDistribution() throws SQLException, IOException;


	public Map<String, Integer> getMedicationDistribution() throws SQLException, IOException;

	/**
	 * Gets the genders , races and patient counts for patients matching the previous call to <code>queryMatchingPatients</code>.
	 * @return A map of patient counts for each gender.
	 * @throws SQLException
	 * @throws IOException
	 */
	public Map<String, Integer> getGenderDistribution() throws SQLException, IOException;
	public Map<String, Integer> getRaceDistribution() throws SQLException, IOException;

	/**
	 * Gets the geographic coordinates and patient counts for patients matching the previous call to <code>queryMatchingPatients</code>.
	 * @return A map of patient counts for each set of coordinates.
	 * @throws SQLException
	 * @throws IOException
	 */
	//public Map<Coordinate, Integer> getGeographicDistribution() throws SQLException, IOException;

	/**
	 * Gets the prescriptions and patient counts for patients matching the previous call to <code>queryMatchingPatients</code>.
	 * @param topCutoff The number of prescriptions to return.
	 * @return A map of the top patient counts for each prescription, in descending order.
	 * @throws SQLException
	 * @throws IOException
	 */
//	public Map<String, Integer> getRxDistribution(final int topCutoff)
//			throws SQLException, IOException, InterruptedException;

	/**
	 * Gets the diagnosis and patient counts for patients matching the previous call to <code>queryMatchingPatients</code>.
	 * @param topCutoff The number of diagnosis to return.
	 * @return A map of the top patient counts for each diagnosis, in descending order.
	 * @throws SQLException
	 * @throws IOException
	 */
	public Map<String, Integer> getDxDistribution(final int topCutoff)
			throws SQLException, IOException, InterruptedException;

	/**
	 * Gets the procedures (CPTs) and patient counts for patients matching the previous call to <code>queryMatchingPatients</code>.
	 * @param topCutoff The number of procedures to return.
	 * @return A map of the top patient counts for each procedure, in descending order.
	 * @throws SQLException
	 * @throws IOException
	 */
	public Map<String, Integer> getProcedureDistribution(final int topCutoff) throws SQLException, IOException;

	/**
	 * Gets all documents for a patient.
	 * @param patientId The identifier of the patient for which all documents will be found.
	 * @return A list of all documents belonging to a patient.
	 * @throws SQLException
	 * @throws IOException
	 * @throws CorruptIndexException
	 */
	public List<SearchResult.Encounter> getEncounters(final String patientId)
			throws SQLException, CorruptIndexException, IOException;

	public String getAllEncounters()
			throws SQLException, CorruptIndexException, IOException;

	/**
	 * Gets the documents for a patient where the documents match the search terms.
	 * @param patientId The identifier of the patient for which documents will be found.
	 * @return The documents for the patient that match the search terms.
	 * @throws SQLException
	//	 * @throws ParseException
	 * @throws IOException
	 */
	public List<SearchResult.Encounter> getMatchingEncounters(final String patientId, final List<SearchTerm> searchTerms)
			throws SQLException, IOException;

	/**
	 * Gets the patients matching the previous call to <code>queryMatchingPatients</code>.
	 * @param page The page of the patient list to get.
	 * @param pagesize The number of patients per page.
	 * @return A list of patients at most <code>pagesize</code> long.
	 * @throws SQLException
	 * @throws IOException
	 */
	public List<Patient> getPatients(final int page, final int pagesize) throws SQLException, IOException;

	/**
	 * Gets a list of diagnoses for a patient.
	 * @param patientId The identifier of the patient for which diagnoses will be found.
	 * @return A list of diagnoses.
	 * @throws SQLException
	 */
	public List<PDiag> getDiagnoses(final String patientId) throws SQLException; //CodeNameCount


	public List<String> select_Diagnosis (final String q_diag) throws SQLException; //CodeNameCount

	public List<String> select_Medication (final String q_med) throws SQLException; //CodeNameCount


	public List<PDiag> getDiagnoses2(final String patientId) throws SQLException;
	public List<PDiag> all_encounter_condition(final String patientId) throws SQLException;
	public List<PDiag> matching_encounter_condition(final String patientId) throws SQLException;


	//public List<CodeNameCount> getICD9Code(String patientId) throws SQLException;

	/**
	 * Gets a list of procedures (CPTs) for a patient.
	 * @param patientId The identifier of the patient for which procedures will be found.
	 * @return A list of procedures.
	 * @throws SQLException
	 */
	public List<PLResult> getLabResult(String patientId) throws SQLException;
	public List<PLResult>getLabResult_ALL_Matches(String patinetId) throws SQLException;
	public List<PLResult>all_encounter_lab(String patinetId) throws SQLException;
	public List<PLResult>encounter_lab(String patinetId) throws SQLException;
	/**
	 * Gets a list of drugs prescribed for a patient.
	 * @param patientId The identifier of the patient for which drugs will be found.
	 * @return A list of drugs.
	 * @throws SQLException
	 */
	public List<PMedication> getDrugs(String patientId) throws SQLException;
	public List<PMedication> getDrugs2(String patientId) throws SQLException;
	public List<PMedication> matching_encounter_drugs (String patientId) throws SQLException;

	public List<PMedication> all_encounter_drugs (String patientId) throws SQLException;

	/**
	 * Cancels any running queries.
	 * @throws SQLException
	 */
	public void cancel() throws SQLException;

	/**
	 * @param numPatients Number of patients whose matching documents should be retrieved
	 * @param ranked If true then the highest ranked patients' documents will be retrieved.  If false then a random sample of patients' documents will be retrieved.
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	public List<Encounter> getDocumentSample(int numPatients, boolean ranked) throws SQLException, IOException;

	//public String queryMatchingDiagnoses () throws SQLException;
}
