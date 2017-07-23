package net.xeona.sampled;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import net.xeona.sampled.value.ValueUpdatePredicate;
import net.xeona.series.index.SeriesIndex;
import net.xeona.series.index.SourceIndexRegistry;

/**
 * A class whose purpose is to reconstruct a step series from periodically polled samples.
 * <p />
 * As samples are offered to the reconstructor, it is decided whether or not the given sample represents a new data
 * point in the series. This is done according to the following logic:
 * 
 * <ol>
 * <li>Retrieve the current {@link SeriesIndex} for the sample source from the assigned {@link SourceIndexRegistry}</li>
 * <li>If this reconstructor does not yet have any data points for the series, skip to 5
 * <li>Select the index from which to begin the walk across the series so far. This is either:
 * <ul>
 * <li>The greatest index in the series which is less than or equal to the source index</li>
 * <li>The first index of the series, if no such candidate exists for the above</li>
 * </ul>
 * </li>
 * <li>From the selected index, walk the reconstructed series until:
 * <ul>
 * <li>An entry is found whose value is equivalent to that of the provided sample, according to the assigned
 * {@link ValueUpdatePredicate}</li>
 * <li>The end of the series is reached</li>
 * </ul>
 * </li>
 * <li>Interpret the outcome of the walk as follows:
 * <ul>
 * <li>If the series was empty, consider the provided sample as the initialising data point and add it to our internal
 * view of the series. Do not adjust the index of the source.</li>
 * <li>If the sample's value matched against an existing data point in the series, we consider the index of this data
 * point to be the new index of our source</li>
 * <li>If no existing data point was matched, and the source index is less than the index of the first data point of the
 * series, then no change is made - this implies that the sample has been provided by a new source, and it is possible
 * that the sample represents a data point that has already been cleared from this reconstructor</li>
 * <li>Otherwise, consider the sample to represent a new data point. If the source index is less than the index of the
 * latest data point, increment the latest data point's index and consider that the source's new index.</li>
 * </ul>
 * </li>
 * <li>Notify the <code>SourceIndexRegistry</code> of the new source index</li>
 * <li>Remove all data points whose index precedes the least source index stored by the
 * <code>SourceIndexRegistry</code>, excluding the latest series data point if necessary</li>
 * <li>Return to the caller whether the given sample represented a new data point</li>
 * </ol>
 * 
 * Be aware that this class is not responsive to updates to the index of sources by other components. It is advised that
 * the reconstructor be invoked with the most recently reported sample from a source if the index of the source of that
 * sample has been updated.
 * <p />
 * The complexity of the algorithm is <code>O(n)</code> in the number of data points in the series in both time and
 * space. However, if all available sources are frequently providing samples with little variance between the rate at
 * which they observe updates, the clearing step results in the algorithm approximating <code>O(1)</code> complexity in
 * both time and space, as only the most recent data point needs to be stored and compared.
 * 
 * @author Wesley Marsh
 *
 * @param <V>
 *            The type of values to be received as part of samples from the series to be reconstructed
 * @param <S>
 *            The type of source identifiers to be received as part of the samples
 * @param <I>
 *            The type of <code>SeriesIndex</code> to be used by this reconstructor to compare the relative freshness of
 *            both data points and sources
 */
public class SampledStepSeriesReconstructor<V, S, I extends SeriesIndex<? super I>> {

	private final ValueUpdatePredicate<? super V> valueUpdatePredicate;
	private final SourceIndexRegistry<? super S, I> sourceIndexRegistry;
	private final SeriesIndex.Operations<I> seriesIndexOperations;

	private final NavigableMap<I, V> seriesValuesByIndex = new TreeMap<>();

	public SampledStepSeriesReconstructor(ValueUpdatePredicate<? super V> valueUpdatePredicate,
			SeriesIndex.Operations<I> seriesIndexOperations, SourceIndexRegistry<? super S, I> sourceIndexRegistry) {
		this.valueUpdatePredicate = requireNonNull(valueUpdatePredicate);
		this.seriesIndexOperations = requireNonNull(seriesIndexOperations);
		this.sourceIndexRegistry = requireNonNull(sourceIndexRegistry);
	}

