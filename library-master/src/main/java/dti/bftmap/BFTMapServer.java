/**
 * BFT Map implementation (server side).
 *
 */
package dti.bftmap;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public class BFTMapServer<K, V> extends DefaultSingleRecoverable {
    private final Logger logger = LoggerFactory.getLogger("bftsmart");
    private final ServiceReplica replica;
    private TreeMap<K, V> replicaMap;
    

    //The constructor passes the id of the server to the super class
    public BFTMapServer(int id) {
        replicaMap = new TreeMap<>();
        replica = new ServiceReplica(id, this, this);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Use: java BFTMapServer <server id>");
            System.exit(-1);
        }
        new BFTMapServer<Integer, String>(Integer.parseInt(args[0]));
    }

    @Override
    public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
        try {
            BFTMapMessage<K,V> response = new BFTMapMessage<>();
            BFTMapMessage<K,V> request = BFTMapMessage.fromBytes(command);
            BFTMapRequestType cmd = request.getType();

            logger.info("Ordered execution of a {} request from {}", cmd, msgCtx.getSender());
            System.out.println(cmd);
            switch (cmd) {
                //write operations on the map
                case PUT:
                    V oldValue = replicaMap.put(request.getKey(), request.getValue());

                    if(oldValue != null) {
                        response.setValue(oldValue);
                    }
                    return BFTMapMessage.toBytes(response);
                case SIZE:
                    
                case REMOVE:
                case MINT:
                    List<Object> list = (List<Object>) request.getValue();
                    Coin coin = new Coin((Integer)list.get(1), (Float)list.get(2),new Random().nextLong());
                    replicaMap.put((K)request.getKey(), (V) coin);
                    response.setValue(0);
                    return BFTMapMessage.toBytes(response);
               /* case SPEND:
                String[] spendTokens = request.getValue().toString().split("\\|");
                String[] coinIds = spendTokens[1].split(",");
                String receiverId = spendTokens[2];
                int valueToTransfer = Integer.parseInt(spendTokens[3]);
                String spclientId = spendTokens[4];
            
                // check if all the coins are valid and belong to the sender
                int senderBalance = 0;
                for (String coinId : coinIds) {
                    int key = Integer.parseInt(coinId);
                    String coin = (String) replicaMap.get(key);
                    if (coin != null) {
                        String[] coinValues = coin.split("\\|");
                        if (coinValues[0].equals("coin") && coinValues[1].equals(spclientId)) {
                            senderBalance += Integer.parseInt(coinValues[2]);
                            replicaMap.remove(key); // remove the spent coin from the map
                        } else {
                            response.setValue(0);
                            return BFTMapMessage.toBytes(response);
                        }
                    } else {
                        response.setValue(0);
                        return BFTMapMessage.toBytes(response);
                    }
                }

                // check if the sender has enough balance to transfer
                if (senderBalance < valueToTransfer) {
                    System.out.println(senderBalance);
                    response.setValue(0);
                    return BFTMapMessage.toBytes(response);
                }

                // transfer the value to the receiver and update the sender's balance
                String spendResult = "spend|" + spendTokens[1] + "|" + receiverId + "|" + valueToTransfer;
                V oldValue2 = replicaMap.put(request.getKey(), (V) spendResult);
                senderBalance -= valueToTransfer;

                if (oldValue2 != null) {
                    response.setValue(oldValue2);
                } else {
                    response.setValue(request.getKey());
                }

            return BFTMapMessage.toBytes(response);
                */
            }

            return null;
        }catch (IOException | ClassNotFoundException ex) {
            logger.error("Failed to process ordered request", ex);
            return new byte[0];
        }
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        try {
            BFTMapMessage<K,V> response = new BFTMapMessage<>();
            BFTMapMessage<K,V> request = BFTMapMessage.fromBytes(command);
            BFTMapRequestType cmd = request.getType();

            logger.info("Unordered execution of a {} request from {}", cmd, msgCtx.getSender());

            switch (cmd) {
                //read operations on the map
                case GET:
                    V ret = replicaMap.get(request.getKey());

                    if (ret != null) {
                        response.setValue(ret);
                    }
                    return BFTMapMessage.toBytes(response);
                
                case KEYSET:
                    Set<K> keySet = replicaMap.keySet();
                    response.setKeySet(keySet);

                    return BFTMapMessage.toBytes(response);
            }
        } catch (IOException | ClassNotFoundException ex) {
            logger.error("Failed to process unordered request", ex);
            return new byte[0];
        }
        return null;
    }

    @Override
    public byte[] getSnapshot() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(replicaMap);
            out.flush();
            bos.flush();
            return bos.toByteArray();
        } catch (IOException ex) {
            ex.printStackTrace(); //debug instruction
            return new byte[0];
        }
    }

    @Override
    public void installSnapshot(byte[] state) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(state);
             ObjectInput in = new ObjectInputStream(bis)) {
            replicaMap = (TreeMap<K, V>) in.readObject();
        } catch (ClassNotFoundException | IOException ex) {
            ex.printStackTrace(); //debug instruction
        }
    }

}
