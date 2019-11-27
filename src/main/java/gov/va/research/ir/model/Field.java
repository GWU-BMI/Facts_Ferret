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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum Field {
	DOCUMENT_TEXT, DOCUMENT_TYPE, ENCOUNTER_YEAR, DIAGNOSIS,  MEDICATION, SURGICAL_PROCEDURE, CLINICAL_EVENT, CARESETTING_SID, AGE, GENDER, COUNTY, STATE, VISN, ICD9DESCRIPTION, ICD9CODE, DRUGS, PATIENT_SID, PATIENT_ICN, DOCUMENT_ID, CPT, DECEASED, STOPCODE, LAB;

	public static final Set<Field> DOCUMENT_FIELDS;
	public static final Set<Field> PATIENT_FIELDS;
	public static final Set<Field> DX_FIELDS;
	public static final Set<Field> DX_CODE_FIELDS;
	public static final Set<Field> RX_FIELDS;
	public static final Set<Field> LAB_FIELDS;
	public static final Set<Field> PROCEDURE_FIELDS;
	public static final Set<Field> VISIT_FIELDS;
	public static final Set<Field> NAME_VALUE_FIELDS;
	public static final List<Field> SEARCHABLE_FIELDS;
	static {
		final Set<Field> docFields = new HashSet<Field>();
		Collections.addAll(docFields, DOCUMENT_TEXT, DOCUMENT_TYPE, DOCUMENT_ID);
		DOCUMENT_FIELDS = Collections.unmodifiableSet(docFields);

		final Set<Field> patFields = new HashSet<Field>();
		Collections.addAll(patFields, ENCOUNTER_YEAR,DIAGNOSIS, MEDICATION, SURGICAL_PROCEDURE,CLINICAL_EVENT,AGE, GENDER, COUNTY, STATE, VISN, PATIENT_SID, PATIENT_ICN, DECEASED, CARESETTING_SID);
		PATIENT_FIELDS = Collections.unmodifiableSet(patFields);

		final Set<Field> dxFields = new HashSet<Field>();
		Collections.addAll(dxFields, ICD9DESCRIPTION);
		DX_FIELDS = Collections.unmodifiableSet(dxFields);

		final Set<Field> dxCodeFields = new HashSet<Field>();
		Collections.addAll(dxCodeFields, ICD9CODE);
		DX_CODE_FIELDS = Collections.unmodifiableSet(dxCodeFields);

		final Set<Field> rxFields = new HashSet<Field>();
		Collections.addAll(rxFields, DRUGS);
		RX_FIELDS = Collections.unmodifiableSet(rxFields);

		final Set<Field> procFields = new HashSet<Field>();
		Collections.addAll(procFields, CPT);
		PROCEDURE_FIELDS = Collections.unmodifiableSet(procFields);

		final Set<Field> visitFields = new HashSet<Field>();
		Collections.addAll(visitFields, STOPCODE);
		VISIT_FIELDS = Collections.unmodifiableSet(visitFields);

		final Set<Field> labFields = new HashSet<Field>();
		Collections.addAll(labFields, LAB);
		LAB_FIELDS = Collections.unmodifiableSet(labFields);

		final Set<Field> nameValueFields = new HashSet<>();
		nameValueFields.add(LAB);
		NAME_VALUE_FIELDS = Collections.unmodifiableSet(nameValueFields);

		final List<Field> searchableFields = new ArrayList<>();
		for (Field f : Field.values()) {
			if (f.isSearchable()) {
				searchableFields.add(f);
			}
		}
		SEARCHABLE_FIELDS = Collections.unmodifiableList(searchableFields);
	}

	@Override
	public String toString() {
		switch (this) {
			case DOCUMENT_ID: return "Document ID";
			case PATIENT_SID: return "Patient SID";
			case CARESETTING_SID: return "Discharge Caresetting"; //hf_f_encounter.discharge_caresetting_id
			case PATIENT_ICN: return "Patient ICN";
			case DOCUMENT_TEXT: return "Document Text";
			case DOCUMENT_TYPE: return "Document Type";
			case ENCOUNTER_YEAR: return "Encounter Year";
			case AGE: return "Age";
			case GENDER: return "Gender";
			case COUNTY: return "County";
			case STATE: return "State";
			case VISN: return "VISN";
			case DIAGNOSIS: return "Diagnosis";
			case MEDICATION: return "Medication";
			case SURGICAL_PROCEDURE: return "Surgical Procedure";
			case CLINICAL_EVENT: return "Clinical Event";
			//case ICD9: return "Diagnosis Category";
			case ICD9DESCRIPTION: return "ICD-9 Description";
			case ICD9CODE: return "ICD-9 Code";
			case DRUGS: return "Drug";
			case CPT: return "CPT";
			case DECEASED: return  "Deceased";
			case STOPCODE: return "Stop Code";
			case LAB: return "Lab";
			default: return super.toString();
		}
	}

	public boolean isSearchable() {
		switch (this) {
			//	case DOCUMENT_ID: //return false;
			//	case PATIENT_SID:
			//case CARESETTING_SID:
			//	case PATIENT_ICN:
			//	case DOCUMENT_TEXT:
			//	case DOCUMENT_TYPE:
			case AGE:


			case DIAGNOSIS:
			case MEDICATION:
			case SURGICAL_PROCEDURE:
			case CLINICAL_EVENT:
			case GENDER:
				//	case COUNTY:
				//	case STATE:
				//	case VISN:
			//case ICD9DESCRIPTION:
			case ICD9CODE:
				//case DRUGS:
			case CPT:
			case ENCOUNTER_YEAR:
				//	case DECEASED:
				//	case STOPCODE:
				//case LAB:

				return true;
			default: return false;
		}
	}
}
