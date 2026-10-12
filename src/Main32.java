import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class Main32 extends JFrame {
    private final JButton[][] buttons = new JButton[8][8];
    private final JLabel statusLabel = new JLabel(" ");
    private ChessGame game = new ChessGame();
    private final Deque<ChessGame> history = new ArrayDeque<>();
    private final JButton undoButton = new JButton("Undo");
    private int selectedRow = -1;
    private int selectedCol = -1;
    private List<Move> selectedMoves = new ArrayList<>();

    private static final Color LIGHT = new Color(240, 217, 181);
    private static final Color DARK = new Color(181, 136, 99);
    private static final Color SELECTED = new Color(246, 246, 105);
    private static final Color LEGAL = new Color(130, 190, 120);
    private static final Font PIECE_FONT = new Font("SansSerif", Font.PLAIN, 42);

    public Main32() {
        super("Chess - Swing");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel boardPanel = new JPanel(new GridLayout(8, 8));
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                JButton button = new JButton();
                button.setFont(PIECE_FONT);
                button.setFocusPainted(true);
                button.setOpaque(true);
                button.setBorderPainted(false);
                final int row = r;
                final int col = c;
                button.addActionListener(e -> handleSquareClick(row, col));
                button.addKeyListener(new KeyAdapter() {
                    @Override public void keyPressed(KeyEvent event) {
                        int nextRow = row, nextCol = col;
                        switch (event.getKeyCode()) {
                            case KeyEvent.VK_LEFT -> nextCol = Math.max(0, col - 1);
                            case KeyEvent.VK_RIGHT -> nextCol = Math.min(7, col + 1);
                            case KeyEvent.VK_UP -> nextRow = Math.max(0, row - 1);
                            case KeyEvent.VK_DOWN -> nextRow = Math.min(7, row + 1);
                            case KeyEvent.VK_ESCAPE -> { clearSelection(); render(); event.consume(); return; }
                            default -> { return; }
                        }
                        buttons[nextRow][nextCol].requestFocusInWindow();
                        event.consume();
                    }
                });
                buttons[r][c] = button;
                boardPanel.add(button);
            }
        }

        JButton newGameButton = new JButton("New Game");
        newGameButton.addActionListener(e -> newGame());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        JPanel actions = new JPanel();
        undoButton.addActionListener(e -> undoMove());
        actions.add(undoButton);
        actions.add(newGameButton);
        bottomPanel.add(actions, BorderLayout.EAST);

        add(boardPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        render();
        setSize(640, 700);
        setLocationRelativeTo(null);
    }

    private void newGame() {
        game = new ChessGame();
        history.clear();
        selectedRow = -1;
        selectedCol = -1;
        selectedMoves.clear();
        render();
    }

    private void undoMove() {
        if (history.isEmpty()) return;
        game = history.pop();
        clearSelection();
        render();
    }

    private void handleSquareClick(int row, int col) {
        if (game.isGameOver()) {
            return;
        }

        Piece clicked = game.getPiece(row, col);

        if (selectedRow == -1) {
            selectSquareIfPossible(row, col);
            return;
        }

        if (row == selectedRow && col == selectedCol) {
            clearSelection();
            render();
            return;
        }

        List<Move> matchingMoves = new ArrayList<>();
        for (Move move : selectedMoves) {
            if (move.toRow == row && move.toCol == col) {
                matchingMoves.add(move);
            }
        }

        if (!matchingMoves.isEmpty()) {
            Move chosenMove = choosePromotionMoveIfNeeded(matchingMoves);
            if (chosenMove == null) return;
            history.push(new ChessGame(game));
            game.makeMove(chosenMove);
            clearSelection();
            render();
            announceGameEndIfNeeded();
            return;
        }

        if (clicked != null && clicked.white == game.isWhiteTurn()) {
            selectSquareIfPossible(row, col);
        } else {
            clearSelection();
            render();
        }
    }

    private void selectSquareIfPossible(int row, int col) {
        Piece piece = game.getPiece(row, col);
        if (piece == null || piece.white != game.isWhiteTurn()) {
            return;
        }

        List<Move> legalMoves = game.getLegalMoves(row, col);
        if (legalMoves.isEmpty()) {
            return;
        }

        selectedRow = row;
        selectedCol = col;
        selectedMoves = legalMoves;
        render();
    }

    private Move choosePromotionMoveIfNeeded(List<Move> moves) {
        if (moves.size() == 1 || moves.get(0).promotion == null) {
            return moves.get(0);
        }

        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Choose promotion piece:",
                "Pawn Promotion",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice < 0) return null;
        PieceType type = switch (choice) {
            case 1 -> PieceType.ROOK;
            case 2 -> PieceType.BISHOP;
            case 3 -> PieceType.KNIGHT;
            default -> PieceType.QUEEN;
        };

        for (Move move : moves) {
            if (move.promotion == type) {
                return move;
            }
        }
        return moves.get(0);
    }

    private void clearSelection() {
        selectedRow = -1;
        selectedCol = -1;
        selectedMoves.clear();
    }

    private void render() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                JButton button = buttons[r][c];
                Piece piece = game.getPiece(r, c);
                String square = "" + (char) ('a' + c) + (8 - r);
                String label = square + ": " + (piece == null ? "empty" : (piece.white ? "White " : "Black ") + piece.type.name().toLowerCase());
                button.getAccessibleContext().setAccessibleName(label);
                button.setToolTipText(label);
                button.setText(piece == null ? "" : piece.symbol());
                button.setBackground(squareColor(r, c));
            }
        }
        undoButton.setEnabled(!history.isEmpty());
        statusLabel.setText(game.statusText() + "  •  " + history.size() + " moves played");
    }

    private Color squareColor(int row, int col) {
        if (row == selectedRow && col == selectedCol) {
            return SELECTED;
        }
        for (Move move : selectedMoves) {
            if (move.toRow == row && move.toCol == col) {
                return LEGAL;
            }
        }
        return (row + col) % 2 == 0 ? LIGHT : DARK;
    }

    private void announceGameEndIfNeeded() {
        if (game.isGameOver()) {
            JOptionPane.showMessageDialog(this, game.statusText());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main32().setVisible(true));
    }
}

