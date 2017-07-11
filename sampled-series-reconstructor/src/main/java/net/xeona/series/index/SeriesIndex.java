package net.xeona.series.index;

public interface SeriesIndex<I extends SeriesIndex<? super I>> extends Comparable<I> {

	interface Operations<I extends SeriesIndex<?>> {

		I initialValue();

		I increment(I index);

	}
}
