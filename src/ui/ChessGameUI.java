package ui;

import core.*;
import client.ChessClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.Executors;


// logic UI 
public class ChessGameUI implements ChessDelegate, ActionListener {
    private static final int PORT = 50000;
    
    private ChessModel chessModel;
    private ChessClient chessClient;

    private JFrame frame;
    private ChessView chessBoardPanel;
    private JButton resetBtn;
    private JButton serverBtn;
    private JButton clientBtn;
    private JLabel statusLabel;

    private boolean isFlipped = false; 
    private boolean gameOver = false;

    public ChessGameUI() {
        chessModel = new ChessModel();
        chessModel.reset();

        frame = new JFrame("Chess Game");
        frame.setSize(8 * 64 + 20, 8 * 64 + 100);
        frame.setLocation(200, 130);
        frame.setLayout(new BorderLayout());

        chessBoardPanel = new ChessView(this, isFlipped);
        frame.add(chessBoardPanel, BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        frame.add(statusLabel, BorderLayout.NORTH);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        
        resetBtn = new JButton("Reset");
        resetBtn.addActionListener(this);
        buttonsPanel.add(resetBtn);

        serverBtn = new JButton("Host Game");
        serverBtn.addActionListener(this);
        buttonsPanel.add(serverBtn);

        clientBtn = new JButton("Join Game");
        clientBtn.addActionListener(this);
        buttonsPanel.add(clientBtn);

        frame.add(buttonsPanel, BorderLayout.PAGE_END);

        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (chessClient != null) {
                    chessClient.disconnect();
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChessGameUI());
    }

    @Override
    public ChessModel getChessModel() {
        return chessModel;
    }

    @Override
    public ChessPiece pieceAt(int col, int row) {
        return chessModel.pieceAt(col, row);
    }

    @Override
    public void movePiece(int fromCol, int fromRow, int toCol, int toRow) {
        movePiece(fromCol, fromRow, toCol, toRow, null);
    }

    public void movePiece(int fromCol, int fromRow, int toCol, int toRow, Rank promotionRank) {
        if (gameOver) return;

        ChessPiece movingPiece = chessModel.pieceAt(fromCol, fromRow);
        if (movingPiece == null || movingPiece.getPlayer() != chessModel.getPlayerInTurn()) {
            return;
        }

        boolean isPromotion = movingPiece.getRank() == Rank.PAWN &&
            ((movingPiece.getPlayer() == Player.WHITE && toRow == 7) ||
             (movingPiece.getPlayer() == Player.BLACK && toRow == 0));

        if (isPromotion && promotionRank == null) {
            String[] choices = {"Hậu", "Xe", "Tượng", "Mã"};
            String choice = (String) JOptionPane.showInputDialog(
                frame, "Chọn quân để phong cấp:", "Promotion", 
                JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]
            );
            
            Rank rank = Rank.QUEEN;
            if ("Xe".equals(choice)) rank = Rank.ROOK;
            else if ("Tượng".equals(choice)) rank = Rank.BISHOP;
            else if ("Mã".equals(choice)) rank = Rank.KNIGHT;
            
            movePiece(fromCol, fromRow, toCol, toRow, rank);
            return;
        }

        ChessModel.MoveResult result = chessModel.movePiece(fromCol, fromRow, toCol, toRow, promotionRank);
        chessBoardPanel.repaint();

        if (chessClient != null && chessClient.isConnected() && (
                result == ChessModel.MoveResult.SUCCESS ||
                result == ChessModel.MoveResult.PROMOTION ||
                result == ChessModel.MoveResult.CHECKMATE ||
                result == ChessModel.MoveResult.STALEMATE ||
                result == ChessModel.MoveResult.DRAW_50_MOVES ||
                result == ChessModel.MoveResult.DRAW_INSUFFICIENT_MATERIAL ||
                result == ChessModel.MoveResult.DRAW_THREEFOLD_REPETITION
        )) {
            String msg = fromCol + "," + fromRow + "," + toCol + "," + toRow;
            if (promotionRank != null) {
                msg += "," + promotionRank.name();
            }
            chessClient.sendMove(msg);
        }

        updateStatus(result);
    }

    public void receiveMove(String moveStr) {
        String[] parts = moveStr.split(",");
        int fromCol = Integer.parseInt(parts[0]);
        int fromRow = Integer.parseInt(parts[1]);
        int toCol = Integer.parseInt(parts[2]);
        int toRow = Integer.parseInt(parts[3]);
        Rank promotionRank = parts.length > 4 ? Rank.valueOf(parts[4]) : null;

        SwingUtilities.invokeLater(() -> {
            ChessModel.MoveResult result = chessModel.movePiece(fromCol, fromRow, toCol, toRow, promotionRank);
            chessBoardPanel.repaint();
            updateStatus(result);
        });
    }

