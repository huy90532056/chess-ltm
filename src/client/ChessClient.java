package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.function.Consumer;

public class ChessClient {
    private String serverAddress;
    private int port;
    private Socket socket;
    private PrintWriter out;
    private Scanner in;
    private Consumer<String> onMoveReceived;
    private boolean isConnected = false;

    public ChessClient(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }


    public boolean connect() {
        try {
            socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new Scanner(socket.getInputStream());
            isConnected = true;
            
            new Thread(this::listenForMoves).start();
            
            System.out.println("Connected to server: " + serverAddress + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("Cannot connect to server: " + e.getMessage());
            return false;
        }
    }


    public void sendMove(String move) {
        if (isConnected && out != null) {
            out.println(move);
            System.out.println("Sent move: " + move);
        } else {
            System.err.println("Not connected to server!");
        }
    }


    private void listenForMoves() {
        try {
            while (isConnected && in.hasNextLine()) {
                String move = in.nextLine();
                System.out.println("Received move: " + move);
                
                if (onMoveReceived != null) {
                    onMoveReceived.accept(move);
                }
            }
        } catch (Exception e) {
            System.err.println("Connection lost: " + e.getMessage());
            isConnected = false;
        }
    }


    public void setOnMoveReceived(Consumer<String> callback) {
        this.onMoveReceived = callback;
    }


    public void disconnect() {
        isConnected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            System.out.println("Disconnected from server");
        } catch (IOException e) {
            System.err.println("Error while disconnecting: " + e.getMessage());
        }
    }


    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }

 String getServerAddress() {
        return serverAddress;
    }

    public int getPort() {
        return port;
    }
}