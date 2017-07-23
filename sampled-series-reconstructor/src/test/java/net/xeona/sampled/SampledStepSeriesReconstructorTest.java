package net.xeona.sampled;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import net.xeona.sampled.value.EqualityValueUpdatePredicate;
import net.xeona.series.index.CounterSeriesIndex;
import net.xeona.series.index.SourceIndexRegistry;

public class SampledStepSeriesReconstructorTest {

	@Test
	public void acceptsFirstProvidedValue() {
		SourceIndexRegistry<Object, CounterSeriesIndex> sourceIndexRegistry = new SourceIndexRegistry<>(
				CounterSeriesIndex.Operations.instance());
		SampledStepSeriesReconstructor<Integer, Object, CounterSeriesIndex> subjectUnderTest = new SampledStepSeriesReconstructor<>(
				EqualityValueUpdatePredicate.instance(), CounterSeriesIndex.Operations.instance(), sourceIndexRegistry);

		Object source = new Object();
		IntegerSeriesSample sample = new IntegerSeriesSample(0, source);

		assertThat(subjectUnderTest.notifySample(sample), is(true));
	}

	@Test
	public void firstSampleInitialisesSourceIndexRegistry() {
		SourceIndexRegistry<Object, CounterSeriesIndex> sourceIndexRegistry = new SourceIndexRegistry<>(
				CounterSeriesIndex.Operations.instance());
		SampledStepSeriesReconstructor<Integer, Object, CounterSeriesIndex> subjectUnderTest = new SampledStepSeriesReconstructor<>(
				EqualityValueUpdatePredicate.instance(), CounterSeriesIndex.Operations.instance(), sourceIndexRegistry);

		Object source = new Object();
		IntegerSeriesSample sample = new IntegerSeriesSample(0, source);

		subjectUnderTest.notifySample(sample);

		assertThat(sourceIndexRegistry.getCurrentIndexForSource(source),
				is(CounterSeriesIndex.Operations.instance().initialValue()));
	}

	@Test
	public void rejectsRepeatedValueFromDifferentSource() {
		SourceIndexRegistry<Object, CounterSeriesIndex> sourceIndexRegistry = new SourceIndexRegistry<>(
				CounterSeriesIndex.Operations.instance());
		SampledStepSeriesReconstructor<Integer, Object, CounterSeriesIndex> subjectUnderTest = new SampledStepSeriesReconstructor<>(
				EqualityValueUpdatePredicate.instance(), CounterSeriesIndex.Operations.instance(), sourceIndexRegistry);

		Object firstSource = new Object();
		IntegerSeriesSample firstSample = new IntegerSeriesSample(0, firstSource);

		subjectUnderTest.notifySample(firstSample);

		Object secondSource = new Object();
		IntegerSeriesSample secondSample = new IntegerSeriesSample(0, secondSource);

		assertThat(subjectUnderTest.notifySample(secondSample), is(false));
	}

	@Test
	public void acceptsUpdatedValueFromSameSource() {
		SourceIndexRegistry<Object, CounterSeriesIndex> sourceIndexRegistry = new SourceIndexRegistry<>(
				CounterSeriesIndex.Operations.instance());
		SampledStepSeriesReconstructor<Integer, Object, CounterSeriesIndex> subjectUnderTest = new SampledStepSeriesReconstructor<>(
				EqualityValueUpdatePredicate.instance(), CounterSeriesIndex.Operations.instance(), sourceIndexRegistry);

		Object source = new Object();
		IntegerSeriesSample firstSample = new IntegerSeriesSample(0, source);
		subjectUnderTest.notifySample(firstSample);

		IntegerSeriesSample secondSample = new IntegerSeriesSample(1, source);
		assertThat(subjectUnderTest.notifySample(secondSample), is(true));
	}

}
