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

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;

/**
 * @author vhaislreddd
 *
 */
public interface ResultsWriter {
	public void writePatientSIDs(final Writer writer) throws SQLException, IOException;
	public void writePatientAndDocumentSIDs(final Writer writer) throws SQLException, IOException;
}
