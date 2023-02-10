package de.fau.ub.folio.connection;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import org.json.JSONObject;

/**
 * @author Martin Scholz, Universitätsbibliothek Erlangen-Nürnberg
 *
 */
public final class OkapiConnection {

	private static final String URL_LOGIN = "authn/login";
	
	private URI uri;
	private String tenant;
	private AuthenticationMethod authMethod;
	private String token = null;
	private Logger logger;
	
	public interface Response {
		public int httpCode();
		public String body();
	}
	
	public OkapiConnection(URI uri, String tenant, AuthenticationMethod authMethod) {
		this(uri, tenant, authMethod, null);
	}
	
	public OkapiConnection(URI uri, String tenant, AuthenticationMethod authMethod, Logger logger) {
		super();
		this.uri = uri;
		this.tenant = tenant;
		this.authMethod = authMethod;
		this.logger = logger != null ? logger : System.getLogger(this.getClass().getCanonicalName());
	}

	
	public Response delete (String path, Map<String, String> parameters, Map<String, String> customHeaders) {
		return doRequest("DELETE", path, parameters, null, customHeaders, "");
	}

	public Response get (String path, Map<String, String> parameters, Map<String, String> customHeaders) {
		return doRequest("GET", path, parameters, null, customHeaders, "");
	}

	public JSONObject postJSON (String path, Map<String, String> customHeaders, JSONObject body) {
		return new JSONObject(post(path, "application/json", customHeaders, body.toString()).body());
	}
	
	public Response put (String path, String contentType, Map<String, String> customHeaders, String body) {
		return doRequest("PUT", path, null, contentType, customHeaders, body);
	}

	public JSONObject putJSON (String path, Map<String, String> customHeaders, JSONObject body) {
		return new JSONObject(put(path, "application/json", customHeaders, body.toString()).body());
	}
	
	public Response post (String path, String contentType, Map<String, String> customHeaders, String body) {
		return doRequest("POST", path, null, contentType, customHeaders, body);
	}

