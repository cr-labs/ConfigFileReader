package com.challengeandresponse.configfilereader;

public class ElementNotFoundException extends ConfigFileReaderException {
	private static final long serialVersionUID = 1L;

	public ElementNotFoundException() {
		super();
	}

	public ElementNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public ElementNotFoundException(String message) {
		super(message);
	}

	public ElementNotFoundException(Throwable cause) {
		super(cause);
	}

}
