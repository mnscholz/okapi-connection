package de.fau.ub.folio.connection;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/** Resembles a connection to a FOLIO backend (Okapi).
 *  An Okapi connection is defined by the base URL/URI of an Okapi instance
 *  and a tenant.
 *  
 *  This class provides methods to make calls to the FOLIO API. The methods come in different 
 *  flavors for each of the REST verbs: GET, POST, PUT, DELETE.
 *  It provides quick access methods that return parsed JSON objects as well as sister methods
 *  that give full access to the response information. 
 *  
 *  This class does not provide functionality for authentication by itself.
 *  Instead, different authentication methods are available through 
 *  implementations of the {@code TokenProvider} interface.
 *  
 * 
 * @author Martin Scholz, Universitätsbibliothek Erlangen-Nürnberg
 *
 */
public final class OkapiConnection {

	public static final String JSON_MIMETYPE = "application/json";
	private static final String URL_LOGIN = "authn/login";
	
	private URI uri;
	private String tenant;
	private TokenProvider tokenProvider;
	private String token = null;
	private Logger logger;
	private Charset utf8 = Charset.forName("utf-8");
	
	/** Encapsulates a response to a request.
	 * 
	 * This class transports the response's http response code, its content mime type,
	 * its headers and the body.
	 * 
	 * @author Martin Scholz, Universitätsbibliothek Erlangen-Nürnberg
	 *
	 */
	public class Response {
		private int httpCode;
		private byte[] body;
		private String contentType;
		private Map<String, List<String>> headers;
		private Response(int httpCode, String contentType, byte[] body, Map<String, List<String>> headers) {
			super();
			this.httpCode = httpCode;
			this.body = body;
			this.contentType = contentType;
			this.headers = headers == null ? Collections.emptyMap() : headers;
		}
		/** Return the HTTP code of the response
		 * 
		 * @return the HTTP code
		 */
		public int httpCode() {
			return httpCode;
		};
		/** Returns the raw response body
		 * 
		 * @return the response body
		 */
		public byte[] body() {
			return body;
		};
		public String contentType() {
			return contentType;
		}
		/** Return the response body as a JSON object.
		 * 
		 * @return the response body as a JSON object
		 */
		public JSONObject toJson() {
			if (! JSON_MIMETYPE.equals(contentType)) return null;
			return new JSONObject(body);
		}
		/** Return the response body as a JSON array.
		 * 
		 * @return the response body as a JSON array
		 */
		public JSONArray toJsonArray() {
			if (! JSON_MIMETYPE.equals(contentType)) return null;
			return new JSONArray(body);
		}
		/** Returns the response headers, @see {@code HttpURLConnection.getHeaderFields()} 
		 * 
		 * @return the response headers
		 */
		public Map<String, List<String>> headers() {
			return headers;
		}
		/** Return the response body as a String.
		 * 
		 * @return the response body as a String
		 */
		@Override
		public String toString() {
			return new String(body, utf8);
		}
	}
	
	public OkapiConnection(URI uri, String tenant, TokenProvider tokenProvider) {
		this(uri, tenant, tokenProvider, null);
	}
	
	public OkapiConnection(URI uri, String tenant, TokenProvider tokenProvider, Logger logger) {
		super();
		this.uri = uri;
		this.tenant = tenant;
		this.tokenProvider = tokenProvider;
		this.logger = logger != null ? logger : System.getLogger(this.getClass().getCanonicalName());
	}

	
	/** Send a delete request.
	 * 
	 * @param path the path of the API endpoint
	 * @param parameters optional URL parameters. Keys and values will get url-encoded. 
	 * 		  Query parameters may alternatively be included in the path argument, but then
	 * 		  the caller must take care of proper url encoding.
	 * @param customHeaders additional headers. @see {@code HttpURLConnection.setRequestProperty()}
	 * @return a {@code Response} to the request.
	 * @throws ConnectionException if something went wrong
	 */
	public Response delete (String path, Map<String, String> parameters, Map<String, String> customHeaders) throws ConnectionException {
		return doRequest("DELETE", path, parameters, null, customHeaders, null);
	}

	/** Send a get request.
	 * 
	 * @param path the path of the API endpoint
	 * @param parameters optional URL parameters. Keys and values will get url-encoded.
	 * 		  Query parameters may alternatively be included in the path argument, but then
	 * 		  the caller must take care of proper url encoding.
	 * @param customHeaders additional headers. @see {@code HttpURLConnection.setRequestProperty()}
	 * @return a {@code Response} to the request.
	 * @throws ConnectionException if something went wrong
	 */
	public Response get (String path, Map<String, String> parameters, Map<String, String> customHeaders) throws ConnectionException {
		return doRequest("GET", path, parameters, null, customHeaders, null);
	}

