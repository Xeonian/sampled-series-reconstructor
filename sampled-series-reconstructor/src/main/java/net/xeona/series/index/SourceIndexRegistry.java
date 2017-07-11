package net.xeona.series.index;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SourceIndexRegistry<S, I extends SeriesIndex<? super I>> {

	private final Map<S, I> seriesIndexBySource = new HashMap<>();

	public Optional<I> getIndex(S source) {
		return Optional.ofNullable(seriesIndexBySource.get(source));
	}

	public void setIndex(S source, I index) {
		seriesIndexBySource.merge(source, index,
				(firstIndex, secondIndex) -> firstIndex.compareTo(secondIndex) > 0 ? firstIndex : secondIndex);
	}

	public void clearIndex(S source) {
		seriesIndexBySource.remove(source);
	}

}
