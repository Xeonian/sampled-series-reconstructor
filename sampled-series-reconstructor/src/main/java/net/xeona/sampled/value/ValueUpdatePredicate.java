package net.xeona.sampled.value;

public interface ValueUpdatePredicate<V> {

	boolean isValueUpdated(V previousValue, V newValue);
	
}
