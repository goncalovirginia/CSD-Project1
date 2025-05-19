package csd.server.services;

import csd.server.exceptions.ContractDoesNotExistException;
import csd.server.models.LedgerEntity;
import csd.server.models.LogEntity;
import csd.server.repositories.LedgerRepository;
import csd.server.repositories.LogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LedgerService {

	private final LedgerRepository ledgerRepository;
	private final LogRepository logRepository;
	private final BFTSMaRTLedgerClient BFTSMaRTLedgerClient;

	public LedgerService(LedgerRepository ledgerRepository, LogRepository logRepository, BFTSMaRTLedgerClient BFTSMaRTLedgerClient) {
		this.ledgerRepository = ledgerRepository;
		this.logRepository = logRepository;
		this.BFTSMaRTLedgerClient = BFTSMaRTLedgerClient;
	}

	public void initBFTSMaRTLedgerClient() {
		BFTSMaRTLedgerClient.init();
	}

	public LedgerEntity getLedgerEntity(String contract) {
		return ledgerRepository.getLedgerByContract(contract).orElseThrow(ContractDoesNotExistException::new);
	}

	public void createContract(String contract, String hmacKey, String publicKey) {
		BFTSMaRTLedgerClient.createContract(contract, hmacKey, publicKey);
	}

	@Async("queueExecutor")
	public void createContractDB(String contract, String hmacKey, String publicKey) {
		ledgerRepository.save(new LedgerEntity(contract, hmacKey, publicKey));
		logRepository.save(new LogEntity("CREATE_CONTRACT", contract, null));
	}

	public void loadMoney(String contract, Long value) {
		BFTSMaRTLedgerClient.loadMoney(contract, value);
	}

	@Async("queueExecutor")
	public void loadMoneyDB(String contract, Long value) {
		ledgerRepository.updateValueByContract(contract, value);
		logRepository.save(new LogEntity("LOAD_MONEY " + value, contract, null));
	}

	public void sendTransaction(String originContract, String destinationContract, Long value) {
		BFTSMaRTLedgerClient.sendTransaction(originContract, destinationContract, value);
	}

	@Async("queueExecutor")
	public void sendTransactionDB(String originContract, String destinationContract, List<Long> bftAccountValues, Long value) {
		ledgerRepository.updateValueByContract(originContract, bftAccountValues.get(0));
		ledgerRepository.updateValueByContract(destinationContract, bftAccountValues.get(1));
		logRepository.save(new LogEntity("SEND_TRANSACTION " + value, originContract, destinationContract));
	}

	public long getBalance(String contract) {
		return ledgerRepository.getLedgerByContract(contract).orElseThrow(ContractDoesNotExistException::new).getValue();
	}

	public List<LogEntity> getExtract(String contract) {
		return logRepository.getLogEntitiesByContract(contract);
	}

	public long getTotalValue(List<String> contracts) {
		return ledgerRepository.getTotalValue(contracts);
	}

	public long getGlobalLedgerValue() {
		return ledgerRepository.getGlobalLedgerValue();
	}

	public List<LedgerEntity> getLedger() {
		return ledgerRepository.getAll();
	}

}
