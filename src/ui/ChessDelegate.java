package ui;

import core.ChessPiece;
import core.ChessModel;

public interface ChessDelegate {
    ChessPiece pieceAt(int col, int row);
    void movePiece(int fromCol, int fromRow, int toCol, int toRow);
    ChessModel getChessModel(); 
}