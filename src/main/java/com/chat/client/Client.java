package com.chat.client;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;
import java.awt.datatransfer.*;
import java.awt.*;

public class Client extends JPanel implements KeyListener, ActionListener {
    JFrame frame;
    int textFiedBoundry = 0;
    boolean textSelected = false;
    ArrayList<String> previousText = new ArrayList<>();
    String currentText = "";
    String inProgressText = "";
    Font font = new Font("Impact", Font.TRUETYPE_FONT, 40);// sets the normal font
    int sub = previousText.size() - 1;// the index of the message the chat history is to start from
    boolean ctrl = false;
    boolean alt = false;
    boolean delete = false;
    String userName = null;
    int localNumOfSavedMessages = 50; // the number of localy loaded messages in the chat
    String configFile = "config.txt";
    String serverDetailsFile = "serverDetails.txt";
    int lastOffsetInBytes = 0;// the number of bytes that were ignored in the last download of messages
    String oldestLoadedChunk = "";
    int numOfExtraLoadedChunks = 0;
    String fontSizeRegex = "^!(fontSize)\\((\\d*)\\)$";
    String colourRegex = "^!(\\w*)Colour\\(((\\d+),(\\d+),(\\d+))\\)$";
    String serverInfoRegex = "^!serverInfo$";
    String setServerInfoRegex = "^!set(\\w*)\\(((?:\\d+.\\d+.\\d+.\\d+)||(?:\\d+))\\)$";
    String reConnectRegex = "^!connect$";
    String connectedUsersRegex = "^!connectedClients$";
    int cursorOffset = 0;
    Config cfg;
    InputStream in = null;
    BufferedReader bin = null;
    Socket sock = null;
    PrintWriter writer = null;
    ClientThread ct = null;
    protected boolean connectedToServer = false;
    protected boolean updateSuccess = false;
    private ArrayList<String> personalMessageLog = new ArrayList<String>();
    private int personalMessageLogIndex = 0;

    protected enum connectionStatuses {
        connected, disconnected, error
    }

    protected connectionStatuses connectionStatus = connectionStatuses.disconnected;
    String ip = "35.189.80.190";
    int port = 5678;
    private String pass = "i;<tc2%Otv(\\5B,w0f\\w9,Tw|8v|uK2;Amibjxy?F`68oh8}\\Y2S|(7V=L;8fd";
    final double version = 1.08;

    public static void main(String[] args) {
        new Client();
    }

    @Override
    public void paint(Graphics g) {
        try {
            this.textFiedBoundry = this.getHeight() - 70;
            g.setColor(cfg.getBackgroundColour());
            g.fillRect(0, 0, frame.getWidth(), frame.getHeight());
            g.setColor(cfg.getTextColour());
            g.setFont(font);
            textFeild(g);
            drawHistory(g);
        } catch (Exception e) {
        }

    }

    Client() {
        frame = new JFrame();
        frame.setBackground(Color.black);
        frame.add(this);

        frame.setSize(900, 700);
        frame.setResizable(false);

        // setting start position of the frame
        frame.setLocationRelativeTo(null);

        // closing
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Title
        frame.setTitle("DISKORD V" + this.version);

        // set frame visibility
        frame.setVisible(true);

        frame.addKeyListener(this);
        addMouseHandler();

        userName = getComputerName();

        sub = previousText.size() - 1;
        cfg = Config.getInstance(Color.darkGray, Color.green, Color.white, 40);
        try {
            loadConfig();
        } catch (Exception e) {
        }
        saveConfig();
        font = new Font("Impact", Font.TRUETYPE_FONT, cfg.getFontSize());
        // previousText.add("connecting...");
        this.outputToConsole("connecting...");
        repaint();
        try {
            connectToServer();
            while (this.connectionStatus == connectionStatuses.disconnected) {
                Thread.sleep(100);
                switch (this.connectionStatus) {
                    case connected:
                        this.connectedToServer = true;
                        System.out.println("connected");
                        break;
                    case disconnected:
                        break;
                    case error:
                        throw new Exception("your connection was probably refused :/ sry lmao");
                }
            }

            // setup client after an update
            this.postUpdateSetup();
            System.out.println("post update setup was sucessful");
            // update the client
            this.update();
            while (!updateSuccess) {
                // do nothing
                Thread.sleep(100);
            }

            // request the message log
            this.readMessages(0, this.localNumOfSavedMessages);
            Thread.sleep(1000);
            oldestLoadedChunk = ArrayListToString(previousText);
            System.out.println("connected and ready to go bby");
            // send welcome message
            outputToConsole("connected, use !help to see a list of commands");

        } catch (Exception e) {
            this.serverConnectionError(e.toString());
        }

        repaint();
    }

