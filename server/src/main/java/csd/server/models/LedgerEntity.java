package csd.server.models;

import jakarta.persistence.*;

@Entity
public class LedgerEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String contract;

	@Column(nullable = false)
	private long value;

	@Column(nullable = false)
	private String publicKey;

	public LedgerEntity(String contract, String publicKey) {
		this.contract = contract;
		this.publicKey = publicKey;
		this.value = 0;
	}

	public LedgerEntity() {

	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getContract() {
		return contract;
	}

	public void setContract(String contract) {
		this.contract = contract;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}

	@Override
	public String toString() {
		return contract + ":" + value + ":" + publicKey;
	}

}
