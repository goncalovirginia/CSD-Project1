package csd.server.services;

import csd.server.exceptions.ContractAlreadyExistsException;
import csd.server.exceptions.ContractDoesNotExistException;
import csd.server.models.LedgerEntity;
import csd.server.models.LogEntity;
import csd.server.repositories.LedgerRepository;
import csd.server.repositories.LogRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LedgerService {

	private final LedgerRepository ledgerRepository;
	private final LogRepository logRepository;
	private final BFTSMaRTLedgerClient BFTSMaRTLedgerClient;
	private final BFTSMaRTLedgerServer BFTSMaRTLedgerServer;

	public LedgerService(LedgerRepository ledgerRepository, LogRepository logRepository, BFTSMaRTLedgerClient BFTSMaRTLedgerClient, BFTSMaRTLedgerServer BFTSMaRTLedgerServer) {
		this.ledgerRepository = ledgerRepository;
		this.logRepository = logRepository;
		this.BFTSMaRTLedgerClient = BFTSMaRTLedgerClient;
		this.BFTSMaRTLedgerServer = BFTSMaRTLedgerServer;
	}

	public LedgerEntity getLedgerEntity(String contract) {
		return ledgerRepository.getLedgerByContract(contract).orElseThrow(ContractDoesNotExistException::new);
	}

	public void createContract(String contract, String hmacKey, String publicKey) {
		String bftContract = BFTSMaRTLedgerClient.createContract(contract);

		createContractAsync(bftContract, hmacKey, publicKey);
	}

	@Async
	protected void createContractAsync(String contract, String hmacKey, String publicKey) {
		ledgerRepository.save(new LedgerEntity(contract, hmacKey, publicKey));
		logRepository.save(new LogEntity("CREATE_CONTRACT", contract, null));
	}

	public void loadMoney(String contract, Long value) {
		Long bftValue = BFTSMaRTLedgerClient.loadMoney(contract, value);

		loadMoneyAsync(contract, bftValue);
	}

	@Async
	protected void loadMoneyAsync(String contract, Long value) {
		ledgerRepository.updateValueByContract(contract, value);
		logRepository.save(new LogEntity("LOAD_MONEY " + value, contract, null));
	}

	public void sendTransaction(String originContract, String destinationContract, Long value) {
		List<Long> bftAccountValues = BFTSMaRTLedgerClient.sendTransaction(originContract, destinationContract, value);

		sendTransactionAsync(originContract, destinationContract, bftAccountValues, value);
	}

	@Async
	protected void sendTransactionAsync(String originContract, String destinationContract, List<Long> bftAccountValues, Long value) {
		ledgerRepository.updateValueByContract(originContract, bftAccountValues.get(0));
		ledgerRepository.updateValueByContract(destinationContract, bftAccountValues.get(1));
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
