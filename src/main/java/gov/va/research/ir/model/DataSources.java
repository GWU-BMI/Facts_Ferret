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

import javax.sql.DataSource;

/**
 * @author vhaislreddd
 *
 */
public class DataSources {

	private DataSource all;
	private DataSource subset;

	public DataSources() { }

	public DataSource getAll() {
		return all;
	}
	public void setAll(DataSource all) {
		this.all = all;
	}
	public DataSource getSubset() {
		return subset;
	}
	public void setSubset(DataSource subset) {
		this.subset = subset;
	}


}
