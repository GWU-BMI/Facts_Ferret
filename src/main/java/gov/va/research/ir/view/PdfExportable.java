/*
 *  Copyright 2013 United States Department of Veterans Affairs
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

import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

/**
 * @author vhaislreddd
 *
 */
public interface PdfExportable {

	/**
	 * Adds a pages representing the object to a pdf document.
	 * @param pdDocument The pdf document the pages will be added to.
	 * @return a list of pdf pages representing the object.
	 * @throws IOException
	 */
	List<PDPage> addPdfPages(PDDocument pdDocument) throws IOException;

}
