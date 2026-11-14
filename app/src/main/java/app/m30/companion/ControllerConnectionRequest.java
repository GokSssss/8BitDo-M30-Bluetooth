package app.m30.companion;

/** An explicit Bluetooth choice waits for a physical button, never for a name match. */
public final class ControllerConnectionRequest {
    public int slot=-1;
    public String address="";
    public long deadline;
    public void begin(String address,int slot,long now){
        if(slot<0||slot>1||address==null||!address.matches("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}"))throw new IllegalArgumentException("Invalid connection target");
        this.address=address;this.slot=slot;deadline=now+120_000;
    }
    public boolean active(long now){if(slot>=0&&now>=deadline)clear();return slot>=0;}
    public boolean confirms(boolean physical,boolean down,int repeats,long now){return active(now)&&physical&&down&&repeats==0;}
    public void clear(){slot=-1;address="";deadline=0;}
}
