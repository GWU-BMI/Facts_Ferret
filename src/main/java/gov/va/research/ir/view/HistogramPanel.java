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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDJpeg;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

/**
 * @author vhaislreddd
 *
 */
public class HistogramPanel extends JPanel implements PdfExportable {

	private static final long serialVersionUID = -741035809913362234L;
	private ChartPanel chart;
	private String title;
	private String xAxisLabel;
	private String yAxisLabel;

	public HistogramPanel(final String title, final String xAxisLabel, final String yAxisLabel) {
		setBackground(Color.WHITE);
		setLayout(new BorderLayout());
		add(new JLabel("<html><b>" + title + "</b></html>"), BorderLayout.NORTH);
		this.title = title;
		this.xAxisLabel = xAxisLabel;
		this.yAxisLabel = yAxisLabel;
	}

	public void addValues(final Map<Integer,Integer> valueMap, int bucketSize) {
		if (valueMap.size() == 0) {
			return;
		}
		List<Integer> keys = new ArrayList<Integer>(valueMap.keySet());
		Collections.sort(keys);

		int minKey = (keys.get(0).intValue() / 5) * 5;
		int maxKey = ((keys.get(keys.size() - 1).intValue() / 5) + 1) * 5;

		int bins = ((maxKey - minKey) / bucketSize);

		final double[] primitiveArray = new double[bins];
		Arrays.fill(primitiveArray, 0d);
		for (Integer key : keys) {
			int idx = (key - minKey) / bucketSize;
			primitiveArray[idx] += valueMap.get(key);
		}
		final ChartPanel newChart = ChartBuilder.buildHistogramChart(primitiveArray,
				bins, bucketSize, (int) minKey, (int) maxKey, xAxisLabel, yAxisLabel);
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				if (chart != null) {
					remove(chart);
				}
				chart = newChart;
				add(chart, BorderLayout.CENTER);
				validate();
			}
		});
	}

	/**
	 *
	 */
	public void reset() {
		if (chart != null) {
			ThreadUtils.runOnEDT(new Runnable() {
				public void run() {
					remove(chart);
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see gov.va.research.ir.view.PdfExportable#buildPdfPage(org.apache.pdfbox.pdmodel.PDDocument)
	 */
	@Override
	public List<PDPage> addPdfPages(final PDDocument pdDocument) throws IOException {
		PDRectangle pageSize = PDPage.PAGE_SIZE_LETTER;
		float scale = ViewUtils.calculateScale(this.getSize().width, this.getSize().height, pageSize.getWidth(), pageSize.getHeight());
		float targetWidth = this.getSize().width * scale;
		float targetHeight = this.getSize().height * scale;
		BufferedImage img = null;
		ChartPanel chartPanel = null;
		if (chart instanceof ChartPanel) {
			chartPanel = (ChartPanel)chart;
		} else {
			for (Component c : chart.getComponents()) {
				if (c instanceof ChartPanel) {
					chartPanel = (ChartPanel)c;
					break;
				}
			}
		}
		if (chartPanel != null) {
			JFreeChart jfc = chartPanel.getChart();
			jfc.setTitle(title);
			img = jfc.createBufferedImage((int)targetWidth, (int)targetHeight, BufferedImage.TYPE_INT_RGB, null);
		} else {
			img = new BufferedImage((int)targetWidth, (int)targetHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = img.createGraphics();
			chart.paint(g);
			g.dispose();
		}
		PDJpeg jpg = new PDJpeg(pdDocument, img);
		PDPage pdPage = new PDPage(pageSize);
		pdDocument.addPage(pdPage);
		PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage);
		contentStream.drawImage(jpg, 0, pageSize.getHeight() - jpg.getHeight());
		contentStream.close();
		return Arrays.asList(pdPage);
	}

}
