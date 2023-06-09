/**
 * BFT Map implementation (interactive client).
 *
 */
package dti.bftmap;

import java.io.Console;
import java.io.IOException;
import java.util.*;

public class BFTMapInteractiveClient {
    //Coins Integer:id_coins, String:ID_USER|C|Value
    //NFT Integer:id_nft, String:ID_USER|N|Name_nft|URI
    //Request Integer:id_request, String:ID_USER|R|id_nft|Value|Validty|id_coins
    public static void main(String[] args) throws IOException {
        int clientId = (args.length > 0) ? Integer.parseInt(args[0]) : 1001;
        BFTMap<Integer, String> bftMap = new BFTMap<>(clientId);
      
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
        System.out.println("\tMY_CLIENT_ID: Retrieve your client ID");
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
            } else if (cmd.equalsIgnoreCase("GET")) {

                int key;
                try {
                    key = Integer.parseInt(console.readLine("Enter a numeric key: "));
                } catch (NumberFormatException e) {
                    System.out.println("\tThe key is supposed to be an integer!\n");
                    continue;
                }

                //invokes the op on the servers
                String value = bftMap.get(key);

                //fazer para o resto
                System.out.println("\nValue associated with " + key + ": " + value + "\n");

            } else if (cmd.equalsIgnoreCase("MY_COINS")) {
                //invokes the op on the servers
                String value;
                Set<Integer> keys = bftMap.keySet();
                System.out.println("Your coins: ");
                for (Integer key : keys) {
                    value = bftMap.get(key);
                    String[] values = value.split("\\|");
                    if (values[0].equals(Integer.toString(clientId)) && values[1].equals("C")) {
                        System.out.println("Coin ID: " + key + " | Value: " + values[2]);
                    }
                }

            } else if (cmd.equalsIgnoreCase("MINT")) {

                float coinValue;
                try {
                    coinValue = Float.parseFloat(console.readLine("Enter a value for the coin: "));
                } catch (NumberFormatException e) {
                    System.out.println("\tThe value is supposed to be an integer!\n");
                    continue;
                }

                Set<Integer> all_keys = bftMap.keySet();
                int key = IDGen(all_keys);
                String value = clientId + "|C|" + coinValue;
                //invokes the op on the servers
                bftMap.put(key, value);

                System.out.println("\nNew coin created with the ID: " + key + "\n");

            } else if (cmd.equalsIgnoreCase("SPEND")) {
                List<Integer> keysList = new ArrayList<>();
                int id = 0, finish = 0, receiverID = 0;
                float value_sent = 0,  sum = 0;

                try {
                    while (finish != -1) {
                        id = Integer.parseInt(console.readLine("Enter the coins IDs (Insert -1 do terminate insertion of the IDs): "));
                        if (!keysList.contains(id) && id > 0) {
                            keysList.add(id);
                        }
                        finish = id;
                    }
                    receiverID = Integer.parseInt(console.readLine("Enter the reciever ID (Integer): "));
                    boolean enough = false;
                    for (Integer key : keysList) {
                        String coin_value = bftMap.get(key);
                        String[] coin_value_split = coin_value.split("\\|");
                        sum += Float.parseFloat(coin_value_split[2]);
                    }
                    System.out.println("sum=" + sum);
                    do {
                        value_sent = Float.parseFloat(console.readLine("Enter a value to be transferred: "));
                        if (sum >= value_sent) {
                            enough = true;
                        } else {
                            System.out.println("Value to be transferred too high");
                        }
                    } while (!enough);

                } catch (NumberFormatException e) {
                    System.out.println("\tAll the IDs should be an integer\n");
                    continue;
                }

                Set<Integer> all_keys = bftMap.keySet();
                int key_receiver = IDGen(all_keys);
                String value_r = receiverID + "|C|" + value_sent;
                //invokes the op on the servers
                bftMap.put(key_receiver, value_r);

                for (Integer key : keysList) {
                    bftMap.remove(key);
                }
                all_keys = bftMap.keySet();
                int key_sender = IDGen(all_keys);
                float value_remain = sum - value_sent;
                String value_s = clientId + "|C|" + value_remain;
                bftMap.put(key_sender, value_s);

                //invokes the op on the servers
                System.out.println("\nkey-value pair added to the map\n");

            } else if (cmd.equalsIgnoreCase("MY_NFTS")) {
                    // invokes the op on the servers
                    String value;
                    Set<Integer> keys = bftMap.keySet();
                    System.out.println("Your Nfts:");
                    for (Integer key : keys) {
                        value = bftMap.get(key);
                        String[] values = value.split("\\|");
                        if (values[0].equals(Integer.toString(clientId)) && values[1].equals("N")) {
                            System.out.println("Nft Id: " + key + " | Name: " + values[2] + " | Url: " + values[3]);
                        }
                    }
                
            } else if (cmd.equalsIgnoreCase("MINT_NFT")) {

                    String userId = Integer.toString(clientId);               
                    String name = console.readLine("Enter the Name of your Nft: ");
                    String url = console.readLine("Enter the Url for your Nft: ");
                
                    // Verificar se já existe um NFT com o mesmo nome
                    boolean nameExists = false;
                    Set<Integer> keys = bftMap.keySet();
                    for (Integer key : keys) {
                        String value = bftMap.get(key);
                        String[] values = value.split("\\|");
                        if (values[0].equals(userId) && values[1].equals("N") && values[2].equals(name)) {
                            nameExists = true;
                            break;
                        }
                    }
                    if (nameExists) {
                        System.out.println("\nAn Nft with that name already exists\n");
                        continue;
                    }
                    int nftId = IDGen(keys);
                    String nftData = userId + "|N|" + name + "|" + url;
                    bftMap.put(nftId, nftData);
                
                    System.out.println("\nA new Nft has been created with the ID: " + nftId + "\n");
            
                } else if (cmd.equalsIgnoreCase("REQUEST_NFT_TRANSFER")) {
                    //fazer a verificação se o cliente tem coins para fazer a oferta e a parte da validade que mexe com tempo

                    int id_nft;
                    float value;
                    long validity;

                    // mostra todas as nfts que outros clientes tenham
                    System.out.println("Available Nfts from other clients:");
                    Set<Integer> all_keys = bftMap.keySet();
                    for (Integer key : all_keys) {
                        String currentValue = bftMap.get(key);
                        String[] values = currentValue.split("\\|");
                        if (values[0].equals(Integer.toString(clientId)) || !values[1].equals("N")) {
                            continue;
                        }
                        System.out.println("\tNft Id: " + key + ", Owner Id: " + values[0] + ", Name: " + values[2] +", Url: " + values[3]);
                    }

                    try {
                        id_nft = Integer.parseInt(console.readLine("Enter the Nft id that you want to give an offer: "));
                        value = Float.parseFloat(console.readLine("Enter the value for the offer: "));
                        validity = Long.parseLong(console.readLine("Enter the validity for the offer: "));
                    } catch (NumberFormatException e) {
                        System.out.println("\tInvalid input!\n");
                        continue;
                    }
                    String nft = bftMap.get(id_nft);
                    if (nft == null || !nft.split("\\|")[1].equals("N")) {
                        System.out.println("\tInvalid Nft id!\n");
                        continue;
                    }

                    String ownerId = nft.split("\\|")[0];
                    if (ownerId.equals(Integer.toString(clientId))) {
                        System.out.println("\tYou already own this Nft!\n");
                        continue;
                    }
                    String requestValue = clientId + "|R|" + id_nft + "|" + value + "|" + validity + "|";
                    boolean exists = false;
                    for (Integer key : all_keys) {
                        String currentValue = bftMap.get(key);
                        String[] values = currentValue.split("\\|");
                        if (values[0].equals(Integer.toString(clientId)) && values[1].equals("R") && values[2].equals(Integer.toString(id_nft))) {
                            System.out.println("\tYou have already created a request for this Nft\n");
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        int key = IDGen(all_keys);
                        bftMap.put(key, requestValue);
                        System.out.println("\tYour Request has been created and your Request Id is: " + key + "\n");
                    }   
            }else if (cmd.equalsIgnoreCase("MY_NFT_REQUESTS")) {
                //invokes the op on the servers
                String value;
                Set<Integer> keys = bftMap.keySet();
                System.out.println("Your NFT requests: ");
                for (Integer key : keys) {
                    value = bftMap.get(key);
                    String[] values = value.split("\\|");
                    if (values[0].equals(Integer.toString(clientId)) && values[1].equals("R")) {
                        String nftName = "";
                        String nftUrl = "";   
                        String nftValue = values[3];
                        String[] nftInfo = bftMap.get(Integer.parseInt(values[2])).split("\\|");
                        if(nftInfo.length >= 3) {
                            nftName = nftInfo[2];
                            nftUrl = nftInfo[3]; 
                        }
                        System.out.println("Request Id: " + key +" | Nft Id: " + values[2] +" | Nft Name: " + nftName + " | Nft Url: " + nftUrl + " | Value Offered: " + nftValue + " | Validity: " + values[4]);
                    }
                }
            
            }else if (cmd.equalsIgnoreCase("CANCEL_NFT_REQUEST")) {
                int id_request;
                try {
                    id_request = Integer.parseInt(console.readLine("Enter the Request Id that you want to cancel: "));
                } catch (NumberFormatException e) {
                    System.out.println("\tThe Request Id is supposed to be an integer!\n");
                    continue;
                }
            
                boolean found = false;
                Set<Integer> keys = bftMap.keySet();
                for (Integer key : keys) {
                    String value = bftMap.get(key);
                    String[] values = value.split("\\|");
                    if (values[1].equals("R") && key == id_request && values[0].equals(Integer.toString(clientId))) {
                        found = true;
                    
                        bftMap.remove(key);
                        System.out.println("Your Request was successfully canceled");
                        break;
                    }
                }
                if (!found) {
                    System.out.println("There is no Request for this Id");
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
