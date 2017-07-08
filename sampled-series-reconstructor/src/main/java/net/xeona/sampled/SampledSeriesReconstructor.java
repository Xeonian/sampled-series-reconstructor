package net.xeona.sampled;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import net.xeona.sampled.value.ValueUpdatePredicate;
import net.xeona.series.index.SeriesIndex;

public class SampledSeriesReconstructor<T extends SeriesSample<V, S>, V, S, I extends SeriesIndex<? super I>> {

	private final ValueUpdatePredicate<? super V> valueUpdatePredicate;
	private final SeriesIndex.Operations<I> seriesIndexOperations;

	private final NavigableMap<I, V> seriesValuesByIndex = new TreeMap<>();
	private final Map<S, I> seriesIndexBySource = new HashMap<>();

	public SampledSeriesReconstructor(ValueUpdatePredicate<? super V> valueUpdatePredicate,
			SeriesIndex.Operations<I> seriesIndexOperations) {
		this.valueUpdatePredicate = requireNonNull(valueUpdatePredicate);
		this.seriesIndexOperations = requireNonNull(seriesIndexOperations);
	}

	public void notifySample(SeriesSample<V, S> sample) {
		V sampleValue = sample.getValue();
		S sampleSource = sample.getSource();
		// TODO: Currently throws NPE on initialisation. Just needs tidying up.
		Optional<I> optSourceIndex = Optional.of(Optional.ofNullable(seriesIndexBySource.get(sampleSource))
				.orElseGet(() -> seriesValuesByIndex.isEmpty() ? null : seriesValuesByIndex.firstKey()));
		Optional<I> optNewSourceIndex;
		for (optNewSourceIndex = optSourceIndex; optNewSourceIndex.map(seriesValuesByIndex::get)
				.map(indexValue -> valueUpdatePredicate.isValueUpdated(indexValue, sampleValue))
				.orElse(false); optNewSourceIndex = Optional
						.ofNullable(seriesValuesByIndex.higherKey(optNewSourceIndex.get())));

		I newSourceIndex = optNewSourceIndex.orElseGet(() -> {
			I newSeriesIndex;
			if (seriesValuesByIndex.isEmpty()) {
				newSeriesIndex = seriesIndexOperations.init();
			} else {
				newSeriesIndex = seriesIndexOperations.increment(seriesValuesByIndex.lastKey());
			}
			seriesValuesByIndex.put(newSeriesIndex, sampleValue);
			return newSeriesIndex;
		});
		seriesIndexBySource.put(sampleSource, newSourceIndex);
	}

}
