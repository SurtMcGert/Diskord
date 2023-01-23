package com.chat.server;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.chat.Message;

import java.io.*;

class ServerClientThread extends Thread {
    private Socket socket; // the socket this thread will run on
    private int clientNo; // the number of the client on this thread
    private Server server; // the server this thread is attached too

    private DataInputStream dis; // data input stream from the thread
    private BufferedInputStream bis; // buffered input stream from the thread
    private DataOutputStream dos; // data output stream onto the thread

    private boolean validated = false; // is the client on this thread validated
    private String pass = "i;<tc2%Otv(\\5B,w0f\\w9,Tw|8v|uK2;Amibjxy?F`68oh8}\\Y2S|(7V=L;8fd"; // the password required
                                                                                               // for validation
    private boolean updatingClient = false; // is the client updating
    private ArrayList<Message[]> messageBuffer; // currently buffered messages that need sending to the client
    private boolean clientReady = false; // is the client ready
    private boolean collectedUsername = false; // is the clients username known
    private String username = ""; // the username of the client on this thread

    /**
     * constructor
     * 
     * @param server   - the server this thread is attached too
     * @param inSocket - the socket this thread will run on
     * @param counter  - the client number of this thread
     */
    public ServerClientThread(Server server, Socket inSocket, int counter) {
        this.socket = inSocket;
        this.clientNo = counter;
        this.server = server;
    }

