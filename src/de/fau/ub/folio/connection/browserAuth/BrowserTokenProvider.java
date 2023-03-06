package de.fau.ub.folio.connection.browserAuth;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileLock;
import java.nio.file.Path;

import de.fau.ub.folio.connection.AuthenticationException;
import de.fau.ub.folio.connection.OkapiConnection;
import de.fau.ub.folio.connection.TokenProvider;

/**
 * @author Martin Scholz, Universitätsbibliothek Erlangen-Nürnberg
 *
 */
public abstract class BrowserTokenProvider implements TokenProvider {

	/** URL scheme for handling "Schnelle Uebernahme" = SUE
	 */
	public final static String FOLIO_BVB_SUE_SCHEME = "folio-bvb-sue";
	
	protected URI stripesUri;
	protected String initialToken;
	protected String lockFilename = "token_lock";
	protected String tokenFilename = "token_content";
	protected Path directory;
	protected File lockFile;
	protected FileLock lock;
	protected FileOutputStream fos;

	
	public BrowserTokenProvider(URI stripesUri, String inititalToken, Path directory) throws TokenForwardingException {
		this(stripesUri, inititalToken, directory, null, null);
	}
	
	public BrowserTokenProvider(URI stripesUri, String inititalToken, Path directory, String lockFilename, String tokenFilename) {
		this.stripesUri = stripesUri;
		this.initialToken = inititalToken;
		this.directory = directory;
		if (directory == null) throw new NullPointerException("directory must not be null");
		if (lockFilename != null && !lockFilename.isBlank())
			this.lockFilename = lockFilename;
		if (tokenFilename != null && !tokenFilename.isBlank())
		this.tokenFilename = tokenFilename;
	}
	

	@Override
	public String getAccessToken(OkapiConnection okapi) throws AuthenticationException {
		String token = initialToken;
		if (token != null) {
			// reset initial token; next time we need to get one
			initialToken = null;
		}
		else {
			// get a new token:
			// first, open Stripes in browser
			// second wait till a new token arrives
			try {
				Desktop.getDesktop().browse(stripesUri);
			} catch (IOException e) {
				throw new AuthenticationException("Error opening browser", e);
			}
			try {
				token = awaitToken();
			} catch (IOException e) {
				throw new AuthenticationException("Error waiting for token", e);
			}
		}
		if (token == null) throw new AuthenticationException("something went wrong while authenticating using browser");
		return token;
	}

	public static boolean isAvailable() {
		return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
	}
	

	public boolean registerInstance() {
		this.lockFile = directory.resolve(lockFilename).toFile();
		try {
			this.fos = new FileOutputStream(lockFile);
			this.lock = fos.getChannel().tryLock();
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				public void run() {
					try {
						fos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}));
		} catch (IOException e) {
			throw new AuthenticationException("cannot obtain lock on file " + lockFile.toString());
		}
		return isRegistered();
	}


	protected String awaitToken() throws IOException {
		// TODO: write method
		return null;
	}


	public void forwardToken(String token) {
		try {
			File tokenFile = directory.resolve(lockFilename).toFile();
			FileWriter fw = new FileWriter(tokenFile);
			fw.write(token);
			fw.close();
		} catch (IOException e) {
			throw new TokenForwardingException();
		}
	}

	
	public void deregisterInstance() {
		try {
			if (this.lock != null) this.lock.release();
			if (this.fos != null) this.fos.close();
		} catch (IOException e) {
			throw new TokenForwardingException("cannot release lock", e);
		}
		this.lock = null;
		this.fos = null;
	}


	public boolean isRegistered() {
		return this.lock != null && this.lock.isValid();
	}
	
}