	private Response doRequest (String method, String path, Map<String, String> parameters, String contentType, Map<String, String> customHeaders, String body) {
		int retries = 1;
		while (retries >= 0) {
			retries--;
			try {
				// prepare connection to okapi and send request
				URL url;
				if (parameters == null || parameters.isEmpty()) { 
					url = this.uri.resolve(path).toURL();
				}
				else {
					StringBuilder tempUrl = new StringBuilder(this.uri.toString());
					tempUrl.append(path);
					StringBuffer delim = new StringBuffer(tempUrl.indexOf("?") == -1 ? '?' : '&');
					parameters.forEach((key, value) -> { 
						tempUrl.append(delim.charAt(0)).append(key).append('=').append(value);
						delim.insert(0, '&');
					});
					url = new URL(tempUrl.toString());
				}
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod(method);
				// authenticate will only return if we have a token
				String token = authenticate();
				// set headers
				// first set custom headers so that they will be overwritten by the fixed values
				if (customHeaders != null && !customHeaders.isEmpty()) {
					customHeaders.forEach((key, value) -> { con.setRequestProperty(key, value); });
				}
				con.setRequestProperty("X-Okapi-Token", token);
				con.setRequestProperty("X-Okapi-Tenant", this.tenant);
				if (contentType != null) con.setRequestProperty("Content-type", contentType);
				// write data for methods that support a body
				if (!"GET".equals(method) && body != null && !body.isEmpty()) {
					con.setDoOutput(true);
					OutputStream os = con.getOutputStream();
					os.write(body.getBytes());
					os.flush();
					os.close();
				}
				// get and parse response
				int responseCode = con.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) { //success
					BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
					String inputLine;
					StringBuffer response = new StringBuffer();
					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}
					in.close();
					
					this.logger.log(Level.INFO, "request succeeded with HTTP code " + responseCode + " response body being '" + response + "'");
					return new Response() {
						public int httpCode() {	return responseCode; }
						public String body() { return response.toString(); }
						
					};
				} 
				else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
					// okapi complains that we are not authenticated
					// we reset the token and retry if we have retries left
					resetAuthentication();
				}
				else {
					StringBuffer response = new StringBuffer();
					if (con.getErrorStream() != null) {
						BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
						String inputLine;
						while ((inputLine = in.readLine()) != null) {
							response.append(inputLine);
						}
						in.close();
					}
					this.logger.log(Level.ERROR, "request for path " + path + " failed with HTTP code " + responseCode + " response error message being '" + response + "'");
					throw new ConnectionException("request for path " + path + " failed with HTTP code " + responseCode + " response error message being '" + response + "'");
				}
			} catch (IOException e) {
				this.logger.log(Level.ERROR, "request for path " + path + " failed", e);
				throw new ConnectionException("request for path " + path + " failed", e);
			}
		}
		return null;
	}

	

	/**Make a call to the authentication path of Okapi.
	 * 
	 * This call is special as it does not require an access token but rather retrieves one.
	 * 
	 * cf. https://s3.amazonaws.com/foliodocs/api/mod-login/r/login.html#authn_login_post
	 * 
	 * @param username
	 * @param userId
	 * @param password
	 * @return access token as String
	 */
	String loginForToken(String username, String userId, char[] password) {
		if (userId == null) userId = "";
		if (username == null) username = "";
		if (password == null) password = new char[0];
		if (userId.isBlank() && username.isBlank()) {
			throw new IllegalArgumentException("Either userId or username must be given");
		}
		try {
			HttpURLConnection con = (HttpURLConnection) this.uri.resolve(URL_LOGIN).toURL().openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("X-Okapi-Tenant", tenant);
			con.setRequestProperty("Content-type", "application/json");
			con.setDoOutput(true);
			JSONObject body = new JSONObject();
			if (!username.isBlank()) body.put("username", username);
			if (!userId.isBlank()) body.put("userId", userId);
			body.put("password", new String(password));
			OutputStream os = con.getOutputStream();
			os.write(body.toString().getBytes());
			os.flush();
			os.close();
			String token = con.getHeaderField("x-okapi-token");
			if (token != null) { // we're logged in
				// it's only here that we get out of this method gracefully;
				return token;
			} else {
				int responseCode = con.getResponseCode();
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				throw new AuthenticationException("login failed for username '" + username + "' and userId '" + userId + "' with response code " + responseCode + " and response being " + response);
			}
		} catch (IOException e) {
			throw new AuthenticationException("cannot log in", e);
		}
	}
	
	
	public synchronized String authenticate() throws AuthenticationException {
		if (this.token == null) {
			this.token = authMethod.getAccessToken(this);
		}
		return this.token;
	}
	
	
	public synchronized void resetAuthentication() {
		this.token = null;
	}
	
	
	public static void main(String[] args) throws IOException, URISyntaxException {
		if (args.length != 4) {
			throw new IllegalArgumentException("Accepted args: okapi_base_url tenant path method; Found: " + Arrays.toString(args));
		}
		String baseUrl = args[0];
		String tenant = args[1];
		String path = args[2];
		String method = args[3].toUpperCase();
		URI uri = new URI(baseUrl);
		OkapiConnection okapi = new OkapiConnection(uri, tenant, new CliCredentialsAuthenticationMethod());
		switch (method) {
		case "DELETE":
			okapi.delete(path, null, null);
			break;
		case "GET":
			okapi.get(path, null, null);
			break;
		case "POST":
		case "PUT":
			BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
			StringBuilder sb = new StringBuilder();
			String line = r.readLine();
			while (line != null) {
				sb.append(line);
				line = r.readLine();
			}
			if (method == "POST") {
				okapi.postJSON(path, null, new JSONObject(sb.toString()));
			}
			else {
				okapi.putJSON(path, null, new JSONObject(sb.toString()));
			}
			break;			
		}
	}
	
}
