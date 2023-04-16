/**
 * BFT Map implementation (interactive client).
 *
 */
package dti.bftmap;

import java.io.Console;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class BFTMapInteractiveClient {

    public static void main(String[] args) throws IOException {
        int keySeq = 0;
        int clientId = (args.length > 0) ? Integer.parseInt(args[0]) : 1001;
        BFTMap<Integer, Object> bftMap = new BFTMap<>(clientId);

        Console console = System.console();

        System.out.println("\nCommands:\n");
        System.out.println("\tMY_COINS: Client's coins");
        System.out.println("\tMINT: Create a new coin for that client");
        System.out.println("\tSPEND: Transfer a certain value to another client");
        System.out.println("\tMY_NFTS: Retrieve all the NTFs of that client");
        System.out.println("\tMINT_NFT: Create a new NFT for that client");
        System.out.println(
                "\tREQUEST_NFT_TRANSFER: Create a request to transfer nft with an offered value and confirmation");
        System.out.println("\tCANCEL_NFT_REQUEST: Cancel a certain nft request created before");
        System.out.println("\tMY_NFT_REQUESTS: Retrieve all NFT requests");
        System.out.println("\tPROCESS_NFT_TRANSFER: Accept or reject a NFT request");
        System.out.println("\tEXIT: Terminate this client\n");

        while (true) {
            String cmd = console.readLine("\n  > ");

            if (cmd.equalsIgnoreCase("MY_COINS")) {
                Set<Integer> keys = bftMap.keySet();
                System.out.println("\nYour Coins:");
                for (int key : keys) {

                    String coin = (String) bftMap.get(key);
                    String[] values = coin.split("\\|");
                    if (values[0].equals("coin") && values[1].equals(Integer.toString(clientId))) {
                        String value = values[2];
                        String idcoin = values[3];
                        System.out.println("Key " + idcoin + " -> value: " + value);
                    }
                }

            } else if (cmd.equalsIgnoreCase("MINT")) {

                String value = console.readLine("Enter the value of the coin: ");
                String coinId = Integer.toString(new Random().nextInt(1000));
                String coin = "coin" + "|" + clientId + "|" + value + "|" + coinId;
                Set<Integer> keysprev = bftMap.keySet();
                keySeq = IDGen(keysprev);
                // invokes the op on the servers
                String res = (String) bftMap.put(keySeq, coin).toString();
                if (res.equals("0")) {
                    System.out.println("Coin creation failed\n");
                } else {
                    System.out.println("\ncoin id: " + coinId + " created");
                }

                // Delete this
                Set<Integer> keys = bftMap.keySet();
                for (Integer k : keys) {
                    System.out.println(k + " : " + bftMap.get(k) + "\n");
                }

            } else if (cmd.equalsIgnoreCase("SPEND")) {
                String coins = "";
                while (true) {
                    coins = console.readLine("Enter the ids of the coins to spend (comma-separated): ");
                    String[] coinIds = coins.split(",");
                    if (coinIds.length < 1) {
                        System.out.println("Invalid number of coin ids. Please enter 2 to 4 comma-separated coin ids.");
                        continue;
                    } else {
                        break;
                    }
                }
                String receiverId = console.readLine("Enter the id of the receiver: ");
                String value = console.readLine("Enter the value to transfer: ");
                String spendCommand = "spend" + "|" + clientId + "|" + coins + "|" + receiverId + "|" + value;
                Set<Integer> keys = bftMap.keySet();
                keySeq = IDGen(keys);

                // invokes the op on the servers
                bftMap.put(keySeq, spendCommand);

                /*if (values.equals("0")) {
                    System.out.println("\nInvalid spend operation.");
                } else {
                    System.out.println("\nCoin id: " + values + " created for issuer.");
                }*/

            } else if (cmd.equalsIgnoreCase("MY_NFTS")) {
                Set<Integer> keys = bftMap.keySet();
                System.out.println("\nYour Nfts:");

                for (int key : keys) {
                    String nft = (String) bftMap.get(key);
                    String[] nftTokens = nft.split("\\|");
                    if (nftTokens[0].equals("nft") && nftTokens[1].equals(String.valueOf(clientId))) {
                        String nftname = nftTokens[2];
                        String uri = nftTokens[3];
                        String nftid = nftTokens[4];
                        System.out.println("Key " + key + " -> name: " + nftname + " URI: " + uri + " nftId: " + nftid);
                    }

                }
            } else if (cmd.equalsIgnoreCase("MINT_NFT")) {

                String name = console.readLine("Enter the name of the nft: ");

                String uri = console.readLine("Enter the URI of the nft: ");
                String nftid = Integer.toString(new Random().nextInt(1000));
                String nft = "nft" + "|" + clientId + "|" + name + "|" + uri + "|" + nftid;
                Set<Integer> keys = bftMap.keySet();
                keySeq = IDGen(keys);
                // invokes the op on the servers
                String res = (String) bftMap.put(keySeq, nft).toString();

                if (res.equals("0")) {
                    System.out.println("Nft creation failed");
                } else {
                    System.out.println("\nNft created\n");
                }

            } else if (cmd.equalsIgnoreCase("REQUEST_NFT_TRANSFER")) {

                String nftId = console.readLine("Enter the id of the NFT: ");
                String coins = console.readLine("Enter the ids of the coins to offer (comma-separated): ");
                String value = console.readLine("Enter the value offered: ");
                String minutes = console.readLine("Enter the confirmation validity in minutes: ");

                LocalDateTime now = LocalDateTime.now().plusMinutes(Long.parseLong(minutes));

                // Format the date and time as a string using a custom formatter
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String formattedDateTime = now.format(formatter);
                System.out.println(formattedDateTime);

                // Dias:Horas:Minutos:Segundos
                String transferRequest = "nft_request" + "|" + clientId + "|" + nftId + "|" + coins + "|" + value + "|"
                        + formattedDateTime;
                Set<Integer> keys = bftMap.keySet();
                keySeq = IDGen(keys);

                String res = (String) bftMap.put(keySeq, transferRequest).toString();

                if (res.equals("0")) {
                    System.out.println("Request failed");
                } else {
                    System.out.println("Request created\n");
                }

            } else if (cmd.equalsIgnoreCase("MY_NFT_REQUESTS")) {

                String nftId = console.readLine("Enter the id of the nft: ");

                Set<Integer> keySet = bftMap.keySet();

                String ownership = "own" + "|" + "nft" + "|" + clientId;

                Set<Integer> keys = bftMap.keySet();
                int own = (Integer) bftMap.put(IDGen(keys), ownership);

                if (own == 1) {

                    for (Integer key : keySet) {

                        String curr = (String) bftMap.get(key);
                        String[] vTokens = curr.toString().split("\\|");

                        if (vTokens[0].equals("nft_request") && vTokens[2].equals(nftId)) {

                            System.out.println("\tNft Id : " + nftId +
                                    "\n    | Issuer   : " + vTokens[1] +
                                    "\n    | Value    : " + vTokens[4] +
                                    "\n    | Validity : " + vTokens[5] + "\n");
                        }
                    }
                }

            } else if (cmd.equalsIgnoreCase("PROCESS_NFT_TRANSFER")) {

                String nftId = console.readLine("Enter the id of the nft: ");
                String buyerId = console.readLine("Enter the buyer id of the nft: ");
                String accept = console.readLine("Enter if you accept (true) or not (false): ");
                Set<Integer> keys = bftMap.keySet();
                keySeq = IDGen(keys);
                int res = (Integer) bftMap.put(keySeq,
                        "process_nft_transfer" + "|" + clientId + "|" + nftId + "|" + buyerId + "|" + accept);

                if (res == 0) {
                    System.out.println("Transfer not processed");
                } else {
                    System.out.println("Transfer processed");
                }

            } else if (cmd.equalsIgnoreCase("CANCEL_NFT_REQUEST")) {
                String nftID = console.readLine("Enter the id of the nft: ");
                String cancelRequest = "cancel_request" + "|" + clientId + "|" + nftID;
                // invokes the op on the servers
                String res = (String) bftMap.remove(cancelRequest).toString();
                if (res.equals("0")) {
                    System.out.println("Request not canceled");
                } else {
                    System.out.println("Request canceled");
                }

            } else if (cmd.equalsIgnoreCase("EXIT")) {

                System.out.println("\tEXIT: Bye bye!\n");
                System.exit(0);

            } else {
                System.out.println("\tInvalid command :P\n");
            }

        }

    }

    public static int IDGen(Set<Integer> keys) {
        Set<Integer> usedIds = new HashSet<>(keys);
        Random random = new Random();

        int random_int = random.nextInt(1000);
        while (usedIds.contains(random_int)) {
            random_int = random.nextInt(1000);
        }

        return random_int;
    }

}