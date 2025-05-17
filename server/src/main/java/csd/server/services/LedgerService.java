package csd.server.services;

import csd.server.exceptions.ContractAlreadyExistsException;
import csd.server.exceptions.ContractDoesNotExistException;
import csd.server.exceptions.InsufficientFundsException;
import csd.server.models.LedgerEntity;
import csd.server.models.LogEntity;
import csd.server.repositories.LedgerRepository;
import csd.server.repositories.LogRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LedgerService {

	private final LedgerRepository ledgerRepository;
	private final LogRepository logRepository;
	private final BFTLedgerClient<String, Long> bftLedgerClient;
	private final BFTLedgerServer<String, Long> bftLedgerServer;

	public LedgerService(LedgerRepository ledgerRepository, LogRepository logRepository, BFTLedgerClient<String, Long> bftLedgerClient, BFTLedgerServer<String, Long> bftLedgerServer) {
		this.ledgerRepository = ledgerRepository;
		this.logRepository = logRepository;
		this.bftLedgerClient = bftLedgerClient;
		this.bftLedgerServer = bftLedgerServer;
	}

	public LedgerEntity getLedgerEntity(String contract) {
		return ledgerRepository.getLedgerByContract(contract).orElseThrow(ContractDoesNotExistException::new);
	}

	public void createContract(String contract, String hmacKey, String publicKey) {
		if (ledgerRepository.existsByContract(contract))
			throw new ContractAlreadyExistsException();

		ledgerRepository.save(new LedgerEntity(contract, hmacKey, publicKey));
		logRepository.save(new LogEntity("CREATE_CONTRACT", contract, null));
	}

	public void loadMoney(String contract, Long value) {
		ledgerRepository.updateValueByContract(contract, value);
		logRepository.save(new LogEntity("LOAD_MONEY " + value, contract, null));
	}

	@Transactional
	public void sendTransaction(String originContract, String destinationContract, Long value) {
		if (ledgerRepository.withdraw(originContract, value) == 0)
			throw new InsufficientFundsException();

		if (ledgerRepository.deposit(destinationContract, value) == 0)
			throw new ContractDoesNotExistException();

		logRepository.save(new LogEntity("SEND_TRANSACTION " + value, originContract, destinationContract));
	}

	public long getBalance(String contract) {
		return ledgerRepository.getLedgerByContract(contract).orElseThrow(ContractDoesNotExistException::new).getValue();
	}

	public String getExtract(String contract) {
		return ledgerRepository.getLedgerByContract(contract).orElseThrow(ContractDoesNotExistException::new).toString();
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
