package net.xeona.sampled;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import net.xeona.sampled.value.ValueUpdatePredicate;
import net.xeona.series.index.SeriesIndex;
import net.xeona.series.index.SourceIndexRegistry;

public class SampledSeriesReconstructor<T extends SeriesSample<V, S>, V, S, I extends SeriesIndex<? super I>> {

	private final ValueUpdatePredicate<? super V> valueUpdatePredicate;
	private final SourceIndexRegistry<S, I> sourceIndexRegistry;
	private final SeriesIndex.Operations<I> seriesIndexOperations;

	private final NavigableMap<I, V> seriesValuesByIndex = new TreeMap<>();

	public SampledSeriesReconstructor(ValueUpdatePredicate<? super V> valueUpdatePredicate,
			SeriesIndex.Operations<I> seriesIndexOperations, SourceIndexRegistry<S, I> sourceIndexRegistry) {
		this.valueUpdatePredicate = requireNonNull(valueUpdatePredicate);
		this.seriesIndexOperations = requireNonNull(seriesIndexOperations);
		this.sourceIndexRegistry = requireNonNull(sourceIndexRegistry);
	}

	public void notifySample(SeriesSample<V, S> sample) {
		V sampleValue = sample.getValue();
		S sampleSource = sample.getSource();
		Optional<I> optSourceIndex = Optional.ofNullable(sourceIndexRegistry.getIndex(sampleSource)
				.orElseGet(() -> seriesValuesByIndex.isEmpty() ? null : seriesValuesByIndex.firstKey()));
		Optional<I> optNewSourceIndex;
		for (optNewSourceIndex = optSourceIndex; optNewSourceIndex.map(seriesValuesByIndex::higherEntry)
				.map(Map.Entry::getValue)
				.map(indexValue -> valueUpdatePredicate.isValueUpdated(indexValue, sampleValue))
				.orElse(false); optNewSourceIndex = Optional
						.ofNullable(seriesValuesByIndex.higherKey(optNewSourceIndex.get())));

		I newSourceIndex = optNewSourceIndex.orElseGet(() -> {
			I newSeriesIndex;
			if (seriesValuesByIndex.isEmpty()) {
				newSeriesIndex = seriesIndexOperations.initialValue();
			} else {
				newSeriesIndex = seriesIndexOperations.increment(seriesValuesByIndex.lastKey());
			}
			seriesValuesByIndex.put(newSeriesIndex, sampleValue);
			return newSeriesIndex;
		});
		sourceIndexRegistry.setIndex(sampleSource, newSourceIndex);
	}

}
