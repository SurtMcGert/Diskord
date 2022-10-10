package com.chat.server;

import java.net.*;
import java.io.*;

class ServerClientThread extends Thread {
    Socket socket;
    int clientNo;
    Server server;

    DataInputStream dis;
    DataOutputStream dos;
    BufferedReader br;
    PrintWriter pw;

    public ServerClientThread(Server server, Socket inSocket, int counter) {
        this.socket = inSocket;
        this.clientNo = counter;
        this.server = server;
    }

    public void run() {
        try {
            System.out.println("client put on new thread");
            // 1.Create DataInputStream and DataOutputStream objects
            this.dis = new DataInputStream(this.socket.getInputStream());
            this.dos = new DataOutputStream(this.socket.getOutputStream());
            this.br = new BufferedReader(new InputStreamReader(dis));
            this.pw = new PrintWriter(dos, true);

            // send the initial 50 messages in the chat log
            this.server.logRequest(this, 0, 50);

            // read input
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("LOGREQUEST")) {
                    System.out.println("LOGREQUEST BY CLIENT " + this.clientNo);
                    String[] params = line.substring(10).split(",");
                    this.server.logRequest(this, Integer.valueOf(params[0]), Integer.valueOf(params[1]));
                } else if (line.startsWith("!")) {
                    switch (line.substring(1, line.length())) {
                        case "connectedClients":
                            sendMessage("numOfConnectedClients: " + this.server.getNumOfClients());
                            break;
                    }
                } else {
                    server.writeMessage(line);
                }
            }

        } catch (Exception ex) {
            System.out.println(ex);
        } finally {
            System.out.println("Client -" + clientNo + " exit!! ");
            this.server.clientExit(this);
        }
    }

    public void sendMessage(String msg) {
        System.out.println("sending to client" + this.clientNo + ": " + msg);
        pw.println(msg);
    }

}