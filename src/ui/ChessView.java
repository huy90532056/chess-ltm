package ui;

import core.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ChessView extends JPanel implements MouseListener, MouseMotionListener {
    private final int cellSize = 64;
    private final int originX = 0;
    private final int originY = 0;

    private ChessDelegate chessDelegate;
    private boolean isFlipped;

    private int fromCol = -1;
    private int fromRow = -1;
    private int movingPieceX = -1;
    private int movingPieceY = -1;
    private ChessPiece movingPiece = null;

    private Map<String, Image> imageCache = new HashMap<>();

    public ChessView(ChessDelegate chessDelegate, boolean isFlipped) {
        this.chessDelegate = chessDelegate;
        this.isFlipped = isFlipped;
        setPreferredSize(new Dimension(8 * cellSize, 8 * cellSize));
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawBoard(g);
        drawPieces(g);
    }

    private void drawBoard(Graphics g) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                boolean isLight = (row + col) % 2 != 0;
                g.setColor(isLight ? new Color(240, 217, 181) : new Color(181, 136, 99));
                g.fillRect(originX + col * cellSize, originY + (7 - row) * cellSize, cellSize, cellSize);
            }
        }
    }

    private void drawPieces(Graphics g) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = chessDelegate.pieceAt(col, row);
                if (piece != null && !(fromCol == col && fromRow == row)) {
                    int displayRow = isFlipped ? 7 - row : row;
                    int displayCol = isFlipped ? 7 - col : col;
                    drawPieceAt(g, displayCol, displayRow, piece.getImgName());
                }
            }
        }

        if (fromCol != -1 && fromRow != -1 && movingPieceX != -1 && movingPieceY != -1 && movingPiece != null) {
            Image img = loadImage(movingPiece.getImgName());
            if (img != null) {
                g.drawImage(img, movingPieceX - cellSize / 2, movingPieceY - cellSize / 2, cellSize, cellSize, null);
            }
        }
    }

    private void drawPieceAt(Graphics g, int col, int row, String imgName) {
        Image img = loadImage(imgName);
        if (img != null) {
            g.drawImage(img, originX + col * cellSize, originY + (7 - row) * cellSize, cellSize, cellSize, null);
        }
    }

    private Image loadImage(String imgName) {
        if (imageCache.containsKey(imgName)) {
            return imageCache.get(imgName);
        }
        try {
            String path = "res/img/" + imgName + ".png";
            Image img = ImageIO.read(new File(path));
            imageCache.put(imgName, img);
            return img;
        } catch (IOException e) {
            System.err.println("Cannot load image: " + imgName);
            return null;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int col = (e.getX() - originX) / cellSize;
        int row = 7 - (e.getY() - originY) / cellSize;

        if (isFlipped) {
            col = 7 - col;
            row = 7 - row;
        }

        if (col >= 0 && col < 8 && row >= 0 && row < 8) {
            ChessPiece p = chessDelegate.pieceAt(col, row);
            if (p != null) {
                Player myPlayer = isFlipped ? Player.BLACK : Player.WHITE;
                ChessModel model = chessDelegate.getChessModel();
                if (p.getPlayer() == myPlayer && model.getPlayerInTurn() == myPlayer) {
                    fromCol = col;
                    fromRow = row;
                    movingPiece = p;
                    movingPieceX = e.getX();
                    movingPieceY = e.getY();
                    repaint();
                }
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        int toCol = (e.getX() - originX) / cellSize;
        int toRow = 7 - (e.getY() - originY) / cellSize;

        if (isFlipped) {
            toCol = 7 - toCol;
            toRow = 7 - toRow;
        }

        if (fromCol != -1 && fromRow != -1 && toCol >= 0 && toCol < 8 && toRow >= 0 && toRow < 8) {
            if (fromCol != toCol || fromRow != toRow) {
                chessDelegate.movePiece(fromCol, fromRow, toCol, toRow);
            }
        }

        fromCol = -1;
        fromRow = -1;
        movingPiece = null;
        movingPieceX = -1;
        movingPieceY = -1;
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (fromCol != -1 && fromRow != -1) {
            movingPieceX = e.getX();
            movingPieceY = e.getY();
            repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {}
}