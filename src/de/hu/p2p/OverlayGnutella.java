package de.hu.p2p;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import javax.swing.Timer;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class OverlayGnutella extends PeerManager implements Runnable{
    // start peer manager das
    // start peer manager
    // start servent
    //  servent accepts socket, addds it to unprocessed sockets overlay passes it to peer manager
    // add to peer manager
    private HashMap<String,JsonObject> seenMessages = new HashMap<>();
    private Set<String> knownPeers = new HashSet<>();
    protected final ArrayBlockingQueue<String> chatQueue = new ArrayBlockingQueue<String>(10);

    public OverlayGnutella(){
        super();
        Thread t1 = new Thread(super::run,"PM");
        Thread t2 = new Thread(this,"og");
        t1.start();
        t2.start();
    }

    public ArrayBlockingQueue<String> getChatQueue(){return chatQueue;}
    public void run(){
        Timer timer = new Timer(30000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // Code to be executed
                if(knownPeers.size()< 7){
                    send_all(createPing(getMyID()));
                }
            }
        });
        timer.setRepeats(true); // Only execute once
        timer.start(); // Go go go!
        try{
            while(true){
              //  while(!messageQueue.isEmpty()){
                    JsonObject message = messageQueue.take();
                    System.out.println("Received Message: "+ message.toString());
                    // da ich noch nichts mit der pipe mache
                    if (message.containsKey("username")) {
                        processChat(message);
                    }
                    else if(message.getString("messageType").equals("handshake")){
                        processHandshake(message);
                    }
                    else if(message.getString("messageType").equals("ping")){
                        processPing(message);
                    }
                    else if(message.getString("messageType").equals("pong")){
                        processPong(message);
                    }
         //       }
        //        Thread.sleep(1);
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public String createPing(String senderID){
        StringWriter sw = new StringWriter();
        Json.createWriter(sw).writeObject(Json.createObjectBuilder()
                .add("messageType", "ping")
                .add("messageID", UUID.randomUUID().toString())
                .add("peerID", senderID)
                .add("transmitterID",getMyID())
                .add("ttl", 3)
                .add("hopCount", 0)
                .build());
        return sw.toString();
    }

    private String updatePing(JsonObject ping){
        StringWriter sw = new StringWriter();
        Json.createWriter(sw).writeObject(Json.createObjectBuilder()
                .add("messageType", "ping")
                .add("messageID",  ping.getString("messageID"))
                .add("peerID", ping.getString("peerID"))
                .add("transmitterID",getMyID())
                .add("ttl", ping.getInt("ttl") - 1)
                .add("hopCount", ping.getInt("hopCount") + 1)
                .build());
        return sw.toString();
    }
    
    public String createPong(String pingID, int neuettl, String pingerID){
        StringWriter sw = new StringWriter();
        Json.createWriter(sw).writeObject(Json.createObjectBuilder()
                .add("messageType", "pong")
                .add("messageID", pingID)
                .add("peerID", getMyID())
                .add("pingerID", pingerID)
                .add("publicIP", getMyIP())
                .add("publicPort", getMyPort())
                .add("ttl", neuettl)
                .add("hopCount", 0)
                .build());
        return sw.toString();
    }
    private String updatePong(JsonObject pong){
        StringWriter sw = new StringWriter();
        Json.createWriter(sw).writeObject(Json.createObjectBuilder()
                .add("messageType", "pong")
                .add("messageID",  pong.getString("messageID"))
                .add("peerID", pong.getString("peerID"))
                .add("publicIP", pong.getString("publicIP"))
                .add("publicPort", pong.getInt("publicPort"))
                .add("pingerID", pong.getString("pingerID"))
                .add("ttl", pong.getInt("ttl") - 1)
                .add("hopCount", pong.getInt("hopCount") + 1)
                .build());
        return sw.toString();
    }


    private void processPing(JsonObject message){
        System.out.println("New ping from " + message.getString("peerID") + " TTL: " + message.getInt("ttl") + " HC: " + message.getInt("hopCount"));
        seenMessages.put(message.getString("messageID"), message);
        if(message.getInt("ttl") > 1 && !knownPeers.contains(message.getString("peerID"))) {
            //hopcount wird incrementiert wenn es in updatePing ist, wir haben hier kein update gemacht, deswegen hop+1
            send_back(message.getString("transmitterID"), createPong(message.getString("messageID"), message.getInt("hopCount") + 2, message.getString("peerID")));
            send_neighbours(message.getString("transmitterID"), updatePing(message));
        }else if(message.getInt("ttl") > 1 && knownPeers.contains(message.getString("peerID"))){
            send_neighbours(message.getString("transmitterID"), updatePing(message));
        } else if(message.getInt("ttl")==0){
            System.out.println("ttl = 0");
        }else{ // wir sind bei ttl=1 niemand mehr nach uns
            send_back(message.getString("transmitterID"), createPong(message.getString("messageID"), message.getInt("hopCount"),message.getString("peerID")));
        }
    }


    private void processPong(JsonObject message){
        if(seenMessages.containsKey(message.getString("messageID"))){
            JsonObject prevPing = seenMessages.get(message.getString("messageID"));
            if(message.getInt("ttl") > 1) {
                send_back(prevPing.getString("transmitterID"), updatePong(message));
            }
        }
        if(message.getString("pingerID").equals(getMyID())) {
            if(!knownPeers.contains(message.getString("peerID"))){//  && knownPeers.size() <= 3
                connect_to_peer(message.getString("publicIP"),message.getInt("publicPort"));
            }
        }
    }

    private void processHandshake(JsonObject message){
        knownPeers.add(message.getString("peerID"));
        System.out.println(knownPeers.size());
        if(knownPeers.size()==1) {
            //send_all(createPing(getMyID()));
        }
    }

    private void processChat(JsonObject message){
        if(!seenMessages.containsKey(message.getString("messageID")) && !message.getString("username").equals(getMyID())){
        System.out.println("overlay [" + message.getString("username") + "]: " + message.getString("message"));
        chatQueue.add("[" + message.getString("username") + "]: " + message.getString("message"));
        seenMessages.put(message.getString("messageID"),message);
            send_all(message.toString());
        }
    }
}
