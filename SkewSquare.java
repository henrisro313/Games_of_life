public class SkewSquare extends AbstractLattice {
	public SkewSquare() {
		super(-4,15,15,4, new int[][] {
				{-4,-4,4,4},
				{0,4,11,7},
				{-4,4,0},
				{4,4,11},
				{4,11,11},
				{7,11,15},
			}, new int[][] {
				{4,-4,-4,4},
				{11,4,8,15},
				{4,4,11},
				{4,-4,0},
				{4,0,8},
				{15,8,15},
			});
	}

	@Override
	public boolean isInterestingOscillationPeriod(int period) {
		return period != 2;
	}
}