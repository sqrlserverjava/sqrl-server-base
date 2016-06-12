package com.grc.sqrl.server;

import java.util.Date;
import java.util.Map;

import com.grc.sqrl.server.backchannel.SqrlNutToken;

/**
 * The application wanting to provide SQRL authentication must implement this interface to give the SQRL library access
 * to a persistence layer (database, etc) to store various data about the users SQRL identity.
 * 
 * @author Dave Badia
 *
 */
public interface SqrlIdentityPersistance {

	/**
	 * Check persistence to see if a user exists with the given sqrlIdk
	 * 
	 * @param sqrlIdk
	 *            the SQRL ID to check for
	 * @return true if sqrlIdk exists, false otherwise
	 */
	public boolean doesSqrlIdentityExistByIdk(String sqrlIdk) throws SqrlPersistenceException;

	/**
	 * The user has updated their SQRL ID but this webapp is still using the old one. The webapp must lookup the user by
	 * previousSqrlIdk, and replace that SQRL ID with nnewSqrlIdk
	 * 
	 * @param previousSqrlIdk
	 *            the old SQRL ID, which is present in persistence
	 * @param newSqrlIdk
	 *            the new SQRL ID, which should replace previousSqrlIdk in persistence
	 */
	public void updateIdkForSqrlIdentity(String previousSqrlIdk, String newSqrlIdk) throws SqrlPersistenceException;

	/**
	 * Indicates that we have received user specific data from the SQRL client that needs to be stored for the user
	 * <p>
	 * 
	 * @param sqrlIdk
	 *            the SQRL ID which the user authenticated with.
	 * @param dataToStore
	 *            SQRL related data that must be persisted for this user and be retreivable via
	 *            {@link #fetchSqrlIdentityDataItem(String, String)}
	 * @return true if this the first time the user has used this sqrlUserId to visit this site, false otherwise
	 */
	public void storeSqrlDataForSqrlIdentity(String sqrlIdk, Map<String, String> dataToStore)
			throws SqrlPersistenceException;

	/**
	 * Indicates that a user was authenticated successfully via SQRL. The webapp must use the given parameters to:
	 * <p>
	 * <ul>
	 * <li>Find the users web session by using nutToken as a correlator
	 * <li>Determine if the sqrlUserId is already associated with an account or not. If not, ask the user if the have an
	 * existing username/password. If not, enroll the user as new
	 * <li>Follow the webapp flow as if the user logged in via username and password. That is, lookup customer data and
	 * display the relevant content
	 * <p>
	 * 
	 * @param sqrlIdk
	 *            the SQRL ID which the user authenticated with.
	 * @param correlator
	 *            The correlator ID that was generated when the login page was presented and embedded in the sqrl url
	 * @param dataToStore
	 *            SQRL related data that must be persisted for this user and be retreivable via
	 *            {@link #fetchSqrlIdentityDataItem(String, String)}
	 * @return true if this the first time the user has used this sqrlUserId to visit this site, false otherwise
	 */

	public void userAuthenticatedViaSqrl(String sqrlIdk, String correlator)
			throws SqrlPersistenceException;

	/**
	 * Request to the data store to retrieve user specific SQRL data that was previously stored via
	 * {@link #userAuthenticatedViaSqrl(String, String, Map)}
	 * <p>
	 * 
	 * @param sqrlIdk
	 *            the SQRL ID which the user authenticated with.
	 * @param toFetch
	 *            The name of the SQRL data to be fetched. Was the key in the {@link Map} when
	 * @return the data
	 */
	public String fetchSqrlIdentityDataItem(String sqrlIdk, String toFetch) throws SqrlPersistenceException;

	/**
	 * Check persistence to see if this token has already been used
	 * 
	 * @param nutTokenString
	 *            the {@link SqrlNutToken} token in sqbase64 format as received from the client the token sent by the SQRL client
	 *            in the request
	 * @return true if the token was already used, false if not
	 */
	public boolean hasTokenBeenUsed(final String nutTokenString) throws SqrlPersistenceException;

	/**
	 * Mark the given token as used in persistence. After this call, any subsequent calls to
	 * {@link #hasTokenBeenUsed(SqrlNutToken)} must return true for this token until expiryTime. Once the expiryTime has been
	 * reached, persistence cleanup can occur and this token can be deleted from persistence
	 * 
	 * @param nut
	 *            the {@link SqrlNutToken} token in sqbase64 format as received from the client
	 * @param expiryTime
	 *            the time at which this token can safely be deleted from persistence since it will fail timestamp
	 *            validation
	 */
	public void markTokenAsUsed(final String nutTokenString, Date expiryTime) throws SqrlPersistenceException;

}
