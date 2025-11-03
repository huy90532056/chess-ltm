package core;

import java.util.*;

// logic chess
public class ChessModel {
    public enum MoveResult {
        SUCCESS,
        INVALID_MOVE,
        STILL_IN_CHECK,
        PROMOTION,
        CHECKMATE,
        STALEMATE,
        DRAW_50_MOVES,
        DRAW_INSUFFICIENT_MATERIAL,
        DRAW_THREEFOLD_REPETITION
    }

    private Set<ChessPiece> piecesBox = new HashSet<>();
    private Player playerInTurn = Player.WHITE;

    private boolean whiteKingMoved = false;
    private boolean blackKingMoved = false;
    private boolean whiteRookLeftMoved = false;
    private boolean whiteRookRightMoved = false;
    private boolean blackRookLeftMoved = false;
    private boolean blackRookRightMoved = false;

    private int enPassantCol = -1;
    private int enPassantRow = -1;

    private int halfMoveClock = 0;
    private Map<String, Integer> positionCount = new HashMap<>();

    public void reset() {
        piecesBox.clear();

        for (int i = 0; i < 2; i++) {
            piecesBox.add(new ChessPiece(0 + i * 7, 7, Player.BLACK, Rank.ROOK, ChessConstants.bRook));
            piecesBox.add(new ChessPiece(0 + i * 7, 0, Player.WHITE, Rank.ROOK, ChessConstants.wRook));

            piecesBox.add(new ChessPiece(1 + i * 5, 7, Player.BLACK, Rank.KNIGHT, ChessConstants.bKnight));
            piecesBox.add(new ChessPiece(1 + i * 5, 0, Player.WHITE, Rank.KNIGHT, ChessConstants.wKnight));

            piecesBox.add(new ChessPiece(2 + i * 3, 7, Player.BLACK, Rank.BISHOP, ChessConstants.bBishop));
            piecesBox.add(new ChessPiece(2 + i * 3, 0, Player.WHITE, Rank.BISHOP, ChessConstants.wBishop));
        }

        for (int i = 0; i < 8; i++) {
            piecesBox.add(new ChessPiece(i, 6, Player.BLACK, Rank.PAWN, ChessConstants.bPawn));
            piecesBox.add(new ChessPiece(i, 1, Player.WHITE, Rank.PAWN, ChessConstants.wPawn));
        }

        piecesBox.add(new ChessPiece(3, 7, Player.BLACK, Rank.QUEEN, ChessConstants.bQueen));
        piecesBox.add(new ChessPiece(3, 0, Player.WHITE, Rank.QUEEN, ChessConstants.wQueen));
        piecesBox.add(new ChessPiece(4, 7, Player.BLACK, Rank.KING, ChessConstants.bKing));
        piecesBox.add(new ChessPiece(4, 0, Player.WHITE, Rank.KING, ChessConstants.wKing));

        playerInTurn = Player.WHITE;

        whiteKingMoved = false;
        blackKingMoved = false;
        whiteRookLeftMoved = false;
        whiteRookRightMoved = false;
        blackRookLeftMoved = false;
        blackRookRightMoved = false;

        enPassantCol = -1;
        enPassantRow = -1;

        halfMoveClock = 0;
        positionCount.clear();
        updatePositionCount();
    }

