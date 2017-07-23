package net.xeona.series.index;

import com.google.common.base.MoreObjects;

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
		return Long.hashCode(count);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof CounterSeriesIndex && ((CounterSeriesIndex) other).count == count;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("count", count).toString();
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
