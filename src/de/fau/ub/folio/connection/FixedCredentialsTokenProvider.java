package de.fau.ub.folio.connection;

/**
 * @author Martin Scholz, Universitätsbibliothek Erlangen-Nürnberg
 *
 */
public class FixedCredentialsTokenProvider extends CredentialsTokenProvider {

	private String username;
	private String userId;
	private char[] password;
	
	public FixedCredentialsTokenProvider() {
		super();
	}

	public FixedCredentialsTokenProvider(String username, String userId, char[] password) {
		super();
		this.username = username;
		this.userId = userId;
		this.password = password;
	}

	@Override
	protected Credentials getCredentials(OkapiConnection okapi) {
		return new Credentials() {
			public String username() { return username; }
			public String userId() { return userId; }
			public char[] password() { return password; }
			public void erase() { 
				// do nothing, pw is stored in this object
				// to obtain more security don't use this class
			}
		};
	}

}
