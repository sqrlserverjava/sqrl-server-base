package com.github.dbadia.sqrl.server;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.github.dbadia.sqrl.server.backchannel.SqrlNutToken;
import com.github.dbadia.sqrl.server.enums.SqrlAuthenticationStatus;
import com.github.dbadia.sqrl.server.enums.SqrlIdentityFlag;
import com.github.dbadia.sqrl.server.exception.SqrlPersistenceException;
import com.github.dbadia.sqrl.server.persistence.SqrlCorrelator;
import com.github.dbadia.sqrl.server.persistence.SqrlIdentity;
import com.github.dbadia.sqrl.server.persistence.SqrlJpaPersistenceProvider;

/**
 * Bridge between the SQRL library and the persistence layer (database, etc)
 *
 * @see SqrlJpaPersistenceProvider
 * @author Dave Badia
 *
 */
public interface SqrlPersistence {
	/* ***************** SqrlIdentity *********************/
	/**
	 * Create a new {@link SqrlIdentity} and enable SQRL authentication
	 *
	 * @param sqrlIdk
	 *            the idk of the SQRL identity
	 */
	public void createAndEnableSqrlIdentity(String sqrlIdk);

	/**
	 * Check persistence to see if a user exists with the given sqrlIdk
	 *
	 * @param sqrlIdk
	 *            the SQRL ID to check for
	 * @return true if sqrlIdk exists, false otherwise
	 */
	public boolean doesSqrlIdentityExistByIdk(String sqrlIdk);

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
	 * the user by previousSqrlIdk, and replace that SQRL ID with newSqrlIdk
	 *
	 * @param previousSqrlIdk
	 *            the old SQRL ID, which is present in persistence
	 * @param newSqrlIdk
	 *            the new SQRL ID, which should replace previousSqrlIdk in persistence
	 */
	public void updateIdkForSqrlIdentity(String previousSqrlIdk, String newSqrlIdk);

	/**
	 * Invoked when the user chooses to remove SQRL authentication for this site
	 *
	 * @param sqrlIdk
	 *            the SQRL ID which represents the user.
	 * @throws SqrlPersistenceException
	 *             if there was an error accessing the persistence store
	 */
	public void deleteSqrlIdentity(String sqrlIdk);

	/**
	 * Called to assign a native user cross reference to the given SQRL identity object so when SQRL authentication
	 * takes place, the application knows which user has authenticated
	 *
	 * @param sqrlIdentityId
	 *            the SQRL identity to update
	 * @param nativeUserXref
	 *            the applications native user id for this user
	 */
	public void updateNativeUserXref(final long sqrlIdentityId, final String nativeUserXref);

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
	 *            The correlator ID that was generated when the login page was presented and embedded in the SQRL url
	 * @param dataToStore
	 *            SQRL related data that must be persisted for this user and be retrievable via
	 *            {@link #fetchSqrlIdentityDataItem(String, String)}
	 * @return true if this the first time the user has used this sqrlUserId to visit this site, false otherwise
	 */
	public void userAuthenticatedViaSqrl(String sqrlIdk, String correlator);

	/**
	 * Invoked to determine if SQRL a given flag is set or unset for a user
	 *
	 * @param sqrlIdk
	 *            the SQRL ID which represents the user.
	 * @return true if the flag is currently set, false if unset
	 * @throws SqrlPersistenceException
	 *             if there was an error accessing the persistence store
	 */
	public boolean fetchSqrlFlagForIdentity(String sqrlIdk, SqrlIdentityFlag flagToFetch);

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
	public void setSqrlFlagForIdentity(String sqrlIdk, SqrlIdentityFlag flagToSet, boolean valueToSet);

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
	public void storeSqrlDataForSqrlIdentity(String sqrlIdk, Map<String, String> dataToStore);

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
	public String fetchSqrlIdentityDataItem(String sqrlIdk, String toFetch);

	/* ***************** SQRL USED TOKENS *********************/
	/**
	 * Check persistence to see if this token has already been used
	 *
	 * @param nutTokenString
	 *            the {@link SqrlNutToken} token in sqbase64 format as received from the client the token sent by the
	 *            SQRL client in the request
	 * @return true if the token was already used, false if not
	 */
	public boolean hasTokenBeenUsed(final String nutTokenString);

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
	public void markTokenAsUsed(final String nutTokenString, final Date expiryTime);

	/**
	 * Fetch a short lived name/value for a given correlator and name
	 *
	 * @param correlator
	 *            correlator to which this data belongs
	 * @param name
	 *            the name of the item to be fetched
	 * @return the value for the correlator and name or null if it does not exist
	 */
	public String fetchTransientAuthData(String correlator, String transientNameServerParrot);

	/* ***************** SqrlCorrelator *********************/

	/**
	 * Create a new correlator instance in the persistence
	 *
	 * @param correlatorString
	 *            the correlator value string
	 * @param expiryTime
	 *            the time at which this correlator expires
	 * @return
	 */
	public SqrlCorrelator createCorrelator(String correlatorString, Date expiryTime);

	/**
	 * Fetch the correlator object for the given string value, or throw an exception if it does not exist
	 *
	 * @param correlator
	 *            the string value to search for
	 * @return the non-null correlator object
	 */
	public SqrlCorrelator fetchSqrlCorrelatorRequired(String correlator);

	/**
	 * Fetch the correlator object for the given string value
	 *
	 * @param correlator
	 *            the string value to search for
	 * @return the correlator object or null if it does not exist
	 */
	public SqrlCorrelator fetchSqrlCorrelator(String correlator);

	/* ***************** TRANSACTION START / STOP *********************/

	/**
	 * Commit all updates since this persistence object was created
	 */
	public void closeCommit();

	/**
	 * Ignore all updates since this persistence object was created
	 */
	public void closeRollback();

	/**
	 * @return true if this persistence object has been closed
	 */
	public boolean isClosed();

	/**
	 * Delete any expired objects in the persistence store
	 */
	public void cleanUpExpiredEntries();

	Map<String, SqrlCorrelator> fetchSqrlCorrelatorsDetached(Set<String> correlatorStringSet);

	/**
	 * Checks for correlators who's status has changed or is {@link SqrlAuthenticationStatus#AUTHENTICATED_BROWSER}. Complete
	 * state is always returned in case the client didn't get the update the first time it was sent
	 *
	 * @param correlatorToCurrentStatusTable
	 *            table of string correlators and their current state as sent by the browser
	 * @return table of correlators and their updated (or complete) states
	 */
	public Map<String, SqrlAuthenticationStatus> fetchSqrlCorrelatorStatusUpdates(
			Map<String, SqrlAuthenticationStatus> correlatorToCurrentStatusTable);

	public void deleteSqrlCorrelator(SqrlCorrelator sqrlCorrelator);

}
