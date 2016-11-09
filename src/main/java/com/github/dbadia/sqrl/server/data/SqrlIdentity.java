package com.github.dbadia.sqrl.server.data;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import com.github.dbadia.sqrl.server.SqrlFlag;

/**
 * Represents a users SQRL identity including a String id the users native app identity
 * 
 * @author Dave Badia
 *
 */
@Entity
@Table(name = "sqrl_identity")
public class SqrlIdentity implements Serializable {
	private static final long serialVersionUID = 8253431723090135998L;

	@Id
	@TableGenerator(name = "identity_gen", table = "sqrl_db_id_gen", pkColumnName = "name", valueColumnName = "value",
			allocationSize = 1)
	@GeneratedValue(generator = "identity_gen")
	@Column(name = "id")
	private long id;

	@Column(name = "idk", nullable = false)
	private String idk;

	@Column(name = "native_user_xref", nullable = true)
	/**
	 * nullable since we have to store the SQRL identity before the app presents the "existing user" screen
	 */
	private String nativeUserXref;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "sqrl_identity_data", joinColumns = @JoinColumn(name = "id", referencedColumnName = "id"))
	@MapKeyColumn(name = "name")
	@Column(name = "value")
	private final Map<String, String> identityDataTable = new HashMap<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "sqrl_identity_flag", joinColumns = @JoinColumn(name = "id", referencedColumnName = "id"))
	@MapKeyColumn(name = "name")
	@MapKeyEnumerated(EnumType.STRING)
	@Column(name = "value")
	private final Map<SqrlFlag, Boolean> flagTable = new EnumMap<>(SqrlFlag.class);

	public SqrlIdentity() {
		// Required by JPA
	}

	public SqrlIdentity(final String sqrlIdk) {
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

	public String getNativeUserXref() {
		return nativeUserXref;
	}

	public void setNativeUserXref(final String nativeUserXref) {
		this.nativeUserXref = nativeUserXref;
	}

	public Map<String, String> getIdentityDataTable() {
		return identityDataTable;
	}

	public Map<SqrlFlag, Boolean> getFlagTable() {
		return flagTable;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("SqrlIdentity [id=").append(id).append(", idk=").append(idk).append(", nativeUserXref=")
				.append(nativeUserXref).append(", identityDataTable=").append(identityDataTable).append(", flagTable=")
				.append(flagTable).append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((flagTable == null) ? 0 : flagTable.hashCode());
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((identityDataTable == null) ? 0 : identityDataTable.hashCode());
		result = prime * result + ((idk == null) ? 0 : idk.hashCode());
		result = prime * result + ((nativeUserXref == null) ? 0 : nativeUserXref.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final SqrlIdentity other = (SqrlIdentity) obj;
		if (flagTable == null) {
			if (other.flagTable != null) {
				return false;
			}
		} else if (!flagTable.equals(other.flagTable)) {
			return false;
		}
		if (id != other.id) {
			return false;
		}
		if (identityDataTable == null) {
			if (other.identityDataTable != null) {
				return false;
			}
		} else if (!identityDataTable.equals(other.identityDataTable)) {
			return false;
		}
		if (idk == null) {
			if (other.idk != null) {
				return false;
			}
		} else if (!idk.equals(other.idk)) {
			return false;
		}
		if (nativeUserXref == null) {
			if (other.nativeUserXref != null) {
				return false;
			}
		} else if (!nativeUserXref.equals(other.nativeUserXref)) {
			return false;
		}
		return true;
	}

}
