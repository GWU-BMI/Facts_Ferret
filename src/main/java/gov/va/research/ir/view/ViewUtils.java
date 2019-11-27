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
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTextArea;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter.HighlightPainter;

/**
 * @author vhaislreddd
 *
 */
public class ViewUtils {
	public static final Pattern CRLF_PATTERN = Pattern.compile("<CRLF>");
	public static final Pattern PARTIAL_BEGIN_CRLF_PATTERN = Pattern.compile("(?s)((?:CRLF|RLF|LF|F)>).*");
	public static final Pattern PARTIAL_END_CRLF_PATTERN = Pattern.compile("(?s).*(<(?:CRLF|CRL|CR|C))");
	public static final HighlightPainter HIGHLIGHT_PAINTER = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

	public static void replaceCRLF(final JTextArea ta, final boolean includePartials) {
		Matcher tam = CRLF_PATTERN.matcher(ta.getText());
		int offset = 0;
		while (tam.find()) {
			ta.replaceRange("\r\n", tam.start() - offset, tam.end() - offset);
			offset += 4;
		}
		if(includePartials) {
			tam = PARTIAL_BEGIN_CRLF_PATTERN.matcher(ta.getText());
			if (tam.matches()) {
				ta.replaceRange("", tam.start(1), tam.end(1));
			}
			tam = PARTIAL_END_CRLF_PATTERN.matcher(ta.getText());
			if (tam.matches()) {
				ta.replaceRange("", tam.start(1), tam.end(1));
			}
		}
	}

	public static String formatHMS(final long milliseconds) {
		long sec = milliseconds / 1000;
		return String.format("%d:%02d:%02d", sec/3600, (sec%3600)/60, sec%60);
	}

	/**
	 * Scales an image while retaining aspect ratio.
	 * @param img the image to be scaled
	 * @param maxWidth the maximum width of the resulting image
	 * @param maxHeight the maximum height of the resulting image
	 * @return a scaled copy of the original image
	 */
	public static BufferedImage scaleImage(final BufferedImage img, float maxWidth, float maxHeight) {
		float scale = calculateScale(img.getWidth(), img.getHeight(), maxWidth, maxHeight);
		int targetWidth = Math.round(img.getWidth() * scale);
		int targetHeight = Math.round(img.getHeight() * scale);
		BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = target.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(img, 0, 0, targetWidth, targetHeight, 0, 0, img.getWidth(), img.getHeight(), null);
		g.dispose();
		return target;
	}

	/**
	 * Calculates the scale necessary to fit the current width and height into the max width and height
	 * while maintaining the aspect ratio.
	 * @param width current width
	 * @param height current height
	 * @param maxWidth maximum width
	 * @param maxHeight maximum height
	 * @return the scale value to apply in order to fit the current width and height into the max width and height, maintaining the same aspect ratio
	 */
	public static float calculateScale(final float width, final float height, final float maxWidth, final float maxHeight) {
		float widthScale = maxWidth / width;
		float heightScale = maxHeight / height;
		return widthScale < heightScale ? widthScale : heightScale;
	}

	/**
	 * Converts an icon to an image: adapted from http://stackoverflow.com/questions/5830533/how-can-i-convert-an-icon-to-an-image
	 * @param icon The source icon
	 * @return An image of the icon
	 */
	public static Image icon2Image(final Icon icon) {
		Image image = null;
		if (icon instanceof ImageIcon) {
			image = ((ImageIcon) icon).getImage();
		} else {
			int w = icon.getIconWidth();
			int h = icon.getIconHeight();
			GraphicsEnvironment ge = GraphicsEnvironment
					.getLocalGraphicsEnvironment();
			GraphicsDevice gd = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gd.getDefaultConfiguration();
			BufferedImage bimage = gc.createCompatibleImage(w, h);
			Graphics2D g = bimage.createGraphics();
			icon.paintIcon(null, g, 0, 0);
			g.dispose();
			image = bimage;
		}
		return image;
	}
}
