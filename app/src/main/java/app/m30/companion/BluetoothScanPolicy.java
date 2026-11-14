package app.m30.companion;

/** Saved pairs do not scan; only explicit discovery chooses one of these paths. */
public final class BluetoothScanPolicy {
    public static final int NATIVE=0,OFFER_EXTERNAL=1,EXTERNAL=2;
    public static int path(int api,boolean externalChosen,boolean locationEnabled){
        if(externalChosen)return EXTERNAL;
        if(api<=30&&!locationEnabled)return OFFER_EXTERNAL;
        return NATIVE;
    }
}
