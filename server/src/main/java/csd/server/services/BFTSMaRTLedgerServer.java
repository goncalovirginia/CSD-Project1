package csd.server.services;

import bftsmart.demo.map.MapRequestType;
import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class BFTSMaRTLedgerServer extends DefaultSingleRecoverable {

	private final Logger logger;
	private final LedgerService ledgerService;
	private Map<String, Long> ledger;

	public BFTSMaRTLedgerServer(@Value("${server.id}") int id, LedgerService ledgerService) {
		this.ledger = new TreeMap<>();
		this.logger = Logger.getLogger(BFTSMaRTLedgerServer.class.getName());
		this.ledgerService = ledgerService;
		new ServiceReplica(id, this, this);
	}

	@Override
	public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
		byte[] reply = new byte[0];
		String originContract, destinationContract;
		Long value;
		boolean hasReply = false;
		try (ByteArrayInputStream byteIn = new ByteArrayInputStream(command); ObjectInput objIn = new ObjectInputStream(byteIn); ByteArrayOutputStream byteOut = new ByteArrayOutputStream(); ObjectOutput objOut = new ObjectOutputStream(byteOut);) {
			LedgerRequestType reqType = (LedgerRequestType) objIn.readObject();
			switch (reqType) {
				case CREATE_CONTRACT:
					originContract = (String) objIn.readObject();
					String hmacKey = (String) objIn.readObject();
					String publicKey = (String) objIn.readObject();

					if (ledger.containsKey(originContract))
						objOut.writeObject(LedgerResponseCode.CONTRACT_ALREADY_EXISTS);
					else {
						ledger.put(originContract, 0L);

						objOut.writeObject(LedgerResponseCode.OK);
						objOut.writeObject(originContract);

						logger.log(Level.INFO, "Created contract: " + originContract);
						this.ledgerService.createContractDB(originContract, hmacKey, publicKey);
					}
					hasReply = true;
					break;
				case LOAD_MONEY:
					originContract = (String) objIn.readObject();
					value = (Long) objIn.readObject();

					if (!ledger.containsKey(originContract))
						objOut.writeObject(LedgerResponseCode.CONTRACT_DOES_NOT_EXIST);
					else {
						Long newValue = ledger.get(originContract) + value;
						ledger.put(originContract, newValue);

						objOut.writeObject(LedgerResponseCode.OK);
						objOut.writeObject(originContract);
						objOut.writeObject(newValue);

						logger.log(Level.INFO, "Loaded money: " + originContract + " " + newValue);
						this.ledgerService.loadMoneyDB(originContract, newValue);
					}
					hasReply = true;
					break;
				case SEND_TRANSACTION:
					originContract = (String) objIn.readObject();
					destinationContract = (String) objIn.readObject();
					value = (Long) objIn.readObject();

					Long originValue = ledger.get(originContract);
					Long destinationValue = ledger.get(destinationContract);

					if (originValue == null || destinationValue == null)
						objOut.writeObject(LedgerResponseCode.CONTRACT_DOES_NOT_EXIST);
					else if (originValue < value)
						objOut.writeObject(LedgerResponseCode.INSUFFICIENT_FUNDS);
					else {
						originValue -= value;
						destinationValue += value;

						ledger.put(originContract, originValue);
						ledger.put(destinationContract, destinationValue);

						objOut.writeObject(LedgerResponseCode.OK);
						objOut.writeObject(originContract);
						objOut.writeObject(originValue);
						objOut.writeObject(destinationContract);
						objOut.writeObject(destinationValue);

						logger.log(Level.INFO, "Sent transaction: " + originContract + " -> " + destinationContract + " " + value);
						this.ledgerService.sendTransactionDB(originContract, destinationContract, List.of(originValue, destinationValue), value);
					}
					hasReply = true;
					break;
			}
			if (hasReply) {
				objOut.flush();
				byteOut.flush();
				reply = byteOut.toByteArray();
			}
		} catch (IOException | ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Ocurred during map operation execution", e);
		}
		return reply;
	}

	@SuppressWarnings("unchecked")
	@Override
	public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
		try (ByteArrayInputStream byteIn = new ByteArrayInputStream(command); ObjectInput objIn = new ObjectInputStream(byteIn); ByteArrayOutputStream byteOut = new ByteArrayOutputStream(); ObjectOutput objOut = new ObjectOutputStream(byteOut);) {
			MapRequestType reqType = (MapRequestType) objIn.readObject();
		} catch (IOException | ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Ocurred during map operation execution", e);
		}

		return new byte[0];
	}

	private void keySet(ObjectOutput out) throws IOException {
		Set<String> keySet = ledger.keySet();
		int size = ledger.size();
		out.writeInt(size);
		for (String key : keySet)
			out.writeObject(key);
	}

	@Override
	public byte[] getSnapshot() {
		try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream(); ObjectOutput objOut = new ObjectOutputStream(byteOut)) {
			objOut.writeObject(ledger);
			return byteOut.toByteArray();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error while taking snapshot", e);
		}
		return new byte[0];
	}

	@SuppressWarnings("unchecked")
	@Override
	public void installSnapshot(byte[] state) {
		try (ByteArrayInputStream byteIn = new ByteArrayInputStream(state); ObjectInput objIn = new ObjectInputStream(byteIn)) {
			ledger = (Map<String, Long>) objIn.readObject();
		} catch (IOException | ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Error while installing snapshot", e);
		}
	}

	public enum LedgerRequestType {
		CREATE_CONTRACT, LOAD_MONEY, SEND_TRANSACTION
	}

	public enum LedgerResponseCode {
		OK, CONTRACT_ALREADY_EXISTS, CONTRACT_DOES_NOT_EXIST, INSUFFICIENT_FUNDS
	}

}
