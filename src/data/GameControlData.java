package data;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * @author: Michel Bartsch
 * 
 * This class is part of the data wich are send to the robots.
 * It just represents this data, reads and writes between C-structure and
 * Java, nothing more.
 */
public class GameControlData implements Serializable
{
    /** Some constants from the C-structure. */
    public static final int GAMECONTROLLER_PORT = 3838;

    public static final String GAMECONTROLLER_STRUCT_HEADER = "RGme";
    public static final int GAMECONTROLLER_STRUCT_VERSION = 7;

    public static final byte TEAM_BLUE = 0;
    public static final byte TEAM_RED = 1;
    public static final byte DROPBALL = 2;

    public static final byte GOAL_BLUE = 0;
    public static final byte GOAL_YELLOW = 1;

    public static final byte STATE_INITIAL = 0;
    public static final byte STATE_READY = 1;
    public static final byte STATE_SET = 2;
    public static final byte STATE_PLAYING = 3;
    public static final byte STATE_FINISHED = 4;

    public static final byte STATE2_NORMAL = 0;
    public static final byte STATE2_PENALTYSHOOT = 1;
    public static final byte STATE2_OVERTIME = 2;
    
    public static final byte C_FALSE = 0;
    public static final byte C_TRUE = 1;
    
    
    /** The size in bytes this class has packed. */
    public static final int SIZE =
            4 + // header
            4 + // version
            1 + // numPlayers
            1 + // gameState
            1 + // firstHalf
            1 + // kickOffTeam
            1 + // secGameState
            1 + // dropInTeam
            2 + // dropInTime
            4 + // secsRemaining
            2 * TeamInfo.SIZE;
    
    //this is streamed
    // GAMECONTROLLER_STRUCT_HEADER                             // header to identify the structure
    // GAMECONTROLLER_STRUCT_VERSION                            // version of the data structure
    public byte playersPerTeam = (byte)Rules.league.teamSize;   // The number of players on a team
    public byte gameState = STATE_INITIAL;                      // state of the game (STATE_READY, STATE_PLAYING, etc)
    public byte firstHalf = C_TRUE;                             // 1 = game in first half, 0 otherwise
    public byte kickOffTeam = TEAM_BLUE;                        // the next team to kick off
    public byte secGameState = STATE2_NORMAL;                   // Extra state information - (STATE2_NORMAL, STATE2_PENALTYSHOOT, etc)
    public byte dropInTeam;                                     // team that caused last drop in
    protected short dropInTime = -1;                            // number of seconds passed since the last drop in. -1 before first dropin
    public int secsRemaining = Rules.league.halfTime;        // estimate of number of seconds remaining in the half
    public TeamInfo[] team = new TeamInfo[2];
    // hacked into the package
    public byte timeOut = 0;                   // if currently there is a timeout, maybe another secState in the future
    public byte penaltyShot[] = {0, 0};        // how many penalty-shots have been made by each team so far
    public short penaltyTries[] = {0, 0};      // each bit, from lower to higher, represents a penalty shot: 1 = goal; 0 = fail
    public short subTime = 0;                  // sub-time (remaining in ready state etc.) in seconds
    
    
    /**
     * Creates a new GameControlData.
     */
    public GameControlData()
    {
        for(int i=0; i<team.length; i++) {
            team[i] = new TeamInfo();
        }
        team[0].teamColor = TEAM_BLUE;
        team[1].teamColor = TEAM_RED;
        team[0].goalColor = GOAL_YELLOW;
        team[1].goalColor = GOAL_YELLOW;
    }
    
