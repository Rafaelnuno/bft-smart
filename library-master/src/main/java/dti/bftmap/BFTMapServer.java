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
import java.util.List;
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
                case SPEND:
                int totalCoins = 0;
                int remainingValue = 0;
                String[] spend = request.getValue().toString().split("\\|");
                List<Integer> usedCoins = new ArrayList<>();
                int senderId = Integer.parseInt(spend[1]);
                int receiverId = Integer.parseInt(spend[3]);
                int spendValue = Integer.parseInt(spend[4]);
                
                for (String coin : spend[2].split(",")) {
                    try {
                        String[] ret = replicaMap.get(Integer.parseInt(coin)).toString().split("\\|");
                        int coinValue = Integer.parseInt(ret[2]);
                        int coinOwnerId = Integer.parseInt(ret[1]);
                
                        // Check if the coin belongs to this client
                        if (coinOwnerId != senderId) {
                            response.setValue(0); 
                            return BFTMapMessage.toBytes(response);
                        }
                
                        totalCoins += coinValue;
                        usedCoins.add(Integer.parseInt(coin));
                    } catch(NumberFormatException | NullPointerException e) {
                        response.setValue(0); 
                        return BFTMapMessage.toBytes(response);
                    }
                }
                
                if(totalCoins < spendValue) {
                    response.setValue(0); 
                    return BFTMapMessage.toBytes(response);
                }
                
                remainingValue = totalCoins - spendValue;
                V receiverCoin = (V) ("coin"+ "|" + spend[3] + "|" + spend[4]);
                replicaMap.put(request.getKey(), receiverCoin);
                
                for (Integer usedCoin : usedCoins) {
                    replicaMap.remove(usedCoin);
                }
                
                if (remainingValue > 0) {
                    V senderCoin = (V) ("coin"+ "|" + spend[1] + "|" + remainingValue); 
                    K key = (K) Integer.valueOf(Integer.valueOf(request.getKey().toString())+1);
                    V oldVal = replicaMap.put(key, senderCoin);
                
                    if(oldVal != null) {
                        response.setValue(oldVal);
                    } else {
                        response.setValue(key);
                    }
                }
                
                return BFTMapMessage.toBytes(response);
                case MINT:
                	String[] coinTokens = request.getValue().toString().split("\\|");
                	String clientId = coinTokens[1];
                	String value = coinTokens[2];
                	
                	//only user with id=0 has permission to MINT coins and value contains only digits
                	if(clientId.equals("4") && value.matches("\\d+")) {
                		V oldV = replicaMap.put(request.getKey(), request.getValue());
                        if(oldV != null) {
                            response.setValue(oldV);
                        }else {
                        	response.setValue(request.getKey());
                        }

                        return BFTMapMessage.toBytes(response);
                	}
                    break;
                
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
