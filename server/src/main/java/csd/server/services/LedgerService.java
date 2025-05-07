package csd.server.services;

import csd.server.exceptions.ContractAlreadyExistsException;
import csd.server.exceptions.ContractDoesNotExistException;
import csd.server.models.LedgerEntity;
import csd.server.repositories.LedgerRepository;
import org.springframework.stereotype.Service;

@Service
public class LedgerService {

	private final LedgerRepository ledgerRepository;

	public LedgerService(LedgerRepository ledgerRepository) {
		this.ledgerRepository = ledgerRepository;
	}

	public String getContractPublicKey(String contract) {
		return ledgerRepository.getLedgerByContract(contract).orElseThrow(ContractDoesNotExistException::new).getPublicKey();
	}

	public void createContract(String contract, String publicKey) {
		if (ledgerRepository.existsByContract(contract))
			throw new ContractAlreadyExistsException();

		ledgerRepository.save(new LedgerEntity(contract, publicKey));
	}

	public void loadMoney(String contract, Long value) {
		ledgerRepository.updateValueByContract(contract, value);
	}

}
