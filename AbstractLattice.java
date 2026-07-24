import java.awt.Point;
import java.util.*;

public abstract class AbstractLattice implements Tiling<AbstractLattice.LatticeCell> {
	// Use the idea of expansion and vertex mapping from my earlier aperiod tiling implementation.
	private Map<Point, Set<LatticeCell>> vertexNeighbourhood = new HashMap<Point, Set<LatticeCell>>();
	private int scale = -1;

	// Geometry
	private final int dx0, dy0, dx1, dy1;
	private final int[][] xs;
	private final int[][] ys;

	protected AbstractLattice(int dx0, int dy0, int dx1, int dy1, int[][] xs, int[][] ys) {
		this.dx0 = dx0;
		this.dy0 = dy0;
		this.dx1 = dx1;
		this.dy1 = dy1;
		// Assume sensible subclasses, so no need to clone the arrays to prevent modification.
		this.xs = xs;
		this.ys = ys;
	}

	private void expand() {
		scale++;
		// We want to enumerate all lattice cells whose extreme coordinate is +/- scale.
		// Corners:
		insertLatticeNeighbourhood(-scale, -scale);
		insertLatticeNeighbourhood(-scale, scale);
		insertLatticeNeighbourhood(scale, -scale);
		insertLatticeNeighbourhood(scale, scale);

		// Edges:
		for (int i = -scale + 1; i < scale; i++) {
			insertLatticeNeighbourhood(-scale, i);
			insertLatticeNeighbourhood(scale, i);
			insertLatticeNeighbourhood(i, -scale);
			insertLatticeNeighbourhood(i, scale);
		}
	}

	private void insertLatticeNeighbourhood(int x, int y) {
		for (int sub = 0; sub < xs.length; sub++) {
			LatticeCell cell = new LatticeCell(x, y, sub);
			int[][] bounds = bounds(cell);
			for (int i = 0; i < bounds[0].length; i++) {
				Point p = new Point(bounds[0][i], bounds[1][i]);

				Set<LatticeCell> adj = vertexNeighbourhood.get(p);
				if (adj == null) vertexNeighbourhood.put(p,  adj = new HashSet<LatticeCell>());
				adj.add(cell);
			}
		}
	}

	public Set<LatticeCell> neighbours(LatticeCell cell) {
		Set<LatticeCell> rv = new HashSet<LatticeCell>();
 
		// +1 because we will border cells from the next scale.
		int requiredScale = Math.max(Math.abs(cell.x), Math.abs(cell.y)) + 1;
		while (scale < requiredScale) expand();

		int[][] bounds = bounds(cell);
		for (int i = 0; i < bounds[0].length; i++) {
			Point p = new Point(bounds[0][i], bounds[1][i]);
			Set<LatticeCell> adj = vertexNeighbourhood.get(p);
			rv.addAll(adj);
		}
 
		rv.remove(cell);
		return rv;
	}
 
	public int[][] bounds(LatticeCell cell) {
		int[][] bounds = new int[2][];
		bounds[0] = xs[cell.sub].clone();
		bounds[1] = ys[cell.sub].clone();
		for (int i = 0; i < bounds[0].length; i++) {
			bounds[0][i] += cell.x * dx0 + cell.y * dx1;
			bounds[1][i] += cell.x * dy0 + cell.y * dy1;
		}

		return bounds;
	}
 
	public LatticeCell initialCell() {
		return new LatticeCell(0, 0, 0);
	}
 
	public abstract boolean isInterestingOscillationPeriod(int period);
 
	public Set<LatticeCell> parseCells(String[] data) {
		Set<LatticeCell> rv = new HashSet<LatticeCell>();
		if (data.length % 3 != 0) throw new IllegalArgumentException("Data should come in triples");
		for (int i = 0; i < data.length; i += 3) {
			if (data[i + 2].length() != 1) throw new IllegalArgumentException("Third data item should be a single letter");
			rv.add(new LatticeCell(Integer.parseInt(data[i]), Integer.parseInt(data[i + 1]), data[i + 2].charAt(0) - 'A'));
		}
		return rv;
	}
 
	public String format(Set<LatticeCell> cells) {
		StringBuilder sb = new StringBuilder();
		for (LatticeCell cell : cells) {
			if (sb.length() > 0) sb.append(' ');
			sb.append(cell.x).append(' ').append(cell.y).append(' ').append((char)(cell.sub + 'A'));
		}
 
		return sb.toString();
	}

	static class LatticeCell {
		public final int x, y, sub;

		LatticeCell(int x, int y, int sub) {
			this.x = x;
			this.y = y;
			this.sub = sub;
		}

		@Override
		public int hashCode() {
			return (x * 0x100025) + (y * 0x959) + sub;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof LatticeCell)) return false;
			LatticeCell other = (LatticeCell)obj;
			return x == other.x && y == other.y && sub == other.sub;
		}

		@Override
		public String toString() {
			return x + " " + y + " " + (char)('A' + sub);
		}
	}

	@Override
	public int distance(LatticeCell a, LatticeCell b) {
	    return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
	}
}
