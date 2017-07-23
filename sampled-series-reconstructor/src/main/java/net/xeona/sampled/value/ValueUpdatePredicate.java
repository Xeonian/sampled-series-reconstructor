package net.xeona.sampled.value;

public interface ValueUpdatePredicate<V> {

	boolean isValueEquivalent(V previousValue, V newValue);
	
}
