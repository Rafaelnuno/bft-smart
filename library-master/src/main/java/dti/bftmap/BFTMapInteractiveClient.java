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
        System.out.println("\tMY_COINS: Retrieve all the coins of that client");
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

            } else if (cmd.equalsIgnoreCase("KEYSET")) {

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
