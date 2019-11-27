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


import java.io.File;
import java.util.EnumSet;

/**
 * @author vhaislreddd
 *
 */
public class SaveFile {

	public final File file;
	public final SaveType type;
	public final DocSampleType docSampleType;
	public final EnumSet<SampleOutputType> sampleOutputTypes;
	public final int numPatients;
	public final int numPrefixWords;
	public final int numSuffixWords;
	public final int numSnippetsPerPatient;

	/**
	 * @param file The file where output will be saved.
	 * @param saveType The type of save to be performed, as defined in <code>SaveType</code>
	 */
	public SaveFile(final File file, final SaveType type) {
		this(file, type, null);
	}

	/**
	 * @param file The file where output will be saved.
	 * @param saveType The type of save to be performed, as defined in <code>SaveType</code>
	 * @param docSampleType When the saveType is <code>DETAILED</code> then this specifies the type of document sampling to be performed, as defined in <code>DocSampleType</code>
	 */
	public SaveFile(final File file, final SaveType type, final DocSampleType docSampleType) {
		this (file, type, docSampleType, null, 0, 0, 0, 0);
	}

	/**
	 * @param file The file where output will be saved.
	 * @param saveType The type of save to be performed, as defined in <code>SaveType</code>
	 * @param docSampleType When the saveType is <code>DETAILED</code> then this specifies the type of document sampling to be performed, as defined in <code>DocSampleType</code>
	 * @param sampleOutputTypes When the saveType is <code>DETAILED</code> then this specifies the types of output to produce, as defined in <code>SampleOutputType</code>
	 * @param numWordsBefore When sampleOutputTypes includes <code>SNIPPETS</code> then produce snippets with this many prefix words
	 * @param numWordsAfter When sampleOutputTypes includes <code>SNIPPETS</code> then produce snippets with this many suffix words
	 */
	public SaveFile(final File file, final SaveType saveType,
					final DocSampleType docSampleType, final EnumSet<SampleOutputType> sampleOutputTypes,
					final int numPatients, final int numPrefixWords, final int numSuffixWords) {
		this (file, saveType, docSampleType, sampleOutputTypes, numPatients, numPrefixWords, numSuffixWords, 0);
	}

	/**
	 * @param file The file where output will be saved.
	 * @param saveType The type of save to be performed, as defined in <code>SaveType</code>
	 * @param docSampleType When the saveType is <code>DETAILED</code> then this specifies the type of document sampling to be performed, as defined in <code>DocSampleType</code>
	 * @param sampleOutputTypes When the saveType is <code>DETAILED</code> then this specifies the types of output to produce, as defined in <code>SampleOutputType</code>
	 * @param numWordsBefore When sampleOutputTypes includes <code>SNIPPETS</code> then produce snippets with this many prefix words
	 * @param numWordsAfter When sampleOutputTypes includes <code>SNIPPETS</code> then produce snippets with this many suffix words
	 * @param numSnippetsPerPatient When sampleOutputTypes includes <code>SNIPPETS</code> then limit to this many snippets per patient
	 */
	public SaveFile(final File file, final SaveType saveType,
					final DocSampleType docSampleType, final EnumSet<SampleOutputType> sampleOutputTypes,
					final int numPatients, final int numPrefixWords, final int numSuffixWords, final int numSnippetsPerPatient) {
		this.file = file;
		this.type = saveType;
		this.docSampleType = docSampleType;
		this.sampleOutputTypes = sampleOutputTypes;
		this.numPatients = numPatients;
		this.numPrefixWords = numPrefixWords;
		this.numSuffixWords = numSuffixWords;
		this.numSnippetsPerPatient = numSnippetsPerPatient;
	}
}
