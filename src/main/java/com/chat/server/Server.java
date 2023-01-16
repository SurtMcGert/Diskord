package com.chat.server;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import com.chat.Message;

import java.awt.Font;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class Server {
    private String chatLogFile = "chatLog.dat";
    private int savedMessageSize = 4000;
    private ArrayList<ServerClientThread> clients = new ArrayList<ServerClientThread>();
    private int counter = 0;
    private int numOfConnectedUsers = 0;
    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private Font font = new Font("Monospaced", Font.TRUETYPE_FONT, 30);
    private ServerSocket server;
    private double latestClientVersion = 1.12;

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
                "number of currently connected users: " + this.numOfConnectedUsers + "\n--------------------------");
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

    protected void writeMessage(Message msg) {
        // write string to server
        File f = new File(chatLogFile);
        boolean exists = f.exists();

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(msg);
            oos.flush();
            byte[] bytes = bos.toByteArray();
            bytes = this.addPadding(bytes);
            // this.serverOutput("size of this message: " + bytes.length);
            FileOutputStream outputStream = new FileOutputStream(f, true);
            outputStream.write(bytes);
            outputStream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        distributeMessage(msg);

        // try {
        // FileWriter fw = new FileWriter(f, true);
        // BufferedWriter bw = new BufferedWriter(fw);
        // if (exists == true) {
        // bw.newLine();
        // }
        // // bw.write(msg);
        // bw.close();

        // } catch (IOException xe) {
        // }

    }

    /**
     * function to add padding to the given array of bytes
     * 
     * @param data - the data to pad
     * @return byte[] - the new padded data array
     */
    private byte[] addPadding(byte[] data) {
        int paddingNeeded = this.savedMessageSize - data.length;
        byte padding = 0;
        byte[] paddedData = new byte[data.length + paddingNeeded];
        for (int i = 0; i < data.length; i++) {
            paddedData[i] = data[i];
        }
        for (int i = data.length; i < paddingNeeded - 4; i++) {
            paddedData[i] = padding;
        }
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(paddingNeeded - 4);
        paddedData[paddedData.length - 4] = bb.array()[0];
        paddedData[paddedData.length - 3] = bb.array()[1];
        paddedData[paddedData.length - 2] = bb.array()[2];
        paddedData[paddedData.length - 1] = bb.array()[3];
        return paddedData;
    }

    /**
     * function to remove padding from the given byte array
     * 
     * @param data - the data to remove padding from
     * @return byte[] - the new clean data array
     */
    private byte[] removePadding(byte[] data) {
        int bytesToRemove = ByteBuffer.wrap(Arrays.copyOfRange(data, data.length - 4, data.length)).getInt();
        byte[] cleanData = new byte[data.length - bytesToRemove - 4];
        for (int i = 0; i < cleanData.length; i++) {
            cleanData[i] = data[i];
        }
        return cleanData;
    }

    private void distributeMessage(Message msg) {
        for (ServerClientThread client : this.clients) {
            client.sendMessage(this.getClass(), msg, true);
        }
    }

    protected void logRequest(ServerClientThread client, int offset, int buffer) {
        serverOutput("logRequest offset: " + offset + " buffer: " + buffer);
        ArrayList<Message> messages = readMessages(offset, buffer);
        for (Message m : messages) {
            client.sendMessage(this.getClass(), m, false);
        }
        serverOutput("log request returned");
    }

    protected String getConnectedClients() {
        String users = "(";
        for (ServerClientThread c : this.clients) {
            users += c.getUsername() + ", ";
        }
        users = users.substring(0, users.length() - 2) + ")";
        return this.numOfConnectedUsers + "\n" + users;
    }

    private ArrayList<Message> readMessages(int offset, int buffer) {
        ArrayList<Message> messages = new ArrayList<Message>();
        File f = new File(chatLogFile);
        StringBuilder builder = new StringBuilder();
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(f, "r");
            long fileEnd = (f.length() - offset) - 1;
            // String message = "";
            randomAccessFile.seek(fileEnd);
            int readMessages = 0;
            int readBytes = 0;
            byte[] messageBytes = new byte[this.savedMessageSize];

            for (long pointer = fileEnd; pointer >= 0; pointer--) {
                randomAccessFile.seek(pointer);
                // char c;
                // read from the end, one byte at a time
                readBytes++;
                messageBytes[this.savedMessageSize - readBytes] = (byte) randomAccessFile.read();
                // break when end of the line
                if (readBytes == this.savedMessageSize) {
                    readBytes = 0;
                    readMessages++;
                    messageBytes = this.removePadding(messageBytes);
                    ByteArrayInputStream bi = new ByteArrayInputStream(messageBytes);
                    ObjectInputStream oi = new ObjectInputStream(bi);
                    Message message = (Message) oi.readObject();
                    bi.close();
                    oi.close();
                    messages.add(0, message);
                    messageBytes = new byte[this.savedMessageSize];
                    if (readMessages == buffer) {
                        break;
                    }
                }
                // builder.append(c);
            }

            randomAccessFile.close();
            // builder.reverse();
            // message = builder.toString();
            // messages = new ArrayList<String>(Arrays.asList(message.split("\r\n")));

        } catch (Exception ex) {
        }

        return messages;
    }

    protected void serverOutput(String msg) {
        System.out.println(dtf.format(LocalDateTime.now()) + " - " + msg);
    }

    protected Font getFont() {
        return this.font;
    }
}
