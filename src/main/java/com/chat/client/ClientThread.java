package com.chat.client;

import java.net.*;
import java.nio.ByteBuffer;
import com.chat.Message;

import java.io.*;

public class ClientThread extends Thread {
    private Socket socket; // the socket this client will run on
    private int clientNo; // the number of this client
    private Client client; // the client that this thread is attached too

    private DataInputStream dis; // data input stream from the socket
    private String logRequest = ""; // the message log request from the server

    private boolean isUpdating = false; // is the client updating
    private int updateKey = 0; // the key for the current update
    private int updateSize = 0; // the size of the current update

    /**
     * constructor
     * 
     * @param client   - the client that this thread is attached too
     * @param inSocket - the socket that this thread will run on
     */
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

            byte[] bytes = new byte[4];
            BufferedInputStream bis = new BufferedInputStream(dis);
            boolean updateStarted = false;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            File f = new File("Diskord1.exe");
            OutputStream outputStream = new FileOutputStream(f);
            int size = 0;
            boolean readingData = false;

            // read incoming data from the server
            for (int length; (length = bis.read(bytes)) != -1;) {

                int dataLength = 0;
                if (readingData == false) {
                    dataLength = ByteBuffer.wrap(bytes).getInt();
                    readingData = true;
                    bytes = new byte[dataLength];

                } else {

                    if ((isUpdating) && (updateStarted)) {
                        bos.write(bytes, 0, length);
                        size += bos.size();
                        bos.writeTo(outputStream);
                        bos.reset();
                        if (size == this.updateSize) {
                            System.out.println("size: " + size);
                            System.out.println("key found, end of update");
                            bos.writeTo(outputStream);
                            bos.close();
                            outputStream.close();
                            bytes = new byte[4];
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
                            readingData = false;
                        }
                    } else {
                        ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
                        ObjectInputStream oi = new ObjectInputStream(bi);
                        Message message = (Message) oi.readObject();
                        bi.close();
                        oi.close();

                        if (isUpdating) {
                            if (message.getMessageCode() == this.updateKey) {
                                System.out.println("key found");
                                this.updateSize = Integer.valueOf(message.getMessage());
                                if (this.updateSize == 0) {
                                    System.out.println("no update required");
                                    this.client.updateSuccess = true;
                                    this.isUpdating = false;
                                    f.delete();
                                    readingData = false;
                                    bytes = new byte[4];
                                } else {
                                    readingData = true;
                                    updateStarted = true;
                                    bytes = new byte[1024];
                                }
                            }
                        } else {
                            // do normal shit
                            readingData = false;
                            bytes = new byte[4];
                            this.client.outputToConsole(message);
                        }
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

    /**
     * function to tell this thread if the client is updating or not
     * 
     * @param updating  - wether or not the client is updating
     * @param updateKey - the update key for the update
     */
    protected void isUpdating(boolean updating, int updateKey) {
        this.isUpdating = updating;
        this.updateKey = updateKey;
    }

    /**
     * function to get the log request
     * 
     * @return String - the log request
     */
    protected String getLastLogRequest() {
        return this.logRequest;
    }

}
