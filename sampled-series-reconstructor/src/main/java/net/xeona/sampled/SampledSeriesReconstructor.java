package net.xeona.sampled;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import net.xeona.sampled.value.ValueUpdatePredicate;
import net.xeona.series.index.SeriesIndex;
import net.xeona.series.index.SourceIndexRegistry;

public class SampledSeriesReconstructor<V, S, I extends SeriesIndex<? super I>> {

	private final ValueUpdatePredicate<? super V> valueUpdatePredicate;
	private final SourceIndexRegistry<? super S, I> sourceIndexRegistry;
	private final SeriesIndex.Operations<I> seriesIndexOperations;

	private final NavigableMap<I, V> seriesValuesByIndex = new TreeMap<>();

	public SampledSeriesReconstructor(ValueUpdatePredicate<? super V> valueUpdatePredicate,
			SeriesIndex.Operations<I> seriesIndexOperations, SourceIndexRegistry<? super S, I> sourceIndexRegistry) {
		this.valueUpdatePredicate = requireNonNull(valueUpdatePredicate);
		this.seriesIndexOperations = requireNonNull(seriesIndexOperations);
		this.sourceIndexRegistry = requireNonNull(sourceIndexRegistry);
	}

	public void notifySample(SeriesSample<V, S> sample) {
		V sampleValue = sample.getValue();
		S sampleSource = sample.getSource();

		I sourceIndex = sourceIndexRegistry.getIndex(sampleSource);

		Optional<I> optNewSourceIndex;
		for (optNewSourceIndex = Optional.ofNullable(seriesValuesByIndex.floorKey(sourceIndex)).map(Optional::of)
				.orElseGet(() -> Optional.ofNullable(seriesValuesByIndex.firstEntry())
						.map(Map.Entry::getKey)); optNewSourceIndex
								.map(newSourceIndex -> valueUpdatePredicate
										.isValueUpdated(seriesValuesByIndex.get(newSourceIndex), sampleValue))
								.orElse(false); optNewSourceIndex = Optional
										.ofNullable(seriesValuesByIndex.higherKey(optNewSourceIndex.get())));

		I newSourceIndex = optNewSourceIndex.orElseGet(() -> {
			I newSeriesIndex = Optional.ofNullable(seriesValuesByIndex.lastEntry()).map(Map.Entry::getKey)
					.filter(latestSeriesIndex -> latestSeriesIndex.compareTo(sourceIndex) > 0)
					.map(seriesIndexOperations::increment).orElse(sourceIndex);
			seriesValuesByIndex.put(newSeriesIndex, sampleValue);
			return newSeriesIndex;
		});
		sourceIndexRegistry.setIndex(sampleSource, newSourceIndex);
	}

}
