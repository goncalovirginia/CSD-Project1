package csd.server.services;

import bftsmart.demo.map.MapRequestType;
import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class BFTLedgerServer<K, V> extends DefaultSingleRecoverable {

	private final Logger logger;
	private Map<K, V> ledger;

	public BFTLedgerServer(@Value("${server.id}") int id) {
		this.ledger = new TreeMap<>();
		this.logger = Logger.getLogger(BFTLedgerServer.class.getName());
		new ServiceReplica(id, this, this);
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: java ... BFTLedgerServer <serverID>");
			System.exit(-1);
		}
		new BFTLedgerServer<String, String>(Integer.parseInt(args[0]));
	}

	@Override
	public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
		byte[] reply = null;
		K key;
		V value;
		boolean hasReply = false;
		try (ByteArrayInputStream byteIn = new ByteArrayInputStream(command); ObjectInput objIn = new ObjectInputStream(byteIn); ByteArrayOutputStream byteOut = new ByteArrayOutputStream(); ObjectOutput objOut = new ObjectOutputStream(byteOut);) {
			MapRequestType reqType = (MapRequestType) objIn.readObject();
			switch (reqType) {
				case PUT:
					key = (K) objIn.readObject();
					value = (V) objIn.readObject();

					V oldValue = ledger.put(key, value);
					if (oldValue != null) {
						objOut.writeObject(oldValue);
						hasReply = true;
					}
					break;
				case GET:
					key = (K) objIn.readObject();
					value = ledger.get(key);
					if (value != null) {
						objOut.writeObject(value);
						hasReply = true;
					}
					break;
				case REMOVE:
					key = (K) objIn.readObject();
					value = ledger.remove(key);
					if (value != null) {
						objOut.writeObject(value);
						hasReply = true;
					}
					break;
				case SIZE:
					int size = ledger.size();
					objOut.writeInt(size);
					hasReply = true;
					break;
				case KEYSET:
					keySet(objOut);
					hasReply = true;
					break;
			}
			if (hasReply) {
				objOut.flush();
				byteOut.flush();
				reply = byteOut.toByteArray();
			} else {
				reply = new byte[0];
			}

		} catch (IOException | ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Ocurred during map operation execution", e);
		}
		return reply;
	}

	@SuppressWarnings("unchecked")
	@Override
	public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
		byte[] reply = null;
		K key = null;
		V value = null;
		boolean hasReply = false;

		try (ByteArrayInputStream byteIn = new ByteArrayInputStream(command);
			 ObjectInput objIn = new ObjectInputStream(byteIn);
			 ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			 ObjectOutput objOut = new ObjectOutputStream(byteOut);) {
			MapRequestType reqType = (MapRequestType) objIn.readObject();
			switch (reqType) {
				case GET:
					key = (K) objIn.readObject();
					value = ledger.get(key);
					if (value != null) {
						objOut.writeObject(value);
						hasReply = true;
					}
					break;
				case SIZE:
					int size = ledger.size();
					objOut.writeInt(size);
					hasReply = true;
					break;
				case KEYSET:
					keySet(objOut);
					hasReply = true;
					break;
				default:
					logger.log(Level.WARNING, "in appExecuteUnordered only read operations are supported");
			}
			if (hasReply) {
				objOut.flush();
				byteOut.flush();
				reply = byteOut.toByteArray();
			} else {
				reply = new byte[0];
			}
		} catch (IOException | ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Ocurred during map operation execution", e);
		}

		return reply;
	}

	private void keySet(ObjectOutput out) throws IOException, ClassNotFoundException {
		Set<K> keySet = ledger.keySet();
		int size = ledger.size();
		out.writeInt(size);
		for (K key : keySet)
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
			ledger = (Map<K, V>) objIn.readObject();
		} catch (IOException | ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Error while installing snapshot", e);
		}
	}

}
