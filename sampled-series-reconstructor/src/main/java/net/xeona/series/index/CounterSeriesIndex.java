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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (count ^ (count >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CounterSeriesIndex other = (CounterSeriesIndex) obj;
		if (count != other.count)
			return false;
		return true;
	}

	public static class Operations implements SeriesIndex.Operations<CounterSeriesIndex> {
		
		private static final Operations INSTANCE = new Operations();
		
		Operations() {}

		@Override
		public CounterSeriesIndex initialValue() {
			return new CounterSeriesIndex(Long.MIN_VALUE);
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
