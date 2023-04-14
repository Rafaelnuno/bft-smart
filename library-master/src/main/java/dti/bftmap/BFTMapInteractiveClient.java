/**
 * BFT Map implementation (interactive client).
 *
 */
package dti.bftmap;

import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        System.out.println("\tREQUEST_NFT_TRANSFER: Create a request to transfer nft with an offered value and confirmation");
        System.out.println("\tCANCEL_NFT_REQUEST: Cancel a certain nft request created before");
        System.out.println("\tMY_NFT_REQUESTS: Retrieve all NFT requests");
        System.out.println("\tPROCESS_NFT_TRANSFER: Accept or reject a NFT request");
        System.out.println("\tEXIT: Terminate this client\n");

        while (true) {
            String cmd = console.readLine("\n  > ");

            if (cmd.equalsIgnoreCase("PUT")) {

                int key;
                try {
                    key = Integer.parseInt(console.readLine("Enter a numeric key: "));
                } catch (NumberFormatException e) {
                    System.out.println("\tThe key is supposed to be an integer!\n");
                    continue;
                }
                String value = console.readLine("Enter an alpha-numeric value: ");

                //invokes the op on the servers
                bftMap.put(key, value);

                System.out.println("\nkey-value pair added to the map\n");
                
            }else if (cmd.equalsIgnoreCase("MY_COINS")) {
                Set<Integer> keys = bftMap.keySet();
                System.out.println("\nCoins owned by client " + clientId + ":");
                for (int key : keys) {
                    Object coinObj = bftMap.get(key);
                    if (coinObj instanceof Coin) {
                        Coin coin = (Coin) coinObj;
                        if (coin.getClientId() == clientId) {
                            System.out.println("Key " + key + " -> value: " + coin.getValue());
                        }
                    }
                }
            }else if (cmd.equalsIgnoreCase("MINT")) {
                String value = console.readLine("Enter the value of the coin: ");
                System.out.println("Value entered by user: " + value);
                List<Object> req = new ArrayList<>();
                req.add("coin");
                req.add(clientId);
                req.add(Float.parseFloat(value));
                
            
                //invokes the op on the servers
                 bftMap.put(keySeq, req);
                Coin mintcoin = (Coin) bftMap.get(keySeq);
                System.out.println(mintcoin.getClientId());
                keySeq++;
            

            }else if (cmd.equalsIgnoreCase("SPEND")) {
                String coins = console.readLine("Enter the ids of the coins to spend (comma-separated): ");
                String receiver = console.readLine("Enter the id of the receiver: ");
                String value = console.readLine("Enter the value to transfer: ");
                String spendCommand = "spend" + "|" + coins + "|" + receiver + "|" + value + "|" + clientId;
            
                //invokes the op on the servers
                String values = bftMap.put(keySeq, spendCommand).toString();
            
                if (values.equals("0")) {
                    System.out.println("\nInvalid spend operation.");
                    
                } else {
                    System.out.println("\nCoin id: " + values + " created for issuer.");
                }
                keySeq+=1;
            }
            else if (cmd.equalsIgnoreCase("MY_NFTS")) {
                Set<Integer> keys = bftMap.keySet();
                System.out.println("\nKeys in the map:");

                for (int key : keys) {
            		String nft = (String) bftMap.get(key);
                	String[] nftTokens = nft.split("\\|");
                	if(nftTokens[0].equals("nft")) {
                		String name = nftTokens[2];
                		String uri = nftTokens[3];
                		System.out.println("Key " + key + " -> name: " + name + " URI: " + uri );
                	}
                    
                }
            }else if (cmd.equalsIgnoreCase("MINT_NFT")){
                
                String name = console.readLine("Enter the name of the nft: ");

                String uri = console.readLine("Enter the URI of the nft: ");
                String nft = "nft"+ "|" + clientId + "|" + name +"|" + uri; 

                //invokes the op on the servers
                bftMap.put(keySeq, nft);

                System.out.println("\nkey-value pair added to the map\n");
                keySeq+=1;

            } 
            else if (cmd.equalsIgnoreCase("REQUEST_NFT_TRANSFER")){
                
                String nftID = console.readLine("Enter the id of the nft: ");
                Boolean _coins = true;
                String coins ="";
                while(_coins){
                    String coin = console.readLine("Enter the ids of the coin you want to use: \n You can type 'DONE' to finish inputing coins");
                    if (coin.equals("DONE")){
                        _coins= false;
                    }else{
                        coins += coin + ",";
                    }
                }
                String validity = console.readLine("Enter the validity of the transfer request");
                
                String request = clientId + "|" + nftID +"|" + coins+"|"+validity; 
                //invokes the op on the servers
                bftMap.put(keySeq, request);

                System.out.println("\nkey-value pair added to the map\n");
                keySeq+=1;

            } 
            else if (cmd.equalsIgnoreCase("GET")) {

                int key;
                try {
                    key = Integer.parseInt(console.readLine("Enter a numeric key: "));
                } catch (NumberFormatException e) {
                    System.out.println("\tThe key is supposed to be an integer!\n");
                    continue;
                }

                //invokes the op on the servers
                //String value = bftMap.get(key);

                //System.out.println("\nValue associated with " + key + ": " + value + "\n");

            } 
                
                
             else if (cmd.equalsIgnoreCase("MY_NFT_REQUESTS")) {

                System.out.println("\tYou are supposed to implement this command :)\n");

            } else if (cmd.equalsIgnoreCase("PROCESS_NFT_TRANSFER")) {

                System.out.println("\tYou are supposed to implement this command :)\n");

            } else if (cmd.equalsIgnoreCase("REMOVE")) {

                System.out.println("\tYou are supposed to implement this command :)\n");

            } else if (cmd.equalsIgnoreCase("SIZE")) {

                System.out.println("\tYou are supposed to implement this command :)\n");

            } else if (cmd.equalsIgnoreCase("EXIT")) {

                System.out.println("\tEXIT: Bye bye!\n");
                System.exit(0);

            } else {
                System.out.println("\tInvalid command :P\n");
            }
        }
    }

}