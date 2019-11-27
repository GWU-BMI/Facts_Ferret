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
package gov.va.research.ir.model;

/**
 * @author vhaislreddd
 *
 */
public class County {
	public final String county;
	public final String state;
	public County(final String county, final String state) {
		this.county = county;
		this.state = state;
	}
	@Override
	public String toString() {
		String str = null;
		if (county == null) {
			if (state == null) {
				str = null;
			} else {
				str = state;
			}
		} else {
			if (state == null) {
				str = county + " county";
			} else {
				str = county + " county " + state;
			}
		}
		return str;
	}

	@Override
	public int hashCode() {
		int hashCode = 17;
		hashCode = 31 * hashCode + (county == null ? 0 : county.hashCode());
		hashCode = 31 * hashCode + (state == null ? 0 : state.hashCode());
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof County)) {
			return false;
		}
		County o = (County)obj;
		return (
				(county == o.county || (county != null && county.equals(o.county)))
						&&
						(state == o.state || (state != null && state.equals(o.state)))
		);
	}

}
