package de.fau.ub.folio.connection;

/**
 * @author Martin Scholz, Universitätsbibliothek Erlangen-Nürnberg
 *
 */
public class FixedTokenProvider implements TokenProvider {

	private String token;
	
	public FixedTokenProvider(String accessToken) {
		super();
		this.token = accessToken;
	}

	@Override
	public String getAccessToken(OkapiConnection okapi) throws AuthenticationException {
		return this.token;
	}

}
