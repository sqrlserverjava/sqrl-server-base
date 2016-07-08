package com.github.dbadia.sqrl.server;

import java.util.Date;
import java.util.Map;

import com.github.dbadia.sqrl.server.backchannel.SqrlNutToken;
import com.github.dbadia.sqrl.server.data.SqrlAuthenticationProgress;
import com.github.dbadia.sqrl.server.data.SqrlIdentity;

/**
 * The application wanting to provide SQRL authentication must implement this interface to give the SQRL library access
 * to a persistence layer (database, etc) to store various data about the users SQRL identity.
 * 
 * @author Dave Badia
 *
 */
public interface SqrlPersistence {
	/* ***************** SqrlIdentity *********************/

	public void createAndEnableSqrlIdentity(String sqrlIdk, Map<String, String> identityDataTable);

	/**
	 * Check persistence to see if a user exists with the given sqrlIdk
	 * 
	 * @param sqrlIdk
	 *            the SQRL ID to check for
	 * @return true if sqrlIdk exists, false otherwise
	 */
	// TODO: remove all exceptions, JPAs are unchecked
	public boolean doesSqrlIdentityExistByIdk(String sqrlIdk) throws SqrlPersistenceException;

	/**
	 * Fetch the sqrl identity for the the given app user cross reference id
	 * 
	 * @param appUserXref
	 *            the app user cross reference value to search by
	 * @return the SQRL identity for this app user
	 */
	public SqrlIdentity fetchSqrlIdentityByUserXref(final String appUserXref);

	/**
	 * The user has updated their SQRL ID but this application is still using the old one. The application must lookup
	 * the user by previousSqrlIdk, and replace that SQRL ID with nnewSqrlIdk
	 * 
	 * @param previousSqrlIdk
	 *            the old SQRL ID, which is present in persistence
	 * @param newSqrlIdk
	 *            the new SQRL ID, which should replace previousSqrlIdk in persistence
	 */
	public void updateIdkForSqrlIdentity(String previousSqrlIdk, String newSqrlIdk) throws SqrlPersistenceException;

	/**
	 * Invoked when the user chooses to remove SQRL authentication for this site
	 * 
	 * @param sqrlIdk
	 *            the SQRL ID which represents the user.
	 * @throws SqrlPersistenceException
	 *             if there was an error accessing the persistence store
	 */
	public void deleteSqrlIdentity(String sqrlIdk) throws SqrlPersistenceException;

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
	 * Invoked to determine if SQRL auth is enabled for a user
	 * 
	 * @param sqrlIdk
	 *            the SQRL ID which represents the user.
	 * @return the auth state of the SQRL user or {@link SqrlEaabledState#NOT_EXIST} if there is none
	 * @throws SqrlPersistenceException
	 *             if there was an error accessing the persistence store
	 */
	public Boolean fetchSqrlFlagForIdentity(String sqrlIdk, SqrlFlag flagToFetch) throws SqrlPersistenceException;

	/**
	 * Invoked when the user chooses to temporarily disable SQRL authentication for this site
	 * 
	 * @param sqrlIdk
	 *            the SQRL ID which represents the user.
	 * @param state
	 *            the auth state to set for this SQRL user
	 * @throws SqrlPersistenceException
	 *             if there was an error accessing the persistence store
	 */
	public void setSqrlFlagForIdentity(String sqrlIdk, SqrlFlag flagToSet, boolean valueToSet)
			throws SqrlPersistenceException;

	/* ***************** SQRL IDENTITY DATA *********************/
	/**
	 * Indicates that we have received user specific data from the SQRL client that needs to be stored for the user;
	 * <b>NOTE<b> this is often the first call made for a new SQRL identity, so if the identity does not currently
	 * exist, it must be created
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
	 * Request to the data store to retrieve user specific SQRL data that was previously stored via
	 * {@link #userAuthenticatedViaSqrl(String, String, Map)}
	 * <p>
	 * 
	 * @param sqrlIdk
	 *            the SQRL ID which the user authenticated with.
	 * @param toFetch
	 *            The name of the SQRL data to be fetched. Was the key in the {@link Map} when
	 * @return the data or null if it does not exist
	 */
	public String fetchSqrlIdentityDataItem(String sqrlIdk, String toFetch) throws SqrlPersistenceException;

	/* ***************** SQRL USED TOKENS *********************/
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
	 * {@link #hasTokenBeenUsed(SqrlNutToken)} must return true for this token until expiryTime. Once the expiryTime has
	 * been reached, persistence cleanup can occur and this token can be deleted from persistence
	 * 
	 * @param nutTokenString
	 *            the {@link SqrlNutToken} token in sqbase64 format as received from the client
	 * @param expiryTime
	 *            the time at which this token can safely be deleted from persistence since it will fail timestamp
	 *            validation
	 */
	public void markTokenAsUsed(final String nutTokenString, Date expiryTime) throws SqrlPersistenceException;

	/* ***************** TRANSIENT AUTH DATA *********************/
	/**
	 * Store or replace a short lived name/value for a given correlator; if the correlator/name pair already exists, the
	 * value and deleteAfter should be updated with the new values
	 * 
	 * @param correlator
	 *            correlator to which this data belongs
	 * @param name
	 *            the name of the item
	 * @param value
	 *            the value of the item
	 * @param deleteAfter
	 *            the time at which this data can be safely deleted
	 * @throws SqrlPersistenceException
	 *             if there was an error accessing the persistence store
	 */
	public void storeTransientAuthenticationData(final String correlator, final String dataName, final String dataValue,
			final Date deleteAfter) throws SqrlPersistenceException;

	/**
	 * Fetch a short lived name/value for a given correlator and name
	 * 
	 * @param correlator
	 *            correlator to which this data belongs
	 * @param name
	 *            the name of the item to be fetched
	 * @return the value for the correlator and name or null if it does not exist
	 * @throws SqrlPersistenceException
	 *             if there was an error accessing the persistence store
	 * @throws SqrlException
	 *             if there was an error accessing the persistence store
	 */
	public String fetchTransientAuthData(String correlator, String transientNameServerParrot)
			throws SqrlPersistenceException;

	/* ***************** TRANSACTION START / STOP *********************/
	/**
	 * Invoked to start a new persistence transaction. All calls made to this class after this must <b>not</b> be
	 * committed until {@link #commitTransaction()} is called
	 */
	public void startTransaction();

	/**
	 * Commit all updates since {@link #startTransaction()} was called
	 */
	public void commitTransaction();

	/**
	 * Ignore all updates since {@link #startTransaction()} was called
	 */
	public void rollbackTransaction();

	public SqrlAuthenticationStatus fetchAuthenticationStatusRequired(String correlator);

	public SqrlAuthenticationProgress fetchAuthenticationProgressRequired(final String correlator);

	public void createAuthenticationProgress(final String correlator, final Date expiryTime);
}
