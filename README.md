# bft-smart

Go to the folder where you downloaded it, enter the project folder and run the following command:

```
./gradlew installldist
```

## To run the application:

1.Open 6 terminals where in the first 4 you will run the following commands:

```
smartrun.sh dti.bftmap.BFTMapServer 0
smartrun.sh dti.bftmap.BFTMapServer 1
smartrun.sh dti.bftmap.BFTMapServer 2
smartrun.sh dti.bftmap.BFTMapServer 3
```

2. On the other 2 terminals you will execute the following commands to run the client:

```
smartrun.sh dti.bftmap.BFTMapInteractiveClient 4
smartrun.sh dti.bftmap.BFTMapInteractiveClient 5
```

3. Then just run the commands on the client side
```
MY_COINS
MINT
SPEND
MY_NFTS
MINT_NFT
REQUEST_NFT_TRANSFER
CANCEL_NFT_REQUEST
MY_NFT_REQUESTS
PROCESS_NFT_TRANSFER
```