	/**
	 * Offer a new sample to the series being reconstructed by this instance and decide whether the sample represents a
	 * new data point
	 * 
	 * @param sample
	 *            The sample to test
	 * @return Whether the new sample has been accepted into the series
	 */
	public boolean notifySample(SeriesSample<V, S> sample) {
		V sampleValue = sample.getValue();
		S sampleSource = sample.getSource();

		I sourceIndex = sourceIndexRegistry.getCurrentIndexForSource(sampleSource);

		SeriesWalkResult<I> seriesWalkResult = findEarliestMatchingSeriesIndex(sampleValue, sourceIndex);
		SeriesWalkInterpretation<I> seriesWalkInterpretation = interpretSeriesWalkResult(sourceIndex, seriesWalkResult);

		boolean isNewValue = seriesWalkInterpretation.isNewValue();
		I newSourceIndex = seriesWalkInterpretation.getNewSourceIndex();
		if (isNewValue) {
			seriesValuesByIndex.put(newSourceIndex, sampleValue);
		}
		sourceIndexRegistry.setCurrentIndexForSource(sampleSource, newSourceIndex);
		clearOldDataPoints();

		return isNewValue;
	}

	private SeriesWalkResult<I> findEarliestMatchingSeriesIndex(V sampleValue, I sourceIndex) {
		SeriesWalkResult<I> seriesWalkResult;
		if (seriesValuesByIndex.isEmpty()) {
			seriesWalkResult = SeriesWalkResult.seriesEmptyResult();
		} else {
			I walkStartIndex = Optional.ofNullable(seriesValuesByIndex.floorKey(sourceIndex))
					.orElseGet(seriesValuesByIndex::firstKey);
			NavigableMap<I, V> seriesSubsetToWalk = seriesValuesByIndex.tailMap(walkStartIndex, true);
			Optional<I> optWalkEndIndex = Optional.empty();
			for (Iterator<Map.Entry<I, V>> it = seriesSubsetToWalk.entrySet().iterator(); !optWalkEndIndex.isPresent()
					&& it.hasNext();) {
				Map.Entry<I, V> entry = it.next();
				if (valueUpdatePredicate.isValueEquivalent(entry.getValue(), sampleValue)) {
					optWalkEndIndex = Optional.of(entry.getKey());
				}
			}
			seriesWalkResult = optWalkEndIndex.map(SeriesWalkResult::matchedExistingIndexResult)
					.orElseGet(SeriesWalkResult::noIndecesMatchedResult);
		}
		return seriesWalkResult;
	}

	private SeriesWalkInterpretation<I> interpretSeriesWalkResult(I sourceIndex, SeriesWalkResult<I> seriesWalkResult) {
		SeriesWalkInterpretation<I> seriesWalkInterpretation;
		SeriesWalkResult.Type seriesWalkResultType = seriesWalkResult.getType();
		switch (seriesWalkResultType) {
		case SERIES_EMPTY:
			seriesWalkInterpretation = SeriesWalkInterpretation.seriesIntroductionResult(sourceIndex);
			break;
		case MATCHED_EXISTING_INDEX:
			seriesWalkInterpretation = SeriesWalkInterpretation
					.matchedExistingIndexResult(seriesWalkResult.getMatchedSeriesIndex());
			break;
		case NO_INDECES_MATCHED:
			if (seriesValuesByIndex.firstKey().isGreaterThan(sourceIndex)) {
				seriesWalkInterpretation = SeriesWalkInterpretation.failedSeriesAlignmentResult(sourceIndex);
			} else {
				I latestSeriesIndex = seriesValuesByIndex.lastKey();
				I newDataPointIndex = sourceIndex.isGreaterThan(latestSeriesIndex) ? sourceIndex
						: seriesIndexOperations.increment(latestSeriesIndex);
				seriesWalkInterpretation = SeriesWalkInterpretation.newDataPointResult(newDataPointIndex);
			}
			break;
		default:
			throw new AssertionError("Unexpected value for enum type: " + seriesWalkResultType);
		}
		return seriesWalkInterpretation;
	}

