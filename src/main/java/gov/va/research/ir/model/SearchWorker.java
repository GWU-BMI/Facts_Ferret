package gov.va.research.ir.model;

import gov.va.research.ir.CollectionUtils;
import gov.va.research.ir.SearchUtils;
import gov.va.research.ir.ThreadUtils;
import gov.va.research.ir.model.SearchResult.Encounter;
import gov.va.research.ir.model.SearchResult.Patient;
import gov.va.research.ir.model.SearchResult.PDiag;
import gov.va.research.ir.model.SearchResult.PMedication;
import gov.va.research.ir.model.SearchResult.PLResult;
import gov.va.vinci.nlp.qeUtils.domain.TermWeight;
import gov.va.vinci.nlp.snip.Snippet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.SwingWorker;
import javax.xml.xpath.XPathExpressionException;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

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
import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author doug
 *
 */
public class SearchWorker extends SwingWorker<Long, SearchResult.Patient> {

	private static final Logger LOG = LoggerFactory.getLogger(SearchWorker.class);
	private static final Marker QUERYLOG = MarkerFactory.getMarker("QUERYLOG");
	private static final DateFormat DATEFORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");
	private static final String USERNAME = System.getProperty("user.name");

	protected static final int SUMMARY_TOP_CUTOFF = 100;

	private static final String LS = System.getProperty("line.separator");

	public enum SearchState {
		PENDING, STARTED, DONE, EXPANDING_QUERY_OPENDB, EXPANDING_QUERY_CUIS, EXPANDING_QUERY_SYNONYMS, COUNTING_DOCUMENTS, QUERYING_DOCUMENTS, QUERYING_PATIENTS, CANCELLED, ERROR
	}

	private List<SearchTerm> searchTerms;

	private SearchState searchState;
	final private DAO dao;

