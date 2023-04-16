/**
 * BFT Map implementation (message types).
 * 
 */

package dti.bftmap;

public enum BFTMapRequestType {
    PUT, GET, SIZE, REMOVE, KEYSET, VALUES, MINT, SPEND, MINT_NFT, REQUEST_NFT_TRANSFER, CANCEL_REQUEST_NFT_TRANSFER,
    PROCESS_NFT_TRANSFER, CHECK_OWNERSHIP
}