    /**
     * Returns the corresponding byte-stream of the state of this object.
     *
     * @return  the corresponding byte-stream of the state of this object
     */
    public ByteBuffer toByteArray()
    {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(GAMECONTROLLER_STRUCT_HEADER.getBytes(), 0, 4);
        buffer.putInt(GAMECONTROLLER_STRUCT_VERSION);
        buffer.put(playersPerTeam);
        buffer.put(gameState);
        buffer.put(firstHalf);
        buffer.put(kickOffTeam);
        buffer.put(secGameState);
        buffer.put(dropInTeam);
        buffer.putShort(dropInTime);
        buffer.putInt(secsRemaining);
        for (TeamInfo aTeam : team) {
            buffer.put(aTeam.toByteArray());
        }
        /* Hack to have this in the package */
        buffer.position(SIZE-9);
        buffer.put(timeOut);
        buffer.put(penaltyShot[0]);
        buffer.put(penaltyShot[1]);
        buffer.putShort(penaltyTries[0]);
        buffer.putShort(penaltyTries[1]);
        buffer.putShort(subTime);
        return buffer;
    }
    
    /**
     * Unpacking the C-structure to the Java class.
     * 
     * @param buffer    The buffered C-structure.
     * @return Whether the structure was well formed. That is, it must have the proper 
     *          {@link #GAMECONTROLLER_STRUCT_VERSION} set.
     */
    public boolean fromByteArray(ByteBuffer buffer)
    {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        byte[] header = new byte[4];
        buffer.get(header, 0, 4);
        if(buffer.getInt() != GAMECONTROLLER_STRUCT_VERSION) {
            return false;
        }
        playersPerTeam = buffer.get();
        gameState = buffer.get();
        firstHalf = buffer.get();
        kickOffTeam = buffer.get();
        secGameState = buffer.get();
        dropInTeam = buffer.get();
        dropInTime = buffer.getShort();
        secsRemaining = buffer.getInt();
        for(int i=0; i<team.length; i++) {
            team[i].fromByteArray(buffer);
        }
        /* Hack to have this in the package */
        buffer.position(SIZE-9);
        timeOut = buffer.get();
        penaltyShot[0] = buffer.get();
        penaltyShot[1] = buffer.get();
        penaltyTries[0] = buffer.getShort();
        penaltyTries[1] = buffer.getShort();
        subTime = buffer.getShort();
        return true;
    }
    
    @Override
    public String toString()
    {
        String out = "";
        String temp;
        
        out += "             Header: "+GAMECONTROLLER_STRUCT_HEADER+"\n";
        out += "            Version: "+GAMECONTROLLER_STRUCT_VERSION+"\n";
        out += "   Players per Team: "+playersPerTeam+"\n";
        switch(gameState) {
            case STATE_INITIAL:  temp = "initial"; break;
            case STATE_READY:    temp = "ready";   break;
            case STATE_SET:      temp = "set";     break;
            case STATE_PLAYING:  temp = "playing"; break;
            case STATE_FINISHED: temp = "finish";  break;
            default: temp = "undefinied("+gameState+")";
        }
        out += "          gameState: "+temp+"\n";
        switch(firstHalf) {
            case C_TRUE:  temp = "true";  break;
            case C_FALSE: temp = "false"; break;
            default: temp = "undefinied("+firstHalf+")";
        }
        out += "          firstHalf: "+temp+"\n";
        switch(kickOffTeam) {
            case TEAM_BLUE: temp = "blue"; break;
            case TEAM_RED:  temp = "red";  break;
            default: temp = "undefinied("+kickOffTeam+")";
        }
        out += "        kickOffTeam: "+temp+"\n";
        switch(secGameState) {
            case STATE2_NORMAL:       temp = "normal"; break;
            case STATE2_PENALTYSHOOT: temp = "penaltyshoot";  break;
            case STATE2_OVERTIME:     temp = "overtime";  break;
            default: temp = "undefinied("+secGameState+")";
        }
        out += "       secGameState: "+temp+"\n";
        switch(dropInTeam) {
            case TEAM_BLUE: temp = "blue"; break;
            case TEAM_RED:  temp = "red";  break;
            default: temp = "undefinied("+dropInTeam+")";
        }
        out += "         dropInTeam: "+temp+"\n";
        out += "         dropInTime: "+dropInTime+"\n";
        out += "      secsRemaining: "+secsRemaining+"\n";
        return out;
    }
}