package de.fau.ub.folio.connection;

/**
 * @author Martin Scholz, Universitätsbibliothek Erlangen-Nürnberg
 *
 */
public abstract class CredentialsAuthenticationMethod implements AuthenticationMethod {

	protected interface Credentials {
		public String username();
		public String userId();
		public char[] password();
		public void erase();
	}

	public String getAccessToken(OkapiConnection okapi) throws AuthenticationException {
		Credentials cred = getCredentials();
		String token = okapi.loginForToken(cred.username(), cred.userId(), cred.password());
		cred.erase();
		return token;		
	}
	
	protected abstract Credentials getCredentials();

}