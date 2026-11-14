package app.m30.companion;

/** A user statement for an observed input connection, never a firmware readout. */
public final class ModeConfirmation {
    private String connection;
    private int mode=-1;
    public void confirm(int mode,String currentConnection){
        if(currentConnection==null||mode<0||mode>3)throw new IllegalArgumentException("Connected source and valid mode required");
        this.mode=mode;this.connection=currentConnection;
    }
    public void observe(String currentConnection){if(currentConnection==null||!currentConnection.equals(connection)){mode=-1;connection=null;}}
    public int current(){return mode;}
}
