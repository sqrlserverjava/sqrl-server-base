package com.github.dbadia.sqrl.server.data;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.github.dbadia.sqrl.server.SqrlAuthenticationStatus;

/**
 * Represents the current state of SQRL authentication for a given correlator
 * 
 * @author Dave Badia
 *
 */
@Entity
@Table(name = "sqrl_auth_progress")
public class SqrlAuthenticationProgress implements Serializable {
	private static final long serialVersionUID = 5145059142867019025L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false)
	private String correlator;

	@Column(nullable = false)
	@Temporal(TemporalType.DATE)
	private Date expiryTime;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private SqrlAuthenticationStatus authenticationStatus;

	@ManyToOne
	@JoinColumn(nullable = true)
	private SqrlIdentity sqrlIdentity;


	@Column(nullable = true)
	@Temporal(TemporalType.DATE)
	private Date authenticationTime;

	public SqrlAuthenticationProgress() {
		// Required by JPA
	}

	public Date getExpiryTime() {
		return expiryTime;
	}

	public SqrlAuthenticationProgress(final String correlator, final Date expiryTime) {
		this.correlator = correlator;
		this.expiryTime = expiryTime;
		this.authenticationStatus = SqrlAuthenticationStatus.CORRELATOR_ISSUED;
	}

	public void setAuthenticated(final String correlator) {
		this.correlator = correlator;
		this.authenticationTime = new Date();
	}

	public SqrlIdentity getSqrlIdentity() {
		return sqrlIdentity;
	}

	public String getAuthenticationCorrelator() {
		return correlator;
	}

	public void setAuthenticationCorrelator(final String authenticationCorrelator) {
		this.correlator = authenticationCorrelator;
	}

	public Date getAuthenticationTime() {
		return authenticationTime;
	}

	public void setAuthenticationTime(final Date authenticationTime) {
		this.authenticationTime = authenticationTime;
	}

	public void setAuthenticationStatus(final SqrlAuthenticationStatus authenticationStatus) {
		this.authenticationStatus = authenticationStatus;
	}

	public SqrlAuthenticationStatus getAuthenticationStatus() {
		return authenticationStatus;
	}

	public void setAuthenticationComplete(final SqrlIdentity sqrlIdentity) {
		this.sqrlIdentity = sqrlIdentity;
		this.authenticationTime = new Date();
		this.authenticationStatus = SqrlAuthenticationStatus.AUTH_COMPLETE;
	}
}
