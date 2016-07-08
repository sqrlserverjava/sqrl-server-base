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
 * Short lived data for a specific correlator which only needs to be persisted while SQRL authentication is in progress.
 * Once SQRL authentication in complete, this data can be destroyed
 * 
 * @author Dave Badia
 *
 */
@Entity
@Table(name = "sqrl_transient_auth_data")
public class SqrlTransientAuthData implements Serializable {
	private static final long serialVersionUID = 8671160594792090445L;

	public static final String COLUMN_NAME_ID = "id";
	public static final String COLUMN_NAME_CORRELATOR = "sqrl_correlator";
	public static final String COLUMN_NAME_NAME = "the_name";
	public static final String COLUMN_NAME_VALUE = "the_value";
	public static final String COLUMN_NAME_DELETE_AFTER = "delete_after";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false)
	private String correlator;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String value;

	@Column(nullable = false)
	@Temporal(TemporalType.DATE)
	private Date deleteAfter;

	public SqrlTransientAuthData() {
		// Required by JPA
	}

	SqrlTransientAuthData(final String correlator, final String name, final String value, final Date deleteAfter) {
		this.correlator = correlator;
		this.name = name;
		this.value = value;
		this.deleteAfter = deleteAfter;
	}

	public long getId() {
		return id;
	}

	public void setId(final long id) {
		this.id = id;
	}

	public Date getDeleteAfter() {
		return deleteAfter;
	}

	public void setDeleteAfter(final Date deleteAfter) {
		this.deleteAfter = deleteAfter;
	}

	public String getCorrelator() {
		return correlator;
	}

	public void setCorrelator(final String correlator) {
		this.correlator = correlator;
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
