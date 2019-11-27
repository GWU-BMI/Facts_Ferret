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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author vhaislreddd
 *
 */
public class ChartBuilder {

	public static final String COUNTS_PANEL_NAME = "counts panel";
	private static final Logger LOG = LoggerFactory.getLogger(ChartBuilder.class);
	public static Image buildGoogleChart(final String title, final List<String> labels, final List<Integer> quantities,  final Dimension dim) throws IOException {
		HttpClientBuilder hcb = HttpClientBuilder.create();
		HttpClient httpClient = hcb.build();
		StringBuilder uri = new StringBuilder("https://chart.googleapis.com/chart?cht=bvg&chtt=" + URLEncoder.encode(title, "UTF-8") + "&chs=" + dim.width + "x" + dim.height + "&chxt=x,y");
		int dataMax = 0;
		for (Integer q : quantities) {
			if (q.intValue() > dataMax) {
				dataMax = q.intValue();
			}
		}
		int scale = 100 / dataMax;
		boolean first = true;
		uri.append("&chd=t:");
		for (Integer q : quantities) {
			if (first) {
				first = false;
			} else {
				uri.append(",");
			}
			uri.append(q * scale);
		}
		uri.append("&chxl=0:");
		for (String l : labels) {
			if (l == null) {
				l = "null";
			}
			uri.append("%7C" + URLEncoder.encode(l, "UTF-8"));
		}
		uri.append("%7C");
		uri.append("&chxr=1,0," + dataMax);

		HttpGet httpGet = new HttpGet(uri.toString());
		HttpResponse httpResponse = httpClient.execute(httpGet);
		HttpEntity httpEntity = httpResponse.getEntity();
		Image img = null;
		if (httpEntity != null) {
			InputStream is = httpEntity.getContent();
			img = ImageIO.read(is);
			is.close();
		}
		return img;
	}

	public static ChartPanel buildHistogramChart(final double[] values, final int bins, final int binsize, final int minval, final int maxval, final String xAxisLabel, final String yAxisLabel) {
		DefaultCategoryDataset ds = new DefaultCategoryDataset();
		for (int i = 0; i < values.length; i++) {
			ds.addValue(values[i], "Ages", String.valueOf((i * binsize) + minval) + "-" + String.valueOf(((i+1) * binsize) + minval - 1));
		}
		JFreeChart chart = ChartFactory.createBarChart(
				null, // title
				xAxisLabel, // x axis label
				yAxisLabel, // y axis label
				ds, // data set
				PlotOrientation.VERTICAL, // orientation
				false, // legend?
				true, // tooltips?
				false // URLs?
		);
		chart.setBackgroundPaint(Color.white);

		CategoryPlot plot = (CategoryPlot)chart.getPlot();
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinePaint(Color.white);
		CategoryAxis domainAxis = (CategoryAxis)plot.getDomainAxis();
		domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
		ChartPanel chartPanel = new ChartPanel(chart, false);
		return chartPanel;
	}

	public static JPanel buildCategoryChart(final Map<String,Integer> valueMap) {
		JPanel returnPanel = null;
		if (valueMap.size() > 3) {
			returnPanel = buildBarChart(valueMap);
		} else {
			returnPanel = buildPieChart(valueMap);
		}
		return returnPanel;
	}

	public static ChartPanel buildBarChart(final Map<String,Integer> valueMap) {
		DefaultCategoryDataset ds = new DefaultCategoryDataset();
		int maxLabelLength = 0;
		for (Map.Entry<String,Integer> valueEntry : valueMap.entrySet()) {
			String label = valueEntry.getKey();
			Integer value = valueEntry.getValue();
			ds.addValue(value, "series1", label);
			if (label.length() > maxLabelLength) {
				maxLabelLength = label.length();
			}
		}
		JFreeChart chart = ChartFactory.createBarChart(
				null,       // chart title
				null,               // domain axis label
				null,                  // range axis label
				ds,                  // data
				PlotOrientation.VERTICAL, // orientation
				false,                     // include legend
				true,                     // tooltips?
				false                     // URLs?
		);
		chart.setBackgroundPaint(Color.white);

		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		if (maxLabelLength > 2) {
			plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		}
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setDomainGridlinesVisible(true);
		plot.setRangeGridlinePaint(Color.white);
		ChartPanel chartPanel = new ChartPanel(chart, false);
		int chartWidth = valueMap.size() * 30;
		if (chartWidth < 150) {
			chartWidth = 150;
		}

		return chartPanel;
	}

	public static JPanel buildPieChart(final Map<String,Integer> valueMap) {
		DefaultPieDataset ds = new DefaultPieDataset();
		for (Map.Entry<String,Integer> valueEntry : valueMap.entrySet()) {
			ds.setValue(valueEntry.getKey(), valueEntry.getValue());
		}
		JFreeChart chart = ChartFactory.createPieChart(
				null,       // title
				ds,         // dataset
				false,      // legend?
				true,      // tooltips?
				false		// URLs?
		);
		chart.setBackgroundPaint(Color.white);
		PiePlot plot = (PiePlot) chart.getPlot();
		plot.setBackgroundPaint(Color.lightGray);
		ChartPanel chartPanel = new ChartPanel(chart, false);
		JPanel panel = new JPanel(new MigLayout());
		panel.add(chartPanel, "growx");
		JPanel countsPanel = new JPanel(new MigLayout("insets 5"));
		countsPanel.setName(COUNTS_PANEL_NAME);
		for (Map.Entry<String,Integer> me : valueMap.entrySet()) {
			countsPanel.add(new JLabel(me.getKey() + ":" + me.getValue().toString()), "wrap");
		}
		countsPanel.setBackground(Color.WHITE);
		countsPanel.setBorder(new LineBorder(Color.BLACK));
		panel.add(countsPanel);
		panel.setBackground(Color.WHITE);
		return panel;
	}
}
