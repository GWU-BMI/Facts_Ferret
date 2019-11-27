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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author vhaislreddd
 * Manages access to a data store of the coordinates of counties.
 */
public class CountyCoordinatesDataSource {
	private static final Logger LOG = LoggerFactory.getLogger(CountyCoordinatesDataSource.class);
	// Default county coordinates file - fields should be tab separated in the order: county state latitude longitude
	private static final String DEFAULT_COUNTY_COORDINATES_FILENAME = "county_coordinates.txt";
	private static final ConcurrentMap<County, LatLon> countyCoordinateMap = new ConcurrentHashMap<County, LatLon>();

	/**
	 * Loads county coordinates from the specified URI of a file.  The fields must be tab separated in the order: county state latitude longitude
	 * @throws URISyntaxException
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public static void loadFromFile(final URI uri) throws URISyntaxException, NumberFormatException, IOException {
		File countyCoordinatesFile = new File(uri);

		BufferedReader br = new BufferedReader(new FileReader(countyCoordinatesFile));
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] tokens = line.split("\\t");
			if (4 != tokens.length) {
				LOG.error("Wrong number of tokens for line in " + countyCoordinatesFile.toString() + ": " + line);
			} else {
				County county = new County(tokens[0], tokens[1]);
				LatLon latLon = new LatLon(Double.valueOf(tokens[2]), Double.valueOf(tokens[3]));
				countyCoordinateMap.put(county, latLon);
			}
		}
		br.close();
	}

	/**
	 * Loads county coordinates from from the default file "county_coordinates.txt" in the system classpath.
	 * @throws NumberFormatException
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public static void loadFromFile() throws NumberFormatException, URISyntaxException, IOException {
		URL countyCoordinatesURL = ClassLoader.getSystemResource(DEFAULT_COUNTY_COORDINATES_FILENAME);
		loadFromFile(countyCoordinatesURL.toURI());
	}

	/**
	 * Gets the latitude and longitude of the give county
	 * @param county
	 * @return
	 * @throws NumberFormatException
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public static LatLon get(final County county) throws NumberFormatException, URISyntaxException, IOException {
		return countyCoordinateMap.get(county);
	}

	/**
	 * Adds the latitude and longitude of the provided county to the data store.
	 * @param county
	 * @param latLon
	 */
	public static void put(final County county, final LatLon latLon) {
		countyCoordinateMap.put(county, latLon);
	}

	/**
	 * Persists the data store to the file identified by the URI.
	 * @param uri
	 * @throws FileNotFoundException
	 */
	public static void writeToFile(final URI uri) throws FileNotFoundException {

		File countyCoordinatesFile = new File(uri);
		PrintWriter pw = new PrintWriter(countyCoordinatesFile);
		try {
			for (Map.Entry<County, LatLon> ccEntry : countyCoordinateMap
					.entrySet()) {
				County county = ccEntry.getKey();
				LatLon latLon = ccEntry.getValue();
				if (county.county != null) {
					pw.print(county.county);
				}
				pw.print('\t');
				if (county.state != null) {
					pw.print(county.state);
				}
				pw.print('\t');
				if (latLon.latitude != null) {
					pw.print(latLon.latitude.toString());
				}
				pw.print('\t');
				if (latLon.longitude != null) {
					pw.println(latLon.longitude.toString());
				}
			}
		} finally {
			pw.flush();
			pw.close();
		}
	}

	/**
	 * Persists the data store to the default file file "county_coordinates.txt" in the system classpath.
	 * @param uri
	 * @throws FileNotFoundException
	 */
	public static void writeToFile() throws FileNotFoundException, URISyntaxException {
		URL countyCoordinatesURL = ClassLoader
				.getSystemResource(DEFAULT_COUNTY_COORDINATES_FILENAME);
		writeToFile(countyCoordinatesURL.toURI());
	}
}