    public MoveResult movePiece(int fromCol, int fromRow, int toCol, int toRow, Rank promotionRank) {
        ChessPiece movingPiece = pieceAt(fromCol, fromRow);
        if (!isValidMove(movingPiece, fromCol, fromRow, toCol, toRow)) {
            return MoveResult.INVALID_MOVE;
        }
        if (isSelfCheck(movingPiece, fromCol, fromRow, toCol, toRow)) {
            return MoveResult.STILL_IN_CHECK;
        }

        boolean isPromotion = false;
        if (movingPiece.getRank() == Rank.PAWN &&
            ((movingPiece.getPlayer() == Player.WHITE && toRow == 7) ||
             (movingPiece.getPlayer() == Player.BLACK && toRow == 0))) {
            isPromotion = true;
        }

        boolean isEnPassant = false;
        ChessPiece target = pieceAt(toCol, toRow);
        if (movingPiece.getRank() == Rank.PAWN && target == null && fromCol != toCol) {
            if (enPassantCol == toCol && enPassantRow == toRow) {
                isEnPassant = true;
            }
        }

        if (movingPiece.getRank() == Rank.KING && Math.abs(toCol - fromCol) == 2) {
            if (movingPiece.getPlayer() == Player.WHITE) {
                whiteKingMoved = true;
                if (toCol == 6) {
                    ChessPiece rook = pieceAt(7, 0);
                    if (rook != null && rook.getRank() == Rank.ROOK) {
                        piecesBox.remove(rook);
                        piecesBox.add(new ChessPiece(5, 0, Player.WHITE, Rank.ROOK, ChessConstants.wRook));
                        whiteRookRightMoved = true;
                    }
                } else if (toCol == 2) {
                    ChessPiece rook = pieceAt(0, 0);
                    if (rook != null && rook.getRank() == Rank.ROOK) {
                        piecesBox.remove(rook);
                        piecesBox.add(new ChessPiece(3, 0, Player.WHITE, Rank.ROOK, ChessConstants.wRook));
                        whiteRookLeftMoved = true;
                    }
                }
            }
            if (movingPiece.getPlayer() == Player.BLACK) {
                blackKingMoved = true;
                if (toCol == 6) {
                    ChessPiece rook = pieceAt(7, 7);
                    if (rook != null && rook.getRank() == Rank.ROOK) {
                        piecesBox.remove(rook);
                        piecesBox.add(new ChessPiece(5, 7, Player.BLACK, Rank.ROOK, ChessConstants.bRook));
                        blackRookRightMoved = true;
                    }
                } else if (toCol == 2) {
                    ChessPiece rook = pieceAt(0, 7);
                    if (rook != null && rook.getRank() == Rank.ROOK) {
                        piecesBox.remove(rook);
                        piecesBox.add(new ChessPiece(3, 7, Player.BLACK, Rank.ROOK, ChessConstants.bRook));
                        blackRookLeftMoved = true;
                    }
                }
            }
        }

        if (movingPiece.getRank() == Rank.KING) {
            if (movingPiece.getPlayer() == Player.WHITE) whiteKingMoved = true;
            else blackKingMoved = true;
        }
        if (movingPiece.getRank() == Rank.ROOK) {
            if (movingPiece.getPlayer() == Player.WHITE) {
                if (fromCol == 0 && fromRow == 0) whiteRookLeftMoved = true;
                if (fromCol == 7 && fromRow == 0) whiteRookRightMoved = true;
            } else {
                if (fromCol == 0 && fromRow == 7) blackRookLeftMoved = true;
                if (fromCol == 7 && fromRow == 7) blackRookRightMoved = true;
            }
        }

        boolean isCapture = false;
        if (isEnPassant) {
            int capturedRow = movingPiece.getPlayer() == Player.WHITE ? toRow - 1 : toRow + 1;
            ChessPiece capturedPawn = pieceAt(toCol, capturedRow);
            if (capturedPawn != null && capturedPawn.getRank() == Rank.PAWN && capturedPawn.getPlayer() != movingPiece.getPlayer()) {
                piecesBox.remove(capturedPawn);
                isCapture = true;
            }
        }

        if (target != null && !isEnPassant) {
            piecesBox.remove(target);
            isCapture = true;
        }
        piecesBox.remove(movingPiece);

        if (isPromotion) {
            if (promotionRank == null) promotionRank = Rank.QUEEN;
            piecesBox.add(new ChessPiece(toCol, toRow, movingPiece.getPlayer(), promotionRank, getPromotionImgName(movingPiece.getPlayer(), promotionRank)));
        } else {
            piecesBox.add(new ChessPiece(toCol, toRow, movingPiece.getPlayer(), movingPiece.getRank(), movingPiece.getImgName()));
        }

        if (movingPiece.getRank() == Rank.PAWN && Math.abs(toRow - fromRow) == 2) {
            enPassantCol = fromCol;
            enPassantRow = (fromRow + toRow) / 2;
        } else {
            enPassantCol = -1;
            enPassantRow = -1;
        }

        if (movingPiece.getRank() == Rank.PAWN || isCapture || isPromotion) {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }

        playerInTurn = playerInTurn == Player.WHITE ? Player.BLACK : Player.WHITE;

        updatePositionCount();

        if (isStalemate(playerInTurn)) {
            return MoveResult.STALEMATE;
        }
        if (isInsufficientMaterial()) {
            return MoveResult.DRAW_INSUFFICIENT_MATERIAL;
        }
        if (isThreefoldRepetition()) {
            return MoveResult.DRAW_THREEFOLD_REPETITION;
        }
        if (halfMoveClock >= 100) {
            return MoveResult.DRAW_50_MOVES;
        }
        if (isCheckmate(playerInTurn)) {
            return MoveResult.CHECKMATE;
        }
        if (isPromotion) {
            return MoveResult.PROMOTION;
        }
        return MoveResult.SUCCESS;
    }

