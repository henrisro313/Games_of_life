public class FourSixTwelve extends AbstractLattice {
	public FourSixTwelve() {
		super(33, 19, 33, -19, new int[][] {
				{-4,4,11,15,15,11,4,-4,-11,-15,-15,-11},
				{-4,4,4,-4},
				{4,11,18,18,11,4},
				{11,15,22,18},
				{15,15,22,29,29,22},
				{15,11,18,22},
			}, new int[][] {
				{15,15,11,4,-4,-11,-15,-15,-11,-4,4,11},
				{15,15,23,23},
				{15,11,15,23,27,23},
				{11,4,8,15},
				{4,-4,-8,-4,4,8},
				{-4,-11,-15,-8},
			});
	}

	@Override
	public boolean isInterestingOscillationPeriod(int period) {
		return period != 2 && period != 3 && period != 5 && period != 6 && period != 9 && period != 10 && period != 30;
	}
}
