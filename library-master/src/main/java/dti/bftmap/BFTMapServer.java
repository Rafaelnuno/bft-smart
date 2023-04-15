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
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public class BFTMapServer<K, V> extends DefaultSingleRecoverable {
    private final Logger logger = LoggerFactory.getLogger("bftsmart");
    private final ServiceReplica replica;
    private TreeMap<K, V> replicaMap;
    private K test;
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
                case REMOVE:
                case SPEND:
                    int totalCoins = 0;
                    int remainingValue = 0;
                    String[] spend = request.getValue().toString().split("\\|");
                    List<K> usedCoins = new ArrayList<>();
                    int senderId = Integer.parseInt(spend[1]);
                    int receiverId = Integer.parseInt(spend[3]);
                    int spendValue = Integer.parseInt(spend[4]);
                    Set<K> set = replicaMap.keySet();

                    for (K key : set) {

                        String[] ret = replicaMap.get(key).toString().split("\\|");
                        if (ret[0].equals("coin") && ret[1].equals(Integer.toString(senderId))) {
                            int coinValue = Integer.parseInt(ret[2]);
                            int coinOwnerId = Integer.parseInt(ret[1]);

                            // Check if the coin belongs to this client
                            if (coinOwnerId != senderId) {
                                response.setValue(0);
                                System.out.println(senderId);
                                return BFTMapMessage.toBytes(response);
                            }

                            totalCoins += coinValue;
                            usedCoins.add(key);
                        }
                    }

                    if (totalCoins < spendValue) {
                        response.setValue(0);
                        System.out.println("saldo excedido");
                        return BFTMapMessage.toBytes(response);
                    }

                    remainingValue = totalCoins - spendValue;
                    V receiverCoin = (V) ("coin" + "|" + spend[3] + "|" + spend[4] + "|"
                            + Integer.toString(counter));
                    // String nk = Integer.toString(new Random().nextInt(1000));
                    replicaMap.put(request.getKey(), receiverCoin);
                    System.out.println("Reqkey :" + request.getKey());
                    for (K usedCoin : usedCoins) {
                        System.out.println("UsedCoin :" + usedCoin);

                        replicaMap.remove(usedCoin);
                    }

                    counter += 1;
                    if (remainingValue > 0) {

                        V senderCoin = (V) ("coin" + "|" + spend[1] + "|" + Integer.toString(remainingValue) + "|"
                                + Integer.toString(counter));
                        K key = usedCoins.get(0);
                        test = key;

                        V oldVal = replicaMap.put(key, senderCoin);
                        System.out.println(replicaMap.get(key));

                        if (oldVal != null) {
                            response.setValue(oldVal);
                            System.out.println("OldVal :_" + oldVal);
                        } else {
                            response.setValue(key);
                        }
                    }
                    System.out.println("Keyset");
                    Set<K> keys = replicaMap.keySet();
                    for (K k : keys) {
                        System.out.println(k);
                    }

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
                    }
                    break;

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
                        response.setValue("Already exists a NFT with that name");
                        return BFTMapMessage.toBytes(response);
                    }

                    V oldV = replicaMap.put(request.getKey(), request.getValue());
                    if (oldV != null) {
                        response.setValue(oldV);
                    } else {
                        response.setValue(request.getKey());
                    }

                    return BFTMapMessage.toBytes(response);

                case REQUEST_NFT_TRANSFER:
                    String[] transferTokens = request.getValue().toString().split("\\|");
                    String clienId = transferTokens[1];
                    String nftId = transferTokens[2];

                    Set<K> keySet = replicaMap.keySet();

                    for (K key : keySet) {
                        V nftEntry = replicaMap.get(key);
                        String[] token = nftEntry.toString().split("\\|");
                        if (token[0].equals("nft_request") && clienId.equals(token[1]) && nftId.equals(token[2])) {
                            response.setValue("You already have a nft request for this nft");
                            return BFTMapMessage.toBytes(response);
                        }
                    }

                    replicaMap.put(request.getKey(), request.getValue());
                    System.out.println(response.equals(null));
                    return BFTMapMessage.toBytes(response);

                case CANCEL_REQUEST_NFT_TRANSFER:
                    String[] cancelTokens = request.getValue().toString().split("\\|");
                    String IssuerId = cancelTokens[1];
                    String nftID = cancelTokens[2];

                    for (Map.Entry<K, V> entry : replicaMap.entrySet()) {
                        String[] entryTokens = entry.getValue().toString().split("\\|");
                        if (entryTokens[0].equals("nft_request") && entryTokens[1].equals(IssuerId)
                                && entryTokens[2].equals(nftID)) {
                            replicaMap.remove(entry.getKey());
                            return BFTMapMessage.toBytes(request);
                        }
                    }

                    response.setValue("No matching NFT transfer request found");
                    return BFTMapMessage.toBytes(response);

                // // Check if the user already owns the NFT
                // V nftEntry = replicaMap.get(request.getKey());
                // String nftOwner = nftEntry.toString().split("\\|")[1];
                // if (clienId.equals(nftOwner)) {
                // response.setValue("You are the owner of the NFT.");
                // return BFTMapMessage.toBytes(response);
                // }

                // Check if the user has already made a purchase offer for the given NFT
                // for (V v : replicaMap.values()) {
                // String[] tokens = v.toString().split("\\|");
                // if (tokens[0].equals("request_transfer") && tokens[1].equals(clienId) &&
                // tokens[2].equals(nftId)) {
                // response.setValue("You have already made a purchase offer for this NFT.");
                // return BFTMapMessage.toBytes(response);
                // }
                // }

                // // Create the purchase offer
                // String coins = transferTokens[3];
                // String value1 = transferTokens[4];
                // String validity = transferTokens[5];
                // String transferRequest = "request_transfer" + "|" + clienId + "|" + nftId +
                // "|" + coins + "|" + value1 + "|" + validity;
                // V oldV = replicaMap.put(request.getKey(), (V) transferRequest);
                // if (oldV != null) {
                // response.setValue(oldV);
                // } else {
                // response.setValue(request.getKey());
                // }

                // return BFTMapMessage.toBytes(response);

            }

            return null;
        } catch (IOException | ClassNotFoundException ex) {
            logger.error("Failed to process ordered request", ex);
            return new byte[0];
        }
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
