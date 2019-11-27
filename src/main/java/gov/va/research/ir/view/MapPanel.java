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

import gov.va.research.ir.ThreadUtils;
import gov.va.research.ir.model.County;
import gov.va.research.ir.model.CountyCoordinatesDataSource;
import gov.va.research.ir.model.LatLon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDJpeg;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * @author vhaislreddd
 *
 */
public class MapPanel extends AbstractMapPanel {

	private static final long serialVersionUID = 2877981508378886192L;
	private static final Logger LOG = LoggerFactory.getLogger(MapPanel.class);
	public static final String COUNTY_TABLE_FILE_NAME = "/shp/states.shp";
	private static final int MAX_CIRCLE_MARKER_RADIUS = 20;
	private static final int MIN_CIRCLE_MARKER_RADIUS = 5;
	public static final float CIRCLE_MARKER_OPACITY = 0.5f;
	public static final Color CIRCLE_MARKER_OUTLINE_COLOR = Color.BLACK;
	public static final Color CIRCLE_MARKER_FILL_COLOR = Color.RED;
	private Layer baseLayer = null;
	private Thread initializationThread;
	private MapContent mapContent = null;
	private BufferedImage image;
	private boolean updatedCoordinates = false;
	private GTRenderer renderer;
	private Rectangle imageBounds;
	private ReferencedEnvelope mapBounds;

	public MapPanel() throws IOException {
		mapContent = new MapContent();
		initialize();
	}

	public void initialize() {
		this.setOpaque(false);
		initializationThread = new Thread(new Runnable() {
			public void run() {
				try {
					// Load shapefile into datastore
					URL shapeFileURL = MapPanel.class.getResource(COUNTY_TABLE_FILE_NAME);
					FileDataStore dataStore = FileDataStoreFinder
							.getDataStore(shapeFileURL);
					if (dataStore == null) {
						throw new IOException(
								"Failed to load data store, is the correct DataStoreFactory"
										+ " in the classpath (e.g. IndexedShapefileDataStore)?");
					}
					// Load the county coordinates
					CountyCoordinatesDataSource.loadFromFile();
					// Create initial map
					FeatureSource<?, ?> featureSource = dataStore.getFeatureSource();
					Style style = SLD.createPolygonStyle(Color.BLACK, Color.WHITE, 1.0f);
					baseLayer = new FeatureLayer(featureSource, style);
					mapContent.addLayer(baseLayer);
				} catch (Exception e) {
					LOG.error(e.getMessage());
				}
			}
		});
		initializationThread.start();
	}


