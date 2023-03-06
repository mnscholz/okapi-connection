package de.fau.ub.folio.connection.browserAuth;

import de.fau.ub.folio.connection.AuthenticationException;

/**
 * @author Martin Scholz, Universitätsbibliothek Erlangen-Nürnberg
 *
 */
public class TokenForwardingException extends AuthenticationException {

	private static final long serialVersionUID = 5121315139241084032L;

	public TokenForwardingException() {
		// TODO Auto-generated constructor stub
	}

	public TokenForwardingException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public TokenForwardingException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public TokenForwardingException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public TokenForwardingException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
