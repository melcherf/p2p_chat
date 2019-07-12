package de.hu.p2p;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.io.*;
import java.net.Socket;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

// TODO pairing, getIP and port asdas dsa
public class PeerIO extends Thread {
    // send
    // read
    // be controlled
    // read in run
    // send in send
    // be controlled in run
    private BufferedReader br;

    public Boolean isDead=false;
    private Socket socket;
    private PrintWriter pw;
    private String ID="";
    private String peerIP;
    private int peerPort;
    private ArrayBlockingQueue<JsonObject> queue;

    public PeerIO(Socket s, ArrayBlockingQueue<JsonObject> q) throws IOException {
        br = new BufferedReader(new InputStreamReader(s.getInputStream()));
        socket = s;
        pw = new PrintWriter(s.getOutputStream(), true);
        //peerID=UUID.randomUUID().toString();
        queue = q;
    }

    public void close_peer() {
        try {
            socket.close();
            System.out.println("Peer ID: " + ID + " closed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // man sollte send verwenden
    public PrintWriter getPrintWriter() {
        return pw;
    }

    public String getPeerID() {
        return ID;
    }

    public int getPeerPort(){return peerPort;}

    public Socket getSocket() {
        return socket;
    }

    // This method is called when the user enters a new message to the commandline
    public void send(String message) {
        try {
            pw.println(message);
            if(ID!=null) {
                System.out.println("Send Message to: " + ID + " " + message);
            } else {
                System.out.println("Send Message to noID " + message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        boolean run = true;
        while (run) {
            try {
 //               if (br.ready()) {
  //                  // JsonReader detects new incoming message
                    JsonObject jo = Json.createReader(br).readObject();
                    if(jo.getString("messageType","").equals("handshake")){
                        peerIP = jo.getString("publicIP", "");
                        peerPort = jo.getInt("publicPort", 0);
                        ID = jo.getString("peerID");
                        System.out.println("Received Handshake from: " + peerIP + ":" + peerPort);
                    }
                    queue.add(jo);
   //             }
    //            Thread.sleep(1);
            } catch (Exception e) {
                isDead=true;
                e.printStackTrace();
                run = false;
                interrupt();
            }
        }
    }
}
