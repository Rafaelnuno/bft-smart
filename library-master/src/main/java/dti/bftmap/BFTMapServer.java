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
import java.util.Set;
import java.util.TreeMap;

public class BFTMapServer<K, V> extends DefaultSingleRecoverable {
    private final Logger logger = LoggerFactory.getLogger("bftsmart");
    private final ServiceReplica replica;
    private TreeMap<K, V> replicaMap;

    //The constructor passes the id of the server to the super class
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
        byte[] reply = new byte[0];
        try {
            ByteArrayInputStream byteIn = new ByteArrayInputStream(command);
            ObjectInputStream objIn = new ObjectInputStream(byteIn);
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(byteOut);
            boolean hasReply = false;
            BFTMapRequestType cmd = (BFTMapRequestType) objIn.readObject();
            logger.info("Ordered execution of a {} request from {}", cmd, msgCtx.getSender());

            switch (cmd) {
                //write operations on the map
                case PUT:
                    K key = (K) objIn.readObject();
                    V value = (V) objIn.readObject();

                    V ret = replicaMap.put(key, value);

                    if (ret != null) {
                        objOut.writeObject(ret);
                        hasReply = true;
                    }
                    break;

                case GET:
                    key = (K)objIn.readObject();
                    value = replicaMap.get(key);
                    if (value != null) {
                        objOut.writeObject(value);
                        hasReply = true;
                    }
                    break;

                case REMOVE:
                    key = (K)objIn.readObject();
                    value = replicaMap.remove(key);
                    if (value != null) {
                        objOut.writeObject(value);
                        hasReply = true;
                    }
                    break;

                case KEYSET:
                keySet(objOut);
                hasReply = true;
                break;
                case SIZE:
                    int size = replicaMap.size();
                    objOut.writeInt(size);
                    hasReply = true;
                    break;
            }
            if (hasReply) {
                objOut.flush();
                byteOut.flush();
                reply = byteOut.toByteArray();
            } else {
                reply = new byte[0];
            }

            
        }catch (IOException | ClassNotFoundException ex) {
            logger.error("Failed to process ordered request", ex);
            return new byte[0];
        }
        return reply;
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        byte[] reply = new byte[0];
        try {
            ByteArrayInputStream byteIn = new ByteArrayInputStream(command);
            ObjectInputStream objIn = new ObjectInputStream(byteIn);
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(byteOut); 
            boolean hasReply = false;
            BFTMapRequestType cmd = (BFTMapRequestType) objIn.readObject();
            logger.info("Ordered execution of a {} request from {}", cmd, msgCtx.getSender());

            switch (cmd) {
                //read operations on the map
                case GET:
                    K key = (K) objIn.readObject();

                    V ret = replicaMap.get(key);

                    if (ret != null) {
                        objOut.writeObject(ret);
                        hasReply = true;
                    }
                    break;
                case KEYSET:
                    keySet(objOut);
                    hasReply = true;
                    break;
                case SIZE:
                    int size = replicaMap.size();
                    objOut.writeInt(size);
                    hasReply = true;
                    break;
            }
            if (hasReply) {
                objOut.flush();
                byteOut.flush();
                reply = byteOut.toByteArray();
            } else {
                reply = new byte[0];
            }

        } catch (IOException | ClassNotFoundException ex) {
            logger.error("Failed to process unordered request", ex);
        }
        return reply;
    }

    @Override
    public byte[] getSnapshot() {
        try  {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(replicaMap);
            out.flush();
            bos.flush();
            return bos.toByteArray();
        } catch (IOException ex) {
            ex.printStackTrace(); //debug instruction
            return new byte[0];
        }
    }

    @Override
    public void installSnapshot(byte[] state) {
        try  {
            ByteArrayInputStream bis = new ByteArrayInputStream(state);
            ObjectInput in = new ObjectInputStream(bis);
            replicaMap = (TreeMap<K, V>) in.readObject();
        } catch (ClassNotFoundException | IOException ex) {
            ex.printStackTrace(); //debug instruction
        }
        
    }
    private void keySet(ObjectOutput out) throws IOException, ClassNotFoundException {
        Set<K> keySet = replicaMap.keySet();
        int size = replicaMap.size();
        out.writeInt(size);
        for (K key : keySet)
            out.writeObject(key);
    }
}
