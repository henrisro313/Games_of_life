import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;
import org.w3c.dom.Node;

/**
 * Implements a Life-like cellular automaton on a generic grid.
 * Originally authored by Peter Taylor, modified and extended by Henrik Schou Guttesen using ChatGPT 5.2
 * See: http://codegolf.stackexchange.com/q/35827/194
 * 
 * Usage:
 *   javac GenericLife.java AbstractLattice.java Tiling.java <tiling>.java
 *   java GenericLife <tiling> [<output.gif> <cell-data>]
 */
public class GenericLife {
	private static final Color GRIDCOL = new Color(0x808080);
	private static final Color DEADCOL = new Color(0xffffff);
	private static final Color LIVECOL = new Color(0x00b092);

	private static final int MARGIN = 8;

	private static void usage() {
		System.out.println("Usage: java GenericLife <tiling> [<output.gif> <cell-data>]");
		System.out.println("For random search, supply just the tiling name");
		System.exit(1);
	}

	public static <Cell> List<Set<Cell>> evolvePublic(Tiling<Cell> tiling, Set<Cell> start, int maxGen) {
    	return evolve(tiling, start, maxGen);
	}

	public static <Cell> void createAnimatedGifPublic(String filename, Tiling<Cell> tiling, List<Set<Cell>> gens) {
    	try {
        	createAnimatedGif(filename, tiling, gens);
    	} catch (Exception e) {
        	e.printStackTrace();
    	}
	}



	// Unchecked warnings due to using reflection to instantation tiling over unknown cell type
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		if (args.length == 0 || args[0].equals("--help")) usage();

