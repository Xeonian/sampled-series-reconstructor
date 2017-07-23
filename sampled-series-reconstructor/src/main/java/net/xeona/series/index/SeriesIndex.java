package net.xeona.series.index;

public interface SeriesIndex<I extends SeriesIndex<? super I>> extends Comparable<I> {

	default boolean isGreaterThan(I other) {
		return compareTo(other) > 0;
	}

	interface Operations<I extends SeriesIndex<?>> {

		I initialValue();

		I increment(I index);

	}
}
