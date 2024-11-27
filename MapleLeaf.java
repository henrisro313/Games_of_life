public class MapleLeaf extends AbstractLattice {
	public MapleLeaf() {
		super(20,7,16,-14, new int[][] {
				{-4,4,8,4,-4,-8},
				{-4,4,0},
				{4,8,12},
				{8,4,12},
				{0,4,8},
				{8,12,16},
				{8,4,12},
				{8,16,12},
				{8,12,16},
			}, new int[][] {
				{7,7,0,-7,-7,0},
				{7,7,14},
				{7,0,7},
				{0,-7,-7},
				{14,7,14},
				{14,7,14},
				{14,7,7},
				{0,0,7},
				{0,-7,0},
			});
	}

	@Override
	public boolean isInterestingOscillationPeriod(int period) {
		return period != 2 && period != 3 && period != 20 && period != 24;
	}
}
