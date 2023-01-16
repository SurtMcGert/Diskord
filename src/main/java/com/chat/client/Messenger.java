package com.chat.client;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.chat.Crypt;
import com.chat.Message;

public class Messenger {
    private static Messenger instance = null; // the instance of this messenger
    private String username = "";
    private PrintWriter writer; // the print writer
    private DataOutputStream dos; // the data output stream

    /**
     * constructor
     * 
     * @param sock - the socket this messenger will work on
     */
    private Messenger(Socket sock, String username) {
        try {
            this.writer = new PrintWriter(sock.getOutputStream(), true);
            this.dos = new DataOutputStream(sock.getOutputStream());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.username = username;
    }

    /**
     * function to get a messenger instance
     * 
     * @param sock     - the socket this messenger will work on
     * @param username - the username of this messenger
     * @return Messenger - a single instance of the Messenger class
     */
    public static Messenger getMessenger(Socket sock, String username) {
        if (instance == null) {
            instance = new Messenger(sock, username);
        }
        return instance;
    }

    /**
     * function to send a message over the socket
     * 
     * @param msg - the message to send
     */
    protected void writeMessage(Message msg) {
        msg.setSender(this.username);
        msg.encrypt();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(msg);
            oos.flush();
            byte[] bytes = bos.toByteArray();
            this.dos.writeInt(bytes.length);
            this.dos.write(bytes);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * function to send a command over the socket
     * 
     * @param cmd - the command to send
     */
    protected void writeCommand(String cmd) {
        try {
            this.dos.writeInt(cmd.getBytes().length);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.writer.println(cmd);
    }

}
