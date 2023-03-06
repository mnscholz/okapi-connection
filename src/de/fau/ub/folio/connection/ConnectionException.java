package de.fau.ub.folio.connection;

import java.io.IOException;

/**
 * @author Martin Scholz, Universitätsbibliothek Erlangen-Nürnberg
 *
 */
public class ConnectionException extends IOException {

	private static final long serialVersionUID = 3461620836994578126L;

	public ConnectionException() {
		// TODO Auto-generated constructor stub
	}

	public ConnectionException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public ConnectionException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public ConnectionException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

}
