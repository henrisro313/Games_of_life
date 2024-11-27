public class FourEight extends AbstractLattice {
	public FourEight() {
		super(7, 7, 7, -7, new int[][] {
				{-2, 2, 5, 5, 2, -2, -5, -5},
				{-2, 2, 2, -2},
			}, new int[][] {
				{5, 5, 2, -2, -5, -5, -2, 2},
				{5, 5, 9, 9},
			});
	}

	@Override
	public boolean isInterestingOscillationPeriod(int period) {
		return period != 2 && period != 3 && period != 4 && period != 6 && period != 7 && period != 10 && period != 12 && period != 14 && period != 20 && period != 30 && period != 60;
	}
}
