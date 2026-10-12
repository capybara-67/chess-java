import java.util.List;

public class ChessTest {
    static long perft(ChessGame game, int depth) {
        if (depth == 0) return 1;
        long positions = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                for (Move move : game.getLegalMoves(row, col)) {
                    ChessGame next = new ChessGame(game);
                    next.makeMove(move);
                    positions += perft(next, depth - 1);
                }
            }
        }
        return positions;
    }
    static void play(ChessGame game, String from, String to) {
        int r = 8 - Character.digit(from.charAt(1), 10), c = from.charAt(0) - 'a';
        int nr = 8 - Character.digit(to.charAt(1), 10), nc = to.charAt(0) - 'a';
        Move chosen = game.getLegalMoves(r, c).stream().filter(m -> m.toRow == nr && m.toCol == nc).findFirst().orElseThrow(() -> new AssertionError(from + to));
        game.makeMove(chosen);
    }
    static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        ChessGame initial = new ChessGame();
        expect(perft(initial, 1) == 20, "Initial legal moves");
        expect(perft(initial, 2) == 400, "Depth-two legal positions");
        expect(perft(initial, 3) == 8902, "Depth-three legal positions");
        ChessGame mate = new ChessGame();
        play(mate, "f2", "f3"); play(mate, "e7", "e5"); play(mate, "g2", "g4"); play(mate, "d8", "h4");
        expect(mate.isGameOver() && mate.statusText().contains("Black"), "Fool's mate");
        ChessGame ep = new ChessGame();
        play(ep, "e2", "e4"); play(ep, "a7", "a6"); play(ep, "e4", "e5"); play(ep, "d7", "d5");
        ChessGame beforeCapture = new ChessGame(ep);
        play(ep, "e5", "d6");
        expect(ep.getPiece(3, 3) == null && ep.getPiece(2, 3).type == PieceType.PAWN, "En passant removes captured pawn");
        expect(beforeCapture.getPiece(3, 3) != null, "Snapshot preserves earlier board");
        ChessGame castle = new ChessGame();
        play(castle, "e2", "e4"); play(castle, "e7", "e5"); play(castle, "g1", "f3"); play(castle, "b8", "c6");
        play(castle, "f1", "c4"); play(castle, "g8", "f6"); play(castle, "e1", "g1");
        expect(castle.getPiece(7, 6).type == PieceType.KING && castle.getPiece(7, 5).type == PieceType.ROOK, "Castling moves both pieces");
        System.out.println("Passed perft 20 / 400 / 8902, checkmate, en passant, snapshot isolation and castling.");
    }
}