    private void update() {
        System.out.println("updating");
        int min = 1000;
        int max = 9999;
        int random_int = (int) Math.floor(Math.random() * (max - min + 1) + min);
        this.ct.isUpdating(true, random_int);
        this.writeMessage("!UPDATE:" + random_int + "," + this.version);
    }

    private void postUpdateSetup() {
        System.out.println("doing post update setup");
        File setupFile = new File("setup1.bat");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // if there has been an update
        if (setupFile.exists()) {
            // delete the old version
            if (setupFile.delete()) {
                this.outputToConsole("updated");
            } else {
                this.outputToConsole("we tried to update but idk, it fucked itself so... AHAHAHAHAH LOSER");
            }
        }
    }

    private void connectToServer() throws IOException {
        System.out.println("trying to connect to server");
        loadServerInfo();
        this.sock = new Socket(this.ip, this.port);
        this.in = sock.getInputStream();
        this.bin = new BufferedReader(new InputStreamReader(in));
        this.writer = new PrintWriter(sock.getOutputStream(), true);
        this.ct = new ClientThread(this, this.sock);
        ct.start();
        this.writeMessage(this.pass);
    }

    private void disconnectFromServer() throws IOException {
        ct.interrupt();
        this.sock.close();
    }

    protected void serverConnectionError(String msg) {
        this.connectedToServer = false;
        outputToConsole("UH OH... STINKY, ERROR CONNECTING TO SERVER \n");
        outputToConsole(msg);
        outputToConsole("use !help to see a list of commands");
        repaint();
    }

    private void loadServerInfo() {
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(serverDetailsFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = "";
            ArrayList<String> data = new ArrayList<>();
            while ((line = bufferedReader.readLine()) != null) {
                data.add(line);
            }
            bufferedReader.close();
            this.ip = data.get(0);
            this.port = Integer.valueOf(data.get(1));

        } catch (Exception ex) {
            System.out.println("error loading config");
            saveServerInfo();
            ex.printStackTrace();
        }
    }

    private void saveServerInfo() {
        File f = new File(serverDetailsFile);
        try {
            FileWriter fw = new FileWriter(f, false);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(this.ip);
            bw.newLine();
            bw.write(String.valueOf(this.port));
            bw.close();
        } catch (IOException xe) {
        }
    }

    private String getComputerName() {
        Map<String, String> env = System.getenv();
        if (env.containsKey("COMPUTERNAME")) {
            return env.get("COMPUTERNAME");
        } else if (env.containsKey("HOSTNAME")) {
            return env.get("HOSTNAME");
        } else {
            return "Unknown Computer";
        }
    }

