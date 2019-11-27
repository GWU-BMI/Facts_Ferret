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
package gov.va.research.ir.view;

import gov.va.research.ir.view.ChartBuilder;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author vhaislreddd
 *
 */
public class ChartBuilderTest {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link gov.va.research.ir.view.ChartBuilder#buildGoogleChart(java.lang.String, java.util.List, java.util.List, java.awt.Dimension)}.
	 */
	@Test
	public void testBuildGoogleChart() {
		List<String> labels = Arrays.asList(new String[] { "< 1", "1-49",
				"50-99", "> 99" });
		List<Integer> quantities = Arrays.asList(new Integer[] { 5, 1, 2, 3 });
		try {
			/*Image img =*/ ChartBuilder.buildGoogleChart("Test Title", labels,
					quantities, new Dimension(250, 100));
		} catch (org.apache.http.conn.HttpHostConnectException | javax.net.ssl.SSLPeerUnverifiedException e) {
			// Internet unreachable
			System.err.println(e.getMessage());
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

}
