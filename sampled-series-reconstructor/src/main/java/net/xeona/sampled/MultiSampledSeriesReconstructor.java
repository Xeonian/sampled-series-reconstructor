package net.xeona.sampled;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.xeona.series.index.SeriesIndex;
import net.xeona.series.index.SourceIndexRegistry;

public class MultiSampledSeriesReconstructor<K, V, S, I extends SeriesIndex<? super I>> {

	private final SourceIndexRegistry<S, I> sourceIndexRegistry;
	private final SampledSeriesReconstructorFactory<V, S, I> seriesReconstructorFactory;

	private final Map<K, SampledSeriesReconstructor<V, S, I>> seriesReconstructorsByKey = new HashMap<>();

	public MultiSampledSeriesReconstructor(SourceIndexRegistry<S, I> sourceIndexRegistry,
			SampledSeriesReconstructorFactory<V, S, I> seriesReconstructorFactory) {
		this.sourceIndexRegistry = requireNonNull(sourceIndexRegistry);
		this.seriesReconstructorFactory = requireNonNull(seriesReconstructorFactory);
	}

	public void notifySamples(Collection<? extends KeyedSeriesSample<K, V, S>> samples) {
		Map<S, List<KeyedSeriesSample<K, V, S>>> samplesBySource = samples.stream()
				.collect(groupingBy(KeyedSeriesSample::getSource));
		for (Map.Entry<S, List<KeyedSeriesSample<K, V, S>>> entry : samplesBySource.entrySet()) {
			S source = entry.getKey();

			I initialSourceIndex;
			I subsequentSourceIndex;
			do {
				initialSourceIndex = sourceIndexRegistry.getIndex(source);
				for (KeyedSeriesSample<K, V, S> sample : entry.getValue()) {
					K sampleSeriesKey = sample.getKey();
					SampledSeriesReconstructor<V, S, I> seriesReconstructor = seriesReconstructorsByKey.computeIfAbsent(
							sampleSeriesKey, absentSeriesKey -> seriesReconstructorFactory.build(sourceIndexRegistry));
					seriesReconstructor.notifySample(sample);
				}
				subsequentSourceIndex = sourceIndexRegistry.getIndex(source);
			} while (subsequentSourceIndex.compareTo(initialSourceIndex) > 0);
		}
	}

	public interface SampledSeriesReconstructorFactory<V, S, I extends SeriesIndex<? super I>> {

		SampledSeriesReconstructor<V, S, I> build(SourceIndexRegistry<S, I> sourceIndexRegistry);

	}

}
