package net.xeona.sampled.value;

public class EqualityValueUpdatePredicate implements ValueUpdatePredicate<Object> {

	private static final EqualityValueUpdatePredicate INSTANCE = new EqualityValueUpdatePredicate();

	EqualityValueUpdatePredicate() {}

	@Override
	public boolean isValueUpdated(Object previousValue, Object newValue) {
		return !newValue.equals(previousValue);
	}

	public static EqualityValueUpdatePredicate instance() {
		return INSTANCE;
	}

}
