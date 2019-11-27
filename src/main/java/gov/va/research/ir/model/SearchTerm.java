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
package gov.va.research.ir.model;

import java.util.ArrayList;
import java.util.List;


/**
 * @author doug
 *
 */
public class SearchTerm {
	public final String term;
	public final Field field;
	public final BoolOp boolOp;
	public final String qualifier;

	/**
	 * @param term
	 * @param fieldName
	 * @param boolOp
	 */
	public SearchTerm(final String term, final String fieldName, final BoolOp boolOp) {
		this(term, Field.valueOf(fieldName), boolOp);
	}

	/**
	 * @param term
	 * @param field
	 * @param boolOp
	 */
	public SearchTerm(final String term, final Field field, final BoolOp boolOp) {
		this(term, field, boolOp, null);
	}

	/**
	 * @param term
	 * @param field
	 * @param boolOp
	 * @param qualifier
	 */
	public SearchTerm(final String term, final Field field, final BoolOp boolOp, final String qualifier) {
		this.term = term;
		this.field = field;
		this.boolOp = boolOp;
		this.qualifier = qualifier;
	}

	@Override
	public String toString() {
		return boolOp.name() + ":" + field.name() + ":" + qualifier + ":" + term;
	}


}
