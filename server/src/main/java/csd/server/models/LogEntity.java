package csd.server.models;

import jakarta.persistence.*;

@Entity
public class LogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String operation;

	@Column(nullable = false)
	private String originContract;

	@Column()
	private String destinationContract;

	public LogEntity(String operation, String originContract, String destinationContract) {
		this.operation = operation;
		this.originContract = originContract;
		this.destinationContract = destinationContract;
	}

	public LogEntity() {

	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getOriginContract() {
		return originContract;
	}

	public String getDestinationContract() {
		return destinationContract;
	}

	@Override
	public String toString() {
		return id.toString() + ":" + operation + ":" + originContract + ":" + destinationContract;
	}
}
