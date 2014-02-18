package edu.arizona.biosemantics.matrixmerge;

public class FromToValue {

	private String fromValue;
	private String toValue;
	private String desiredValue;
	
	public FromToValue(String fromValue, String toValue) {
		this.fromValue = fromValue;
		this.toValue = toValue;
	}

	public String getFromValue() {
		return fromValue;
	}

	public void setFromValue(String fromValue) {
		this.fromValue = fromValue;
	}

	public String getToValue() {
		return toValue;
	}

	public void setToValue(String toValue) {
		this.toValue = toValue;
	}

	public String getDesiredValue() {
		return desiredValue;
	}

	public void setDesiredValue(String desiredValue) {
		this.desiredValue = desiredValue;
	}
	
	public String toString() {
		return fromValue + " -> " + toValue;
	}
	
}
