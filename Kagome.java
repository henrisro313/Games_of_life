public class Kagome extends AbstractLattice {
	public Kagome() {
		super(8,14,16,0, new int[][] {
				{-4,4,8,4,-4,-8},
				{-4,4,0},
				{4,8,12},
			}, new int[][] {
				{7,7,0,-7,-7,0},
				{7,7,14},
				{7,0,7},
			});
	}

	@Override
	public boolean isInterestingOscillationPeriod(int period) {
		return period != 2;
	}
}
