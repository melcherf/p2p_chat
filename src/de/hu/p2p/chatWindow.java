package de.hu.p2p;

import javax.json.Json;
import javax.json.JsonObject;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

// dssad dsa dsa
public class chatWindow implements Runnable {
    private JTextArea chat_display;
    private JTextField chat_input;
    private JTextArea connected_peers;
    private JButton chat_send;
    private JPanel p2pChatView;
    private JTextField firstIP;
    private JTextField firstPort;
    private JButton connectionButton;
    private JTextField username;
    private OverlayGnutella overlay;

    public chatWindow(OverlayGnutella overlay){
        this.overlay=overlay;
        chat_send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(e.getActionCommand().equals("Send")) {
                    String txt = chat_input.getText();
                    if (!txt.equals("")) {
                        // Create a message object
                        StringWriter sw = new StringWriter();
                        Json.createWriter(sw).writeObject(Json.createObjectBuilder()
                                .add("username", username.getText())
                                .add("messageID", UUID.randomUUID().toString())
                                .add("message", chat_input.getText())
                                .build());
                        // Send message to all known outgoing connections
                        overlay.send_all(sw.toString());
                        chat_display.append("[" + username.getText() + "]: " + chat_input.getText()+"\n");
                    }
                    System.out.println("Sending text: "+txt);
                }
            }
        });
        connectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ip = firstIP.getText();
                String port = firstPort.getText();
                overlay.connect_to_peer(ip,Integer.valueOf(port));
                overlay.send_all(overlay.createPing(overlay.getMyID()));
            }
        });
        Thread t1=new Thread(this::run,"myChat");
        t1.start();
    }

    public void run(){
        username.setText(overlay.getMyID());
        Boolean run = true;
        while(run){
            while(!overlay.getChatQueue().isEmpty()){
                try {
                    chat_display.append(overlay.getChatQueue().take()+"\n");
                }catch(Exception e){e.printStackTrace();run=false;}
            }
            connected_peers.setText(String.join("\n",overlay.getPeerIDs()));
            try {
                Thread.sleep(1);
            } catch(Exception e){e.printStackTrace();}

        }
    }
    public static void main(String[] args) {
        JFrame frame = new JFrame("chatWindow");
        OverlayGnutella overlay = new OverlayGnutella();
        frame.setContentPane(new chatWindow(overlay).p2pChatView);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

}