class ChessGame {
    private final Piece[][] board = new Piece[8][8];
    private boolean whiteTurn = true;

    private boolean whiteKingMoved = false;
    private boolean blackKingMoved = false;
    private boolean whiteQueensideRookMoved = false;
    private boolean whiteKingsideRookMoved = false;
    private boolean blackQueensideRookMoved = false;
    private boolean blackKingsideRookMoved = false;

    private int enPassantRow = -1;
    private int enPassantCol = -1;

    public ChessGame() {
        setupInitialPosition();
    }

    ChessGame(ChessGame other) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                board[r][c] = other.board[r][c] == null ? null : other.board[r][c].copy();
            }
        }
        whiteTurn = other.whiteTurn;
        whiteKingMoved = other.whiteKingMoved;
        blackKingMoved = other.blackKingMoved;
        whiteQueensideRookMoved = other.whiteQueensideRookMoved;
        whiteKingsideRookMoved = other.whiteKingsideRookMoved;
        blackQueensideRookMoved = other.blackQueensideRookMoved;
        blackKingsideRookMoved = other.blackKingsideRookMoved;
        enPassantRow = other.enPassantRow;
        enPassantCol = other.enPassantCol;
    }

    private void setupInitialPosition() {
        board[0][0] = new Piece(PieceType.ROOK, false);
        board[0][1] = new Piece(PieceType.KNIGHT, false);
        board[0][2] = new Piece(PieceType.BISHOP, false);
        board[0][3] = new Piece(PieceType.QUEEN, false);
        board[0][4] = new Piece(PieceType.KING, false);
        board[0][5] = new Piece(PieceType.BISHOP, false);
        board[0][6] = new Piece(PieceType.KNIGHT, false);
        board[0][7] = new Piece(PieceType.ROOK, false);
        for (int c = 0; c < 8; c++) {
            board[1][c] = new Piece(PieceType.PAWN, false);
        }

        board[7][0] = new Piece(PieceType.ROOK, true);
        board[7][1] = new Piece(PieceType.KNIGHT, true);
        board[7][2] = new Piece(PieceType.BISHOP, true);
        board[7][3] = new Piece(PieceType.QUEEN, true);
        board[7][4] = new Piece(PieceType.KING, true);
        board[7][5] = new Piece(PieceType.BISHOP, true);
        board[7][6] = new Piece(PieceType.KNIGHT, true);
        board[7][7] = new Piece(PieceType.ROOK, true);
        for (int c = 0; c < 8; c++) {
            board[6][c] = new Piece(PieceType.PAWN, true);
        }
    }

    public Piece getPiece(int row, int col) {
        return board[row][col];
    }

    public boolean isWhiteTurn() {
        return whiteTurn;
    }

    public List<Move> getLegalMoves(int row, int col) {
        Piece piece = board[row][col];
        List<Move> legalMoves = new ArrayList<>();
        if (piece == null || piece.white != whiteTurn) {
            return legalMoves;
        }

        for (Move move : getPseudoLegalMoves(row, col)) {
            ChessGame copy = new ChessGame(this);
            copy.applyMoveWithoutValidation(move);
            if (!copy.isInCheck(piece.white)) {
                legalMoves.add(move);
            }
        }
        return legalMoves;
    }

    public void makeMove(Move move) {
        Piece piece = board[move.fromRow][move.fromCol];
        if (piece == null || piece.white != whiteTurn) {
            return;
        }

        boolean legal = false;
        for (Move legalMove : getLegalMoves(move.fromRow, move.fromCol)) {
            if (legalMove.sameChoiceAs(move)) {
                legal = true;
                break;
            }
        }

        if (legal) {
            applyMoveWithoutValidation(move);
        }
    }

    public boolean isGameOver() {
        return !hasAnyLegalMove(whiteTurn);
    }

    public String statusText() {
        boolean sideToMove = whiteTurn;
        boolean inCheck = isInCheck(sideToMove);
        boolean hasLegalMove = hasAnyLegalMove(sideToMove);

        if (!hasLegalMove && inCheck) {
            return "Checkmate! " + (sideToMove ? "Black" : "White") + " wins.";
        }
        if (!hasLegalMove) {
            return "Stalemate! Draw.";
        }
        return (sideToMove ? "White" : "Black") + " to move" + (inCheck ? " — check!" : "");
    }

    private boolean hasAnyLegalMove(boolean white) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board[r][c];
                if (piece != null && piece.white == white) {
                    boolean oldTurn = whiteTurn;
                    whiteTurn = white;
                    boolean hasMove = !getLegalMoves(r, c).isEmpty();
                    whiteTurn = oldTurn;
                    if (hasMove) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<Move> getPseudoLegalMoves(int row, int col) {
        Piece piece = board[row][col];
        List<Move> moves = new ArrayList<>();
        if (piece == null) {
            return moves;
        }

        switch (piece.type) {
            case PAWN -> addPawnMoves(row, col, piece, moves);
            case KNIGHT -> addKnightMoves(row, col, piece, moves);
            case BISHOP -> addSlidingMoves(row, col, piece, moves, new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}});
            case ROOK -> addSlidingMoves(row, col, piece, moves, new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}});
            case QUEEN -> addSlidingMoves(row, col, piece, moves, new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}});
            case KING -> addKingMoves(row, col, piece, moves);
        }
        return moves;
    }

    private void addPawnMoves(int row, int col, Piece piece, List<Move> moves) {
        int direction = piece.white ? -1 : 1;
        int startRow = piece.white ? 6 : 1;
        int promotionRow = piece.white ? 0 : 7;
        int oneStepRow = row + direction;

        if (isInside(oneStepRow, col) && board[oneStepRow][col] == null) {
            addPawnMoveOrPromotions(row, col, oneStepRow, col, promotionRow, false, moves);

            int twoStepRow = row + 2 * direction;
            if (row == startRow && board[twoStepRow][col] == null) {
                moves.add(new Move(row, col, twoStepRow, col));
            }
        }

        for (int dc : new int[]{-1, 1}) {
            int captureRow = row + direction;
            int captureCol = col + dc;
            if (!isInside(captureRow, captureCol)) {
                continue;
            }

            Piece target = board[captureRow][captureCol];
            if (target != null && target.white != piece.white) {
                addPawnMoveOrPromotions(row, col, captureRow, captureCol, promotionRow, false, moves);
            }

            if (captureRow == enPassantRow && captureCol == enPassantCol) {
                Move move = new Move(row, col, captureRow, captureCol);
                move.enPassant = true;
                moves.add(move);
            }
        }
    }

    private void addPawnMoveOrPromotions(int fromRow, int fromCol, int toRow, int toCol,
                                         int promotionRow, boolean enPassant, List<Move> moves) {
        if (toRow == promotionRow) {
            for (PieceType type : new PieceType[]{PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT}) {
                Move move = new Move(fromRow, fromCol, toRow, toCol);
                move.promotion = type;
                move.enPassant = enPassant;
                moves.add(move);
            }
        } else {
            Move move = new Move(fromRow, fromCol, toRow, toCol);
            move.enPassant = enPassant;
            moves.add(move);
        }
    }

    private void addKnightMoves(int row, int col, Piece piece, List<Move> moves) {
        int[][] directions = {{2, 1}, {2, -1}, {-2, 1}, {-2, -1}, {1, 2}, {1, -2}, {-1, 2}, {-1, -2}};
        for (int[] d : directions) {
            int r = row + d[0];
            int c = col + d[1];
            if (isInside(r, c) && (board[r][c] == null || board[r][c].white != piece.white)) {
                moves.add(new Move(row, col, r, c));
            }
        }
    }

    private void addSlidingMoves(int row, int col, Piece piece, List<Move> moves, int[][] directions) {
        for (int[] d : directions) {
            int r = row + d[0];
            int c = col + d[1];
            while (isInside(r, c)) {
                if (board[r][c] == null) {
                    moves.add(new Move(row, col, r, c));
                } else {
                    if (board[r][c].white != piece.white) {
                        moves.add(new Move(row, col, r, c));
                    }
                    break;
                }
                r += d[0];
                c += d[1];
            }
        }
    }

    private void addKingMoves(int row, int col, Piece piece, List<Move> moves) {
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) {
                    continue;
                }
                int r = row + dr;
                int c = col + dc;
                if (isInside(r, c) && (board[r][c] == null || board[r][c].white != piece.white)) {
                    moves.add(new Move(row, col, r, c));
                }
            }
        }

        addCastlingMoves(row, col, piece, moves);
    }

    private void addCastlingMoves(int row, int col, Piece king, List<Move> moves) {
        if (king.type != PieceType.KING || isInCheck(king.white)) {
            return;
        }

        if (king.white) {
            if (!whiteKingMoved && row == 7 && col == 4) {
                addCastlingMoveIfAllowed(7, true, whiteKingsideRookMoved, whiteQueensideRookMoved, king.white, moves);
            }
        } else {
            if (!blackKingMoved && row == 0 && col == 4) {
                addCastlingMoveIfAllowed(0, false, blackKingsideRookMoved, blackQueensideRookMoved, king.white, moves);
            }
        }
    }

    private void addCastlingMoveIfAllowed(int row, boolean white, boolean kingsideRookMoved,
                                          boolean queensideRookMoved, boolean kingWhite, List<Move> moves) {
        if (!kingsideRookMoved
                && hasRook(row, 7, white)
                && board[row][5] == null
                && board[row][6] == null
                && !isSquareAttacked(row, 5, !kingWhite)
                && !isSquareAttacked(row, 6, !kingWhite)) {
            Move move = new Move(row, 4, row, 6);
            move.castle = true;
            moves.add(move);
        }

        if (!queensideRookMoved
                && hasRook(row, 0, white)
                && board[row][1] == null
                && board[row][2] == null
                && board[row][3] == null
                && !isSquareAttacked(row, 3, !kingWhite)
                && !isSquareAttacked(row, 2, !kingWhite)) {
            Move move = new Move(row, 4, row, 2);
            move.castle = true;
            moves.add(move);
        }
    }

    private boolean hasRook(int row, int col, boolean white) {
        Piece piece = board[row][col];
        return piece != null && piece.white == white && piece.type == PieceType.ROOK;
    }

    private void applyMoveWithoutValidation(Move move) {
        Piece piece = board[move.fromRow][move.fromCol];
        Piece captured = board[move.toRow][move.toCol];

        updateCastlingRightsForMovingPiece(move, piece);
        updateCastlingRightsForCapturedPiece(move.toRow, move.toCol, captured);

        board[move.fromRow][move.fromCol] = null;

        if (move.enPassant) {
            int capturedPawnRow = piece.white ? move.toRow + 1 : move.toRow - 1;
            board[capturedPawnRow][move.toCol] = null;
        }

        if (move.castle) {
            board[move.toRow][move.toCol] = piece;
            if (move.toCol == 6) {
                board[move.toRow][5] = board[move.toRow][7];
                board[move.toRow][7] = null;
            } else if (move.toCol == 2) {
                board[move.toRow][3] = board[move.toRow][0];
                board[move.toRow][0] = null;
            }
        } else {
            board[move.toRow][move.toCol] = piece;
            if (piece.type == PieceType.PAWN && (move.toRow == 0 || move.toRow == 7)) {
                piece.type = move.promotion == null ? PieceType.QUEEN : move.promotion;
            }
        }

        if (piece.type == PieceType.PAWN && Math.abs(move.toRow - move.fromRow) == 2) {
            enPassantRow = (move.fromRow + move.toRow) / 2;
            enPassantCol = move.fromCol;
        } else {
            enPassantRow = -1;
            enPassantCol = -1;
        }

        whiteTurn = !whiteTurn;
    }

    private void updateCastlingRightsForMovingPiece(Move move, Piece piece) {
        if (piece == null) {
            return;
        }

        if (piece.type == PieceType.KING) {
            if (piece.white) {
                whiteKingMoved = true;
            } else {
                blackKingMoved = true;
            }
        }

        if (piece.type == PieceType.ROOK) {
            if (move.fromRow == 7 && move.fromCol == 0) whiteQueensideRookMoved = true;
            if (move.fromRow == 7 && move.fromCol == 7) whiteKingsideRookMoved = true;
            if (move.fromRow == 0 && move.fromCol == 0) blackQueensideRookMoved = true;
            if (move.fromRow == 0 && move.fromCol == 7) blackKingsideRookMoved = true;
        }
    }

    private void updateCastlingRightsForCapturedPiece(int row, int col, Piece captured) {
        if (captured == null || captured.type != PieceType.ROOK) {
            return;
        }

        if (row == 7 && col == 0) whiteQueensideRookMoved = true;
        if (row == 7 && col == 7) whiteKingsideRookMoved = true;
        if (row == 0 && col == 0) blackQueensideRookMoved = true;
        if (row == 0 && col == 7) blackKingsideRookMoved = true;
    }

    private boolean isInCheck(boolean white) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board[r][c];
                if (piece != null && piece.white == white && piece.type == PieceType.KING) {
                    return isSquareAttacked(r, c, !white);
                }
            }
        }
        return true;
    }

    private boolean isSquareAttacked(int row, int col, boolean byWhite) {
        int pawnSourceRow = row + (byWhite ? 1 : -1);
        for (int dc : new int[]{-1, 1}) {
            int pawnSourceCol = col + dc;
            if (isInside(pawnSourceRow, pawnSourceCol)) {
                Piece piece = board[pawnSourceRow][pawnSourceCol];
                if (piece != null && piece.white == byWhite && piece.type == PieceType.PAWN) {
                    return true;
                }
            }
        }

        int[][] knightDirections = {{2, 1}, {2, -1}, {-2, 1}, {-2, -1}, {1, 2}, {1, -2}, {-1, 2}, {-1, -2}};
        for (int[] d : knightDirections) {
            int r = row + d[0];
            int c = col + d[1];
            if (isInside(r, c)) {
                Piece piece = board[r][c];
                if (piece != null && piece.white == byWhite && piece.type == PieceType.KNIGHT) {
                    return true;
                }
            }
        }

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) {
                    continue;
                }
                int r = row + dr;
                int c = col + dc;
                if (isInside(r, c)) {
                    Piece piece = board[r][c];
                    if (piece != null && piece.white == byWhite && piece.type == PieceType.KING) {
                        return true;
                    }
                }
            }
        }

        if (isAttackedBySlidingPiece(row, col, byWhite, new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}, PieceType.ROOK, PieceType.QUEEN)) {
            return true;
        }

        return isAttackedBySlidingPiece(row, col, byWhite, new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}, PieceType.BISHOP, PieceType.QUEEN);
    }

    private boolean isAttackedBySlidingPiece(int row, int col, boolean byWhite, int[][] directions,
                                             PieceType type1, PieceType type2) {
        for (int[] d : directions) {
            int r = row + d[0];
            int c = col + d[1];
            while (isInside(r, c)) {
                Piece piece = board[r][c];
                if (piece != null) {
                    if (piece.white == byWhite && (piece.type == type1 || piece.type == type2)) return true;
                    break;
                }
                r += d[0];
                c += d[1];
            }
        }
        return false;
    }

    private boolean isInside(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }
}

class Move {
    final int fromRow;
    final int fromCol;
    final int toRow;
    final int toCol;
    PieceType promotion;
    boolean castle;
    boolean enPassant;

    Move(int fromRow, int fromCol, int toRow, int toCol) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
    }

    boolean sameChoiceAs(Move other) {
        return fromRow == other.fromRow
                && fromCol == other.fromCol
                && toRow == other.toRow
                && toCol == other.toCol
                && promotion == other.promotion
                && castle == other.castle
                && enPassant == other.enPassant;
    }
}

class Piece {
    PieceType type;
    final boolean white;

    Piece(PieceType type, boolean white) {
        this.type = type;
        this.white = white;
    }

    Piece copy() {
        return new Piece(type, white);
    }

    String symbol() {
        return switch (type) {
            case KING -> white ? "♔" : "♚";
            case QUEEN -> white ? "♕" : "♛";
            case ROOK -> white ? "♖" : "♜";
            case BISHOP -> white ? "♗" : "♝";
            case KNIGHT -> white ? "♘" : "♞";
            case PAWN -> white ? "♙" : "♟";
        };
    }
}

enum PieceType {
    KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
}