    private void updateStatus(ChessModel.MoveResult result) {
        Player checked = chessModel.getPlayerInTurn() == Player.WHITE ? Player.BLACK : Player.WHITE;
        if (chessModel.isKingChecked(checked)) {
            statusLabel.setText("Check! " + (checked == Player.WHITE ? "White" : "Black") + " king is in check!");
        } else {
            statusLabel.setText(" ");
        }

        switch (result) {
            case INVALID_MOVE:
                JOptionPane.showMessageDialog(frame, "Nước đi không hợp lệ!");
                break;
            case STILL_IN_CHECK:
                JOptionPane.showMessageDialog(frame, "Bạn đang bị chiếu!");
                break;
            case CHECKMATE:
                JOptionPane.showMessageDialog(frame, 
                    chessModel.getPlayerInTurn() == Player.WHITE ? "Chiếu hết!" : "Chiếu hết!");
                gameOver = true;
                break;
            case STALEMATE:
                JOptionPane.showMessageDialog(frame, "Hòa do hết nước đi (Stalemate)!");
                gameOver = true;
                break;
            case DRAW_50_MOVES:
                JOptionPane.showMessageDialog(frame, "Hòa do luật 50 nước!");
                gameOver = true;
                break;
            case DRAW_INSUFFICIENT_MATERIAL:
                JOptionPane.showMessageDialog(frame, "Hòa do không đủ quân chiếu hết!");
                gameOver = true;
                break;
            case DRAW_THREEFOLD_REPETITION:
                JOptionPane.showMessageDialog(frame, "Hòa do lặp lại thế cờ 3 lần!");
                gameOver = true;
                break;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == resetBtn) {
            Executors.newSingleThreadExecutor().execute(() -> {
                if (chessClient != null) {
                    chessClient.disconnect();
                    chessClient = null;
                }
            });

            chessModel.reset();
            isFlipped = false; 
            frame.remove(chessBoardPanel);
            chessBoardPanel = new ChessView(this, isFlipped);
            frame.add(chessBoardPanel, BorderLayout.CENTER);
            frame.revalidate();
            frame.repaint();
            statusLabel.setText(" ");
            gameOver = false;

            serverBtn.setEnabled(true);
            clientBtn.setEnabled(true);
            resetBtn.setEnabled(true);
            frame.setTitle("Chess Game");
        } else if (e.getSource() == serverBtn) {
            isFlipped = false; 
            frame.remove(chessBoardPanel);
            chessBoardPanel = new ChessView(this, isFlipped);
            frame.add(chessBoardPanel, BorderLayout.CENTER);
            frame.revalidate();
            frame.repaint();

            serverBtn.setEnabled(false);
            clientBtn.setEnabled(false);
            resetBtn.setEnabled(true);
            frame.setTitle("Chess Server (White)");

            chessClient = new ChessClient("localhost", PORT);
            chessClient.setOnMoveReceived(this::receiveMove);
            if (chessClient.connect()) {
                JOptionPane.showMessageDialog(frame, "Bạn là quân trắng! Đợi đối thủ kết nối.");
            } else {
                JOptionPane.showMessageDialog(frame, "Không thể kết nối server!");
                serverBtn.setEnabled(true);
                clientBtn.setEnabled(true);
            }
        } else if (e.getSource() == clientBtn) {
            isFlipped = true; 
            frame.remove(chessBoardPanel);
            chessBoardPanel = new ChessView(this, isFlipped);
            frame.add(chessBoardPanel, BorderLayout.CENTER);
            frame.revalidate();
            frame.repaint();

            serverBtn.setEnabled(false);
            clientBtn.setEnabled(false);
            resetBtn.setEnabled(true);
            frame.setTitle("Chess Client (Black)");

            String serverIP = JOptionPane.showInputDialog(frame, "Enter server IP:", "localhost");
            if (serverIP != null && !serverIP.trim().isEmpty()) {
                chessClient = new ChessClient(serverIP, PORT);
                chessClient.setOnMoveReceived(this::receiveMove);

                if (chessClient.connect()) {
                    JOptionPane.showMessageDialog(frame, "Connected to server!");
                } else {
                    JOptionPane.showMessageDialog(frame, "Cannot connect to server!");
                    serverBtn.setEnabled(true);
                    clientBtn.setEnabled(true);
                }
            }
        }
    }
}