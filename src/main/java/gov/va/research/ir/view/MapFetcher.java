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

import gov.va.research.ir.model.LatLon;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.image.BufferedImage;
//import java.awt.image.ColorModel;
//import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
//import javax.media.jai.JAI;
//import javax.media.jai.LookupTableJAI;
import javax.media.jai.PlanarImage;
//import javax.media.jai.RenderedOp;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author vhaislreddd
 *
 */
public class MapFetcher {

	private static final Logger LOG = LoggerFactory.getLogger(MapFetcher.class);
	private static final Pattern LOCATION_PATTERN = Pattern.compile("\"location\"\\s{0,}:\\s{0,}\\{\\s{0,}\"lat\"\\s{0,}:\\s{0,}([\\d-.]+),\\s{0,}\"lng\"\\s{0,}:\\s{0,}([\\d-.]+)\\s{0,}\\}");
	private static Boolean accessible;

	/**
	 * Tests if google maps api is accessible
	 * @return <code>true</code> if google maps api is accessible, <code>false</code> otherwise
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static boolean isAccessible() {
		if (accessible != null) {
			return accessible;
		}
		final String uri = "http://maps.googleapis.com/maps/api/geocode/json?region=us&sensor=false&address=salt+lake+city";
		ExecutorService ex = Executors.newSingleThreadExecutor();
		Future<Integer> f = ex.submit(new Callable<Integer>() {
			public Integer call() throws Exception {
				HttpClient httpClient = HttpClientBuilder.create().build();
				HttpGet httpGet = new HttpGet(uri);
				HttpResponse httpResponse = httpClient.execute(httpGet);
				Integer statusCode = null;
				if (httpResponse != null && httpResponse.getStatusLine() != null) {
					statusCode = Integer.valueOf(httpResponse.getStatusLine().getStatusCode());
				}
				return statusCode;
			}
		});
		ex.shutdown();
		try {
			boolean terminated = ex.awaitTermination(5, TimeUnit.SECONDS);
			if (!terminated) {
				LOG.debug("Timeout waiting for internet mapping api. Assume it is not accessible.");
				f.cancel(true);
				accessible = Boolean.FALSE;
			} else {
				accessible = Boolean.valueOf((200 == f.get()));
			}
		} catch (Exception e) {
			accessible = Boolean.FALSE;
			LOG.info("Cannot access internet mapping api: " + e.getMessage());
		}
		if (!accessible) {
			LOG.warn("Internet Map APIs are not accessible, lookup of locations not previously known will be disabled.");
		}
		return accessible;
	}

	public static Image getMap(final List<String> locationNames) throws ClientProtocolException, IOException {
		// Get the base map image
		String baseUri = "http://maps.googleapis.com/maps/api/staticmap?center=38,-96&zoom=4&size=640x400&sensor=false";
		BufferedImage baseMapImage = getMapImage(baseUri);

		// Consolidate location names
		Map<String, Integer> locationCountMap = new HashMap<String, Integer>();
		for (String ln : locationNames) {
			Integer count = locationCountMap.get(ln);
			if (count == null) {
				count = Integer.valueOf(1);
			} else {
				count = Integer.valueOf(count.intValue() + 1);
			}
			locationCountMap.put(ln, count);
		}
		StringBuilder uri = new StringBuilder(baseUri + "&markers=");
		int baseUriLength = baseUri.length();
		List<BufferedImage> imageList = new ArrayList<BufferedImage>();
		boolean first = true;
		for (String ln : locationCountMap.keySet()) {
			String location = null;
			if (first) {
				location = URLEncoder.encode(ln, "UTF-8");
				first = false;
			} else {
				location = URLEncoder.encode("|" + ln, "UTF-8");
			}
			if (location.length() + baseUriLength > 2048) {
				imageList.add(getMapImage(uri.toString()));
				uri = new StringBuilder(baseUri);
			} else {
				uri.append(location);
			}
		}
		if (uri.length() > baseUri.length()) {
			imageList.add(getMapImage(uri.toString()));
		}
//		ColorModel colorModel = baseMapImage.getColorModel();
		PlanarImage combinedImg = PlanarImage.wrapRenderedImage(baseMapImage);
//		for (BufferedImage img : imageList) {
//			RenderedOp subtractedOp = JAI.create("subtract", img, baseMapImage);
//			BufferedImage subtractedImg = subtractOp.getAsBufferedImage(subtractOp.getBounds(), colorModel);
//			PlanarImage dst = null;
//			if (combinedImg.getColorModel() instanceof IndexColorModel) {
//				IndexColorModel icm = (IndexColorModel)combinedImg.getColorModel();
//				byte[][] data = new byte[3][icm.getMapSize()];
//				icm.getReds(data[0]);
//	            icm.getGreens(data[1]);
//	            icm.getBlues(data[2]);
//
//	            LookupTableJAI lut = new LookupTableJAI(data);
//
//	            dst = JAI.create("lookup", subtractedOp, lut);
//			} else {
//				dst = subtractedOp;
//			}
		//combinedImg = JAI.create("overlay", combinedImg, dst);
		//combinedImg = JAI.create("overlay", combinedImg, subtractOp).getAsBufferedImage();
//		}
		return combinedImg.getAsBufferedImage();
	}

	public static LatLon getCoordinate(final String location) throws ClientProtocolException, IOException {
		LatLon coordinate = null;
		if (location != null) {
			String uri = "http://maps.googleapis.com/maps/api/geocode/json?region=us&sensor=false&address="
					+ URLEncoder.encode(location, "UTF-8");
			HttpClient httpClient = HttpClientBuilder.create().build();
			HttpGet httpGet = new HttpGet(uri);
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();
			if (httpEntity != null) {
				InputStream is = httpEntity.getContent();
				InputStreamReader isr = new InputStreamReader(is);
				char[] cbuff = new char[1024];
				int numread = 0;
				StringBuffer response = new StringBuffer();
				while ((numread = isr.read(cbuff)) != -1) {
					response.append(cbuff, 0, numread);
				}
				Matcher m = LOCATION_PATTERN.matcher(response.toString());
				if (m.find()) {
					Double lat = Double.valueOf(m.group(1));
					Double lng = Double.valueOf(m.group(2));
					coordinate = new LatLon(lat, lng);
				}
			}
		}
		return coordinate;
	}

	private static BufferedImage getMapImage(final String uri) throws ClientProtocolException, IOException {
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpGet httpGet = new HttpGet(uri);
		HttpResponse httpResponse = httpClient.execute(httpGet);
		HttpEntity httpEntity = httpResponse.getEntity();
		BufferedImage img = null;
		if (httpEntity != null) {
			InputStream is = httpEntity.getContent();
			img = ImageIO.read(is);
			is.close();
		}
		return img;
	}

	public static void main (String[] args) throws ClientProtocolException, IOException {
		List<String> locationNames = new ArrayList<String>();
		locationNames.add("Salt Lake County Utah");
		locationNames.add("Orange County New York");
		final Image img = MapFetcher.getMap(locationNames);
		final LatLon coordinate = MapFetcher.getCoordinate(locationNames.get(0));
		if (coordinate == null) {
			LOG.info("Failed to get coordinate for: " + locationNames.get(0));
		} else {
			LOG.info(locationNames.get(0) + " coordinate: " + coordinate.latitude + ", " + coordinate.longitude);
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					JFrame frame = new JFrame();
					frame.setBounds(0,  0, 700, 500);
					frame.setLayout(new BorderLayout());
					frame.add(new JLabel(new ImageIcon(img)), BorderLayout.CENTER);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}
}
