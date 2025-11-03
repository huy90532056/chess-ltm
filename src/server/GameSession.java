package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class GameSession extends Thread {
    private Socket player1Socket; 
    private Socket player2Socket; 
    
    private PrintWriter player1Out;
    private PrintWriter player2Out;
    
    private Scanner player1In;
    private Scanner player2In;
    
    private boolean running = false;
    private int moveCount = 0;

    public GameSession(Socket player1Socket, Socket player2Socket) {
        this.player1Socket = player1Socket;
        this.player2Socket = player2Socket;
    }

    @Override
    public void run() {
        try {
            player1Out = new PrintWriter(player1Socket.getOutputStream(), true);
            player2Out = new PrintWriter(player2Socket.getOutputStream(), true);
            
            player1In = new Scanner(player1Socket.getInputStream());
            player2In = new Scanner(player2Socket.getInputStream());
            
            running = true;
            
            Thread player1Thread = new Thread(() -> listenToPlayer(player1In, player2Out, "Player 1 (White)"));
            Thread player2Thread = new Thread(() -> listenToPlayer(player2In, player1Out, "Player 2 (Black)"));
            
            player1Thread.start();
            player2Thread.start();
            
            player1Thread.join();
            player2Thread.join();
            
        } catch (IOException | InterruptedException e) {
            System.err.println("Game session error: " + e.getMessage());
        } finally {
            stopSession();
        }
    }

    private void listenToPlayer(Scanner in, PrintWriter opponentOut, String playerName) {
        try {
            while (running && in.hasNextLine()) {
                String move = in.nextLine();
                moveCount++;
                
                System.out.println(playerName + " move #" + moveCount + ": " + move);
                
                if (opponentOut != null && running) {
                    opponentOut.println(move);
                }
            }
        } catch (Exception e) {
            if (running) {
                System.err.println(playerName + " disconnected: " + e.getMessage());
            }
        } finally {
            System.out.println(playerName + " stopped listening");
        }
    }

    public void stopSession() {
        running = false;
        try {
            if (player1In != null) player1In.close();
            if (player2In != null) player2In.close();
            if (player1Out != null) player1Out.close();
            if (player2Out != null) player2Out.close();
            
            if (player1Socket != null && !player1Socket.isClosed()) {
                player1Socket.close();
            }
            if (player2Socket != null && !player2Socket.isClosed()) {
                player2Socket.close();
            }
            
            System.out.println("Game session stopped. Total moves: " + moveCount);
        } catch (IOException e) {
            System.err.println("Error stopping game session: " + e.getMessage());
        }
    }


    public boolean isActive() {
        return running;
    }


    public int getMoveCount() {
        return moveCount;
    }
}