	public SearchWorker(List<SearchTerm> searchTermList, final DAO dao) {
		this.searchTerms = searchTermList;
		this.dao = dao;
		this.searchState = SearchState.PENDING;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.SwingWorker#doInBackground()
	 */
	@Override
	protected Long doInBackground() throws Exception {
		long startTime = System.currentTimeMillis();
		setSearchState(SearchState.STARTED);
		String sql = null;
		try {
			sql = dao.queryMatchingPatients(searchTerms);
		} catch (final Exception e) {
			e.printStackTrace();
			this.cancel(true);
			this.setSearchState(SearchState.ERROR);
			ThreadUtils.runOnEDT(new Runnable() {
				public void run() {
					firePropertyChange("error", null, e);
				}
			});
			throw e;
		}
		long endTime = System.currentTimeMillis();
		Long elapsed = Long.valueOf(endTime - startTime);
		LOG.info(QUERYLOG, "{}\t{}\t{}\t{}\t{}\t{}",
				USERNAME, this.searchTerms.toString(),
				DATEFORMAT.format(new Date(startTime)),
				DATEFORMAT.format(new Date(endTime)), elapsed.toString(), sql);
		return elapsed;
	}

//	public void getMatchingDiagnoses() throws SQLException {
//		dao.queryMatchingDiagnoses();
//	}


	private void setSearchState(final SearchState searchState) {
		if (!SearchState.CANCELLED.equals(this.searchState) && !this.searchState.equals(searchState)) {
			SearchState oldSearchState = this.searchState;
			this.searchState = this.isCancelled() ? SearchState.CANCELLED : searchState;
			firePropertyChange("state", oldSearchState, this.searchState);
		}
	}

	public SearchState getSearchState() {
		return this.searchState;
	}

	@Override
	// Executed on event dispatch thread to receive publish calls from
	// doInBackground
	protected void process(final List<SearchResult.Patient> chunks) {
		if (chunks != null && chunks.size() > 0) {
			for (SearchResult.Patient p : chunks) {
				if (p != null) {
					this.firePropertyChange("newResults", null, p);
				}
			}
		}
		super.process(chunks);
	}

	@Override
	// Executed on event dispatch thread when work is done
	protected void done() {
		setSearchState(SearchState.DONE);
		super.done();
	}

	/**
	 * @return
	 * @throws SQLException
	 * @throws XPathExpressionException
	 * @throws IOException
	 */
	public int getPatientResultCount() throws SQLException, IOException {
		int count = dao.getMatchingPatientCount();
		this.firePropertyChange("resultCount", null, count);
		return count;
	}

	/**
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 * @throws XPathExpressionException
	 */
	public int getDocumentResultCount() throws SQLException, IOException {
		int count = dao.getMatchingDocumentCount();
		this.firePropertyChange("documentResultCount", null, count);
		return count;
	}

//	public Map<Coordinate, Integer> getGeographicDistribution() throws SQLException, IOException {
//		return dao.getGeographicDistribution();
//	}

	public Map<Integer, Integer> getAgeDistribution() throws SQLException, IOException {
		return dao.getAgeDistribution();
	}

	public Map<String,Integer> getDeceasedDistribution() throws SQLException, IOException {
		return dao.getDeceasedDistribution();
	}
	public Map<String,Integer> getMedicationDistribution() throws SQLException, IOException {
		return dao.getMedicationDistribution();
	}
	public Map<String,Integer> getDocumentTypeDistribution(final int topCutoff) throws SQLException, IOException {
		return dao.getDocumentTypeDistribution(topCutoff);
	}

	public Map<String,Integer> getDxDistribution(final int topCutoff) throws SQLException, IOException, InterruptedException {
		return dao.getDxDistribution(topCutoff);
	}

	public Map<String,Integer> getGenderDistribution() throws SQLException, IOException {
		return dao.getGenderDistribution();
	}

	public Map<String,Integer> getRaceDistribution() throws SQLException, IOException {
		return dao.getRaceDistribution();
	}


	public Map<String,Integer> getProcedureDistribution(final int topCutoff) throws SQLException, IOException {
		return dao.getProcedureDistribution(topCutoff);
	}

//	public Map<String,Integer> getRxDistribution(final int topCutoff) throws SQLException, IOException, InterruptedException {
//		return dao.getRxDistribution(topCutoff);
//	}

	public List<SearchResult.Encounter> getEncounters(final String patientId) throws SQLException, CorruptIndexException, IOException {
		return dao.getEncounters(patientId);
	}

	public String getAllEncounters() throws SQLException, IOException {
		return dao.getAllEncounters();
	}



	public List<SearchResult.Patient> getPatients(final int page, final int pagesize) throws SQLException, IOException {
		return dao.getPatients(page, pagesize);
	}


	public List<PDiag> getDiagnoses(final String patientId) throws SQLException {
		return dao.getDiagnoses(patientId);
	}

	public List<String> select_Diagnosis(final String q_diag) throws SQLException {
		return dao.select_Diagnosis(q_diag);
	}

	public List<String> select_Medication (final String q_med) throws SQLException {
		return dao.select_Medication (q_med);
	}




    public List<PDiag> getDiagnoses2(final String patientId) throws SQLException {
        return dao.getDiagnoses2(patientId);
    }

	public List<PDiag> all_encounter_condition(final String enc_id) throws SQLException {
		return dao.all_encounter_condition(enc_id);
	}

	public List<PDiag> matching_encounter_condition(final String enc_id) throws SQLException {
		return dao.matching_encounter_condition(enc_id);
	}


	public List<PLResult> getLabResult(final String patientId) throws SQLException {
		return dao.getLabResult(patientId);
	}

	public List<PLResult> encounter_lab(final String patientId) throws SQLException {
		return dao.encounter_lab(patientId);
	}

	public List<PLResult> getLabResult_ALL_Matches(final String patientId) throws SQLException {
		return dao.getLabResult_ALL_Matches(patientId);
	}



	public List<PLResult> all_encounter_lab(final String enc_id) throws SQLException {
		return dao.all_encounter_lab(enc_id);
	}






	public List<PMedication> getDrugs(final String patientId) throws SQLException {
		return dao.getDrugs(patientId);
	}

	public List<PMedication> getDrugs2(final String patientId) throws SQLException {
		return dao.getDrugs2(patientId);
	}

	public List<PMedication> matching_encounter_drugs(final String enc_id) throws SQLException {
		return dao.matching_encounter_drugs(enc_id);
	}

	public List<PMedication> all_encounter_drugs(final String enc_id) throws SQLException {
		return dao.all_encounter_drugs(enc_id);
	}

	public List<SearchResult.Encounter> getMatchingEncounters(final String patientId, final List<SearchTerm> searchTerms) throws SQLException, IOException {
		return dao.getMatchingEncounters(patientId, searchTerms);
	}

	public boolean hasSensitiveData() {
		return dao.hasSensitiveData();
	}




	public void saveResults(final SaveFile saveFile) throws IOException, SQLException, InterruptedException, ExecutionException {
		if (SaveType.PATIENT_SIDS == saveFile.type) {
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(
					saveFile.file));) {
				bw.append(buildHeader());
				bw.append("# PatientSID");
				if (dao instanceof ResultsWriter) {
					((ResultsWriter) dao).writePatientSIDs(bw);
				} else {
					int patientCount = dao.getMatchingPatientCount();
					int pageSize = 50;
					int pages = (patientCount / pageSize) + 1;
					for (int i = 0; i < pages; i++) {
						List<SearchResult.Patient> patients = dao.getPatients(
								i + 1, pageSize);
						for (SearchResult.Patient p : patients) {
							bw.newLine();
							bw.write("" + p.id);
						}
					}
				}
				bw.flush();
				bw.close();
			}
		} else if (SaveType.PATIENT_AND_DOCUMENT_SIDS == saveFile.type) {
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(
					saveFile.file));) {

				bw.append(buildHeader());
				bw.append("# PatientSID,TIUDocumentSID");
				if (dao instanceof ResultsWriter) {
					((ResultsWriter) dao).writePatientAndDocumentSIDs(bw);
				} else {
					int patientCount = dao.getMatchingPatientCount();
					int pageSize = 50;
					int pages = (patientCount / pageSize) + 1;
					for (int i = 0; i < pageSize; i += pages) {
						List<SearchResult.Patient> patients = dao.getPatients(
								i + 1, pageSize);
						for (SearchResult.Patient p : patients) {
							List<SearchResult.Encounter> docs = dao
									.getMatchingEncounters(p.id, searchTerms);
							for (SearchResult.Encounter d : docs) {
								bw.newLine();
								bw.write("" + p.id + "," + d.id);
							}
						}
					}
				}
				bw.flush();
				bw.close();
			}
		} else if (SaveType.DETAILS == saveFile.type) {
			writeSearchSummary(saveFile);
		}
	}






	private String buildHeader() throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("# "
				+ new SimpleDateFormat("HH:mm MMM d, y")
				.format(new Date()));
		sb.append(LS);
		sb.append("# Search Terms");
		sb.append(LS);
		for (SearchTerm st : searchTerms) {
			sb.append("#   " + st.toString());
			sb.append(LS);
		}
		return sb.toString();
	}

	public void cancel() throws SQLException {
		this.dao.cancel();
		super.cancel(true);
	}

	/**
	 * @param numPatients Number of patients whose matching documents should be retrieved
	 * @param ranked If true then the highest ranked documents will be retrieved.  If false then a random sample of documents will be retrieved.
	 * @throws SQLException
	 * @throws IOException
	 */
	public List<SearchResult.Encounter> getDocumentSample(int numPatients, boolean ranked) throws SQLException, IOException {
		return this.dao.getDocumentSample(numPatients, ranked);
	}

	public void writeSearchSummary(final SaveFile saveFile)
			throws InterruptedException, IOException, ExecutionException {
		// write the sample documents
		if (saveFile.docSampleType != null && DocSampleType.NONE != saveFile.docSampleType) {
			writeSamples(saveFile);
		}
	}

	public SummaryResults getSearchSummaryResults() throws IOException,
			InterruptedException, ExecutionException {
		// Set up collection of summarization tasks
		List<Runnable> summarizers = new ArrayList<Runnable>();
		final SummaryResults results = new SummaryResults();
		results.date = new Date();
		results.searchTerms = searchTerms;
		try {
			results.numPatients = getPatientResultCount();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		if (SearchUtils.containsDocumentFields(searchTerms)) {
			// Add task for counting the matching documents
			summarizers.add(new Runnable() {
				public void run() {
					try {
						final int documentCount = getDocumentResultCount();
						results.numDocuments = documentCount;
					} catch (SQLException e) {
						throw new RuntimeException(e);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			});
		}

		// Add task for location distribution
//		summarizers.add(new Runnable() {
//			public void run() {
//				try {
//					Map<Coordinate, Integer> countySubtotalsMap = getGeographicDistribution();
//					Map<Coordinate, Integer> topCounties = CollectionUtils
//							.getTopFreqs(SUMMARY_TOP_CUTOFF, countySubtotalsMap);
//					results.topCounties = topCounties;
//				} catch (Exception e) {
//					throw new RuntimeException(e);
//				}
//			}
//		});

		// Add task for finding the age distribution
		summarizers.add(new Runnable() {
			public void run() {
				try {
					Map<Integer, Integer> age2Freq = getAgeDistribution();
					Map<String, Integer> ageCategoryFreq = CollectionUtils
							.categorize(age2Freq, 0, 150, 5);
					results.ageDistribution = ageCategoryFreq;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		// Add task for finding the deceased distribution
		summarizers.add(new Runnable() {
			public void run() {
				try {
					Map<String, Integer> deceased2Freq = getDeceasedDistribution();
					results.deceasedDistribution = deceased2Freq;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});


		// for finding the Medication distribution
		summarizers.add(new Runnable() {
			public void run() {
				try {
					Map<String, Integer> MedFreq = getMedicationDistribution();
					results.topPrescriptions = MedFreq;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});


		// Add task for finding the gender distribution
		summarizers.add(new Runnable() {
			public void run() {
				try {
					Map<String, Integer> gender2Freq = getGenderDistribution();
					results.genderDistribution = gender2Freq;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});



		summarizers.add(new Runnable() {
			public void run() {
				try {
					Map<String, Integer> race2Freq = getRaceDistribution();
					results.raceDistribution = race2Freq;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});






		// Add task for finding the drug distribution
//		summarizers.add(new Runnable() {
//			public void run() {
//				try {
//					Map<String, Integer> rx2Freq = getRxDistribution(SUMMARY_TOP_CUTOFF);
//					Map<String, Integer> topRx = CollectionUtils.getTopFreqs(SUMMARY_TOP_CUTOFF, rx2Freq);
//					results.topPrescriptions = topRx;
//				} catch (Exception e) {
//					throw new RuntimeException(e);
//				}
//			}
//		});





		// Add task for finding the diagnosis distribution
		summarizers.add(new Runnable() {
			public void run() {
				try {
					Map<String, Integer> dx2Freq = getDxDistribution(SUMMARY_TOP_CUTOFF);
					Map<String, Integer> topDx = CollectionUtils.getTopFreqs(
							SUMMARY_TOP_CUTOFF, dx2Freq);
					results.topDiagnoses = topDx;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		// Add task for finding the procedure distribution
		summarizers.add(new Runnable() {
			public void run() {
				try {
					Map<String, Integer> cpt2Freq = getProcedureDistribution(SUMMARY_TOP_CUTOFF);
					Map<String, Integer> topCpt = CollectionUtils.getTopFreqs(
							SUMMARY_TOP_CUTOFF, cpt2Freq);
					results.topProcedures = topCpt;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		// Add task for finding the document type distribution
		summarizers.add(new Runnable() {
			public void run() {
				try {
					Map<String, Integer> doctype2Freq = getDocumentTypeDistribution(SUMMARY_TOP_CUTOFF);
					Map<String, Integer> topDocType = CollectionUtils
							.getTopFreqs(SUMMARY_TOP_CUTOFF, doctype2Freq);
					results.topDocumentTypes = topDocType;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		// Set up a thread pool to execute the individual summarizations
		ExecutorService executor = Executors.newFixedThreadPool(summarizers
				.size());
		// Set up a collection of futures to access the results
		List<Future<?>> futures = new ArrayList<Future<?>>(summarizers.size());
		// Execute all of the summarizers
		for (Runnable r : summarizers) {
			futures.add(executor.submit(r));
		}
		// Do an orderly shutdown of the thread pool
		executor.shutdown();
		while (!executor.isTerminated()) {
			Thread.sleep(100);
		}
		// Do a get() on each future in order to re-throw any exceptions from
		// the summarizers
		for (Future<?> f : futures) {
			f.get();
		}
		return results;
	}

	public void writeSamples(final SaveFile saveFile) throws IOException, InterruptedException {
		String noExtFilename = saveFile.file.getName();
		int dotIdx = noExtFilename.lastIndexOf('.');
		if (dotIdx > 0) {
			noExtFilename = noExtFilename.substring(0, dotIdx);
		}
		File saveDir = null;
		if (saveFile.sampleOutputTypes.contains(SampleOutputType.DOCUMENTS)) {
			String saveDirExt = null;
			if (DocSampleType.RANDOM == saveFile.docSampleType) {
				saveDirExt = "-randomPatientDocs";
			} else if (DocSampleType.TOPRANKPATIENT == saveFile.docSampleType) {
				saveDirExt = "-topRankedPatientDocs";
			} else {
				saveDirExt = "-sampleDocs";
			}
			saveDir = new File(saveFile.file.getParentFile(),
					noExtFilename + saveDirExt);
			if (saveDir.exists()) {
				for (File f : saveDir.listFiles()) {
					f.delete();
				}
			} else {
				saveDir.mkdir();
			}
		}
		Workbook snippetsWB = null;
		Sheet snippetsSheet = null;
		if (saveFile.sampleOutputTypes.contains(SampleOutputType.SNIPPETS)) {
			snippetsWB = new XSSFWorkbook();
			snippetsSheet = snippetsWB.createSheet();
			Row headerrow = snippetsSheet.createRow((short)0);
			headerrow.createCell(0).setCellValue("Patient ID");
			headerrow.createCell(1).setCellValue("Document ID");
			headerrow.createCell(2).setCellValue("Snippet Number");
			headerrow.createCell(3).setCellValue("Snippet Text");
		}
		List<SearchResult.Encounter> sampleDocs = getSampleDocuments(saveFile.docSampleType, saveFile.numPatients);
		Collections.sort(sampleDocs, new SearchResult.PatientEncounterComparator());
		Snippet snip = new Snippet();
		int patientSnippets = 0;
		int sheetRow = 0;
		String lastPatientId = null;
		SearchUtils su = new SearchUtils();
		StopWords stopWords = null;
		List<String> regexList = su.buildRegexList(searchTerms, stopWords);
		Set<String> snippetSet = new HashSet<>();
		for (Encounter doc : sampleDocs) {
			if (!doc.patientId.equals(lastPatientId)) {
				patientSnippets = 0;
				lastPatientId = doc.patientId;
			}
			if (saveDir != null) { // save sample doc
				try (
						Writer writer = new FileWriter(new File(saveDir,
								String.valueOf(doc.patientId) + "-"
										+ String.valueOf(doc.id) + ".txt"));
				) {
					writer.append(doc.text);
					writer.flush();
					writer.close();
				}
			}
			if (snippetsSheet != null && patientSnippets < saveFile.numSnippetsPerPatient) { // save snippets
				List<String> snippets;
				try {
					snippets = snip.getSnippets(doc.text, regexList, saveFile.numPrefixWords, saveFile.numSuffixWords, false);
					int snippetNum = 0;
					for (String snippet : snippets) {
						if (patientSnippets < saveFile.numSnippetsPerPatient && snippetSet.add(snippet)) {
							snippetNum++;
							patientSnippets++;
							sheetRow++;
							Row row = snippetsSheet.createRow(sheetRow);
							row.createCell(0).setCellValue(doc.patientId.toString());
							row.createCell(1).setCellValue(doc.id.toString());
							row.createCell(2).setCellValue(snippetNum);
							row.createCell(3).setCellValue(snippet);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					LOG.error("Exception getting snippets for document: patient id=" + doc.patientId + ", document id=" + doc.id + ", text=" + doc.text);
				}
			}
		}
		if (snippetsWB != null) {
			try (
					FileOutputStream fos = new FileOutputStream(new File(saveFile.file.getParentFile(), noExtFilename + "-snippets.xlsx"));
			) {
				snippetsWB.write(fos);
				fos.flush();
				fos.close();
			}
		}
	}

	public List<SearchResult.Encounter> getSampleDocuments(final DocSampleType docSampleType, final int numPatients) throws InterruptedException {
		final ListRef<SearchResult.Encounter> sampleDocs = new ListRef<>();
		if (docSampleType != null && DocSampleType.NONE != docSampleType) {
			Runnable runnable = null;
			if (DocSampleType.RANDOM == docSampleType) {
				runnable = new Runnable() {
					public void run() {
						try {
							sampleDocs.list = getDocumentSample(
									numPatients, false);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				};
			} else if (DocSampleType.TOPRANKPATIENT == docSampleType) {
				runnable = new Runnable() {
					public void run() {
						try {
							ListRef<Patient> topPatients = new ListRef<>();
							topPatients.list = getPatients(1, numPatients);
							for (Patient pat : topPatients.list) {
								List<Encounter> docs = getEncounters(pat.id);
								if (sampleDocs.list == null) {
									sampleDocs.list = docs;
								} else {
									sampleDocs.list.addAll(docs);
								}
							}
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				};
			} else if (DocSampleType.ALL == docSampleType) {
				runnable = new Runnable() {
					public void run() {
						try {
							sampleDocs.list = getDocumentSample(dao.getMatchingPatientCount(), false);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				};
			} else {
				LOG.error("Unrecognized DocSampleType: " + docSampleType);
			}
			if (runnable != null) {
				Thread thread = new Thread(runnable, "Document Sampler");
				thread.start();
				while (thread.isAlive()) {
					Thread.sleep(100);
				}
			}
		}
		return sampleDocs.list;
	}

	public DAO getDAO() {
		return dao;
	}

	public class SummaryResults {
		public Date date;
		public List<SearchTerm> searchTerms;
		public int numPatients;
		public int numDocuments;
		public Map<Coordinate, Integer> topCounties;
		public Map<String, Integer> ageDistribution;
		public Map<String, Integer> deceasedDistribution;
		public Map<String, Integer> genderDistribution;
		public Map<String, Integer> raceDistribution;
		public Map<String, Integer> topPrescriptions;
		public Map<String, Integer> topDiagnoses;
		public Map<String, Integer> topProcedures;
		public Map<String, Integer> topDocumentTypes;
	}

	public class ListRef <T> {
		public List<T> list;
	}

}