	/** Send a post request.
	 * 
	 * A post request typically has a JSON payload/message body. 
	 * This method, however, allows to send arbitrary byte data.
	 * 
	 * Okapi usually does not expect URL query parameters in POST or PUT requests.
	 * If you nevertheless need to include a query in your URL, you have to add it to the path
	 * and don't forget to take care of proper escaping!
	 * 
	 * @param path the path of the API endpoint
	 * @param contentType the content mime type 
	 * @param customHeaders additional headers. @see {@code HttpURLConnection.setRequestProperty()}
	 * @param body the body as a byte array
	 * @return a {@code Response} to the request.
	 * @throws ConnectionException if something went wrong
	 */
	public Response post (String path, String contentType, Map<String, String> customHeaders, byte[] body) throws ConnectionException {
		return doRequest("POST", path, null, contentType, customHeaders, body);
	}

	/** Send a put request.
	 * 
	 * A put request typically has a JSON payload/message body. 
	 * This method, however, allows to send arbitrary byte data.
	 * 
	 * Okapi usually does not expect URL query parameters in POST or PUT requests.
	 * If you nevertheless need to include a query in your URL, you have to add it to the path
	 * and don't forget to take care of proper escaping!
	 * 
	 * @param path the path of the API endpoint
	 * @param contentType the content mime type 
	 * @param customHeaders additional headers. @see {@code HttpURLConnection.setRequestProperty()}
	 * @param body the body as a byte array
	 * @return a {@code Response} to the request.
	 * @throws ConnectionException if something went wrong
	 */
	public Response put (String path, String contentType, Map<String, String> customHeaders, byte[] body) throws ConnectionException {
		return doRequest("PUT", path, null, contentType, customHeaders, body);
	}

	public JSONObject getJSON (String path, Map<String, String> parameters, Map<String, String> customHeaders) throws ConnectionException {
		return new JSONObject(get(path, parameters, customHeaders));
	}
	
	public JSONObject postJSON (String path, Map<String, String> customHeaders, JSONObject body) throws ConnectionException {
		return new JSONObject(post(path, JSON_MIMETYPE, customHeaders, body.toString().getBytes(utf8)).body());
	}
	
	public JSONObject putJSON (String path, Map<String, String> customHeaders, JSONObject body) throws ConnectionException {
		return new JSONObject(put(path, JSON_MIMETYPE, customHeaders, body.toString().getBytes(utf8)).body());
	}
	
