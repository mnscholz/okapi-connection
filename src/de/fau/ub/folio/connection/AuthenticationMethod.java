package de.fau.ub.folio.connection;

/**
 * @author Martin Scholz, Universitätsbibliothek Erlangen-Nürnberg
 *
 */
public interface AuthenticationMethod {

	public String getAccessToken(OkapiConnection okapi) throws AuthenticationException;
	
}
