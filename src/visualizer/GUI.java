package visualizer;

import common.Log;
import data.GameControlData;
import data.Rules;
import data.Teams;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

/**
 * @author: Michel Bartsch
 * 
 * This class displays the game-state
 */
public class GUI extends JFrame
{
    /**
     * Some constants defining this GUI`s appearance as their names say.
     * Feel free to change them and see what happens.
     */
    private static final boolean IS_OSX = System.getProperty("os.name").contains("OS X");
    private static final String WINDOW_TITLE = "Visualizer";
    private static final int DISPLAY_UPDATE_DELAY = 500;
    private static final String STANDARD_FONT = Font.DIALOG;
    private static final double STANDARD_FONT_SIZE = 0.09;
    private static final double STANDARD_FONT_XXL_SIZE = 0.16;
    private static final double STANDARD_FONT_S_SIZE = 0.06;
    private static final String TEST_FONT = "Lucida Console";
    private static final double TEST_FONT_SIZE = 0.01;
    private static final String CONFIG_PATH = "config/";
    private static final String BACKGROUND = "background";
    private static final String WAITING_FOR_PACKAGE = "waiting for package...";

    /** Available screens. */
    private static final GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

    BufferStrategy bufferStrategy;
    /** If testmode is on to just display whole GameControlData. */
    private boolean testmode = false;
    /** The last data received to show. */
    private GameControlData data = null;
    /** The background. */
    private BufferedImage background;
    
    /** The fonts used. */
    private Font testFont;
    private Font standardFont;
    private Font standardSmalFont;
    private Font scoreFont;
    private SimpleDateFormat clockFormat = new SimpleDateFormat("mm:ss");
    
