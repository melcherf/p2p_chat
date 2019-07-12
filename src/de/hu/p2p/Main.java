package de.hu.p2p;

import javax.json.Json;
import java.io.*;

public class Main {
    private OverlayGnutella og;
    // start overlay
    // send connection to overlay
    // read display pipes from peers
    // send message
    // read pipe, send commands to overlay?
    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter username and known peer (username localhost:23844)");
        String[] input = br.readLine().split(" ");
        new Main().joinOverlayNetwork(br, input[0], input[1]);
    }

    // Called to join the network with a defined username und servent with choosen port
    public void joinOverlayNetwork(BufferedReader br,String username, String knownPeer) throws Exception {
        og =  new OverlayGnutella();
        System.out.println("Create new overlay? (y/n)");
        String input = br.readLine();
        if(!input.equals("y")){
            try{
                    og.connect_to_peer(knownPeer.split(":")[0], Integer.valueOf(knownPeer.split(":")[1]));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        startChat(br, username, knownPeer);
    }

    public void startChat(BufferedReader br, String username, String knownPeer){
        try {
            System.out.println("Send messages (e to exit and c to setup ne clients):");
            boolean run = true;
            while(run) {
                String input = br.readLine();
                if (input.equals("e")) {
                    run = false;
                    break;
                } else if (input.equals("c")) {
                    joinOverlayNetwork(br, username, knownPeer);
                } else {
                    // Create a message object
                    StringWriter sw = new StringWriter();
                    Json.createWriter(sw).writeObject(Json.createObjectBuilder()
                            .add("username", username)
                            .add("message", input)
                            .build());
                    // Send message to all known outgoing connections
                    og.send_all(sw.toString());
                }
            }
            System.out.println("EXIT");
            System.exit(0);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
