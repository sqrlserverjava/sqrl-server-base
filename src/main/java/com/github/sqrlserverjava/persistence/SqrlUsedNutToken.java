package com.github.sqrlserverjava.persistence;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "sqrl_used_nut_token")
public class SqrlUsedNutToken implements Serializable {
	private static final long serialVersionUID = 9159251149539334522L;

	@Id
	@Column(name = "value", nullable = false)
	private String value;

	@Column(name = "expiryTime", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date expiryTime;

	public SqrlUsedNutToken() {
		// No arg required for JPA
	}

	public SqrlUsedNutToken(final String nutTokenString, final Date expiryTime) {
		this.value = nutTokenString;
		this.expiryTime = expiryTime;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public Date getExpiryTime() {
		return expiryTime;
	}

	public void setExpiryTime(final Date expiryTime) {
		this.expiryTime = expiryTime;
	}
}
