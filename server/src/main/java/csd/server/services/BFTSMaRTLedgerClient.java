package csd.server.services;

import bftsmart.tom.ServiceProxy;
import csd.server.exceptions.BFTException;
import csd.server.exceptions.ContractAlreadyExistsException;
import csd.server.exceptions.ContractDoesNotExistException;
import csd.server.exceptions.InsufficientFundsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;

@Service
public class BFTSMaRTLedgerClient {

	private final ServiceProxy serviceProxy;

	public BFTSMaRTLedgerClient(@Value("${server.id}") int id) {
		this.serviceProxy = new ServiceProxy(id);
	}

	public String createContract(String contract) {
		try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream(); ObjectOutput objOut = new ObjectOutputStream(byteOut)) {
			objOut.writeObject(BFTSMaRTLedgerServer.LedgerRequestType.CREATE_CONTRACT);
			objOut.writeObject(contract);

			objOut.flush();
			byteOut.flush();

			byte[] reply = serviceProxy.invokeOrdered(byteOut.toByteArray());
			if (reply.length == 0)
				throw new BFTException();
			try (ByteArrayInputStream byteIn = new ByteArrayInputStream(reply); ObjectInput objIn = new ObjectInputStream(byteIn)) {
				if (objIn.readObject() == BFTSMaRTLedgerServer.LedgerResponseCode.CONTRACT_ALREADY_EXISTS)
					throw new ContractAlreadyExistsException();

				return (String) objIn.readObject();
			}
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Exception putting contract into ledger: " + e.getMessage());
		}
		throw new BFTException();
	}

	public long loadMoney(String contract, long value) {
		try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream(); ObjectOutput objOut = new ObjectOutputStream(byteOut)) {
			objOut.writeObject(BFTSMaRTLedgerServer.LedgerRequestType.LOAD_MONEY);
			objOut.writeObject(contract);
			objOut.writeObject(value);

			objOut.flush();
			byteOut.flush();

			byte[] reply = serviceProxy.invokeOrdered(byteOut.toByteArray());
			if (reply.length == 0)
				throw new BFTException();
			try (ByteArrayInputStream byteIn = new ByteArrayInputStream(reply); ObjectInput objIn = new ObjectInputStream(byteIn)) {
				if (objIn.readObject() == BFTSMaRTLedgerServer.LedgerResponseCode.CONTRACT_DOES_NOT_EXIST)
					throw new ContractDoesNotExistException();

				String responseContract = (String) objIn.readObject();
				return (Long) objIn.readObject();
			}
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Exception putting value into ledger: " + e.getMessage());
		}
		throw new BFTException();
	}

	public List<Long> sendTransaction(String originContract, String destinationContract, long value) {
		try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream(); ObjectOutput objOut = new ObjectOutputStream(byteOut)) {
			objOut.writeObject(BFTSMaRTLedgerServer.LedgerRequestType.SEND_TRANSACTION);
			objOut.writeObject(originContract);
			objOut.writeObject(destinationContract);
			objOut.writeObject(value);

			objOut.flush();
			byteOut.flush();

			byte[] reply = serviceProxy.invokeOrdered(byteOut.toByteArray());
			if (reply.length == 0)
				throw new BFTException();
			try (ByteArrayInputStream byteIn = new ByteArrayInputStream(reply); ObjectInput objIn = new ObjectInputStream(byteIn)) {
				BFTSMaRTLedgerServer.LedgerResponseCode responseCode = (BFTSMaRTLedgerServer.LedgerResponseCode) objIn.readObject();
				if (responseCode == BFTSMaRTLedgerServer.LedgerResponseCode.CONTRACT_DOES_NOT_EXIST)
					throw new ContractDoesNotExistException();
				if (responseCode == BFTSMaRTLedgerServer.LedgerResponseCode.INSUFFICIENT_FUNDS)
					throw new InsufficientFundsException();

				String responseOriginContract = (String) objIn.readObject();
				Long responseOriginValue = (Long) objIn.readObject();
				String responseDestinationContract = (String) objIn.readObject();
				Long responseDestinationValue = (Long) objIn.readObject();
				return List.of(responseOriginValue, responseDestinationValue);
			}
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Exception transferring value in ledger: " + e.getMessage());
		}
		throw new BFTException();
	}

}
