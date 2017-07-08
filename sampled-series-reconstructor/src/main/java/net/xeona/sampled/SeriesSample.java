package net.xeona.sampled;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class SeriesSample<V, S> {

	private final V value;
	private final S source;

	private SeriesSample(Builder<V, S> builder) {
		value = builder.getValue();
		source = builder.getSource();
	}

	public V getValue() {
		return value;
	}

	public S getSource() {
		return source;
	}

	@Override
	public String toString() {
		return "SeriesSample [value=" + value + ", source=" + source + "]";
	}

	public static class Builder<V, S> extends BuilderBase<V, S, SeriesSample<V, S>, Builder<V, S>> {

		protected Builder() {
			super(SeriesSample<V, S>::new);
		}

	}

	@SuppressWarnings("unchecked")
	protected static abstract class BuilderBase<V, S, T extends SeriesSample<V, S>, B extends BuilderBase<V, S, T, B>> {

		private final Predicate<? super B> builderValidPredicate;
		private final Function<? super B, ? extends T> constructorFunction;

		private Optional<V> value = Optional.empty();
		private Optional<S> source = Optional.empty();

		protected BuilderBase(Function<? super B, ? extends T> constructorFunction) {
			this(builder -> true, constructorFunction);
		}

		protected BuilderBase(Predicate<? super B> builderValidPredicate,
				Function<? super B, ? extends T> constructorFunction) {
			this.builderValidPredicate = requireNonNull(builderValidPredicate);
			this.constructorFunction = requireNonNull(constructorFunction);
		}

		public B setValue(V value) {
			this.value = Optional.of(value);
			return (B) this;
		}

		public B setSource(S source) {
			this.source = Optional.of(source);
			return (B) this;
		}

		public T build() {
			if (builderValidPredicate.test((B) this)) {
				return constructorFunction.apply((B) this);
			} else {
				throw new IllegalStateException("Builder failed validation: " + this);
			}
		}

		protected V getValue() {
			return getField("value", value);
		}

		protected S getSource() {
			return getField("source", source);
		}

		protected <R> R getField(String fieldName, Optional<R> fieldValue) {
			return fieldValue.orElseThrow(() -> new IllegalStateException(
					"Attempted to retrieve value for field " + fieldName + " before it was populated: " + this));
		}

	}

}
