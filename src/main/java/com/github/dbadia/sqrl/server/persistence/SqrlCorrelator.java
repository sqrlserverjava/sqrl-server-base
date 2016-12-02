package com.github.dbadia.sqrl.server.persistence;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.github.dbadia.sqrl.server.SqrlAuthenticationStatus;
import com.github.dbadia.sqrl.server.exception.SqrlPersistenceException;

/**
 * Represents a correlator which is used to track the multiple transactions required in a SQRL authentication
 *
 * @author Dave Badia
 *
 */
@Entity
@Table(name = "sqrl_correlator")
public class SqrlCorrelator implements Serializable {
	private static final long serialVersionUID = -670589151677266808L;

	@Id
	@TableGenerator(name = "correlator_gen", table = "sqrl_db_id_gen", pkColumnName = "name", valueColumnName = "value",
	allocationSize = 1)
	@GeneratedValue(generator = "correlator_gen")
	@Column(name = "id")
	private long id;

	@Column(name = "value", nullable = false)
	private String value;

	@Column(name = "authenticationStatus", nullable = false)
	@Enumerated(EnumType.STRING)
	SqrlAuthenticationStatus authenticationStatus = SqrlAuthenticationStatus.CORRELATOR_ISSUED;

	@Column(name = "expiryTime", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date expiryTime;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "sqrl_transient_auth_data",
	joinColumns = @JoinColumn(name = "id", referencedColumnName = "id"))
	@MapKeyColumn(name = "name")
	@Column(name = "value")
	private final Map<String, String> transientAuthDataTable = new HashMap<>();

	@ManyToOne
	@JoinColumn(name = "authenticated_identity", nullable = true)
	private SqrlIdentity authenticatedIdentity;

	public SqrlCorrelator() {
		// Required by JPA
	}

	public SqrlCorrelator(final String correlatorString, final Date expiryTime) {
		this.value = correlatorString;
		this.expiryTime = expiryTime;
	}

	public Date getExpiryTime() {
		return expiryTime;
	}

	public void setExpiryTime(final Date expiryTime) {
		this.expiryTime = expiryTime;
	}

	public Map<String, String> getTransientAuthDataTable() {
		return transientAuthDataTable;
	}

	public SqrlIdentity getAuthenticatedIdentity() {
		if (getAuthenticationStatus() != SqrlAuthenticationStatus.AUTH_COMPLETE) {
			throw new SqrlPersistenceException(
					"getAuthenticatedIdentity() can only be called when getAuthenticationStatus() == SqrlAuthenticationStatus.AUTH_COMPLETE");
		}
		return authenticatedIdentity;
	}

	public void setAuthenticationStatus(final SqrlAuthenticationStatus authenticationStatus) {
		this.authenticationStatus = authenticationStatus;
	}

	public SqrlAuthenticationStatus getAuthenticationStatus() {
		return authenticationStatus;
	}

	public String getCorrelatorString() {
		return value;
	}

	public void setAuthenticatedIdentity(final SqrlIdentity authenticatedIdentity) {
		this.authenticatedIdentity = authenticatedIdentity;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("SqrlCorrelator [id=");
		builder.append(id);
		builder.append(", value=");
		builder.append(value);
		builder.append(", authenticationStatus=");
		builder.append(authenticationStatus);
		builder.append(", expiryTime=");
		builder.append(expiryTime);
		builder.append(", transientAuthDataTable=");
		builder.append(transientAuthDataTable);
		builder.append(", authenticatedIdentity=");
		builder.append(authenticatedIdentity);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((authenticatedIdentity == null) ? 0 : authenticatedIdentity.hashCode());
		result = prime * result + ((authenticationStatus == null) ? 0 : authenticationStatus.hashCode());
		result = prime * result + ((expiryTime == null) ? 0 : expiryTime.hashCode());
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((transientAuthDataTable == null) ? 0 : transientAuthDataTable.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final SqrlCorrelator other = (SqrlCorrelator) obj;
		if (authenticatedIdentity == null) {
			if (other.authenticatedIdentity != null)
				return false;
		} else if (!authenticatedIdentity.equals(other.authenticatedIdentity))
			return false;
		if (authenticationStatus != other.authenticationStatus)
			return false;
		if (expiryTime == null) {
			if (other.expiryTime != null)
				return false;
		} else if (!expiryTime.equals(other.expiryTime))
			return false;
		if (id != other.id)
			return false;
		if (transientAuthDataTable == null) {
			if (other.transientAuthDataTable != null)
				return false;
		} else if (!transientAuthDataTable.equals(other.transientAuthDataTable))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	public long getId() {
		return id;
	}

}
