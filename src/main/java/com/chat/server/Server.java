package com.chat.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class Server {
    private String chatLogFile = "chatLog.txt";
    private ArrayList<ServerClientThread> clients = new ArrayList<ServerClientThread>();
    private int counter = 0;
    private int numOfConnectedUsers = 0;
    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private ServerSocket server;
    private double latestClientVersion = 1.04;

    public static void main(String[] args) throws Exception {
        new Server();
    }

    public Server() {

        try {
            this.server = new ServerSocket(5678);
        } catch (Exception e) {
            this.serverOutput(e.toString());
        }
        this.serverOutput("-- Server started --");
        while (true) {
            // server accept the client connection request
            Socket serverClient;
            try {
                serverClient = this.server.accept();
                this.counter++;
                this.numOfConnectedUsers++;
                serverOutput("\n--------------------------\nClient " + counter + " started!");
                serverOutput("number of currently connected users: " + this.numOfConnectedUsers);
                // send the request to a separate thread
                ServerClientThread sct = new ServerClientThread(this, serverClient, counter);
                clients.add(sct);
                sct.start();
                Thread.sleep(500);
            } catch (Exception e) {
                this.serverOutput(e.toString());
            }

        }
    }

    protected void clientExit(ServerClientThread client, String msg) {
        this.numOfConnectedUsers--;
        this.serverOutput("\n--------------------------\nClient " + client.getClientNumber() + " exit!! ");
        if (!msg.equals("")) {
            this.serverOutput(msg);
        }
        this.serverOutput(
                "number of currently connected users: " + this.getNumOfClients() + "\n--------------------------");
        clients.remove(client);
        if (this.numOfConnectedUsers == 0) {
            this.counter = 0;
        }
    }

    protected File getUpdate(double clientV) {
        if (clientV == this.latestClientVersion) {
            return null;
        } else {
            File f = new File("Diskord.exe");
            return f;
        }
    }

    protected void writeMessage(String msg) {
        // write string to server
        File f = new File(chatLogFile);
        boolean exists = f.exists();
        try {
            FileWriter fw = new FileWriter(f, true);
            BufferedWriter bw = new BufferedWriter(fw);
            if (exists == true) {
                bw.newLine();
            }
            bw.write(msg);
            bw.close();
            distributeMessage(msg);
        } catch (IOException xe) {
        }

    }

    private void distributeMessage(String msg) {
        for (ServerClientThread client : this.clients) {
            client.sendMessage(this.getClass(), msg, true);
        }
    }

    protected void logRequest(ServerClientThread client, int offset, int buffer) {
        serverOutput("logRequest offset: " + offset + " buffer: " + buffer);
        ArrayList<String> messages = readMessages(offset, buffer);
        for (String m : messages) {
            client.sendMessage(this.getClass(), m, false);
        }
        serverOutput("log request returned");
    }

    protected int getNumOfClients() {
        return this.numOfConnectedUsers;
    }

    private ArrayList<String> readMessages(int offset, int buffer) {
        ArrayList<String> messages = new ArrayList<String>();
        File f = new File(chatLogFile);
        StringBuilder builder = new StringBuilder();
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(f, "r");
            long fileEnd = (f.length() - offset) - 1;
            String message = "";
            randomAccessFile.seek(fileEnd);
            int readLines = 0;

            for (long pointer = fileEnd; pointer >= 0; pointer--) {
                randomAccessFile.seek(pointer);
                char c;
                // read from the last one char at the time
                c = (char) randomAccessFile.read();
                // break when end of the line
                if (c == '\n') {
                    readLines++;
                    if (readLines == buffer) {
                        break;
                    }
                }
                builder.append(c);
            }

            randomAccessFile.close();
            builder.reverse();
            message = builder.toString();
            messages = new ArrayList<String>(Arrays.asList(message.split("\r\n")));

        } catch (Exception ex) {
        }

        return messages;
    }

    protected void serverOutput(String msg) {
        System.out.println(dtf.format(LocalDateTime.now()) + " - " + msg);
    }
}
