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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

/**
 * @author vhaislreddd
 *
 */
public class CategoryListPanel extends JPanel implements PdfExportable {

	private static final long serialVersionUID = -9185404127791775658L;
	private String title;
	private JLabel label;
	private Map<String,Integer> valueMap;
	private Comparator<Map.Entry<String, Integer>> valueComparator = new Comparator<Map.Entry<String, Integer>>() {
		public int compare(Entry<String, Integer> o1,
						   Entry<String, Integer> o2) {
			return o2.getValue() - o1.getValue();
		}
	};

	public CategoryListPanel(final String title) {
		this.title = title;
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				setBackground(Color.WHITE);
				setLayout(new BorderLayout());
				JPanel titlePanel = new JPanel(new BorderLayout());
				titlePanel.setBackground(Color.WHITE);
				titlePanel.add(new JLabel("<html><b>" + title
						+ "</b></html>"), BorderLayout.SOUTH);
				add(titlePanel, BorderLayout.NORTH);

				label = new JLabel();
				add(label, BorderLayout.CENTER);
			}
		});
	}

	public void addValues(final Map<String, Integer> valueMap) {
		this.valueMap = valueMap;
		String labelHtml = buildCategoryHtml(valueMap);
		final JLabel newLabel = new JLabel(labelHtml);
		ThreadUtils.runOnEDT(new Runnable() {
			public void run() {
				if (label != null) {
					remove(label);
					validate();
					repaint();
				}
				label = newLabel;
				label.setBorder(new EmptyBorder(0, 5, 0, 5));
				add(label, BorderLayout.CENTER);
				validate();
			}
		});
	}

	public void reset() {
		if (label != null) {
			ThreadUtils.runOnEDT(new Runnable() {
				public void run() {
					remove(label);
				}
			});
		}
	}

	private String buildCategoryHtml(final Map<String, Integer> valueMap) {
		List<Map.Entry<String, Integer>> valueList = new ArrayList<Map.Entry<String, Integer>>(
				valueMap.entrySet());
		Collections.sort(valueList, valueComparator);
		StringBuilder sb = new StringBuilder("<html><table>");
		for (Map.Entry<String, Integer> valueEntry : valueList) {
			String label = valueEntry.getKey();
			if (label != null && label.length() > 50) {
				label = label.substring(0, 26) + " ...";
			}
			sb.append("<tr><td>" + label + "</td><td>" + valueEntry.getValue()
					+ "</td></tr>");
		}
		sb.append("</table></html>");
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see gov.va.research.ir.view.PdfExportable#buildPdfPage(org.apache.pdfbox.pdmodel.PDDocument)
	 */
	@Override
	public List<PDPage> addPdfPages(final PDDocument pdDocument) throws IOException {
		List<Map.Entry<String, Integer>> valueList = new ArrayList<Map.Entry<String, Integer>>(
				valueMap.entrySet());
		Collections.sort(valueList, valueComparator);
		PDRectangle pageSize = PDPage.PAGE_SIZE_LETTER;
		PDPage pdPage = new PDPage(pageSize);
		pdDocument.addPage(pdPage);
		PDFont font = PDType1Font.COURIER;
		int fontHeight = Math.round((font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000) * 12);
		PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage);
		contentStream.beginText();
		contentStream.setFont(font, 12);
		contentStream.appendRawCommands(fontHeight + " TL\n");
		contentStream.moveTextPositionByAmount(fontHeight, pageSize.getHeight() - (fontHeight * 2));
		contentStream.drawString(title);
		contentStream.appendRawCommands("T*\n");
		for (Map.Entry<String, Integer> entry : valueList) {
			contentStream.appendRawCommands("T*\n");
			contentStream.drawString(entry.toString());
		}
		contentStream.endText();
		contentStream.close();
		return Arrays.asList(pdPage);
	}

}
