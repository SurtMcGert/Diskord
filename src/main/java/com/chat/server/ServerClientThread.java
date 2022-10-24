package com.chat.server;

import java.net.*;
import java.util.ArrayList;
import java.io.*;

class ServerClientThread extends Thread {
    private Socket socket;
    private int clientNo;
    private Server server;

    private DataInputStream dis;
    private DataOutputStream dos;
    private BufferedReader br;
    private PrintWriter pw;

    private boolean validated = false;
    private String pass = "i;<tc2%Otv(\\5B,w0f\\w9,Tw|8v|uK2;Amibjxy?F`68oh8}\\Y2S|(7V=L;8fd";
    private boolean updatingClient = false;
    private ArrayList<String[]> messageBuffer;
    private boolean clientReady = false;
    private boolean collectedUsername = false;
    private String username = "";

    public ServerClientThread(Server server, Socket inSocket, int counter) {
        this.socket = inSocket;
        this.clientNo = counter;
        this.server = server;
    }

    public void run() {
        try {
            this.server.serverOutput("Client put on new thread\n--------------------------");
            this.messageBuffer = new ArrayList<String[]>();
            // 1.Create DataInputStream and DataOutputStream objects
            this.dis = new DataInputStream(this.socket.getInputStream());
            this.dos = new DataOutputStream(this.socket.getOutputStream());
            this.br = new BufferedReader(new InputStreamReader(dis));
            this.pw = new PrintWriter(dos, true);

            // read input
            String line;
            while ((line = br.readLine()) != null) {
                if (this.validated == false) {
                    // client not valid yet
                    if (line.equals(this.pass)) {
                        this.validated = true;
                        this.sendMessage(this.getClass(), "time for a bit of updating, hold tight", false);
                        this.updatingClient = true;
                    } else {
                        this.server.clientExit(this, "connection refused");
                        break;
                    }

                } else {
                    if (line.startsWith("LOGREQUEST")) {
                        if (this.clientReady == false) {
                            this.clientReady = true;
                        }
                        this.server.serverOutput("LOGREQUEST BY CLIENT " + this.clientNo);
                        String[] params = line.substring(10).split(",");
                        this.server.logRequest(this, Integer.valueOf(params[0]), Integer.valueOf(params[1]));
                    } else if (line.startsWith("!")) {
                        switch (line.substring(1, line.indexOf(":"))) {
                            case "connectedClients":
                                sendMessage(this.getClass(),
                                        "numOfConnectedClients: " + this.server.getConnectedClients(),
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
                                    this.server.serverOutput("client " + this.clientNo + " already up to date");
                                    this.sendMessage(this.getClass(), params[0], true);
                                    this.sendMessage(this.getClass(), params[0], true);
                                } else {
                                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
                                    this.server.serverOutput("updating client " + this.clientNo);
                                    byte[] bytes = new byte[1024];
                                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                    for (int length; (length = bis.read(bytes)) != -1;) {
                                        bos.write(bytes, 0, length);
                                    }
                                    bis.close();

                                    this.server.serverOutput("update size: " + bos.size());
                                    this.sendMessage(this.getClass(), params[0], false);
                                    this.sendBytes(this.getClass(), bos.toByteArray(), true);
                                    this.sendMessage(this.getClass(), params[0], false);
                                }
                                this.updatingClient = false;
                                // flush the message buffer that needs sending to the client
                                this.sendMessage(this.getClass(), "", false);
                                break;
                        }
                    } else {
                        int pos = line.indexOf(":");
                        if (!line.substring(0, pos).equals(this.username)) {
                            collectedUsername = false;
                        }
                        if (collectedUsername == false) {
                            try {
                                this.username = line.substring(0, pos);
                                this.collectedUsername = true;
                            } catch (Exception e) {
                                // its fine, ignore it, everything will be okay
                            }
                        }
                        server.writeMessage(line);
                    }
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

    protected int getClientNumber() {
        return this.clientNo;
    }

    protected void sendMessage(Class sender, String msg, boolean log) {
        if ((this.updatingClient == true) || (this.clientReady == false)) {
            if (sender != this.getClass()) {
                String[] tmp = { msg, String.valueOf(log) };
                this.messageBuffer.add(tmp);
                return;
            }
        } else {
            for (String[] buffer : this.messageBuffer) {
                if (Boolean.valueOf(buffer[1]) == true) {
                    this.server.serverOutput("sending to client" + this.clientNo);
                }
                pw.println(buffer[0]);
            }
        }
        if (log == true) {
            this.server.serverOutput("sending to client" + this.clientNo);
        }
        if (!msg.equals("")) {
            pw.println(msg);
        }
    }

    protected void sendBytes(Class sender, byte[] data, boolean log) {
        try {
            this.dos.write(data);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected String getUsername() {
        if (this.collectedUsername) {
            return this.username;
        } else {
            return "unknown";
        }
    }

}