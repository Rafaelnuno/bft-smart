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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class BFTMapServer<K, V> extends DefaultSingleRecoverable {
    private final Logger logger = LoggerFactory.getLogger("bftsmart");
    private final ServiceReplica replica;
    private TreeMap<K, V> replicaMap;
    private int counter = 0;

    // The constructor passes the id of the server to the super class
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
            counter += 1;
            BFTMapMessage<K, V> response = new BFTMapMessage<>();
            BFTMapMessage<K, V> request = BFTMapMessage.fromBytes(command);
            BFTMapRequestType cmd = request.getType();

            logger.info("Ordered execution of a {} request from {}", cmd, msgCtx.getSender());
            System.out.println(cmd);
            switch (cmd) {
                // write operations on the map
                case PUT:
                    V oldValue = replicaMap.put(request.getKey(), request.getValue());

                    if (oldValue != null) {
                        response.setValue(oldValue);
                    }
                    return BFTMapMessage.toBytes(response);
                case CHECK_OWNERSHIP:

                    String[] ownTokens = request.getValue().toString().split("\\|");
                    Set<K> keySetOwn = replicaMap.keySet();

                    for (K key : keySetOwn) {
                        String curr = (String) replicaMap.get(key);
                        String[] vTokens = curr.toString().split("\\|");

                        if (vTokens[0].equals(ownTokens[1]) && vTokens[1].equals(ownTokens[2])) {
                            response.setValue(1);
                            return BFTMapMessage.toBytes(response);
                        }
                    }

                    response.setValue(0);
                    return BFTMapMessage.toBytes(response);

                case SPEND:
                    response.setValue(spend(request));
                    return BFTMapMessage.toBytes(response);
                case MINT:
                    String[] coinTokens = request.getValue().toString().split("\\|");
                    String clientId = coinTokens[1];
                    String value = coinTokens[2];

                    // only user with id=0 has permission to MINT coins and value contains only
                    // digits
                    if (clientId.equals("4") && value.matches("\\d+")) {
                        V oldV = replicaMap.put(request.getKey(), request.getValue());
                        if (oldV != null) {
                            response.setValue(oldV);
                        } else {
                            response.setValue(request.getKey());
                        }

                        return BFTMapMessage.toBytes(response);
                    } else {
                        response.setValue("0");
                        return BFTMapMessage.toBytes(response);
                    }

                case MINT_NFT:

                    String[] nftTokens = request.getValue().toString().split("\\|");
                    String nftName = nftTokens[2];

                    boolean nftExists = false;
                    for (Map.Entry<K, V> nftEntry : replicaMap.entrySet()) {
                        V nftValue = nftEntry.getValue();
                        String[] nft = ((String) nftValue).split("\\|");
                        if (nft[0].equals("nft") && nftName.equals(nft[2])) {
                            System.out.println("entrei1");
                            nftExists = true;
                            break;
                        }
                    }
                    System.out.println(nftExists);
                    if (nftExists) {
                        System.out.println("entrei");
                        response.setValue("0");
                        return BFTMapMessage.toBytes(response);
                    }

                    V oldV = replicaMap.put(request.getKey(), request.getValue());
                    if (oldV != null) {
                        response.setValue("1");
                    } else {
                        response.setValue(request.getKey());
                    }

                    return BFTMapMessage.toBytes(response);

                case REQUEST_NFT_TRANSFER:
                    String[] transferTokens = request.getValue().toString().split("\\|");
                    String clienId = transferTokens[1];
                    String nftId = transferTokens[2];
                    int val = Integer.parseInt(transferTokens[4]);
                    String[] coinsId = transferTokens[3].toString().split(",");
                    int size_coinsId = coinsId.length;
                    int nft_exists = 0, coin_exists = 0, alr_req = 0;
                    int total_amount = 0;
                    Set<K> keySet = replicaMap.keySet();
                    System.out.println("Tree before request nft transfer");

                    // Check if nft exists
                    for (K key : keySet) {
                        V nftEnt = replicaMap.get(key);
                        String[] nft_token = nftEnt.toString().split("\\|");
                        if (nft_token[0].equals("nft") && nft_token[4].equals(nftId)) {
                            nft_exists = 1;
                        }

                    }

                    // Check if coinid exists and coin belong to clientId and total amount
                    for (K key : keySet) {
                        V nftEnt = replicaMap.get(key);
                        String[] nft_token = nftEnt.toString().split("\\|");
                        for (String c : coinsId) {
                            if (nft_token[0].equals("coin") && nft_token[3].equals(c) && nft_token[1].equals(clienId)) {
                                coin_exists += 1;
                                total_amount += Integer.parseInt(nft_token[2]);
                            }
                        }
                    }

                    // Check if there's already a req
                    for (K key : keySet) {

                        V nftEntry = replicaMap.get(key);
                        System.out.println(key + " : " + nftEntry + "\n");
                        String[] token = nftEntry.toString().split("\\|");
                        if (token[0].equals("nft_request") && clienId.equals(token[1]) && nftId.equals(token[2])) {
                            alr_req = 1;
                            response.setValue("0");
                            return BFTMapMessage.toBytes(response);
                        }
                    }

                    if (size_coinsId == coin_exists && total_amount >= val && nft_exists == 1 && alr_req == 0) {
                        replicaMap.put(request.getKey(), request.getValue());
                        response.setValue("1");
                        return BFTMapMessage.toBytes(response);
                    } else {
                        response.setValue("0");
                        return BFTMapMessage.toBytes(response);
                    }

                case CANCEL_REQUEST_NFT_TRANSFER:
                    String[] cancelTokens = request.getValue().toString().split("\\|");
                    String IssuerId = cancelTokens[1];
                    String nftID = cancelTokens[2];
                    int nft_canc_exists = 0;
                    Set<K> keySetCancel = replicaMap.keySet();

                    for (Map.Entry<K, V> entry : replicaMap.entrySet()) {
                        String[] entryTokens = entry.getValue().toString().split("\\|");
                        if (entryTokens[0].equals("nft_request") && entryTokens[1].equals(IssuerId)
                                && entryTokens[2].equals(nftID)) {
                            replicaMap.remove(entry.getKey());
                            response.setValue("1");
                            return BFTMapMessage.toBytes(request);
                        }
                    }

                    response.setValue("0");
                    return BFTMapMessage.toBytes(response);

                case PROCESS_NFT_TRANSFER:

                    String[] processTokens = request.getValue().toString().split("\\|");
                    String cliId = processTokens[1];
                    String nId = processTokens[2];
                    String buyerId = processTokens[3];
                    String accept = processTokens[4];
                    Set<K> keSet = replicaMap.keySet();
                    int own = 0, exist = 0, has_money = 0, validity = 0 , money = 0;
                    int v = 0, spendDone = 0, nftOwnerTrsf = 0;
                    ArrayList<String> coinsIdList = new ArrayList<>();
                    LocalDateTime now = LocalDateTime.now();

                    // Checking ownership
                    for (K key : keSet) {
                        String curr = (String) replicaMap.get(key);
                        String[] vTokens = curr.toString().split("\\|");

                        if (vTokens[0].equals("nft") && vTokens[4].equals(nId)) {
                            if (!vTokens[1].equals(cliId)) {
                                System.out.println("The user doesn't own this nft\n");
                                break;
                            } else {
                                own = 1;
                                break;
                            }
                        }
                    }

                    for (K key : keSet) {

                        String curr = (String) replicaMap.get(key);
                        String[] vTokens = curr.toString().split("\\|");

                        if (vTokens[0].equals("nft_request") && vTokens[2].equals(nId)
                                && vTokens[1].equals(buyerId)) {
                            exist = 1;
                            v = Integer.parseInt(vTokens[4]);
                            coinsIdList.add(curr);

                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            LocalDateTime date = LocalDateTime.parse(vTokens[5], formatter);
                            if (date.isAfter(now)) {
                                validity = 1;
                            } else {
                                validity = 0;
                            }

                        }

                    }

                    for( String c : coinsIdList) {
                        String [] normalized = c.toString().split("\\|");
                        money += Integer.parseInt(normalized[2]);
                    }
                    System.out.println(own);
                    System.out.println(money >= v);
                    System.out.println(exist);
                    System.out.println(validity);
                    if (own == 1 && money >= v && exist == 1 && validity == 1) {
                        System.out.println("All ckecked");
                        if (accept.equals("true")) {

                            String allcoins = "";

                            for( String c : coinsIdList) {
                                String [] normalized = c.toString().split("\\|");
                                allcoins += normalized[3];
                                allcoins += ",";
                            }

                               
                            System.out.println("Strt spending");
                            String spendCommand = "spend" + "|" + buyerId + "|" + allcoins + "|" + cliId
                                    + "|"
                                    + v;

                            BFTMapMessage<K, V> n_req = new BFTMapMessage<>();
                            n_req.setType(BFTMapRequestType.SPEND);
                            n_req.setKey(request.getKey());
                            n_req.setValue(spendCommand);

                            if (spend(n_req) == 1) {
                                spendDone = 1;
                            } else {
                                spendDone = 0;
                            }

                            System.out.println("Nft Searchin");
                            for (K k : keSet) {

                                String incurr = (String) replicaMap.get(k);
                                System.out.println(k + " : " + incurr + "\n");
                                String[] invTokens = incurr.toString().split("\\|");

                                if (invTokens[0].equals("nft") && invTokens[4].equals(nId)) {

                                    System.out.println("Entered");
                                    String new_nft = "nft" + "|" + buyerId + "|" + invTokens[2] + "|"
                                            + invTokens[3] + "|"
                                            + invTokens[4];
                                    replicaMap.put(k, (V) new_nft);
                                    nftOwnerTrsf = 1;
                                    break;

                                }

                            }

                            if (spendDone == 1 & nftOwnerTrsf == 1) {
                                response.setValue(1);
                                return BFTMapMessage.toBytes(response);
                            } else {
                                response.setValue(0);
                                return BFTMapMessage.toBytes(response);
                            }

                        } else {
                        System.out.println("False");
                        response.setValue(0);
                        return BFTMapMessage.toBytes(response);
                    }
                    } else {
                        System.out.println("Checking went wrong");
                        response.setValue(0);
                        return BFTMapMessage.toBytes(response);
                    }

            }

            return null;
        } catch (IOException | ClassNotFoundException ex) {
            logger.error("Failed to process ordered request", ex);
            return new byte[0];
        }
    }

    public int spend(BFTMapMessage<K, V> request) throws IOException {
        BFTMapMessage<K, V> response = new BFTMapMessage<>();
                    int sum_coins = 0;
                	int remaining_value = 0;
                	String[] spend = request.getValue().toString().split("\\|");
                    Set<K> keSet = replicaMap.keySet();
                	for (String coin : spend[2].split(",")) {

                        for (K k : keSet) {
                            try {
                                String[] ret = replicaMap.get(k).toString().split("\\|");
                                // Check if the coin belongs to this client
                                if (Integer.parseInt(spend[1]) == Integer.parseInt(ret[1]) && ret[3].equals(coin)) {
                                    sum_coins += Integer.parseInt(ret[2]); 
                                    replicaMap.remove(k);
                                    
                                }else {
                                    response.setValue(0);
                                }
                            }
                            // Check if the coin exists
                            catch(NumberFormatException | NullPointerException e) {
                                response.setValue(0); 
                                
                            }
                        }
                		
                	}
                    System.out.println(sum_coins);
                	if(sum_coins >= Integer.parseInt(spend[4])) {

                		//create new coin for the receiver 
                		V receiver_coin = (V) ("coin"+ "|" + spend[3] + "|" + spend[4] + "|" + counter+5);
                		replicaMap.put(request.getKey(), receiver_coin);
                        System.out.println("1");
                        Set<K> keys = replicaMap.keySet();
                        //Set<K> keys2 = replicaMap.keySet();
                        for (K k : keys) {
                        System.out.println(k + " : " + replicaMap.get(k) + "\n");
                        }
                        // //remove all sender coins used in the transaction
                		// for (String used_coin : spend[2].split(",")) {

                        //     for(K k : keys2) {

                        //         String[] ret = replicaMap.get(k).toString().split("\\|");

                        //         if(ret[0].equals("coin") && ret[3].equals(used_coin)) {
                        //             V removedValue = replicaMap.remove(k);   
                        //         }
                        //     }
                			
                        //}
                        System.out.println("2");
                        for (K k : keys) {
                            System.out.println(k + " : " + replicaMap.get(k) + "\n");
                        }

                        //create new coin for the sender with the remaining value
                		remaining_value = sum_coins - Integer.parseInt(spend[4]);
                		V sender_coin = (V) ("coin"+ "|" + spend[1] + "|" + remaining_value + "|" + counter+10); 
                		K key = (K) Integer.valueOf(Integer.valueOf(request.getKey().toString())+1);
                		V oldVal = replicaMap.put(key, sender_coin);
                        System.out.println("3");
                        for (K k : keys) {
                            System.out.println(k + " : " + replicaMap.get(k) + "\n");
                            }
                		if(oldVal != null) {
                            response.setValue(oldVal);
                        }else {
                        	response.setValue(key);
                        }
                		
                	}
                	
                    response.setValue(request.getValue()); //TODO change this to new coin
                    
                	return 1;
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        try {
            counter += 1;
            BFTMapMessage<K, V> response = new BFTMapMessage<>();
            BFTMapMessage<K, V> request = BFTMapMessage.fromBytes(command);
            BFTMapRequestType cmd = request.getType();

            logger.info("Unordered execution of a {} request from {}", cmd, msgCtx.getSender());

            switch (cmd) {
                // read operations on the map
                case GET:
                    V ret = replicaMap.get(request.getKey());

                    System.out.println(ret);

                    if (ret != null) {
                        System.out.println("Thre is a value");
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
            ex.printStackTrace(); // debug instruction
            return new byte[0];
        }
    }

    @Override
    public void installSnapshot(byte[] state) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(state);
                ObjectInput in = new ObjectInputStream(bis)) {
            replicaMap = (TreeMap<K, V>) in.readObject();
        } catch (ClassNotFoundException | IOException ex) {
            ex.printStackTrace(); // debug instruction
        }
    }

}
