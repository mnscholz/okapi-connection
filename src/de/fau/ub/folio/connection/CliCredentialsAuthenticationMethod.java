package de.fau.ub.folio.connection;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;


/**
 * @author Martin Scholz, Universitätsbibliothek Erlangen-Nürnberg
 *
 */
public class CliCredentialsAuthenticationMethod extends CredentialsAuthenticationMethod {
	
	public CliCredentialsAuthenticationMethod() {
		super();
	}
	
	protected Credentials getCredentials() throws AuthenticationException {
		Console c = System.console();
		String user;
		char[] password;
		if (c != null) {
			user = c.readLine("User:");
			password = c.readPassword("%s's password:", user);
		}
		else {
			try {
				BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
				System.out.println("User:");
				user = r.readLine();
				System.out.println("%s's password:".formatted(user));
				password = r.readLine().toCharArray();
			} catch (IOException e) {
				throw new AuthenticationException("cannot read credentials from stdin", e);
			}
		}
		return new Credentials() {
			public String username() { return user; }
			public String userId() { return null; }
			public char[] password() { return password; }
			public void erase() { Arrays.fill(password, '0'); }
		};
	}


}
