/**
 * BFT Map implementation (interactive client).
 *
 */
package dti.bftmap;

import java.io.Console;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import io.netty.handler.codec.ValueConverter;



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
                
            if (cmd.equalsIgnoreCase("MY_COINS")) {
            	Set<Integer> keys = bftMap.keySet();
                System.out.println("\nKeys in the map:");
            	for (int key : keys) {
            		String coin = (String) bftMap.get(key);
                	String[] values = coin.split("\\|");
                	if(values[0].equals("coin")&& values[1].equals(String.valueOf(clientId))) {
                		String value = values[2];
                        String idcoin = values[3];
                		System.out.println("Key " + idcoin + " -> value: " + value );
                	}
               	}
            	
            }else if (cmd.equalsIgnoreCase("MINT")) {
            	String value = console.readLine("Enter the value of the coin: ");
                String coinId = Integer.toString(new Random().nextInt(1000));
                String coin = "coin"+ "|" + clientId + "|" + value + "|" + coinId; 
                Set<Integer> keys = bftMap.keySet();
                keySeq = IDGen(keys);
                //invokes the op on the servers
                bftMap.put(keySeq, coin).toString();
               
                System.out.println("\ncoin id: " + coinId + " created");
                

            }else if (cmd.equalsIgnoreCase("SPEND")) {
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
                
                //invokes the op on the servers
                String values = bftMap.put(keySeq, spendCommand).toString();
                
                if (values.equals("0")) {
                    System.out.println("\nInvalid spend operation.");
                } else {
                    System.out.println("\nCoin id: " + values + " created for issuer.");
                }
                keySeq+=1;
        
            } else if (cmd.equalsIgnoreCase("MY_NFTS")) {
                Set<Integer> keys = bftMap.keySet();
                System.out.println("\nKeys in the map:");

                for (int key : keys) {
            		String nft = (String) bftMap.get(key);
                	String[] nftTokens = nft.split("\\|");
                	if(nftTokens[0].equals("nft")&& nftTokens[1].equals(String.valueOf(clientId))) {
                		String nftname = nftTokens[2];
                		String uri = nftTokens[3];
                        String nftid = nftTokens[4];
                		System.out.println("Key " + key + " -> name: " + nftname + " URI: " + uri + " nftId: " + nftid );
                	}
                    
                }
              }  else if (cmd.equalsIgnoreCase("MINT_NFT")){
                
                String name = console.readLine("Enter the name of the nft: ");

                String uri = console.readLine("Enter the URI of the nft: ");
                String nftid = Integer.toString(new Random().nextInt(1000));
                String nft = "nft"+ "|" + clientId + "|" + name +"|" + uri + "|" + nftid; 
                Set<Integer> keys = bftMap.keySet();
                keySeq = IDGen(keys);
                //invokes the op on the servers
                bftMap.put(keySeq, nft);

                System.out.println("\nkey-value pair added to the map\n");
                

            } 
            else if (cmd.equalsIgnoreCase("REQUEST_NFT_TRANSFER")){
                
               
                    String nftId = console.readLine("Enter the id of the NFT: ");
                    String coins = console.readLine("Enter the ids of the coins to offer (comma-separated): ");
                    String value = console.readLine("Enter the value offered: ");
                    String minutes = console.readLine("Enter the confirmation validity in minutes: ");

                    LocalDateTime now = LocalDateTime.now().plusMinutes(Long.parseLong(minutes));

                    // Format the date and time as a string using a custom formatter
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    String formattedDateTime = now.format(formatter);
                    System.out.println(formattedDateTime);

                    //Dias:Horas:Minutos:Segundos
                    String transferRequest = "nft_request" + "|" + clientId + "|" + nftId + "|" + coins + "|" + value + "|" + formattedDateTime;
                    Set<Integer> keys = bftMap.keySet();
                    keySeq = IDGen(keys);

                    
                    // invokes the op on the servers
                    bftMap.put(keySeq, transferRequest);
                    System.out.println("Done");
                    // if (responseValue.equals("0")) {
                    //     System.out.println("\nInvalid transfer request.");
                    // } else {
                    //     System.out.println("\nTransfer request created with id: " + responseValue);
                    // }
                   
                

            
         } else if (cmd.equalsIgnoreCase("GET")) {

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

                String nftId = console.readLine("Enter the id of the nft: ");

                Set<Integer> keySet = bftMap.keySet();
                List<String> nftsIds = new ArrayList<>();
                int own = 0;

                //Checking ownership
                for (Integer key : keySet) {
                    String curr = (String) bftMap.get(key);
                    String[] vTokens = curr.toString().split("\\|");

                    if (vTokens[0].equals("nft") && vTokens[4].equals(nftId)) {
                        if(!vTokens[1].equals(Integer.toString(clientId))) {
                            System.out.println("The user doesn't own this nft\n");
                            break;
                        }
                        else {
                            own = 1;
                            break;
                        }
                    }
                }

                if(own == 1) {
                    
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

                Set<Integer> keySet = bftMap.keySet();
                int own = 0,exist = 0,has_money = 0;
                int value = 0;
                String coinId = "";

                //Checking ownership
                for (Integer key : keySet) {
                    String curr = (String) bftMap.get(key);
                    String[] vTokens = curr.toString().split("\\|");

                    if (vTokens[0].equals("nft") && vTokens[4].equals(nftId)) {
                        if(!vTokens[1].equals(Integer.toString(clientId))) {
                            System.out.println("The user doesn't own this nft\n");
                            break;
                        }
                        else {
                            own = 1;
                            break;
                        }
                    }
                }

                for (Integer key : keySet) {

                    String curr = (String) bftMap.get(key);
                    String[] vTokens = curr.toString().split("\\|");

                    if (vTokens[0].equals("nft_request") && vTokens[2].equals(nftId) && vTokens[1].equals(buyerId)) {
                        exist = 1;
                        value = Integer.parseInt(vTokens[4]);
                        coinId = vTokens[3];
                    }
                
                }

                for (Integer key : keySet) {
                    String curr = (String) bftMap.get(key);
                    String[] vTokens = curr.toString().split("\\|");

                    if(vTokens[0].equals("coin") && vTokens[1].equals(buyerId) && exist == 1) {

                        has_money = (value <= Integer.parseInt(vTokens[2])) ? 1 : 0;
                    }
                }

                if(own == 1 && has_money == 1 && exist == 1) {
                    System.out.println("All ckecked");
                    if (accept.equals("true")) {

                        System.out.println(coinId);
                        for (Integer key : keySet) {
                            String curr = (String) bftMap.get(key);
                            System.out.println(curr);
                            String[] vTokens = curr.toString().split("\\|");

                            if( vTokens[0].equals("coin") && vTokens[3].equals(coinId)) {

                                String newCoinId = Integer.toString(new Random().nextInt(1000));
                                String coin_recv = "coin"+ "|" + vTokens[1] + "|" + Integer.toString(Integer.parseInt(vTokens[2]) - value) + "|" + newCoinId; 
                                String coin_send = "coin"+ "|" + Integer.toString(clientId) + "|" + Integer.toString(value) + "|" + Integer.toString(new Random().nextInt(1000)); 

                                //System.out.println(coin_recv);                                
                                //bftMap.put(key,coin_recv);
                                keySeq = IDGen(keySet);
                                System.out.println(coin_send);        
                                bftMap.put(keySeq,coin_send);


                            }
                        }

                        for (Integer key : keySet) {

                            String curr = (String) bftMap.get(key);
                            String[] vTokens = curr.toString().split("\\|");
        
                            if (vTokens[0].equals("nft") && vTokens[2].equals(nftId)) {
                                
                                String new_nft = "nft" + "|" + buyerId + "|" + vTokens[2] + "|" + vTokens[3] + "|" + vTokens[4];
                                bftMap.put(key,new_nft);
                            }
                        
                        }
                        //ir buscar a coin usada para comprar e deduzir lhe 
                        //ir buscar a coin do receiver e dar lhe dinheiro

                        //ir buscar a nft e mudar o owner
                        //cancel request
                    }
                    else {
                        System.out.println("False");
                        //cancel requst blah blah
                    }
                }
                else {
                    System.out.println("Checking went wrong");
                }
 
                //Check if request exist or is inside validity
                //Check if buyer has enough coins
                //If all goes well, create two coins, One for buyer one for owner. Transfer nft to another owner
                System.out.println("\tYou are supposed to implement this command :)\n");

            } else if (cmd.equalsIgnoreCase("CANCEL_NFT_REQUEST")) {
                // int id_request;
                // try {
                //     id_request = Integer.parseInt(console.readLine("Enter the Request Id that you want to cancel: "));
                // } catch (NumberFormatException e) {
                //     System.out.println("\tThe Request Id is supposed to be an integer!\n");
                //     continue;
                // }
                // Set<Integer> all_keys = bftMap.keySet();
                // String request = "cancel" + "|" + clientId ; 

                //  int key = IDGen(all_keys);

                // bftMap.remove(key);
                // System.out.println("Your Request was successfully canceled");
                // break;

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