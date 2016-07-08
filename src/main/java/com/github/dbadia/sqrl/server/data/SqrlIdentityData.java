package com.github.dbadia.sqrl.server.data;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Generic name value pair records which relate to a SQRL users identity (vuk, suc, etc)
 * 
 * @author Dave Badia
 *
 */
@Entity
@Table(name = "sqrl_identity_data")
public class SqrlIdentityData implements Serializable {
	private static final long serialVersionUID = -9005567116077805871L;

	public static final String COLUMN_NAME_ID = "id";
	public static final String COLUMN_NAME_SQRL_ID = "sqrl_identity_id";
	public static final String COLUMN_NAME_NAME = "the_name";
	public static final String COLUMN_NAME_VALUE = "the_value";
	public static final String COLUMN_NAME_ENABLED = "sqrlEnabled";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne
	@JoinColumn(nullable = true)
	private SqrlIdentity identity;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String value;

	public SqrlIdentityData() {
		// Required by JPA TODO: is it?
	}

	public SqrlIdentityData(final SqrlIdentity sqrlIdentity, final String name, final String value) {
		this.identity = sqrlIdentity;
		this.name = name;
		this.value = value;
	}

	public long getId() {
		return id;
	}

	public void setId(final long id) {
		this.id = id;
	}

	public SqrlIdentity getUser() {
		return identity;
	}

	public void setUser(final SqrlIdentity user) {
		this.identity = user;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

}
