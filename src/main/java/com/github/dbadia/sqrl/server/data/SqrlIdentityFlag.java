package com.github.dbadia.sqrl.server.data;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.github.dbadia.sqrl.server.SqrlFlag;

/**
 * TODO:
 * 
 * @author Dave Badia
 *
 */
@Entity
@Table(name = "sqrl_identity_flags")
public class SqrlIdentityFlag implements Serializable {
	private static final long serialVersionUID = -7558861124967850375L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne
	// @JoinColumn(nullable = false)
	private SqrlIdentity identity;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private SqrlFlag flagName; // TODO: change all to theName

	@Column(nullable = false)
	private boolean enabled;


	public SqrlIdentityFlag() {
		// Required by JPA
	}

	public SqrlIdentityFlag(final SqrlIdentity identity, final SqrlFlag flagName, final boolean enabled) {
		super();
		this.identity = identity;
		this.flagName = flagName;
		this.enabled = enabled;
	}

	public SqrlFlag getFlagName() {
		return flagName;
	}

	public boolean getFlagValue() {
		return enabled;
	}

	public void setValue(final boolean valueToSet) {
		this.enabled = valueToSet;
	}
}
