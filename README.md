# okapi-connection
A Java library for connecting to FOLIO Okapi

## Usage
 
Main class is `OkapiConnection`, which also provides a small main method/command line script
that can do a request on Okapi.

For authorisation/authentication, a `TokenProvider` interface and various implementations thereof
are introduced that provide different means of retrieving an access token, e.g. via login credentials.

The subpackage `browserAuth` contains a TokenProvider that implements authentication via browser.