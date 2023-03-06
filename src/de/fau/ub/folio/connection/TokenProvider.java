package de.fau.ub.folio.connection;

/**Provides an access token for an Okapi connection.
 * An access token is used to authenticate and authorize a user agent.
 * 
 * @author Martin Scholz, Universitätsbibliothek Erlangen-Nürnberg
 *
 */
public interface TokenProvider {

	/** Returns an access token for the given Okapi connection.
	 * 
	 *  This method should always return a valid access token, ie. that has not expired.
	 *  The AccessTokenProvider implementation should make sure that
	 *  the returned access token is still valid.
	 *  
	 * @param okapi The okapi connection for which an access token shall be returned 
	 * @return the access token as a string
	 */
	public String getAccessToken(OkapiConnection okapi) throws AuthenticationException;
	
}
