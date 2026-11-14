package app.m30.companion;

/** Correlates platform results with the selected address; BOND_BONDED is never HID-ready. */
public final class BluetoothPairingState {
    public static final int IDLE=0,PAIRING=1,PAIRED=2,ERROR=3;
    public static final long TIMEOUT_MS=90_000;
    private String address="";
    private int phase;
    private long deadline;
    private boolean sawBonding;
    public String address(){return address;}
    public int phase(){return phase;}
    public long deadline(){return deadline;}
    public boolean sawBonding(){return sawBonding;}
    public void select(String selected,long now){
        if(selected==null||!selected.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}"))throw new IllegalArgumentException("Invalid Bluetooth address");
        address=selected;phase=PAIRING;deadline=now+TIMEOUT_MS;sawBonding=false;
    }
    public boolean bondEvent(String from,int state){return update(from,state,true);}
    public boolean poll(String from,int state){return update(from,state,false);}
    private boolean update(String from,int state,boolean event){
        if(phase!=PAIRING||!address.equalsIgnoreCase(from==null?"":from))return false;
        if(state==12)phase=PAIRED;
        else if(state==11)sawBonding=true;
        else if(state==10&&(event||sawBonding))phase=ERROR;
        else return false;
        return true;
    }
    public boolean expire(long now){if(phase!=PAIRING||now<deadline)return false;phase=ERROR;return true;}
    public void fail(){if(phase==PAIRING)phase=ERROR;}
    public void restore(String selected,int savedPhase,long savedDeadline,boolean seen,long now){
        if(selected==null||!selected.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")||savedPhase<0||savedPhase>ERROR)return;
        address=selected;phase=savedPhase;deadline=Math.min(savedDeadline,now+TIMEOUT_MS);sawBonding=seen;expire(now);
    }
}
