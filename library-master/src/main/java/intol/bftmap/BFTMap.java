/**
 * BFT Map implementation (client side).
 *
 */
package intol.bftmap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Map;

import bftsmart.tom.ServiceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

public class BFTMap<K, V> implements Map<K, V> {
    private final Logger logger = LoggerFactory.getLogger("bftsmart");
    private final ServiceProxy serviceProxy;

    public BFTMap(int id) {
        serviceProxy = new ServiceProxy(id);
    }

    /**
     *
     * @param key The key associated to the value
     * @return value The value previously added to the map
     */
    @Override
    public V get(Object key) {
        byte[] rep;
        try {
            BFTMapMessage<K,V> request = new BFTMapMessage<>();
            request.setType(BFTMapRequestType.GET);
            request.setKey(key);

            //invokes BFT-SMaRt
            rep = serviceProxy.invokeUnordered(BFTMapMessage.toBytes(request));
        } catch (IOException e) {
            logger.error("Failed to send GET request");
            return null;
        }

        if (rep.length == 0) {
            return null;
        }
        try {
            BFTMapMessage<K,V> response = BFTMapMessage.fromBytes(rep);
            return response.getValue();
        } catch (ClassNotFoundException | IOException ex) {
            logger.error("Failed to deserialized response of GET request");
            return null;
        }
    }

    /**
     *
     * @param key The key associated to the value
     * @param value Value to be added to the map
     */
    @Override
    public V put(K key, V value) {
        byte[] rep;
        try {
            BFTMapMessage<K,V> request = new BFTMapMessage<>();
            request.setType(BFTMapRequestType.PUT);
            request.setKey(key);
            request.setValue(value);

            //invokes BFT-SMaRt
            rep = serviceProxy.invokeOrdered(BFTMapMessage.toBytes(request));
        } catch (IOException e) {
            logger.error("Failed to send PUT request");
            return null;
        }
        if (rep.length == 0) {
            return null;
        }

        try {
            BFTMapMessage<K,V> response = BFTMapMessage.fromBytes(rep);
            return response.getValue();
        } catch (ClassNotFoundException | IOException ex) {
            logger.error("Failed to deserialized response of PUT request");
            return null;
        }
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("You are supposed to implement this method :)");
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException("You are supposed to implement this method :)");
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException("You are supposed to implement this method :)");
    }

    @Override
    public boolean containsKey(Object key) {
        throw new UnsupportedOperationException("You are supposed to implement this method :)");
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("You are supposed to implement this method :)");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("You are supposed to implement this method :)");
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("You are supposed to implement this method :)");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException("You are supposed to implement this method :)");
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException("You are supposed to implement this method :)");
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("You are supposed to implement this method :)");
    }
}
