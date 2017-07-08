package net.xeona.series.index;

public interface SeriesIndex<I extends SeriesIndex<? super I>> extends Comparable<I> {
	
	interface Operations<I extends SeriesIndex<? super I>> {
		
		I init();
		
		I increment(I index);
		
	}
}
