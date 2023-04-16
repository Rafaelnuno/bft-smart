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
                    System.out.println("Tree before request nft transfer");
                    for (K key : keySet) {

                        V nftEntry = replicaMap.get(key);
                        System.out.println(key + " : " + nftEntry + "\n");
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

                case PROCESS_NFT_TRANSFER:

                    String[] processTokens = request.getValue().toString().split("\\|");
                    String cliId = processTokens[1];
                    String nId = processTokens[2];
                    String buyerId = processTokens[3];
                    String accept = processTokens[4];
                    Set<K> keSet = replicaMap.keySet();
                    int own = 0, exist = 0, has_money = 0, validity = 0;
                    int v = 0, spendDone = 0, nftOwnerTrsf = 0;
                    String coinId = "";
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
                            coinId = vTokens[3];

                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            LocalDateTime date = LocalDateTime.parse(vTokens[5], formatter);
                            if (date.isAfter(now)) {
                                validity = 1;
                            } else {
                                validity = 0;
                            }

                        }

                    }

                    for (K key : keSet) {
                        String curr = (String) replicaMap.get(key);
                        String[] vTokens = curr.toString().split("\\|");

                        if (vTokens[0].equals("coin") && vTokens[1].equals(buyerId) && exist == 1) {

                            has_money = (v <= Integer.parseInt(vTokens[2])) ? 1 : 0;
                        }
                    }

                    if (own == 1 && has_money == 1 && exist == 1 && validity == 1) {
                        System.out.println("All ckecked");
                        if (accept.equals("true")) {

                            for (K key : keSet) {
                                String curr = (String) replicaMap.get(key);
                                String[] vTokens = curr.toString().split("\\|");

                                if (vTokens[0].equals("coin") && vTokens[3].equals(coinId)) {

                                    System.out.println("Strt spending");
                                    String spendCommand = "spend" + "|" + vTokens[1] + "|" + coinId + "|" + cliId
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
                                    break;

                                }
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
                    return 0;
                }

                totalCoins += coinValue;
                usedCoins.add(key);
            }
        }

        if (totalCoins < spendValue) {
            response.setValue(0);
            System.out.println("saldo excedido");
            return 0;
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
            System.out.println(k + " : " + replicaMap.get(k) + "\n");
        }

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