    // keylistner
    @Override
    public void keyPressed(KeyEvent ke) {
        System.out.println(ke.getKeyCode());
        if (textSelected == true) {

            switch (ke.getKeyCode()) {

                case 8:
                    // backspace
                    if (currentText.length() != 0) {
                        String temp = "";
                        if (cursorOffset == 0) {
                            temp = currentText.substring(0, currentText.length() - 1);
                        } else {
                            temp = currentText.substring(0, (currentText.length() - 1) - cursorOffset)
                                    + currentText.substring(currentText.length() - cursorOffset);
                        }
                        currentText = temp;
                    }
                    break;

                case 10:
                    // enter
                    if (!currentText.equals("")) {
                        if ((connectedToServer == true) && (!currentText.startsWith("!", 0))) {
                            this.personalMessageLog.add(0, currentText);
                            writeMessage(userName + ": " + currentText);
                        }
                        if (currentText.startsWith("!", 0)) {
                            if (currentText.equals("!help")) {
                                outputToConsole("----commands----");
                                outputToConsole("!fontColour(r,g,b)");
                                outputToConsole("!backgroundColour(r,g,b)");
                                outputToConsole("!cursorColour(r,g,b)");
                                outputToConsole("!fontSize(<size>)");
                                outputToConsole("!serverInfo");
                                outputToConsole("!setIP(<ip address>)");
                                outputToConsole("!setPort(<port no>)");
                                outputToConsole("!connect");
                                outputToConsole("!connectedClients");
                            } else {
                                // colour commands
                                Pattern pattern;
                                Matcher m;
                                pattern = Pattern.compile(colourRegex);
                                m = pattern.matcher(currentText);
                                String message = "done";

                                if (m.matches() == true) {
                                    Color c = new Color(0);
                                    try {
                                        int r = Integer.valueOf(m.group(3));
                                        int g = Integer.valueOf(m.group(4));
                                        int b = Integer.valueOf(m.group(5));
                                        c = new Color(r, g, b);
                                    } catch (Exception e) {
                                        message = "command error";
                                    }

                                    switch (m.group(1)) {
                                        case "background":
                                            cfg.setBackgroundColour(c);
                                            break;
                                        case "font":
                                            cfg.setTextColour(c);
                                            break;
                                        case "cursor":
                                            cfg.setCursorColour(c);
                                            break;
                                        default:
                                            message = "command error";
                                    }
                                    outputToConsole(message);

                                } else {
                                    // font size commands
                                    pattern = Pattern.compile(fontSizeRegex);
                                    m = pattern.matcher(currentText);
                                    if (m.matches() == true) {
                                        try {
                                            cfg.setFontSize(Integer.valueOf(m.group(2)));
                                        } catch (Exception e) {
                                            message = "command error";
                                        }
                                        outputToConsole(message);
                                    } else {
                                        // show server info commands
                                        pattern = Pattern.compile(serverInfoRegex);
                                        m = pattern.matcher(currentText);
                                        if (m.matches() == true) {
                                            if (this.sock.isClosed()) {
                                                this.connectedToServer = false;
                                            } else {
                                                this.connectedToServer = true;
                                            }
                                            String info = "Server Info---->    IP: " + this.ip + "    port: "
                                                    + this.port
                                                    + "    connected: " + this.connectedToServer;
                                            this.outputToConsole(info);
                                            repaint();
                                        } else {
                                            // set server info commands
                                            pattern = Pattern.compile(setServerInfoRegex);
                                            m = pattern.matcher(currentText);
                                            if (m.matches() == true) {
                                                switch (m.group(1)) {
                                                    case "IP":
                                                        this.ip = m.group(2);
                                                        break;
                                                    case "Port":
                                                        this.port = Integer.valueOf(m.group(2));
                                                }
                                                this.saveServerInfo();
                                                outputToConsole("done");
                                            } else {
                                                pattern = Pattern.compile(reConnectRegex);
                                                m = pattern.matcher(currentText);
                                                if (m.matches() == true) {
                                                    try {
                                                        this.disconnectFromServer();
                                                        this.connectToServer();
                                                    } catch (IOException e) {
                                                        // TODO Auto-generated catch block
                                                        e.printStackTrace();
                                                    }
                                                } else {
                                                    pattern = Pattern.compile(connectedUsersRegex);
                                                    m = pattern.matcher(currentText);
                                                    if (m.matches() == true) {
                                                        this.writeMessage("!connectedClients:");
                                                    } else {
                                                        writeMessage(userName + ": " + currentText);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                saveConfig();
                            }
                        }

                    }
                    currentText = "";
                    cursorOffset = 0;
                    // repaint();
                    break;

                case 16:
                    // shift
                    break;

                case 20:
                    // caps lock
                    break;

                case 17:
                    // Ctrl
                    ctrl = true;
                    break;

                case 18:
                    // ALT
                    alt = true;
                    break;

                case 86:
                    // v
                    if (ctrl == true) {
                        // Create a Clipboard object using getSystemClipboard() method
                        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();

                        // Get data stored in the clipboard that is in the form of a string (text)
                        try {
                            this.currentText += String.valueOf(c.getData(DataFlavor.stringFlavor));
                        } catch (UnsupportedFlavorException | IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    break;

                case 525:
                    // page key
                    break;

                case 39:
                    // right arrow
                    if (cursorOffset > 0) {
                        cursorOffset--;
                    }
                    break;

                case 37:
                    // left arrow
                    if (cursorOffset < currentText.length()) {
                        cursorOffset++;
                    }
                    break;

                case 38:
                    // up arrow
                    if (this.personalMessageLogIndex == 0) {
                        this.inProgressText = this.currentText;
                    }
                    if ((personalMessageLog.size() > 0)
                            && (this.personalMessageLogIndex < personalMessageLog.size())) {
                        this.currentText = this.personalMessageLog.get(personalMessageLogIndex);
                    }
                    if (this.personalMessageLogIndex < personalMessageLog.size()) {
                        this.personalMessageLogIndex++;
                    }
                    break;

                case 40:
                    // down arrow
                    if (this.personalMessageLogIndex == personalMessageLog.size()) {
                        this.personalMessageLogIndex--;
                    }
                    if (this.personalMessageLogIndex == 0) {
                        this.currentText = this.inProgressText;
                    } else {
                        this.personalMessageLogIndex--;
                        this.currentText = this.personalMessageLog.get(personalMessageLogIndex);
                    }
                    break;

                case 127:
                    // delete
                    delete = true;
                    break;

                case 192:
                    // backtick
                    if ((ctrl == true) && (alt == true)) {

                        System.exit(0);

                    }

                default:
                    if (cursorOffset == 0) {
                        currentText += ke.getKeyChar();
                    } else {
                        String temp = currentText.substring(0, currentText.length() - cursorOffset) + ke.getKeyChar()
                                + currentText.substring(currentText.length() - cursorOffset);
                        currentText = temp;
                    }

            }
            repaint();

        }

    }

    // adds a mouse listner to the program so the user can scroll thrugh chat
    private void addMouseHandler() {

        MouseInputAdapter mia = new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                System.out.println("mouse clicked");

                Point p = e.getPoint();

                int mouseX = p.x;
                int mouseY = p.y;

                if ((mouseX > 0) && (mouseX < frame.getWidth()) && (mouseY > textFiedBoundry)
                        && (mouseY < frame.getHeight())) {

                    System.out.println("textField");
                    if (textSelected == false) {

                        textSelected = true;

                    }

                } else {

                    textSelected = false;

                }
                repaint();

            }

        };
        frame.addMouseListener(mia);
        frame.addMouseMotionListener(mia);

        MouseWheelListener listener = new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent mwe) {

                int scroll = mwe.getWheelRotation();
                if (textSelected == false) {

                    if (scroll < 1) {

                        System.out.println("up");
                        sub--;
                        if (sub == -1) {
                            numOfExtraLoadedChunks++;
                            int numbOfBytes = oldestLoadedChunk.getBytes().length + 1;
                            ArrayList<String> nextChunk = new ArrayList<>();
                            try {
                                if (previousText.size() >= localNumOfSavedMessages) {
                                    ArrayList<String> tmp = new ArrayList<>(previousText.size());
                                    for (int i = 0; i < previousText.size(); i++) {
                                        tmp.add(previousText.get(i));
                                    }
                                    nextChunk = tmp;
                                    // previousText.clear();
                                    previousText = new ArrayList<>(previousText.size() + localNumOfSavedMessages);
                                    // nextChunk =
                                    readMessages(lastOffsetInBytes + numbOfBytes, localNumOfSavedMessages);
                                    // wait until all the older messages have been added to previous text
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }

                                    // add the most recent messages to the end of the list
                                    for (String s : tmp) {
                                        previousText.add(s);
                                    }
                                }
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            // ArrayList<String> temp = new ArrayList<>();
                            // temp.addAll(nextChunk);
                            // temp.addAll(previousText);
                            // previousText = temp;
                            lastOffsetInBytes += numbOfBytes;

                            oldestLoadedChunk = ArrayListToString(nextChunk);
                            // sub = (previousText.size() - (previousText.size() - nextChunk.size())) - 1;
                            sub = (previousText.size() - nextChunk.size()) - 1;
                            System.out.println("SUB: " + sub);
                            repaint();

                        }

                    } else {

                        System.out.println("down");
                        if (sub < previousText.size() - 1) {
                            sub++;
                            if (sub == previousText.size() - 1) {
                                try {
                                    if (previousText.size() >= localNumOfSavedMessages) {
                                        // previousText =
                                        readMessages(0, localNumOfSavedMessages);
                                    }
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                oldestLoadedChunk = ArrayListToString(previousText);
                                lastOffsetInBytes = 0;
                                numOfExtraLoadedChunks = 0;

                                sub = previousText.size() - 1;
                            }
                        }

                    }

                }
                repaint();

            }
        };

        frame.addMouseWheelListener(listener);
    }

    private void textFeild(Graphics g) {

        g.setColor(Color.white);
        g.drawRect(0, textFiedBoundry, this.getWidth(), this.getHeight());

        if (textSelected == true) {
            g.setColor(cfg.getTextColour());
            FontMetrics metrics = g.getFontMetrics(font);
            int adv = (metrics.stringWidth(currentText) + 4);
            // System.out.println(adv);
            g.setFont(font);
            int x = 5;
            if (adv > this.getWidth()) {

                int move = (adv - this.getWidth());
                x = x - move - 2;
                adv = adv - move - 2;

            }
            g.drawString(currentText, x, this.getHeight() - 20);

            int offsetLength = 0;
            if (!currentText.isEmpty()) {
                String sub = currentText.substring(currentText.length() - cursorOffset, currentText.length());
                offsetLength = metrics.stringWidth(sub);
            }
            drawCursor(g, adv - offsetLength);

        }
    }

    private void drawCursor(Graphics g, int x) {

        g.setColor(cfg.getCursorColour());
        int y = textFiedBoundry + 4;
        int width = 3;
        int height = frame.getHeight() - textFiedBoundry;

        g.fillRect(x, y, width, height);

    }

    private void drawHistory(Graphics g) {
        if (!previousText.isEmpty()) {
            g.setColor(cfg.getTextColour());

            int x = 10;
            int y = textFiedBoundry - 10;
            FontMetrics metrics = g.getFontMetrics(font);
            int hgt = metrics.getHeight();

            for (int i = sub; i >= 0; i--) {
                int width = metrics.stringWidth(previousText.get(i));
                if (width > (frame.getWidth() - 100)) {
                    ArrayList<String> newMessage = wordWrap(previousText.get(i), metrics, frame.getWidth() - 100);
                    for (int j = newMessage.size() - 1; j >= 0; j--) {
                        String s = "";
                        if (j == 0) {
                            s += ">";
                        }
                        s += newMessage.get(j);
                        g.drawString(s, x, y);
                        y = y - hgt;
                    }
                } else {
                    g.drawString(">" + previousText.get(i), x, y);
                    y = y - hgt;
                }

            }

        }

    }

    @Override
    public void keyTyped(KeyEvent ke) {
    }

    @Override
    public void keyReleased(KeyEvent ke) {
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
    }

    protected void outputToConsole(String outp) {

        if (sub == previousText.size() - 1) {
            int lastSubPos = sub;
            previousText.add(outp);
            if ((previousText.size() > localNumOfSavedMessages) && (numOfExtraLoadedChunks == 0)) {
                previousText.remove(0);
            }

            if (numOfExtraLoadedChunks > 0) {
                lastOffsetInBytes += (outp.length() + 2);
            }

            if (lastSubPos == previousText.size() - 2) {
                sub = previousText.size() - 1;
            }
            repaint();
        }
    }

    protected void writeMessage(String msg) {
        this.writer.println(msg);
    }

    private void readMessages(int offset, int buffer) throws IOException {
        System.out.println("requesting messages");
        ArrayList<String> messages = new ArrayList<String>();
        DataInputStream dis = new DataInputStream(this.sock.getInputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(dis));
        // ask the server for the log request
        this.writer.println("LOGREQUEST" + offset + "," + buffer);

    }

    private String ArrayListToString(ArrayList<String> list) {

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i != 0) {
                builder.append("\r\n");
            }
            builder.append(list.get(i));
        }
        return builder.toString();
    }

    private ArrayList wordWrap(String msg, FontMetrics metrics, int limit) {
        // format string into arraylist
        ArrayList<String> words = new ArrayList<>();
        int cumulativeSum = 0;
        words = new ArrayList<String>(Arrays.asList(msg.split(" ")));
        ArrayList<Integer> positionsForNewline = new ArrayList<>();
        boolean addNewlines = true;

        while (addNewlines == true) {
            cumulativeSum = 0;
            int i;
            try {
                i = positionsForNewline.get(positionsForNewline.size() - 1);
            } catch (IndexOutOfBoundsException e) {
                i = 0;
            }
            while (i < words.size()) {
                int wordSize = metrics.stringWidth(words.get(i));
                cumulativeSum += wordSize;
                if (cumulativeSum > limit) {
                    positionsForNewline.add(i - 1);
                    addNewlines = true;
                    break;
                }
                addNewlines = false;
                i++;
            }
        }

        StringBuilder builder = new StringBuilder();
        for (int j = 0; j < words.size(); j++) {
            if (j != 0) {
                builder.append(" ");
            }
            builder.append(words.get(j));
            if (!positionsForNewline.isEmpty()) {
                if (j == positionsForNewline.get(0)) {
                    builder.append("\n");
                    positionsForNewline.remove(0);
                }
            }
        }

        ArrayList<String> lines = new ArrayList<String>(Arrays.asList(builder.toString().split("\n")));
        return lines;

    }

    private void saveConfig() {

        File f = new File(configFile);
        try {
            FileWriter fw = new FileWriter(f, false);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(String.valueOf(cfg.getFontSize()));
            bw.newLine();
            bw.write(String.valueOf(cfg.getAutoMessageColour().getRGB()));
            bw.newLine();
            bw.write(String.valueOf(cfg.getBackgroundColour().getRGB()));
            bw.newLine();
            bw.write(String.valueOf(cfg.getCursorColour().getRGB()));
            bw.newLine();
            bw.write(String.valueOf(cfg.getTextColour().getRGB()));
            bw.close();
        } catch (IOException xe) {
        }

    }

    private void loadConfig() {

        FileReader fileReader = null;
        try {
            fileReader = new FileReader(configFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = "";
            ArrayList<String> data = new ArrayList<>();
            while ((line = bufferedReader.readLine()) != null) {
                data.add(line);
            }
            cfg.setFontSize(Integer.valueOf(data.get(0)));
            Color c;
            c = new Color(Integer.valueOf(data.get(2)));
            cfg.setBackgroundColour(c);
            c = new Color(Integer.valueOf(data.get(3)));
            cfg.setCursorColour(c);
            c = new Color(Integer.valueOf(data.get(4)));
            cfg.setTextColour(c);

        } catch (Exception ex) {
            System.out.println("error loading config");
            ex.printStackTrace();
        }

    }
}

// TODO ----------------------------------------------------------------
/*
 * add support to copy and paste
 * add support for images
 * add encryption
 * add character limit
 * add notification
 * add icon
 */
