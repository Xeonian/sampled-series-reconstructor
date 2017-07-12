package net.xeona.series.index;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

public class SourceIndexRegistry<S, I extends SeriesIndex<? super I>> {

	private final SeriesIndex.Operations<I> seriesIndexOperations;

	private final Map<S, I> seriesIndexBySource = new HashMap<>();

	public SourceIndexRegistry(SeriesIndex.Operations<I> seriesIndexOperations) {
		this.seriesIndexOperations = requireNonNull(seriesIndexOperations);
	}

	public I getIndex(S source) {
		return seriesIndexBySource.computeIfAbsent(source, absentSource -> seriesIndexOperations.initialValue());
	}

	public void setIndex(S source, I index) {
		seriesIndexBySource.merge(source, index,
				(firstIndex, secondIndex) -> firstIndex.compareTo(secondIndex) > 0 ? firstIndex : secondIndex);
	}

	public void clearIndex(S source) {
		seriesIndexBySource.remove(source);
	}

}
