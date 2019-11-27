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
package gov.va.research.ir;

import gov.va.research.ir.model.Field;
import gov.va.research.ir.model.SearchTerm;
import gov.va.research.ir.model.StopWords;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author vhaislreddd
 *
 */
public class SearchUtils {
	public static final Pattern STAR_PATTERN = Pattern.compile("\\*");
	public static final Pattern RANGE_PATTERN = Pattern.compile("\\d\\s*(-)\\s*\\d");
	public static final Pattern ANDOR_PATTERN = Pattern.compile("[\\s\\)]*(?:AND)[\\s\\(]*", Pattern.CASE_INSENSITIVE);  //AND|OR  //Seyed

	private static final Pattern AND_OR_PATTERN = Pattern.compile(
			"\\b(?:and|or)\\b", Pattern.CASE_INSENSITIVE);

	/**
	 * @param searchTerms
	 * @return
	 */
	public static boolean containsDocumentFields(final List<SearchTerm> searchTerms) {
		for (SearchTerm st : searchTerms) {
			if (Field.DOCUMENT_FIELDS.contains(st.field)) {
				return true;
			}
		}
		return false;
	}
	
	public List<Point> findMatches(final String docText,
			final List<SearchTerm> searchTerms, final StopWords stopWords) {
		List<Point> matches = new ArrayList<Point>();
		if (docText != null && docText.length() > 0) {
			List<String> regexList = buildRegexList(searchTerms, stopWords);
			for (String regex : regexList) {
				Pattern termPattern = Pattern.compile(regex);
				Matcher m = termPattern.matcher(docText);
				while (m.find()) {
					matches.add(new Point(m.start(), m.end() - 1));
				}
			}
		}
		return matches;
	}

	public List<String> buildRegexList(final List<SearchTerm> searchTerms, final StopWords stopWords) {
		Pattern stopwordPattern = Pattern.compile("(?<!\\*)" + stopWords.getRegEx() + "(?!\\*)", Pattern.CASE_INSENSITIVE);

		List<String> textList = new ArrayList<String>();
		for (SearchTerm st : searchTerms) {
			if (Field.DOCUMENT_TEXT.equals(st.field)) {
				String[] textArr = AND_OR_PATTERN.split(st.term);
				for (String text : textArr) {
					textList.add(text.trim());
				}
			}
		}

		List<String> regexList = new ArrayList<String>();
		for (String textTerm : textList) {
			// Let spaces include punctuation
			textTerm = textTerm.replace(" ", "(?: |\\p{Punct})");
			// Replace stopwords with stopword regex
			if (stopWords != null) {
				Matcher m = stopwordPattern.matcher(textTerm);
				m.replaceAll(stopWords.getRegEx());
			}
			// Replace * wildcards
			textTerm = textTerm.replace("*", "\\S*");
			regexList.add("(?i)(?m)\\b" + textTerm.replaceAll("\\s{1,}", "\\\\s{1,}") + "\\b");
		}
		return regexList;
	}
	
	public static boolean containsAny(final Collection<Field> fieldSet1,
			final Collection<Field> fieldSet2) {
		for (Field f2 : fieldSet2) {
			if (fieldSet1.contains(f2)) {
				return true;
			}
		}
		return false;
	}

}
