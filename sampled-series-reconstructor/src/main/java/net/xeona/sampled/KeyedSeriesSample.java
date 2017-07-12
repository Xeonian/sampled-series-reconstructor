package net.xeona.sampled;

public interface KeyedSeriesSample<K, V, S> extends SeriesSample<V, S> {

	K getKey();

}
