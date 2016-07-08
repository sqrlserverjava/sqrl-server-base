package com.github.dbadia.sqrl.server.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;


/**
 * Represents a users SQRL identity including a String id the users native app identity
 * 
 * @author Dave Badia
 *
 */
@Entity
@Table(name = "sqrl_identities")
public class SqrlIdentity implements Serializable {
	private static final long serialVersionUID = 2278524035284028525L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false)
	private String idk;

	@Column(nullable = true)
	// nullable since we have to store the SQRL identity before the app presents the "existing user" screen
	private String nativeUserXref;

	@Column(nullable = false)
	private boolean sqrlEnabled;

	@OneToMany(mappedBy = "identity", cascade = CascadeType.ALL)
	private Collection<SqrlIdentityData> userDataList = new ArrayList<>();

	@OneToMany(mappedBy = "identity", cascade = CascadeType.ALL)
	private final Collection<SqrlIdentityFlag> flagList = new ArrayList<>();

	public SqrlIdentity() {
		// Required by JPA
	}

	public Collection<SqrlIdentityFlag> getFlagList() {
		return flagList;
	}

	public SqrlIdentity(final String sqrlIdk) {
		this.sqrlEnabled = true;
		this.idk = sqrlIdk;
	}

	public long getId() {
		return id;
	}

	public void setId(final long id) {
		this.id = id;
	}


	public String getIdk() {
		return idk;
	}

	public void setIdk(final String idk) {
		this.idk = idk;
	}

	public boolean isSqrlEnabled() {
		return sqrlEnabled;
	}

	public String getNativeUserXref() {
		return nativeUserXref;
	}

	public void setNativeUserXref(final String nativeUserXref) {
		this.nativeUserXref = nativeUserXref;
	}

	public void setSqrlEnabled(final boolean sqrlEnabled) {
		this.sqrlEnabled = sqrlEnabled;
	}

	public void setUserDataList(final Collection<SqrlIdentityData> userDataList) {
		this.userDataList = userDataList;
	}

	public Collection<SqrlIdentityData> getUserDataList() { // TODO: rename to identity data
		return userDataList;
	}
}
