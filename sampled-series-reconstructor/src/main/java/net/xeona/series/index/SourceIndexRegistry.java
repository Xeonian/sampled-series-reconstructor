package net.xeona.series.index;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;

public class SourceIndexRegistry<S, I extends SeriesIndex<? super I>> {

	private final SeriesIndex.Operations<I> seriesIndexOperations;

	private final Map<S, I> seriesIndexBySource = new HashMap<>();
	private final SortedMultiset<I> currentSourceIndeces = TreeMultiset.create();

	public SourceIndexRegistry(SeriesIndex.Operations<I> seriesIndexOperations) {
		this.seriesIndexOperations = requireNonNull(seriesIndexOperations);
	}

	public I getLeastCurrentIndex() {
		return Optional.ofNullable(currentSourceIndeces.firstEntry())
				.orElseThrow(() -> new IllegalStateException("No sources currently registered")).getElement();
	}

	public I getCurrentIndexForSource(S source) {
		return seriesIndexBySource.computeIfAbsent(source, absentSource -> {
			I initialSeriesIndex = seriesIndexOperations.initialValue();
			currentSourceIndeces.add(initialSeriesIndex);
			return initialSeriesIndex;
		});
	}

	public void setCurrentIndexForSource(S source, I index) {
		seriesIndexBySource.compute(source, (key, nullableCurrentIndex) -> {
			Optional<I> optCurrentIndex = Optional.ofNullable(nullableCurrentIndex);
			boolean indexUpdated = optCurrentIndex.map(index::isGreaterThan).orElse(true);
			I newIndex;
			if (indexUpdated) {
				optCurrentIndex.ifPresent(currentSourceIndeces::remove);
				currentSourceIndeces.add(index);
				newIndex = index;
			} else {
				newIndex = optCurrentIndex.get();
			}
			return newIndex;
		});
	}

	public void clearCurrentIndexForSource(S source) {
		I sourceIndex = seriesIndexBySource.remove(source);
		currentSourceIndeces.remove(sourceIndex);
	}

}
