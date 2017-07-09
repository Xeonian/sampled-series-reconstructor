package net.xeona.series.index;

public class CounterSeriesIndex implements SeriesIndex<CounterSeriesIndex> {

	private final long count;

	CounterSeriesIndex(long count) {
		this.count = count;
	}

	@Override
	public int compareTo(CounterSeriesIndex other) {
		return Long.compare(count, other.count);
	}

	public static class Operations implements SeriesIndex.Operations<CounterSeriesIndex> {
		
		private static final Operations INSTANCE = new Operations();
		
		Operations() {}

		@Override
		public CounterSeriesIndex init() {
			return new CounterSeriesIndex(0);
		}

		@Override
		public CounterSeriesIndex increment(CounterSeriesIndex index) {
			return new CounterSeriesIndex(index.count + 1);
		}
		
		public static Operations instance() {
			return INSTANCE;
		}

	}

}
