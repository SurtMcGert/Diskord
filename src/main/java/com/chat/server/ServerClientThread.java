package com.chat.server;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public ServerClientThread(Server server, Socket inSocket, int counter) {
        this.socket = inSocket;
        this.clientNo = counter;
        this.server = server;
    }

    public void run() {
        try {
            this.server.serverOutput("Client put on new thread\n--------------------------");
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
                        // send the initial 50 messages in the chat log
                        this.server.logRequest(this, 0, 50);
                    } else {
                        this.server.clientExit(this, "connection refused");
                        break;
                    }

                } else {
                    if (line.startsWith("LOGREQUEST")) {
                        this.server.serverOutput("LOGREQUEST BY CLIENT " + this.clientNo);
                        String[] params = line.substring(10).split(",");
                        this.server.logRequest(this, Integer.valueOf(params[0]), Integer.valueOf(params[1]));
                    } else if (line.startsWith("!")) {
                        switch (line.substring(1, line.length())) {
                            case "connectedClients":
                                sendMessage("numOfConnectedClients: " + this.server.getNumOfClients(), true);
                                break;
                        }
                    } else {
                        server.writeMessage(line);
                    }
                }

            }

        } catch (Exception ex) {
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

    protected void sendMessage(String msg, boolean log) {
        if (log == true) {
            this.server.serverOutput("sending to client" + this.clientNo);
        }
        pw.println(msg);
    }

}