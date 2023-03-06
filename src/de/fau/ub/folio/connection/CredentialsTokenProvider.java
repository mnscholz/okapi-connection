package de.fau.ub.folio.connection;

/**
 * @author Martin Scholz, Universitätsbibliothek Erlangen-Nürnberg
 *
 */
public abstract class CredentialsTokenProvider implements TokenProvider {

	/**Represents a set of credentials to log in to Okapi.
	 * 
	 * Note that in order to log in to credentials must contain either a user 
	 * name or a user id and the corresponding password.
	 *
	 * Note also that the password is passed as a char array in order to avoid
	 * that the password lingers in the JVM string pool and gets leaked.
	 * Implementations should provide a means to safely erase the internal
	 * password data. See also erase() method.
	 *
	 * @author Martin Scholz, Universitätsbibliothek Erlangen-Nürnberg
	 *
	 */
	protected interface Credentials {
		public String username();
		public String userId();
		/**Returns the password.
		 * 
		 * As Strings are handled in a pool by JVM and may internally live 
		 * indefinitely, the password is kept only as a char array. This way 
		 * it can be safely discarded by overwriting the array.
		 * 
		 * @return the password as a char array
		 */
		public char[] password();
		/**This method should erase the password, e.g. by filling the 
		 * underlying char array with zeros. It is invoked when the calling 
		 * method no longer needs the credentials.
		 */
		public void erase();
	}
	
	@Override
	public String getAccessToken(OkapiConnection okapi) throws AuthenticationException {
		Credentials cred = getCredentials(okapi);
		String token = okapi.loginForToken(cred.username(), cred.userId(), cred.password());
		cred.erase();
		return token;		
	}
	
	/**Returns the credentials needed to log in to Okapi.
	 * 
	 * Note that in order to log in to credentials must contain either a user 
	 * name or a user id and the corresponding password.
	 * 
	 * @param the okapi that the credentials are for
	 * @return the credentials
	 */
	protected abstract Credentials getCredentials(OkapiConnection okapi);

}