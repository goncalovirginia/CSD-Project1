package csd.server.services;

import csd.server.exceptions.ContractAlreadyExistsException;
import csd.server.exceptions.ContractDoesNotExistException;
import csd.server.exceptions.InsufficientFundsException;
import csd.server.models.LedgerEntity;
import csd.server.repositories.LedgerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

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

	@Transactional
	public void sendTransaction(String originContract, String destinationContract, Long value) {
		if (ledgerRepository.withdraw(originContract, value) == 0)
			throw new InsufficientFundsException();

		if (ledgerRepository.deposit(destinationContract, value) == 0)
			throw new ContractDoesNotExistException();
	}

	public long getBalance(String contract) {
		return ledgerRepository.getLedgerByContract(contract).orElseThrow(ContractDoesNotExistException::new).getValue();
	}

	public String getExtract(String contract) {
		LedgerEntity l = ledgerRepository.getLedgerByContract(contract).orElseThrow(ContractDoesNotExistException::new);
		return l.getContract() + ":" + l.getValue() + ":" + l.getPublicKey();
	}

	public long getTotalValue(List<String> contracts) {
		return ledgerRepository.getTotalValue(contracts);
	}

	public long getGlobalLedgerValue() {
		return ledgerRepository.getGlobalLedgerValue();
	}

	public List<String> getLedger() {
		return ledgerRepository.getAll().stream().map(LedgerEntity::toString).toList();
	}

}
