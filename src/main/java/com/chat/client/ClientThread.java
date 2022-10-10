package com.chat.client;

import java.net.*;
import java.io.*;

public class ClientThread extends Thread {
    Socket socket;
    int clientNo;
    Client client;

    DataInputStream dis;
    DataOutputStream dos;
    BufferedReader br;
    PrintWriter pw;
    String logRequest = "";

    public ClientThread(Client client, Socket inSocket) {
        this.socket = inSocket;
        this.client = client;
    }

    public void run() {
        try {
            System.out.println("client thread created");
            // 1.Create DataInputStream and DataOutputStream objects
            this.dis = new DataInputStream(this.socket.getInputStream());
            this.dos = new DataOutputStream(this.socket.getOutputStream());
            this.br = new BufferedReader(new InputStreamReader(dis));
            this.pw = new PrintWriter(dos, true);

            // read input
            String line;
            while ((line = br.readLine()) != null) {
                // display message for client
                this.client.outputToConsole(line);
            }

        } catch (Exception ex) {
            System.out.println(ex);
        } finally {
            System.out.println("Client -" + clientNo + " exit!! ");
        }
    }

    protected String getLastLogRequest() {
        return this.logRequest;
    }

}
