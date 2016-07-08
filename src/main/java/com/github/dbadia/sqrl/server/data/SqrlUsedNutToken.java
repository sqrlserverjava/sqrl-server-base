package com.github.dbadia.sqrl.server.data;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * A SQRL "nut" token which has been used by a SQRL Client. This data only needs to be persisted until the nut expires.
 * 
 * @see TODO: cleanup task
 * @author Dave Badia
 *
 */
@Entity
@Table(name = "sqrl_used_tokens")
public class SqrlUsedNutToken implements Serializable {
	private static final long serialVersionUID = -2419118120342877751L;

	static final String COLUMN_NAME_TOKEN = "token";
	static final String COLUMN_NAME_EXPIRES_AT = "expires_at";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false)
	private String token;

	@Column(nullable = false)
	@Temporal(TemporalType.DATE)
	private Date expiryTime;

	public SqrlUsedNutToken() {
		// Required by JPA
	}

	SqrlUsedNutToken(final String nutTokenString, final Date tokenExpiry) {
		token = nutTokenString;
		expiryTime = tokenExpiry;
	}

	public long getId() {
		return id;
	}

	public void setId(final long id) {
		this.id = id;
	}

	public String getToken() {
		return token;
	}

	public void setToken(final String token) {
		this.token = token;
	}

	public Date getExpiryTime() {
		return expiryTime;
	}

	public void setExpiryTime(final Date expiryTime) {
		this.expiryTime = expiryTime;
	}

	@Override
	public String toString() {
		return "SqrlUsedNutToken [id=" + id + ", token=" + token + ", expiryTime=" + expiryTime + "]";
	}
}
