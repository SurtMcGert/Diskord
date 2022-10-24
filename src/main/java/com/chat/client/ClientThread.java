package com.chat.client;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.*;

public class ClientThread extends Thread {
    private Socket socket;
    private int clientNo;
    private Client client;

    private DataInputStream dis;
    private String logRequest = "";

    private boolean isUpdating = false;
    private int updateKey = 0;

    public ClientThread(Client client, Socket inSocket) {
        this.socket = inSocket;
        this.client = client;
    }

    public void run() {
        try {
            System.out.println("client thread created");
            // 1.Create DataInputStream and DataOutputStream objects
            this.dis = new DataInputStream(this.socket.getInputStream());

            Thread.sleep(1000);
            if (this.socket.isClosed()) {
                this.client.connectionStatus = Client.connectionStatuses.error;
            } else {
                this.client.connectionStatus = Client.connectionStatuses.connected;
            }

            byte[] bytes = new byte[1];
            BufferedInputStream bis = new BufferedInputStream(dis);
            String line = "";
            boolean updateStarted = false;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            File f = new File("Diskord1.exe");
            OutputStream outputStream = new FileOutputStream(f);
            int size = 0;

            // read incoming data from the server
            for (int length; (length = bis.read(bytes)) != -1;) {
                if ((isUpdating) && (updateStarted)) {
                    // collect update data
                    byte[] keyBytes = Arrays.copyOfRange(bytes, length - 6, length - 2);
                    String key = new String(keyBytes);
                    try {
                        if (Integer.valueOf(key) == this.updateKey) {
                            bos.write(bytes, 0, length - 6);
                            size += length - 6;
                            System.out.println("size: " + size);
                            System.out.println("key found, end of update");
                            bos.writeTo(outputStream);
                            bos.close();
                            outputStream.close();
                            bytes = new byte[1];
                            this.client.updateSuccess = true;
                            // write data to file
                            if (size > 0) {
                                // create the setup bat files
                                File setup = new File("setup1.bat");
                                FileWriter writer = new FileWriter(setup, true);

                                writer.write("@ECHO OFF");
                                writer.write("\ncd /d %~dp0");
                                writer.write("\n:while");
                                writer.write("\ndel Diskord.exe");
                                writer.write("\nif exist Diskord.exe goto while");
                                writer.write("\n:while1");
                                writer.write("\nif not exist Diskord1.exe goto while1");
                                writer.write("\nrename Diskord1.exe Diskord.exe");
                                writer.write("\n:while2");
                                writer.write("\ndel setup.bat");
                                writer.write("\nif exist setup.bat goto while2");
                                writer.write("\nstart /B Diskord.exe");
                                writer.write("\nEXIT /B");
                                writer.close();

                                setup = new File("setup.bat");
                                writer = new FileWriter(setup, true);

                                writer.write("@ECHO OFF");
                                writer.write("\ncd /d %~dp0");
                                writer.write("\n:while1");
                                writer.write("\nif not exist setup1.bat goto while1");
                                writer.write("\nstart /B setup1.bat");
                                writer.write("\nEXIT /B");
                                writer.close();

                                // execute the setup file
                                Runtime runtime = Runtime.getRuntime();
                                Thread.sleep(1000);
                                try {
                                    runtime.exec("cmd /c setup.bat");
                                    Thread.sleep(1000);
                                    // p.waitFor();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                // exit this program
                                System.exit(0);
                            } else {
                                f.delete();
                            }
                            this.isUpdating = false;
                        }
                    } catch (NumberFormatException e) {
                        bos.write(bytes, 0, length);
                        size += bos.size();
                        bos.writeTo(outputStream);
                        bos.reset();
                    }

                } else {
                    String character = new String(bytes, 0, length);
                    switch (character) {
                        case "\n":
                            if ((this.isUpdating == true) && (line.equals(String.valueOf(this.updateKey)))) {
                                // time to update
                                System.out.println("key found, start update");
                                updateStarted = true;
                                bytes = new byte[1024];
                            } else {
                                // System.out.println(line);
                                this.client.outputToConsole(line);
                            }
                            line = "";
                            break;
                        case "\r":
                            // do nothing
                            break;
                        default:
                            line += character;
                    }
                }
            }

        } catch (Exception ex) {
            System.out.println(ex);
        } finally {
            System.out.println("Client -" + clientNo + " exit!! ");
            client.serverConnectionError("");
        }
    }

    protected void isUpdating(boolean updating, int updateKey) {
        this.isUpdating = updating;
        this.updateKey = updateKey;
    }

    protected String getLastLogRequest() {
        return this.logRequest;
    }

}