		Tiling tiling = (Tiling)Class.forName(args[0]).newInstance();
		if (args.length > 1) {
			String[] cellData = new String[args.length - 2];
			System.arraycopy(args, 2, cellData, 0, cellData.length);
			Set alive;
			try { alive = tiling.parseCells(cellData); }
			catch (Exception ex) { usage(); return; }

			createAnimatedGif(args[1], tiling, evolve(tiling, alive, 300));
		}
		else search(tiling);
	}

	private static <Cell> void search(Tiling<Cell> tiling) throws IOException {
		while (true) {
			// Build a starting generation within a certain radius of the initial cell.
			// This is a good place to tweak.
			Set<Cell> alive = new HashSet<Cell>();
			double density = Math.random();
			Set<Cell> visited = new HashSet<Cell>();
			Set<Cell> boundary = new HashSet<Cell>();
			boundary.add(tiling.initialCell());
			for (int r = 0; r < 10; r++) {
				visited.addAll(boundary);
				Set<Cell> nextBoundary = new HashSet<Cell>();
				for (Cell cell : boundary) {
					if (Math.random() < density) alive.add(cell);
					for (Cell neighbour : tiling.neighbours(cell)) {
						if (!visited.contains(neighbour)) nextBoundary.add(neighbour);
					}
				}

				boundary = nextBoundary;
			}

			final int MAX = 100;
			List<Set<Cell>> gens = evolve(tiling, alive, MAX);
			// Long-lived starting conditions might mean a glider, so are interesting.
			boolean interesting = gens.size() == MAX;
			String desc = "gens-" + MAX;
			if (!interesting) {
				// We hit some oscillator - but was it an interesting one?
				int lastGen = gens.size() - 1;
				gens = evolve(tiling, gens.get(lastGen), gens.size());
				if (gens.size() > 1) {
					int period = gens.size() - 1;
					desc = "oscillator-" + period;
					interesting = tiling.isInterestingOscillationPeriod(period);
					if (period > 10) {
						System.out.println("Oscillator period " + period);
					}
				}
				else {
					String result = gens.get(0).isEmpty() ? "Extinction" : "Still life";
					System.out.println(result + " at gen " + lastGen);
				}
			}

			if (interesting) {
				String filename = System.getProperty("java.io.tmpdir") + "/" + tiling.getClass().getSimpleName() + "-" + System.nanoTime() + "-" + desc + ".gif";
				createAnimatedGif(filename, tiling, gens);
				System.out.println("Wrote " + gens.size() + " generations to " + filename);
			}
		}
	}

	public static <Cell> List<Set<Cell>> evolve(Tiling<Cell> tiling, Set<Cell> gen0, int numGens) {
		Map<Set<Cell>, Integer> firstSeen = new HashMap<Set<Cell>, Integer>();
		List<Set<Cell>> gens = new ArrayList<Set<Cell>>();
		gens.add(gen0);
		firstSeen.put(gen0, 0);

		Set<Cell> alive = gen0;
		for (int gen = 1; gen < numGens; gen++) {
			if (alive.size() == 0) break;

			Set<Cell> nextGen = nextGeneration(tiling, alive);
			Integer prevSeen = firstSeen.get(nextGen);
			if (prevSeen != null) {
				if (gen - prevSeen > 1) gens.add(nextGen); // Finish the loop.
				break;
			}

			alive = nextGen;
			gens.add(alive);
			firstSeen.put(alive, gen);
		}

		return gens;
	}

	private static <Cell> void createAnimatedGif(String filename, Tiling<Cell> tiling, List<Set<Cell>> gens) throws IOException {
		OutputStream out = new FileOutputStream(filename);
		ImageWriter imgWriter = ImageIO.getImageWritersByFormatName("gif").next();
		ImageOutputStream imgOut = ImageIO.createImageOutputStream(out);
		imgWriter.setOutput(imgOut);
		imgWriter.prepareWriteSequence(null);

		Rectangle bounds = bbox(tiling, gens);
		Set<Cell> gen0 = gens.get(0);
		int numGens = gens.size();

		for (int gen = 0; gen < numGens; gen++) {
			Set<Cell> alive = gens.get(gen);

			// If we have an oscillator which loops cleanly back to the start, skip the last frame.
			// if (gen > 0 && alive.equals(gen0)) break;

			writeGifFrame(imgWriter, render(tiling, bounds, alive), gen == 0, gen == numGens - 1);
		}

		imgWriter.endWriteSequence();
		imgOut.close();
		out.close();
	}

	private static <Cell> Rectangle bbox(Tiling<Cell> tiling, Collection<? extends Collection<Cell>> gens) {
		Rectangle bounds = new Rectangle(-1, -1);
		Set<Cell> allGens = new HashSet<Cell>();
		for (Collection<Cell> gen : gens) allGens.addAll(gen);
		for (Cell cell : allGens) {
			int[][] cellBounds = tiling.bounds(cell);
			int[] xs = cellBounds[0], ys = cellBounds[1];
			for (int i = 0; i < xs.length; i++) bounds.add(xs[i], ys[i]);
		}

		bounds.grow(MARGIN, MARGIN);
		return bounds;
	}

	private static void writeGifFrame(ImageWriter imgWriter, BufferedImage img, boolean isFirstFrame, boolean isLastFrame) throws IOException {
		IIOMetadata metadata = imgWriter.getDefaultImageMetadata(new ImageTypeSpecifier(img), null);

		String metaFormat = metadata.getNativeMetadataFormatName();
		Node root = metadata.getAsTree(metaFormat);

		IIOMetadataNode grCtlExt = findOrCreateNode(root, "GraphicControlExtension");
		grCtlExt.setAttribute("delayTime", isLastFrame ? "500" : "5"); // Extra delay for last frame
		grCtlExt.setAttribute("disposalMethod", "doNotDispose");

		if (isFirstFrame) {
			// Configure infinite looping.
			IIOMetadataNode appExts = findOrCreateNode(root, "ApplicationExtensions");
			IIOMetadataNode appExt = findOrCreateNode(appExts, "ApplicationExtension");
			appExt.setAttribute("applicationID", "NETSCAPE");
			appExt.setAttribute("authenticationCode", "2.0");
			appExt.setUserObject(new byte[] { 1, 0, 0 });
		}

		metadata.setFromTree(metaFormat, root);
		imgWriter.writeToSequence(new IIOImage(img, null, metadata), null);
	}

	private static IIOMetadataNode findOrCreateNode(Node parent, String nodeName) {
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child.getNodeName().equals(nodeName)) return (IIOMetadataNode)child;
		}

		IIOMetadataNode node = new IIOMetadataNode(nodeName);
		parent.appendChild(node);
		return node ;
	}

	public static <Cell> Set<Cell> nextGeneration(Tiling<Cell> tiling, Set<Cell> gen) {
		Map<Cell, Integer> neighbourCount = new HashMap<Cell, Integer>();
		for (Cell cell : gen) {
			for (Cell neighbour : tiling.neighbours(cell)) {
				Integer curr = neighbourCount.get(neighbour);
				neighbourCount.put(neighbour, 1 + (curr == null ? 0 : curr.intValue()));
			}
		}

		Set<Cell> nextGen = new HashSet<Cell>();
		// S23 condition:
		for (Map.Entry<Cell, Integer> e : neighbourCount.entrySet()) {
			if (e.getValue() == 3 || (e.getValue() == 2 && gen.contains(e.getKey()))) {
				nextGen.add(e.getKey());
			}
		}

		return nextGen;
	}

	public static <Cell> BufferedImage render(Tiling<Cell> tiling, Rectangle bounds, Collection<Cell> alive) {
		// Create a suitable paletted image
		int width = bounds.width;
		int height = bounds.height;
		byte[] data = new byte[width * height];
		int[] pal = new int[]{ GRIDCOL.getRGB(), DEADCOL.getRGB(), LIVECOL.getRGB() };
		ColorModel colourModel = new IndexColorModel(8, pal.length, pal, 0, false, -1, DataBuffer.TYPE_BYTE);
		DataBufferByte dbb = new DataBufferByte(data, width * height);
		WritableRaster raster = Raster.createPackedRaster(dbb, width, height, width, new int[]{0xff}, new Point(0, 0));
		BufferedImage img = new BufferedImage(colourModel, raster, true, null);
		Graphics g = img.createGraphics();

		// Render the tiling.
		// We assume that either one of the live cells or the "initial cell" is in bounds.
		Set<Cell> visited = new HashSet<Cell>();
		Set<Cell> unvisited = new HashSet<Cell>(alive);
		unvisited.add(tiling.initialCell());
		while (!unvisited.isEmpty()) {
			Iterator<Cell> it = unvisited.iterator();
			Cell current = it.next();
			it.remove();
			visited.add(current);

			Rectangle cellBounds = new Rectangle(-1, -1);
			int[][] cellVertices = tiling.bounds(current);
			int[] xs = cellVertices[0], ys = cellVertices[1];
			for (int i = 0; i < xs.length; i++) {
				cellBounds.add(xs[i], ys[i]);
				xs[i] -= bounds.x;
				ys[i] -= bounds.y;
			}

			if (!bounds.intersects(cellBounds)) continue;

			g.setColor(alive.contains(current) ? LIVECOL : DEADCOL);
			g.fillPolygon(xs, ys, xs.length);
			g.setColor(GRIDCOL);
			g.drawPolygon(xs, ys, xs.length);

			for (Cell neighbour : tiling.neighbours(current)) {
				if (!visited.contains(neighbour)) unvisited.add(neighbour);
			}
		}

		g.dispose();
		return img;
	}
}
