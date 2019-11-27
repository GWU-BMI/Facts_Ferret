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

import gov.va.research.ir.model.County;

import java.awt.HeadlessException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;


/**
 * @author vhaislreddd
 *
 */
public class MapPanelTest {

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
	 * Test method for {@link gov.va.research.ir.view.MapPanel#updateMap()}.
	 */
	@Test
	public void testBuildMap() {
		try {
			double lat = 41.0161739333333;
			double lon = -111.977382866667;
			Coordinate coord = new Coordinate(lon, lat);
			MapPanel cmp = new MapPanel();
			JFrame frame = new JFrame("MapBuilder Test");
			frame.getContentPane().add(cmp);
			frame.setSize(800,600);
			frame.getContentPane().setSize(800,600);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			Map<Coordinate, Integer> countySubtotalsMap = new HashMap<>();
			countySubtotalsMap.put(coord, 1);
			cmp.updateMap(countySubtotalsMap);
			frame.setVisible(true);
			Thread.sleep(10000);
			frame.setVisible(false);
		} catch (HeadlessException e) {
			System.out.println("Warning: Failed to show map because there is no GUI");
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

}
