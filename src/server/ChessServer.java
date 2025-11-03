package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChessServer {
    private static final int PORT = 50000;
    private ServerSocket serverSocket;
    private List<GameSession> activeSessions;
    private boolean isRunning = false;

    public ChessServer() {
        activeSessions = new ArrayList<>();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            isRunning = true;
            System.out.println("Chess Server started on port " + PORT);

            while (isRunning) {
                try {
                    System.out.println("Waiting for players...");
                    
                    Socket player1Socket = serverSocket.accept();
                    System.out.println("Player 1 (White) connected: " + player1Socket.getInetAddress());

                    Socket player2Socket = serverSocket.accept();
                    System.out.println("Player 2 (Black) connected: " + player2Socket.getInetAddress());

                    GameSession session = new GameSession(player1Socket, player2Socket);
                    activeSessions.add(session);
                    session.start();

                    System.out.println("Game session started!");

                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Error accepting connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Cannot start server: " + e.getMessage());
        }
    }


    public void stopServer() {
        isRunning = false;
        try {
            for (GameSession session : activeSessions) {
                session.stopSession(); 
            }
            activeSessions.clear();

            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("Server stopped");
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }


    public int getActiveSessionCount() {
        return activeSessions.size();
    }


    public static void main(String[] args) {
        ChessServer server = new ChessServer();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            server.stopServer();
        }));

        server.start();
    }
}