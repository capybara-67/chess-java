# Chess in Java

A two-player chess game built with Java Swing. Select a piece to see its legal moves, then select a destination. The board supports castling, en passant, pawn promotion, checkmate, stalemate, and undo.

## Run

Requires JDK 17 or newer.

```sh
mkdir -p build
javac -d build src/Main32.java
java -cp build Main32
```

Use arrow keys to move keyboard focus between squares and Space to select a piece or destination. Escape clears the selected piece. Closing the promotion chooser cancels that move.

## Check the rules

```sh
javac -d build src/Main32.java tests/ChessTest.java
java -cp build ChessTest
```

The headless checks cover 20, 400, and 8,902 legal positions from the opening at depths one through three, Fool's Mate, en passant, castling, and board snapshot isolation.

## Design

`Main32` renders the board. `ChessGame` generates candidate moves and rejects those that leave the moving side in check. Undo restores a copied game state, including castling rights and the en passant square. Every square has a coordinate tooltip and an accessible piece label.

This is a local class project. It has no engine opponent, network play, clocks, repetition detection, or fifty-move draw handling.