    public void run() {
        try {
            this.server.serverOutput("Client put on new thread\n--------------------------");
            this.messageBuffer = new ArrayList<Message[]>();
            // 1.Create DataInputStream and DataOutputStream objects
            this.dis = new DataInputStream(this.socket.getInputStream());
            this.bis = new BufferedInputStream(dis);
            this.dos = new DataOutputStream(this.socket.getOutputStream());
            byte[] bytes = new byte[4];
            boolean readingData = false;

            // read input
            String line;
            for (int length; (length = bis.read(bytes)) != -1;) {
                int dataLength = 0;
                if (readingData == false) {
                    dataLength = ByteBuffer.wrap(bytes).getInt();
                    readingData = true;
                    bytes = new byte[dataLength];
                } else {
                    line = new String(bytes);
                    if (this.validated == false) {
                        // client not valid yet
                        if (line.equals(this.pass)) {
                            this.validated = true;
                            this.sendMessage(this.getClass(),
                                    new Message("time for a bit of updating, hold tight", this.server.getFont()),
                                    false);
                            this.updatingClient = true;
                        } else {
                            this.server.clientExit(this, "connection refused");
                            break;
                        }
                        // removes the \r\n left in the buffer
                        bytes = new byte[2];
                        bis.read(bytes);
                    } else {
                        if (line.startsWith("LOGREQUEST")) {
                            if (this.clientReady == false) {
                                this.clientReady = true;
                            }
                            this.server.serverOutput("LOGREQUEST BY CLIENT " + this.clientNo);
                            String[] params = line.substring(10).split(",");
                            this.server.logRequest(this, Integer.valueOf(params[0]), Integer.valueOf(params[1]));
                            // removes the \r\n left in the buffer
                            bytes = new byte[2];
                            bis.read(bytes);
                        } else if (line.startsWith("!")) {
                            switch (line.substring(1, line.indexOf(":"))) {
                                case "connectedClients":
                                    sendMessage(this.getClass(),
                                            new Message("numOfConnectedClients: " + this.server.getConnectedClients(),
                                                    this.server.getFont()),
                                            true);
                                    break;
                                case "UPDATE":
                                    this.updatingClient = true;
                                    this.server.serverOutput("update request by client " + this.clientNo);
                                    String[] params = line.substring(8, line.length()).split(",");
                                    this.server.serverOutput(
                                            "client " + this.clientNo + " key/version: " + params[0] + "/" + params[1]);
                                    File f = this.server.getUpdate(Double.valueOf(params[1]));
                                    if (f == null) {
                                        Message updateMessage = new Message(String.valueOf(0),
                                                this.server.getFont());
                                        updateMessage.setMessageCode(Integer.valueOf(params[0]));
                                        this.server.serverOutput("client " + this.clientNo + " already up to date");
                                        this.sendMessage(this.getClass(), updateMessage, true);
                                        // this.sendMessage(this.getClass(), new Message(params[0],
                                        // this.server.getFont()),
                                        // true);
                                    } else {
                                        BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));
                                        this.server.serverOutput("updating client " + this.clientNo);
                                        bytes = new byte[1024];
                                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                                        for (int l; (l = is.read(bytes)) != -1;) {
                                            os.write(bytes, 0, l);
                                        }
                                        is.close();

                                        Message updateMessage = new Message(String.valueOf(os.toByteArray().length),
                                                this.server.getFont());
                                        updateMessage.setMessageCode(Integer.valueOf(params[0]));
                                        // updateMessage.addAttachment(Message.AttachmentType.EXE, os.toByteArray());
                                        this.server.serverOutput("update size: " + os.size());
                                        this.sendMessage(this.getClass(), updateMessage, false);
                                        this.sendBytes(this.getClass(), os.toByteArray(), true);
                                    }
                                    this.updatingClient = false;
                                    // flush the message buffer that needs sending to the client
                                    this.sendMessage(this.getClass(), new Message("", this.server.getFont()), false);
                                    break;
                            }

                            // removes the \r\n left in the buffer
                            bytes = new byte[2];
                            bis.read(bytes);
                        } else {
                            ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
                            ObjectInputStream oi = new ObjectInputStream(bi);
                            Message message = (Message) oi.readObject();
                            bi.close();
                            oi.close();
                            if (this.collectedUsername == false) {
                                this.username = message.getSender();
                                this.collectedUsername = true;
                            }
                            server.writeMessage(message);
                        }
                    }
                    readingData = false;
                    bytes = new byte[4];
                }
            }

        } catch (

        Exception ex) {
            System.out.println(ex);
        } finally {
            if (this.validated == true) {
                this.server.clientExit(this, "client exited");
            }
        }
    }

    /**
     * function to get the number of the client on this thread
     * 
     * @return int - the number of the client
     */
    protected int getClientNumber() {
        return this.clientNo;
    }

    /**
     * function to send a message on the thread to the client
     * 
     * @param sender - the sender of the message
     * @param msg    - the message to send
     * @param log    - whether or not to log the message is the server log
     */
    protected void sendMessage(Class sender, Message msg, boolean log) {
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if ((this.updatingClient == true) || (this.clientReady == false)) {
            if (sender != this.getClass()) {
                Message[] tmp = { msg, new Message(String.valueOf(log), this.server.getFont()) };
                this.messageBuffer.add(tmp);
                return;
            }
        } else {
            for (Message[] buffer : this.messageBuffer) {
                if (Boolean.valueOf(buffer[1].getMessage()) == true) {
                    this.server.serverOutput("sending to client" + this.clientNo);
                }
                try {
                    oos.writeObject(buffer[0]);
                    oos.flush();
                    byte[] bytes = bos.toByteArray();
                    this.dos.writeInt(bytes.length);
                    this.dos.write(bytes);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        if (log == true) {
            this.server.serverOutput("\n--------------------------\nsending to client" + this.clientNo);
        }
        if (!msg.getMessage().equals("")) {
            try {
                oos.writeObject(msg);
                oos.flush();
                byte[] bytes = bos.toByteArray();
                if (log == true) {
                    this.server.serverOutput("sending " + bytes.length + " bytes\n--------------------------");
                }
                this.dos.writeInt(bytes.length);
                this.dos.write(bytes);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * function to send bytes on the thread to the client
     * 
     * @param sender - the sender of the bytes
     * @param data   - the byte array to send
     * @param log    - whether or not this should be output in the server log
     */
    protected void sendBytes(Class sender, byte[] data, boolean log) {
        try {
            this.dos.write(data);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * function to get the username of the client on this thread
     * 
     * @return String - the username of the client on this thread returns "unknown"
     *         if the username has not been collected yet
     */
    protected String getUsername() {
        if (this.collectedUsername) {
            return this.username;
        } else {
            return "unknown";
        }
    }

}