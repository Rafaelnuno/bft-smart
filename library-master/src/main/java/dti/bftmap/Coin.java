package dti.bftmap;

import java.util.Random;

public class Coin {
    private float value;
    private int clientId;
    private long id;
    Random rd = new Random();
      

    public Coin(int clientId, float value, long id) {
       
        this.clientId = clientId;
        this.value = value;
        this.id = id;
    }

    public float getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
   
    public Coin spend(float value, int recv) {

        this.value -= value;
        return new Coin(this.clientId, value, new Random().nextLong());
    }
}