	public void updateMap(final Map<Coordinate, Integer> coordinateSubtotalMap) throws IOException,
			SchemaException, SQLException, ClassNotFoundException,
			URISyntaxException {
		while (initializationThread.isAlive()) {
			LOG.info("Waiting for initialization to complete");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}

		SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
		ftb.setName("Location");
		ftb.setCRS(DefaultGeographicCRS.WGS84);
		ftb.add("Location", Point.class);
		final SimpleFeatureType loctype = ftb.buildFeatureType();

		GeometryFactory gfact = JTSFactoryFinder.getGeometryFactory(null);
		PrecisionModel precModel = gfact.getPrecisionModel();
		SimpleFeatureBuilder fbuilder = new SimpleFeatureBuilder(loctype);

		Map<Integer, SimpleFeatureCollection> sizeFeaturesMap = new HashMap<Integer, SimpleFeatureCollection>();
		Collection<Integer> subtotals = coordinateSubtotalMap.values();
		List<Integer> subtotalList = null;
		if (subtotals instanceof List) {
			subtotalList = (List<Integer>)subtotals;
		} else {
			subtotalList = new ArrayList<Integer>(subtotals);
		}
		Collections.sort(subtotalList);
		int max = 0;
		if (subtotalList.size() > 0) {
			max = subtotalList.get(subtotalList.size() - 1);
		}
		if (max == 0) {
			max = 1;
		}
		float scale = ((float)MAX_CIRCLE_MARKER_RADIUS - MIN_CIRCLE_MARKER_RADIUS) / ((float)max);
		for (Map.Entry<Coordinate, Integer> countySubtotal : coordinateSubtotalMap
				.entrySet()) {
			int radius = Math.round((countySubtotal.getValue() * scale)
					+ MIN_CIRCLE_MARKER_RADIUS);
			Coordinate coordinate = countySubtotal.getKey();
			precModel.makePrecise(coordinate);
			Point point = gfact.createPoint(coordinate);
			fbuilder.add(point);
			SimpleFeature feature = fbuilder.buildFeature(null);
			SimpleFeatureCollection fcoll = sizeFeaturesMap.get(radius);
			if (fcoll == null) {
				fcoll = FeatureCollections.newCollection();
				sizeFeaturesMap.put(radius, fcoll);
			}
			synchronized (fcoll) {
				fcoll.add(feature);
			}
		}
		for (Map.Entry<Integer, SimpleFeatureCollection> e : sizeFeaturesMap
				.entrySet()) {
			Style pstyle = SLD.createPointStyle("Circle",
					CIRCLE_MARKER_OUTLINE_COLOR, CIRCLE_MARKER_FILL_COLOR,
					CIRCLE_MARKER_OPACITY, e.getKey());
			SimpleFeatureCollection fcoll = e.getValue();
			getMapContent().addLayer(new FeatureLayer(fcoll, pstyle));
		}
		renderer = new StreamingRenderer();
		renderer.setMapContent(getMapContent());
		mapBounds = getMapContent().getMaxBounds();
		Dimension parentSize = getParent().getSize();
		int imageWidth = parentSize.width;
		double heightToWidth = mapBounds.getSpan(1) / mapBounds.getSpan(0);
		int imageHeight = (int)Math.round(imageWidth * heightToWidth);
		if (Double.isNaN(heightToWidth)) {
			heightToWidth = 1;
		}
		imageBounds = new Rectangle(0, 0, imageWidth, imageHeight);
		this.setPreferredSize(new Dimension(imageWidth, imageHeight));
		image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);
		final Graphics2D gr = image.createGraphics();
		gr.setPaint(Color.WHITE);
		gr.fill(imageBounds);
		renderer.paint(gr, imageBounds, mapBounds);
		gr.dispose();
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				validate();
				repaint();
			}
		});
	}

	@Override
	protected void paintComponent(final Graphics g) {
		g.drawImage(image, 0, 0, null);
		super.paintComponent(g);
	}

	public void dispose() {
		mapContent.dispose();
		if (updatedCoordinates) {
			try {
				CountyCoordinatesDataSource.writeToFile();
			} catch (FileNotFoundException e) {
				LOG.error(e.getMessage());
			} catch (URISyntaxException e) {
				LOG.error(e.getMessage());
			}
		}
	}

	public void reset() {
		for (Layer layer : this.getMapContent().layers()) {
			if (this.baseLayer != layer) {
				this.getMapContent().removeLayer(layer);
			}
		}
		this.image = null;
	}

	private MapContent getMapContent() {
		return this.mapContent;
	}

	/* (non-Javadoc)
	 * @see gov.va.research.ir.view.PdfExportable#buildPdfPage(org.apache.pdfbox.pdmodel.PDDocument)
	 */
	@Override
	public List<PDPage> addPdfPages(final PDDocument pdDocument) throws IOException {
		PDRectangle pageSize = PDPage.PAGE_SIZE_LETTER;
		float scale = ViewUtils.calculateScale(imageBounds.width, imageBounds.height, pageSize.getWidth(), pageSize.getHeight());
		int targetWidth = (int)(imageBounds.width * scale);
		int targetHeight = (int)(imageBounds.height * scale);
		Rectangle targetBounds = new Rectangle(targetWidth, targetHeight);
		BufferedImage img = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setPaint(Color.WHITE);
		g.fill(targetBounds);
		renderer.paint(g, targetBounds, mapBounds);
		g.dispose();
		PDJpeg jpg = new PDJpeg(pdDocument, img);
		PDPage pdPage = new PDPage(PDPage.PAGE_SIZE_LETTER);
		pdDocument.addPage(pdPage);
		PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage);
		contentStream.drawImage(jpg, 0, pageSize.getHeight() - jpg.getHeight());
		contentStream.close();
		return Arrays.asList(pdPage);
	}

}