    /**
     * Creates a new GUI.
     */
    GUI()
    {
        super(WINDOW_TITLE, devices[devices.length - 1].getDefaultConfiguration());
        
        setUndecorated(true);
        if(IS_OSX && devices.length != 1) {
            setSize(devices[devices.length-1].getDefaultConfiguration().getBounds().getSize());
        } else {
            devices[devices.length-1].setFullScreenWindow(this);
        }

        for (String format : new String [] {".png", ".jpeg", ".jpg"}) {
            try {
                background = ImageIO.read(new File(CONFIG_PATH+Rules.league.leagueDirectory+"/"+BACKGROUND+format));
            } catch(IOException e) {
            }
        }
        if(background == null) {
            Log.error("Unable to load background image");
        }
        float scaleFactor = (float)getWidth()/background.getWidth();
        Image tmp = (new ImageIcon(background).getImage()).getScaledInstance(
                (int)(background.getWidth()*scaleFactor),
                (int)(background.getHeight()*scaleFactor),
                Image.SCALE_DEFAULT);
        background = new BufferedImage((int) (background.getWidth() * scaleFactor), (int) (background.getWidth() * scaleFactor), BufferedImage.TYPE_INT_ARGB);
        background.getGraphics().drawImage(tmp, 0, 0, null);
        
        testFont = new Font(TEST_FONT, Font.PLAIN, (int)(TEST_FONT_SIZE*getWidth()));
        standardFont = new Font(STANDARD_FONT, Font.PLAIN, (int)(STANDARD_FONT_SIZE*getWidth()));
        standardSmalFont = new Font(STANDARD_FONT, Font.PLAIN, (int)(STANDARD_FONT_S_SIZE*getWidth()));
        scoreFont = new Font(STANDARD_FONT, Font.PLAIN, (int)(STANDARD_FONT_XXL_SIZE*getWidth()));
        
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e) {
                Main.exit();
            }
        });
        
        setVisible(true);
        createBufferStrategy(2);
        bufferStrategy = getBufferStrategy();
        Thread displayUpdater = new Thread()
        {
            @Override
            public void run() {
                while(true) {
                    update(data);
                    try {
                        Thread.sleep(DISPLAY_UPDATE_DELAY);
                    } catch (InterruptedException e) {}
                }
            }
        };
        displayUpdater.start();
    }
    
    /**
     * This toggles the visualizer´s testmode on and off.
     */
    public void toggleTestmode()
    {
        testmode = !testmode;
        update(data);
    }
    
    /**
     * This is called by the Listener after receiving GameControlData to show
     * them on the gui.
     * 
     * @param data  The GameControlData to show.
     */
    public synchronized void update(GameControlData data)
    {
        this.data = data;
        do {
            do {
                Graphics g = bufferStrategy.getDrawGraphics();
                draw(g);
                g.dispose();
            } while (bufferStrategy.contentsRestored());
            bufferStrategy.show();
        } while (bufferStrategy.contentsLost());
    }
    
    /**
     * This draws the whole visualizer.
     * 
     * @param g  The graphics object to draw on.
     */
    public final void draw(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.drawImage(background, 0, 0, null);
        
        if(data == null) {
            drawNoPackage(g);
        } else if(testmode) {
            drawTestmode(g);
        } else {
            drawTeams(g);
            drawScores(g);
            drawTime(g);
            drawSecState(g);
            drawState(g);
            drawSubTime(g);
            drawPenaltyInfo(g);
        }
    }
    
    /**
     * This draws something to inform that there is no package to draw.
     * 
     * @param g  The graphics object to draw on.
     */
    private void drawNoPackage(Graphics g)
    {
        g.setColor(Color.BLACK);
        g.setFont(testFont);
        g.drawString(WAITING_FOR_PACKAGE, (int)(0.2*getWidth()), (int)(0.3*getHeight()));
    }
    
    /**
     * This draws everything in the package in a simple way, just for testing.
     * 
     * @param g  The graphics object to draw on.
     */
    private void drawTestmode(Graphics g)
    {
        g.setColor(Color.BLACK);
        g.setFont(testFont);
        int x = getSizeToWidth(0.08);
        int y = getSizeToHeight(0.3);
        String[] out = data.toString().split("\n");
        for(int i=0; i<out.length; i++) {
            g.drawString(out[i], x, y);
            y += testFont.getSize()*1.2;
        }
        for(int j=0; j<2; j++) {
            out = data.team[j].toString().split("\n");
            for(int i=0; i<out.length; i++) {
                g.drawString(out[i], x, y);
                y += testFont.getSize()*1.2;
            }
        }
        
        x = getSizeToWidth(0.35);
        for(int i=0; i<2; i++) {
            y = getSizeToHeight(0.2);
            for(int j=0; j<data.team[i].player.length; j++) {
                out = data.team[i].player[j].toString().split("\n");
                for(int k=0; k<out.length; k++) {
                    g.drawString(out[k], x, y);
                    y += testFont.getSize()*1.2;
                }
            }
            x = getSizeToWidth(0.64);
        }
    }

    /**
     * This draws the teams´s icons.
     * 
     * @param g  The graphics object to draw on.
     */
    private void drawTeams(Graphics g)
    {
        int x = getSizeToWidth(0.01);
        int y = getSizeToHeight(0.33);
        int size = getSizeToWidth(0.3);
        //int yName = (int)(y + size * 1.15);
        BufferedImage[] icons = new BufferedImage[] {
            Teams.getIcon(data.team[0].teamNumber),
            Teams.getIcon(data.team[1].teamNumber)};
        /* Use this lines to display team-names and one line below
        g.setFont(standardSmalFont);
        int fontSize = g.getFont().getSize();
        boolean fittingSize = false;
        while(!fittingSize) {
            fittingSize = true;
            for(int i=0; i<2; i++) {
                if(g.getFontMetrics().stringWidth(Teams.getNames(false)[data.team[i].teamNumber]) > size) {
                    fittingSize = false;
                    g.setFont(g.getFont().deriveFont(Font.PLAIN, --fontSize));
                }
            }
        }*/
        for(int i=0; i<2; i++) {
            g.setColor(Rules.league.teamColor[data.team[i].teamColor]);
            float scaleFactorX = 1;
            float scaleFactorY = 1;
            if(icons[i].getWidth() * 1.2f > icons[i].getHeight()) {
                scaleFactorY = icons[i].getHeight()/(float)icons[i].getWidth();
            } else {
                scaleFactorX = icons[i].getWidth()/(float)icons[i].getHeight();
            }
            int offsetX = (int)((size - size*scaleFactorX)/2);
            int offsetY = (int)((size - size*scaleFactorY)/2);
            g.drawImage(icons[i],
                    (i==1 ? x : getWidth()-x-size) + offsetX,
                    y+offsetY,
                    (int)(scaleFactorX*size),
                    (int)(scaleFactorY*size), null);
            /* Use this line to display team-names
            drawCenteredString(g, Teams.getNames(false)[data.team[i].teamNumber],
                    (i==1 ? x : getWidth()-x-size) + offsetX,
                    yName,
                    size);*/
        }
    }
    
    /**
     * This draws the score.
     * 
     * @param g  The graphics object to draw on.
     */
    private void drawScores(Graphics g)
    {
        g.setFont(scoreFont);
        int x = getSizeToWidth(0.34);
        int y = getSizeToHeight(0.59);
        int yDiv = getSizeToHeight(0.59);
        int size = getSizeToWidth(0.12);
        g.setColor(Color.BLACK);
        drawCenteredString(g, ":", getWidth()/2-size, yDiv, 2*size);
        for(int i=0; i<2; i++) {
            g.setColor(Rules.league.teamColor[data.team[i].teamColor]);
            drawCenteredString(
                    g,
                    data.team[i].score+"",
                    i==1 ? x : getWidth()-x-size,
                    y,
                    size);
        }
    }
    
    /**
     * This draws the main time.
     * 
     * @param g  The graphics object to draw on.
     */
    private void drawTime(Graphics g)
    {
        g.setColor(Color.BLACK);
        g.setFont(standardFont);
        int x = getSizeToWidth(0.4);
        int y = getSizeToHeight(0.35);
        int size = getSizeToWidth(0.2);
        drawCenteredString(g, formatTime(data.secsRemaining), x, y, size);
    }
    
    /**
     * This draws the secondary state, for example "First Half".
     * 
     * @param g  The graphics object to draw on.
     */
    private void drawSecState(Graphics g)
    {
        g.setColor(Color.BLACK);
        g.setFont(standardSmalFont);
        int x = getSizeToWidth(0.4);
        int y = getSizeToHeight(0.74);
        int size = getSizeToWidth(0.2);
        String state;
        if(data.timeOut == GameControlData.C_FALSE) {
            switch(data.secGameState) {
                case GameControlData.STATE2_NORMAL:
                    if(data.firstHalf == GameControlData.C_TRUE) {
                        if(data.gameState == GameControlData.STATE_FINISHED) {
                            state = "Half Time";
                        } else {
                            state = "First Half";
                        }
                    } else {
                        if(data.gameState == GameControlData.STATE_INITIAL) {
                            state = "Half Time";
                        } else {
                            state = "Second Half";
                        }
                    }
                    break;
                case GameControlData.STATE2_OVERTIME:     state = "Overtime";   break;
                case GameControlData.STATE2_PENALTYSHOOT: state = "Penalty Shoot";     break;
                default: state = "";
            }
        } else {
            state = "Time Out";
        }
        drawCenteredString(g, state, x, y, size);
    }
    
    /**
     * This draws the state, for example "Initial".
     * 
     * @param g  The graphics object to draw on.
     */
    private void drawState(Graphics g)
    {
        g.setColor(Color.BLACK);
        g.setFont(standardSmalFont);
        int x = getSizeToWidth(0.4);
        int y = getSizeToHeight(0.85);
        int size = getSizeToWidth(0.2);
        String state;
        switch(data.gameState) {
            case GameControlData.STATE_INITIAL:  state = "Initial"; break;
            case GameControlData.STATE_READY:    state = "Ready";   break;
            case GameControlData.STATE_SET:      state = "Set";     break;
            case GameControlData.STATE_PLAYING:  state = "Playing"; break;
            case GameControlData.STATE_FINISHED: state = "Finished";  break;
            default: state = "";
        }
        drawCenteredString(g, state, x, y, size);
    }
    
    /**
     * This draws the sub time, for example the ready time.
     * 
     * @param g  The graphics object to draw on.
     */
    private void drawSubTime(Graphics g)
    {
        if(data.subTime == 0) {
            return;
        }
        g.setColor(Color.BLACK);
        g.setFont(standardSmalFont);
        int x = getSizeToWidth(0.4);
        int y = getSizeToHeight(0.96);
        int size = getSizeToWidth(0.2);
        drawCenteredString(g, formatTime(data.subTime), x, y, size);
    }
    
    /**
     * This draws the penalty tries and if they scored.
     * 
     * @param g  The graphics object to draw on.
     */
    private void drawPenaltyInfo(Graphics g)
    {
        g.setColor(Color.RED);
        int x = getSizeToWidth(0.05);
        int y = getSizeToHeight(0.86);
        int size = getSizeToWidth(0.02);
        for(int i=0; i<2; i++) {
            g.setColor(Rules.league.teamColor[data.team[i].teamColor]);
            for(int j=0; j<data.penaltyShot[i]; j++) {
                if((data.penaltyTries[i] & (1<<j)) != 0) {
                    g.fillOval(i==1 ? x+j*2*size : getWidth()-x-(5-j)*2*size-size, y, size, size);
                } else {
                    g.drawOval(i==1 ? x+j*2*size : getWidth()-x-(5-j)*2*size-size, y, size, size);
                }
            }
        }
    }
    
    /**
     * This is used to scale sizes depending on the visuaizers width.
     * 
     * @param size  A size between 0.0 (nothing) and 1.0 (full viualizers width).
     * 
     * @return A size in pixel.
     */
    private int getSizeToWidth(double size)
    {
        return (int)(size*getWidth());
    }
    
    /**
     * This is used to scale sizes depending on the visuaizers height.
     * 
     * @param size  A size between 0.0 (nothing) and 1.0 (full viualizers height).
     * 
     * @return A size in pixel.
     */
    private int getSizeToHeight(double size)
    {
        return (int)(size*getHeight());
    }
    
    /**
     * This simply draws a string horizontal centered on a given position.
     * 
     * @param g     The graphics object to draw on.
     * @param s     The string to draw.
     * @param x     Left position of the area to draw the string in.
     * @param y     Upper position of the area to draw the string in.
     * @param width The width of the area to draw the string centered in.
     */
    private void drawCenteredString(Graphics g, String s, int x, int y, int width)
    {
        int offset = (width - g.getFontMetrics().stringWidth(s)) / 2;
        g.drawString(s, x+offset, y);
    }
    
    /**
     * Formats a time in seconds to a usual looking minutes and seconds time as string.
     * 
     * @param seconds   Time to format in seconds.
     * 
     * @return Time formated.
     */
    private String formatTime(int seconds) {
        return (seconds < 0 ? "-" : "") + clockFormat.format(new Date(Math.abs(seconds) * 1000));
    }
}