package de.hu.p2p;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Map.Entry;

// das
public class PeerManager implements Runnable {
    private HashMap<String,PeerIO> peers = new HashMap<String,PeerIO>();
    private ServerSocket serverSocket;
    private int myPort = 0;
    private String myID;
    private String myIP;
    protected final ArrayBlockingQueue<JsonObject> messageQueue = new ArrayBlockingQueue<JsonObject>(100);
    // test connections and close bad ones
    // stop peer process (close socket, peer should stop)

    public PeerManager(){
        try {
            myPort = getRandomPort();
            //myID = UUID.randomUUID().toString();
            serverSocket = new ServerSocket(myPort);
            myIP = serverSocket.getInetAddress().getLocalHost().getHostAddress();
            myID = myIP + ":" + myPort;
            System.out.println("My Port is " + myPort);
            System.out.println("My myID is " + myID);
        } catch (Exception e) {
            System.out.println("Couldn't create Server Socket: " + myPort);
        }
    }

    public String[] getPeerIDs(){
        HashSet<String> list=new HashSet<String>();
        for(PeerIO peer: peers.values()){
            if(!peer.isDead){
                list.add(peer.getPeerID());
            }
        }
        return list.toArray(new String[list.size()]);
    }
    private Integer getRandomPort() throws IOException {
            ServerSocket socket = new ServerSocket(0);
            int port = socket.getLocalPort();
            socket.close();
            return port;
    }

    public void connect_to_peer(String ip, int port) {
        try {
            if(!peers.containsKey(ip + ":" + port) && !(ip + ":" + port).equals(myID)){
                PeerIO ic = new PeerIO(new Socket(ip,port), messageQueue);
                ic.start();
                handshake(ic);
                while(ic.getPeerID().equals("")){Thread.sleep(1);}
                System.out.println("New Connection: " + ic.getSocket().getInetAddress() + " Port: " + ic.getPeerPort() + " id: " + ic.getPeerID());
                peers.put(ic.getPeerID(),ic);
            }
        } catch (Exception e) {
            System.out.println("No peer at " + ip + ":" + port + "found.");
        }
    }

    public void disconnect_from_peer(String peerID){
        peers.get(peerID).close_peer();
        peers.remove(peerID);
    }

    public void send_all(String message) {
        if(peers.size()==0){
            System.out.println("no peers");
        }
        for (PeerIO peer : peers.values()) {
            peer.send(message);
        }
    }

    public void send_back(String peerID, String message){
        System.out.println("Keys: "+peers.keySet().toString());
        if(peers.containsKey(peerID)) {
            PeerIO io = peers.get(peerID);
            System.out.println("sendback to " + peerID);
            io.send(message);
        }
    }

    public void send_neighbours(String peerID, String message) {
        for (PeerIO peer : peers.values()) {
            if(!peer.getPeerID().equals(peerID)){
                peer.send(message);
            }
        }
    }

    public void handshake(PeerIO peer){
        StringWriter sw = new StringWriter();
        Json.createWriter(sw).writeObject(Json.createObjectBuilder()
                .add("messageType", "handshake")
                .add("messageID", UUID.randomUUID().toString())
                .add("peerID", myID)
                .add("publicIP", myIP )
                .add("publicPort", getMyPort())
                .build());
        peer.send(sw.toString());
    }

    public void run() {
        try {
            while (true) {
                // Is called after another peer is trying to create connection in their joinNetwork() method
                // Via this connection we send the messages
                PeerIO ic = new PeerIO(serverSocket.accept(),messageQueue);
                ic.start();
                handshake(ic);
                while(ic.getPeerID().equals("")){Thread.sleep(1);}
                System.out.println("New Connection: " + ic.getSocket().getInetAddress() + " Port: " + ic.getPeerPort() + " id: " + ic.getPeerID());

                peers.put(ic.getPeerID(),ic);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String getMyID() {
        return myID;
    }

    public String getMyIP() {
        return myIP;
    }

    public int getMyPort() {
        return myPort;
    }
}
