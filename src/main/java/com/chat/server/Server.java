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

public class Server {
    private String chatLogFile = "chatLog.txt";
    private ArrayList<ServerClientThread> clients = new ArrayList<ServerClientThread>();
    private int counter = 0;

    public static void main(String[] args) throws Exception {
        new Server();
    }

    public Server() {
        try {
            ServerSocket server = new ServerSocket(5678);
            System.out.println("-- Server started --");
            while (true) {
                counter++;
                // server accept the client connection request
                Socket serverClient = server.accept();
                System.out.println("Client " + counter + " started!");
                // send the request to a separate thread
                ServerClientThread sct = new ServerClientThread(this, serverClient, counter);
                clients.add(sct);
                sct.start();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    protected void clientExit(ServerClientThread client) {
        clients.remove(client);
        this.counter--;
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
            client.sendMessage(msg);
        }
    }

    protected void logRequest(ServerClientThread client, int offset, int buffer) {
        System.out.println("logRequest offset: " + offset + " buffer: " + buffer);
        ArrayList<String> messages = readMessages(offset, buffer);
        for (String m : messages) {
            client.sendMessage(m);
        }
    }

    protected int getNumOfClients() {
        return this.counter - 1;
    }

    private ArrayList readMessages(int offset, int buffer) {
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

        // String output = "";
        // for (String m : messages) {
        // output += m + "\n";
        // }
        // output = output.substring(0, output.length() - 1);

        return messages;
    }
}
