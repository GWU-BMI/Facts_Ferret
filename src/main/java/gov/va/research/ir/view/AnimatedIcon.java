package gov.va.research.ir.view;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

public class AnimatedIcon implements Icon {

	private static final int DELAY = 100;
	private static final int PERIOD = 100;

	private List<BufferedImage> images;
	private Iterator<BufferedImage> imageIter;
	private ImageIcon realIcon;
	private int width;
	private int height;
	private boolean animating = true;
	private Timer timer;
	private Component owner;
	private IterateImageTimerTask timerTask;

	public static List<BufferedImage> extractFrames(final URL animatedGIFURL) throws IOException {
		List<BufferedImage> frames = null;
		try (
				InputStream is = animatedGIFURL.openStream();
		) {
			ImageInputStream iis = ImageIO.createImageInputStream(is);
			ImageReader ir = ImageIO.getImageReadersByFormatName("gif").next();
			ir.setInput(iis, false);
			int numImages = ir.getNumImages(true);
			frames = new ArrayList<BufferedImage>(numImages);
			for (int i = 0; i < numImages; i++) {
				frames.add(ir.read(i));
			}
		}
		return frames;
	}

	public AnimatedIcon(final Component owner, final URL animatedGIFURL) throws IOException {
		this(owner, extractFrames(animatedGIFURL));
	}

	public AnimatedIcon(final Component owner, final List<BufferedImage> images) {
		this.owner = owner;
		this.images = images;
		this.imageIter = this.images.iterator();
		BufferedImage firstImage = this.imageIter.next();
		this.realIcon = new ImageIcon(firstImage);
		for (BufferedImage bi : images) {
			if (this.width < bi.getWidth()) {
				this.width = bi.getWidth();
			}
			if (this.height < bi.getHeight()) {
				this.height = bi.getHeight();
			}
		}
		this.timer = new Timer();
		this.timerTask = new IterateImageTimerTask(this);
		this.timer.schedule(this.timerTask, DELAY, PERIOD);
	}

	@Override
	public int getIconHeight() {
		return height;
	}

	@Override
	public int getIconWidth() {
		return width;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		if (c != null) {
			this.realIcon.paintIcon(c, g, x, y);
		}
	}

	public void setAnimating(final boolean animating) {
		this.animating = animating;
		if (this.animating) {
			if (this.timer == null) {
				this.timer = new Timer();
				this.timer.schedule(this.timerTask, DELAY, PERIOD);
			}
		} else {
			this.timer.cancel();
			this.timer = null;
		}
	}

	public boolean isAnimating() {
		return this.animating;
	}

	public void iterate() {
		if (!this.imageIter.hasNext()) {
			this.imageIter = this.images.iterator();
		}
		this.realIcon.setImage(this.imageIter.next());
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				owner.repaint();
			}
		});
	}

	private class IterateImageTimerTask extends TimerTask {

		private AnimatedIcon animatedIcon;

		public IterateImageTimerTask(final AnimatedIcon ai) {
			this.animatedIcon = ai;
		}

		@Override
		public void run() {
			this.animatedIcon.iterate();
		}

	}

}