	private void clearOldDataPoints() {
		I leastSourceIndex = sourceIndexRegistry.getLeastCurrentIndex();
		I greatestSeriesIndex = seriesValuesByIndex.lastKey();
		I seriesOldDataThreshold = min(leastSourceIndex, greatestSeriesIndex);
		seriesValuesByIndex.headMap(seriesOldDataThreshold).clear();
	}

	private static <C extends Comparable<? super C>> C min(C first, C second) {
		return first.compareTo(second) < 0 ? first : second;
	}

	@SuppressWarnings({ "unchecked" })
	private static class SeriesWalkResult<I extends SeriesIndex<?>> {

		private static final SeriesWalkResult<?> SERIES_EMPTY_RESULT = new SeriesWalkResult<>(Type.SERIES_EMPTY,
				Optional.empty());
		private static final SeriesWalkResult<?> NO_INDECES_MATCHED_RESULT = new SeriesWalkResult<>(
				Type.NO_INDECES_MATCHED, Optional.empty());

		private final Type type;
		private final Optional<I> matchedSeriesIndex;

		private SeriesWalkResult(Type type, Optional<I> matchedSeriesIndex) {
			this.type = type;
			this.matchedSeriesIndex = matchedSeriesIndex;
		}

		public Type getType() {
			return type;
		}

		public I getMatchedSeriesIndex() {
			return matchedSeriesIndex
					.orElseThrow(() -> new AssertionError("No matched series index for result of type " + type));
		}

		public static <I extends SeriesIndex<?>> SeriesWalkResult<I> seriesEmptyResult() {
			return (SeriesWalkResult<I>) SERIES_EMPTY_RESULT;
		}

		public static <I extends SeriesIndex<?>> SeriesWalkResult<I> matchedExistingIndexResult(I matchedIndex) {
			return new SeriesWalkResult<>(Type.MATCHED_EXISTING_INDEX, Optional.of(matchedIndex));
		}

		public static <I extends SeriesIndex<?>> SeriesWalkResult<I> noIndecesMatchedResult() {
			return (SeriesWalkResult<I>) NO_INDECES_MATCHED_RESULT;
		}

		private enum Type {
			SERIES_EMPTY, MATCHED_EXISTING_INDEX, NO_INDECES_MATCHED
		}

	}

	private static class SeriesWalkInterpretation<I extends SeriesIndex<?>> {

		private final boolean newValue;
		private final I newSourceIndex;

		private SeriesWalkInterpretation(boolean newValue, I newSourceIndex) {
			this.newValue = newValue;
			this.newSourceIndex = newSourceIndex;
		}

		public boolean isNewValue() {
			return newValue;
		}

		public I getNewSourceIndex() {
			return newSourceIndex;
		}

		public static <I extends SeriesIndex<?>> SeriesWalkInterpretation<I> seriesIntroductionResult(I sourceIndex) {
			return new SeriesWalkInterpretation<>(true, sourceIndex);
		}

		public static <I extends SeriesIndex<?>> SeriesWalkInterpretation<I> matchedExistingIndexResult(
				I matchedIndex) {
			return new SeriesWalkInterpretation<>(false, matchedIndex);
		}

		public static <I extends SeriesIndex<?>> SeriesWalkInterpretation<I> newDataPointResult(I dataPointIndex) {
			return new SeriesWalkInterpretation<>(true, dataPointIndex);
		}

		public static <I extends SeriesIndex<?>> SeriesWalkInterpretation<I> failedSeriesAlignmentResult(
				I sourceIndex) {
			return new SeriesWalkInterpretation<I>(false, sourceIndex);
		}

	}

}