    private void updatePositionCount() {
        String fen = getSimpleFEN();
        positionCount.put(fen, positionCount.getOrDefault(fen, 0) + 1);
    }

    private String getSimpleFEN() {
        StringBuilder sb = new StringBuilder();
        ChessPiece[] arr = piecesBox.toArray(new ChessPiece[0]);
        Arrays.sort(arr, Comparator.comparingInt(ChessPiece::getRow).thenComparingInt(ChessPiece::getCol));
        for (ChessPiece p : arr) {
            sb.append(p.getPlayer()).append(p.getRank()).append(p.getCol()).append(p.getRow()).append(";");
        }
        sb.append(playerInTurn);
        return sb.toString();
    }

    private boolean isThreefoldRepetition() {
        String fen = getSimpleFEN();
        return positionCount.getOrDefault(fen, 0) >= 3;
    }

    private boolean isInsufficientMaterial() {
        if (piecesBox.size() == 2) return true;
        if (piecesBox.size() == 3) {
            int bishopOrKnight = 0;
            for (ChessPiece p : piecesBox) {
                if (p.getRank() == Rank.BISHOP || p.getRank() == Rank.KNIGHT) bishopOrKnight++;
            }
            if (bishopOrKnight == 1) return true;
        }
        if (piecesBox.size() == 4) {
            int bishops = 0;
            int color = -1;
            for (ChessPiece p : piecesBox) {
                if (p.getRank() == Rank.BISHOP) {
                    bishops++;
                    int sqColor = (p.getCol() + p.getRow()) % 2;
                    if (color == -1) color = sqColor;
                    else if (color != sqColor) return false;
                }
            }
            if (bishops == 2) return true;
        }
        return false;
    }

