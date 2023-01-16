package com.chat;

import java.awt.Font;
import java.io.Serializable;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Message implements Serializable {

    private int code;
    private String sender; // the sender of this message
    private String message; // the string this message holds

    /**
     * supported attachments
     */
    public enum AttachmentType {
        PNG,
        JPG,
        TXT,
        EXE,
    }

    private ArrayList<AttachmentType> attachmentTypes; // the types for each attachment in this message
    private ArrayList<byte[]> attachmentData; // the array of attachment data
    private Font font; // the font of this message
    private boolean encrypted = false; // is the data in this message encrypted

    /**
     * constructor
     * 
     * @param message the string this message will hold
     * @param font    the font of this message
     */
    public Message(String message, Font font) {
        this.sender = null;
        this.message = message;
        this.font = font;
    }

    public void encrypt() {
        this.encrypted = true;
        try {
            this.message = Base64.getEncoder().encodeToString(Crypt.encrypt(this.message.getBytes()));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * function to set the sender
     * 
     * @param sender - the sender of this message
     */
    public void setSender(String sender) {
        if (sender != null) {
            this.sender = sender;
        }
    }

    /**
     * function to get the sender of this message
     * 
     * @return String - the sender of the message
     */
    public String getSender() {
        if (this.sender == null) {
            return null;
        }
        return this.sender;
    }

    /**
     * function to get the string this message holds
     * 
     * @return String - the string held by this message
     */
    public String getMessage() {
        if (this.encrypted == true) {
            try {
                this.message = new String(Crypt.decrypt(Base64.getDecoder().decode(this.message)));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            this.encrypted = false;
        }
        return this.message;
    }

    /**
     * function to get the font of this message
     * 
     * @return Font - the font of this message
     */
    public Font getFont() {
        return this.font;
    }

    /**
     * function to add an attachment to this message
     * 
     * @param type - the type of this attachment
     * @param data - the data for this attachment
     */
    public void addAttachment(AttachmentType type, byte[] data) {
        this.attachmentTypes.add(type);
        this.attachmentData.add(data);
    }

    /**
     * function to get the attachment type of the attachment in the given index
     * 
     * @param i - the index of the attachment type to get
     * @return AttachmentType - the attachment type
     */
    public AttachmentType getAttachmentType(int i) {
        return this.attachmentTypes.get(i);
    }

    /**
     * function to get the attachment data of the attachment in the given index
     * 
     * @param i - the index of the attachment data to get
     * @return byte[] - the attachment data
     */
    public byte[] getAttachmentData(int i) {
        return this.attachmentData.get(i);
    }

    /**
     * function to set the message code
     * 
     * @param code - the code for this message
     */
    public void setMessageCode(int code) {
        this.code = code;
    }

    /**
     * function to get the message code
     * 
     * @return int - the code for this message
     */
    public int getMessageCode() {
        return this.code;
    }
}
