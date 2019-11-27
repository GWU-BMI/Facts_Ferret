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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.util.PriorityQueue;

/**
 * @author vhaislreddd
 *
 */
public class CollectionUtils {

	public static Map<String,Integer> categorize(final Map<Integer, Integer> freqMap, final int domainLow, final int domainHigh, final int categorySize) {
		Map<String, Integer> category2Freq = new TreeMap<String, Integer>(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				StringTokenizer st = new StringTokenizer(o1, "-");
				int n1 = Integer.parseInt(st.nextToken());
				st = new StringTokenizer(o2, "-");
				int n2 = Integer.parseInt(st.nextToken());
				return n1 - n2;
			}
		});
		for (int i = domainLow; i < domainHigh; i += categorySize) {
			int count = 0;
			int categoryLow = i;
			int categoryHigh = i + (categorySize - 1);
			for (Map.Entry<Integer, Integer> keyFreq : freqMap.entrySet()) {
				int key = keyFreq.getKey().intValue();
				int freq = keyFreq.getValue().intValue();
				if (key >= categoryLow && key <= categoryHigh && freq != 0) {
					count += freq;
				}
			}
			if (count != 0) {
				category2Freq.put("" + categoryLow + "-" + categoryHigh, count);
			}
		}
		return category2Freq;
	}

	private static class ValueComparator<T> implements Comparator<T> {
		Map<T, Integer> src;
		ValueComparator(Map<T, Integer> srcMap) {
			this.src = srcMap;
		}

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(T o1, T o2) {
			// sort high to low
			return src.get(o2).compareTo(src.get(o1));
		}
	}


	public static <T> Map<T, Integer> getTopFreqs(final int topCutoff,
												  final Map<T, Integer> freqMap) {
		// Get the most frequent
		TopQueue<T> tq = new TopQueue<>(topCutoff);
		for (Map.Entry<T, Integer> dtf : freqMap.entrySet()) {
			tq.insertWithOverflow(dtf);
		}
		// put them into a sorted map
		Map<T, Integer> topDocTypeFreqs = new TreeMap<T, Integer>(new ValueComparator<T>(freqMap));
		while (tq.size() > 0) {
			Map.Entry<T, Integer> top = tq.pop();
			topDocTypeFreqs.put(top.getKey(), top.getValue());
		}
		return topDocTypeFreqs;
	}

	public static class TopQueue<T> extends PriorityQueue<Map.Entry<T,Integer>> {

		public TopQueue(int maxSize) {
			super(maxSize);
		}

		@Override
		protected boolean lessThan(Entry<T, Integer> a, Entry<T, Integer> b) {
			return a.getValue().compareTo(b.getValue()) < 0;
		}

	}

}