	/** Does the main work of preparing and sending a request to Okapi as well as retrieving the response.
	 * 
	 * @param method the HTTP method/verb, ie. one of DELETE, GET, POST, PUT
	 * @param path the path part of the URL of the Okapi API endpoint
	 * @param parameters URL query parameters to be included into the URL
	 * @param contentType the mimetype of the body; sets the {@code Content-Type} request header to this value.
	 * @param customHeaders custom or extended headers sent with the request. Cannot be used to overwrite content type, access token or okapi tenant headers. 
	 * @param body the request body. 
	 * @return a {@code Response} object
	 * @throws ConnectionException if something went wrong
	 */
	private Response doRequest (String method, String path, Map<String, String> parameters, String contentType, Map<String, String> customHeaders, byte[] body) throws ConnectionException {
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
						tempUrl
							.append(delim.charAt(0))
							.append(URLEncoder.encode(key, utf8))
							.append('=')
							.append(URLEncoder.encode(key, utf8));
						delim.insert(0, '&');
					});
					url = new URL(tempUrl.toString());
				}
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod(method);
				// set headers
				// first set custom headers so that they will be overwritten by the fixed values
				if (customHeaders != null && !customHeaders.isEmpty()) {
					customHeaders.forEach((key, value) -> { con.setRequestProperty(key, value); });
				}
				String token = getAccessToken();
				con.setRequestProperty("X-Okapi-Token", token);
				con.setRequestProperty("X-Okapi-Tenant", this.tenant);
				if (contentType != null) con.setRequestProperty("Content-type", contentType);
				// write data for methods that support a body
				if (("POST".equals(method) || "PUT".equals(method)) && body != null && body.length != 0) {
					con.setDoOutput(true);
					OutputStream os = con.getOutputStream();
					os.write(body);
					os.flush();
					os.close();
				} 
				// get and parse response
				int responseCode = con.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) { //success
					BufferedInputStream in = new BufferedInputStream(con.getInputStream());
					byte[] response = in.readAllBytes();
					in.close();
					this.logger.log(Level.INFO, "request succeeded with HTTP code " + responseCode + " response body being '" + response + "'");
					return new Response(responseCode, con.getHeaderField("Content-Type"), response, con.getHeaderFields());
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
	 * @see CredentialsTokenProvider
	 * @param username
	 * @param userId
	 * @param password
	 * @return access token as String
	 */
	public String loginForToken(String username, String userId, char[] password) {
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
			// write the login data to the connection
			// we do not use the org.json classes in order to
			// not convert the password data into a string
			con.setDoOutput(true);
			BufferedWriter w = new BufferedWriter(new OutputStreamWriter(con.getOutputStream()));
			w.write("{");
			if (!username.isBlank()) {
				w.write("\"username\":\"");
				w.write(escapeCharsForJSON(username.toCharArray()));
				w.write("\",");
			};
			if (!userId.isBlank()) {
				w.write("\"userId\":\"");
				w.write(escapeCharsForJSON(userId.toCharArray()));
				w.write("\",");
			};
			w.write("\"password\":\"");
			w.write(escapeCharsForJSON(password));
			w.write("\"}");
			w.flush();
			w.close();
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
	
	/** Escape special chars in JSON. Helper method for escaping of char array in {@code loginForToken()}.
	 * 
	 * @param chars
	 * @return
	 */
	private char[] escapeCharsForJSON(char[] chars) {
		int newlen = chars.length;
		for (int i = 0; i < chars.length; i++) {
			if (	   chars[i] == '\\'
					|| chars[i] == '"'
					|| chars[i] == '\b'
					|| chars[i] == '\f'
					|| chars[i] == '\n'
					|| chars[i] == '\r'
					|| chars[i] == '\t'
					) {
				newlen++;
			} 
			else if ((int) chars[i] < 20) {
				throw new IllegalArgumentException("illegal character for credentials: " + chars[i]);
			}
		}
		char[] newchars = new char[newlen];
		int j = 0;
		for (int i = 0; i < chars.length; i++) {
			switch (chars[i]) {
			case '\\': newchars[j++] = '\\'; newchars[j++] = '\\'; break;
			case '"': newchars[j++] = '\\'; newchars[j++] = '"'; break;
			case '\b': newchars[j++] = '\\'; newchars[j++] = 'b'; break;
			case '\f': newchars[j++] = '\\'; newchars[j++] = 'f'; break;
			case '\n': newchars[j++] = '\\'; newchars[j++] = 'n'; break;
			case '\r': newchars[j++] = '\\'; newchars[j++] = 'r'; break;
			case '\t': newchars[j++] = '\\'; newchars[j++] = 't'; break;
			default: newchars[j++] = chars[i]; 
			}
		}
		return newchars;
	}
	
	
	/** Retrieves the "cached" access token or gets a fresh one
	 * 
	 * As this method may wait for the TokenProvider to return a token,
	 * we make sure, that we do not reset the token in the meantime.
	 * 
	 * @return
	 * @throws AuthenticationException
	 */
	private synchronized String getAccessToken() throws AuthenticationException {
		if (this.token == null) {
			this.token = tokenProvider.getAccessToken(this);
		}
		return this.token;
	}
	
	
	/** Reset the "cached" token.
	 * 
	 */
	private synchronized void resetAuthentication() {
		this.token = null;
	}
	
	
	/** A small script to send a request using the commandline. 
	 * 
	 * The request body, if needed, is passed via stdin.
	 * 
	 * Credentials are prompted from the user. If there is a GUI, a graphical dialog is displayed,
	 * otherwise it falls back to a command line prompt
	 * 
	 * @param args The cli args
	 * 	- okapi_base_url: the okapi base url
	 *  - tenant: the tenant
	 *  - path: the relative path to the endpoint; query parameters need to be correctly included already
	 *  - the HTTP method
	 */
	public static void main(String[] args) {
		try {
			if (args.length != 4) {
				throw new IllegalArgumentException("Accepted args: okapi_base_url tenant path method; Found: " + Arrays.toString(args));
			}
			String baseUrl = args[0];
			String tenant = args[1];
			String path = args[2];
			String method = args[3].toUpperCase();
			URI uri = new URI(baseUrl);
			
			// if we have access to a GUI, we display a graphical dialog,
			// otherwise we fall back to a command line prompt
			TokenProvider tp = new CliCredentialsTokenProvider();
			if (DialogCredentialsTokenProvider.isMethodAvailable()) {
				tp = new DialogCredentialsTokenProvider();
			}
			
			OkapiConnection okapi = new OkapiConnection(uri, tenant, tp);
			
			JSONObject response = null;
			switch (method) {
			case "DELETE":
				okapi.delete(path, null, null);
				break;
			case "GET":
				System.out.println(okapi.get(path, null, null));
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
					response = okapi.postJSON(path, null, new JSONObject(sb.toString()));
				}
				else {
					response = okapi.putJSON(path, null, new JSONObject(sb.toString()));
				}
				break;			
			}
			if (response != null) {
				System.out.println(response.toString(2));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}
		
}
