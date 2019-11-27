/*
 *  Copyright 2013 United States Department of Veterans Affairs,
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
package gov.va.research.ir.model;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * @author vhaislreddd
 *
 */
public enum SaveType {
	PATIENT_SIDS, PATIENT_AND_DOCUMENT_SIDS, DETAILS;

	public String getFileExtension() {
		String ext = null;
		switch (this) {
			case PATIENT_SIDS:
			case PATIENT_AND_DOCUMENT_SIDS:
				ext = "csv";
				break;
			case DETAILS:
				ext = "pdf";
				break;
			default:
				break;
		}
		return ext;
	}

	public FileFilter getFileFilter() {
		FileFilter ff = null;
		switch (this) {
			case PATIENT_SIDS:
			case PATIENT_AND_DOCUMENT_SIDS:
				ff = new FileNameExtensionFilter("CSV file (*.csv)", "csv");
				break;
			case DETAILS:
				ff = new FileNameExtensionFilter("PDF file (*.pdf)", "pdf");
			default:
				break;
		}
		return ff;
	}
}