    private boolean isStalemate(Player player) {
        if (isKingChecked(player)) return false;
        Set<ChessPiece> piecesCopy = new HashSet<>(piecesBox);
        for (ChessPiece p : piecesCopy) {
            if (p.getPlayer() == player) {
                for (int toCol = 0; toCol < 8; toCol++) {
                    for (int toRow = 0; toRow < 8; toRow++) {
                        if (isValidMove(p, p.getCol(), p.getRow(), toCol, toRow) &&
                            !isSelfCheck(p, p.getCol(), p.getRow(), toCol, toRow)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private String getPromotionImgName(Player player, Rank rank) {
        if (rank == null) return ChessConstants.wQueen;
        if (player == Player.WHITE) {
            switch (rank) {
                case QUEEN: return ChessConstants.wQueen;
                case ROOK: return ChessConstants.wRook;
                case BISHOP: return ChessConstants.wBishop;
                case KNIGHT: return ChessConstants.wKnight;
            }
        } else {
            switch (rank) {
                case QUEEN: return ChessConstants.bQueen;
                case ROOK: return ChessConstants.bRook;
                case BISHOP: return ChessConstants.bBishop;
                case KNIGHT: return ChessConstants.bKnight;
            }
        }
        return ChessConstants.wQueen;
    }

    public ChessPiece pieceAt(int col, int row) {
        for (ChessPiece chessPiece : piecesBox) {
            if (chessPiece.getCol() == col && chessPiece.getRow() == row) {
                return chessPiece;
            }
        }
        return null;
    }

    public Player getPlayerInTurn() {
        return playerInTurn;
    }

    public boolean isValidMove(ChessPiece piece, int fromCol, int fromRow, int toCol, int toRow) {
        if (piece == null) return false;
        if (fromCol == toCol && fromRow == toRow) return false;
        ChessPiece target = pieceAt(toCol, toRow);
        if (target != null && target.getPlayer() == piece.getPlayer()) return false;

        int dx = toCol - fromCol;
        int dy = toRow - fromRow;

        switch (piece.getRank()) {
            case PAWN:
                int dir = piece.getPlayer() == Player.WHITE ? 1 : -1;
                if (dx == 0 && dy == dir && target == null) return true;
                if (dx == 0 && dy == 2 * dir && target == null &&
                    ((piece.getPlayer() == Player.WHITE && fromRow == 1) ||
                     (piece.getPlayer() == Player.BLACK && fromRow == 6)) &&
                    pieceAt(fromCol, fromRow + dir) == null) return true;
                if (Math.abs(dx) == 1 && dy == dir && target != null && target.getPlayer() != piece.getPlayer()) return true;
                if (Math.abs(dx) == 1 && dy == dir && target == null &&
                    enPassantCol == toCol && enPassantRow == toRow) return true;
                return false;
            case KNIGHT:
                return (Math.abs(dx) == 2 && Math.abs(dy) == 1) || (Math.abs(dx) == 1 && Math.abs(dy) == 2);
            case BISHOP:
                if (Math.abs(dx) != Math.abs(dy)) return false;
                return isPathClear(fromCol, fromRow, toCol, toRow);
            case ROOK:
                if (dx != 0 && dy != 0) return false;
                return isPathClear(fromCol, fromRow, toCol, toRow);
            case QUEEN:
                if (dx == 0 || dy == 0 || Math.abs(dx) == Math.abs(dy)) {
                    return isPathClear(fromCol, fromRow, toCol, toRow);
                }
                return false;
            case KING:
                if (piece.getPlayer() == Player.WHITE && fromCol == 4 && fromRow == 0 && !whiteKingMoved) {
                    if (toCol == 6 && toRow == 0 && !whiteRookRightMoved &&
                        pieceAt(5, 0) == null && pieceAt(6, 0) == null &&
                        !isKingChecked(Player.WHITE)) return true;
                    if (toCol == 2 && toRow == 0 && !whiteRookLeftMoved &&
                        pieceAt(3, 0) == null && pieceAt(2, 0) == null && pieceAt(1, 0) == null &&
                        !isKingChecked(Player.WHITE)) return true;
                }
                if (piece.getPlayer() == Player.BLACK && fromCol == 4 && fromRow == 7 && !blackKingMoved) {
                    if (toCol == 6 && toRow == 7 && !blackRookRightMoved &&
                        pieceAt(5, 7) == null && pieceAt(6, 7) == null &&
                        !isKingChecked(Player.BLACK)) return true;
                    if (toCol == 2 && toRow == 7 && !blackRookLeftMoved &&
                        pieceAt(3, 7) == null && pieceAt(2, 7) == null && pieceAt(1, 7) == null &&
                        !isKingChecked(Player.BLACK)) return true;
                }
                return Math.abs(dx) <= 1 && Math.abs(dy) <= 1;
            default:
                return false;
        }
    }

    private boolean isPathClear(int fromCol, int fromRow, int toCol, int toRow) {
        int dx = Integer.compare(toCol, fromCol);
        int dy = Integer.compare(toRow, fromRow);
        int col = fromCol + dx;
        int row = fromRow + dy;
        while (col != toCol || row != toRow) {
            if (pieceAt(col, row) != null) return false;
            col += dx;
            row += dy;
        }
        return true;
    }

    public boolean isKingChecked(Player player) {
        ChessPiece king = null;
        for (ChessPiece p : piecesBox) {
            if (p.getPlayer() == player && p.getRank() == Rank.KING) {
                king = p;
                break;
            }
        }
        if (king == null) return false;

        for (ChessPiece p : piecesBox) {
            if (p.getPlayer() != player) {
                if (isValidMove(p, p.getCol(), p.getRow(), king.getCol(), king.getRow())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isSelfCheck(ChessPiece piece, int fromCol, int fromRow, int toCol, int toRow) {
        Set<ChessPiece> backup = new HashSet<>(piecesBox);
        ChessPiece movingPiece = pieceAt(fromCol, fromRow);
        ChessPiece target = pieceAt(toCol, toRow);
        piecesBox.remove(movingPiece);
        if (target != null) piecesBox.remove(target);

        if (movingPiece.getRank() == Rank.PAWN &&
            ((movingPiece.getPlayer() == Player.WHITE && toRow == 7) ||
             (movingPiece.getPlayer() == Player.BLACK && toRow == 0))) {
            piecesBox.add(new ChessPiece(toCol, toRow, movingPiece.getPlayer(), Rank.QUEEN, getPromotionImgName(movingPiece.getPlayer(), Rank.QUEEN)));
        } else {
            piecesBox.add(new ChessPiece(toCol, toRow, movingPiece.getPlayer(), movingPiece.getRank(), movingPiece.getImgName()));
        }
        boolean result = isKingChecked(movingPiece.getPlayer());
        piecesBox.clear();
        piecesBox.addAll(backup);
        return result;
    }

    public boolean isCheckmate(Player player) {
        if (!isKingChecked(player)) return false;
        Set<ChessPiece> piecesCopy = new HashSet<>(piecesBox);
        for (ChessPiece p : piecesCopy) {
            if (p.getPlayer() == player) {
                for (int toCol = 0; toCol < 8; toCol++) {
                    for (int toRow = 0; toRow < 8; toRow++) {
                        if (isValidMove(p, p.getCol(), p.getRow(), toCol, toRow) &&
                            !isSelfCheck(p, p.getCol(), p.getRow(), toCol, toRow)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}