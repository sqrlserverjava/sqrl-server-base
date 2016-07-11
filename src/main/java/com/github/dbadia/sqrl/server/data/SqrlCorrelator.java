package com.github.dbadia.sqrl.server.data;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

/**
 * Represents a correlator which is used to track the multiple transactions required in a SQRL authentication
 * 
 * @author Dave Badia
 *
 */
@Entity
@Table(name = "sqrl_correlator")
public class SqrlCorrelator implements Serializable {
	private static final long serialVersionUID = 6724800159628367708L;

	@Id
	@TableGenerator(name = "correlator_gen", table = "sqrl_db_id_gen", pkColumnName = "name", valueColumnName = "value", allocationSize = 1)
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
	@CollectionTable(name = "sqrl_transient_auth_data", joinColumns = @JoinColumn(name = "id", referencedColumnName = "id"))
	@MapKeyColumn(name = "name")
	@Column(name = "value")
	private final Map<String, String> transientAuthDataTable = new HashMap<>();

	@ElementCollection
	@CollectionTable(name = "sqrl_used_nut_token", joinColumns = @JoinColumn(name = "id", referencedColumnName = "id"))
	@Column(name = "value")
	private final Set<String> usedNutTokenList = new HashSet<>();

	@ManyToOne
	@JoinColumn(name = "authenticated_identity", nullable = true)
	private SqrlIdentity authenticatedIdentity;

	public void setAuthenticatedIdentity(final SqrlIdentity authenticatedIdentity) {
		this.authenticatedIdentity = authenticatedIdentity;
	}

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

	public Set<String> getUsedNutTokenList() {
		return usedNutTokenList;
	}

	public SqrlIdentity getAuthenticatedIdentity() {
		return authenticatedIdentity;
	}

	public void setAuthenticationStatus(final SqrlAuthenticationStatus authenticationStatus) {
		this.authenticationStatus = authenticationStatus;
	}

	public SqrlAuthenticationStatus getAuthenticationStatus() {
		return authenticationStatus;
	}


}
