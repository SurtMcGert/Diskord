package com.chat.client;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.*;

public class ClientThread extends Thread {
    private Socket socket;
    private int clientNo;
    private Client client;

    private DataInputStream dis;
    private DataOutputStream dos;
    private BufferedReader br;
    private PrintWriter pw;
    private String logRequest = "";

    private boolean validated = false;

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

            if (this.validated == false) {
                Thread.sleep(500);
                if (this.validated == false) {
                    this.client.connectionStatus = Client.connectionStatuses.error;
                } else {
                    this.client.connectionStatus = Client.connectionStatuses.connected;
                    this.validated = true;
                }
            }

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
            client.serverConnectionError("");
        }
    }

    protected String getLastLogRequest() {
        return this.logRequest;
    }

}
