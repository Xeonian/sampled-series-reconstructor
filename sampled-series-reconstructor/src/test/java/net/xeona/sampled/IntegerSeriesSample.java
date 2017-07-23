package net.xeona.sampled;

public class IntegerSeriesSample implements SeriesSample<Integer, Object> {

	private final int value;
	private final Object source;

	public IntegerSeriesSample(int value, Object source) {
		this.value = value;
		this.source = source;
	}

	@Override
	public Integer getValue() {
		return value;
	}

	@Override
	public Object getSource() {
		return source;
	}

}
