package app.m30.companion;

/** Capability and explicit fallback choice; discovery/bonding is not proof of connected input. */
public final class BluetoothActionPolicy {
    public static final int NATIVE=0,LOCAL_LABEL=1,EXTERNAL=2,UNAVAILABLE=3,PAIR=4,CONNECTED=5;
    public static int forget(int api,boolean companion){return api>=36&&companion?NATIVE:EXTERNAL;}
    public static int rename(int api,boolean companion,boolean labels){return labels?LOCAL_LABEL:api>=31&&companion?NATIVE:UNAVAILABLE;}
    public static int disconnect(int api,boolean companion,boolean external){return external?EXTERNAL:api>=37&&companion?NATIVE:UNAVAILABLE;}
    public static int connect(int api,boolean companion,boolean saved,boolean external,boolean confirmedInput){
        if(confirmedInput)return CONNECTED;
        if(external)return EXTERNAL;
        if(!saved)return PAIR;
        return api>=37&&companion?NATIVE:UNAVAILABLE;
    }
}
