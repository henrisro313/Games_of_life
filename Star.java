public class Star extends AbstractLattice {
	public Star() {
		super(30, 0, 15, 26, new int[][] {
				{-4,4,11,15,15,11,4,-4,-11,-15,-15,-11},
				{-4,4,0},
				{11,15,19},
				{15,11,19},
			}, new int[][] {
				{15,15,11,4,-4,-11,-15,-15,-11,-4,4,11},
				{11,4,11},
				{11,4,11},
				{-4,-11,-11},
			});
	}

	@Override
	public boolean isInterestingOscillationPeriod(int period) {
		return period != 2 && period != 3 && period != 6 && period != 8 && period != 9 && period != 18;
	}
}
