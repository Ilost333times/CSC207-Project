package entity;

import entity.board.Board;
import entity.card.Card;
import entity.card.CharacterCard;
import entity.deck.Deck;
import entity.deck.DeckFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Game {
    private Board board;
    private HashMap<Player, Deck> drawPiles;
    private HashMap<Player, Deck> discardPiles;
    private HashMap<Player, Deck> handPiles;
    private final ArrayList<Player> players;
    private int turn;
    private int maxTurn;

    /// Initializes a new Game
    /// @param playerNames An ArrayList of the names of the players that will be playing this game
    public Game(ArrayList<Player> playerNames) {
        board = new Board();
        drawPiles = new HashMap<>();
        discardPiles = new HashMap<>();
        handPiles = new HashMap<>();

        this.players = new ArrayList<>(playerNames);

        turn = 0;
        maxTurn = playerNames.size();

        for (Player player : playerNames) {
            discardPiles.put(player, new Deck());
            handPiles.put(player, new Deck());

            // The only non-empty deck should be the draw pile deck
            drawPiles.put(player, DeckFactory.createDeck(player));
        }
    }

    public void dealInitialHands(int cardsPerPlayer) {
        for (Player player : players) {
            Deck draw = drawPiles.get(player);
            Deck hand = handPiles.get(player);
            for (int i = 0; i < cardsPerPlayer && !draw.isEmpty(); i++) {
                hand.addCard(draw.drawCard());
            }
        }
    }

    public GameResult finish() {
        List<Player> players = new ArrayList<>(drawPiles.keySet());
        Player p1 = players.get(0);
        Player p2 = players.get(1);

        int p1Score = board.calculateScore(p1);
        int p2Score = board.calculateScore(p2);

        String winnerName;
        if (p1Score > p2Score) winnerName = p1.getName();
        else if (p2Score > p1Score) winnerName = p2.getName();
        else winnerName = null;

        return new GameResult(
                p1.getName(), p2.getName(),
                p1Score, p2Score,
                winnerName, LocalDateTime.now()
        );
    }
    /// Ends the current turn
    /// TODO: Part of 'End Turn' Use Case
    public void endTurn() {}

    public Board getBoard() { return board; }

    public synchronized Player getCurrentPlayer() {
        return players.isEmpty() ? null : players.get(turn);
    }

    /**
     * Turn lifecycle hook for End Turn to call after its other work succeeds.
     * Does not draw cards, check wins, or implement the End Turn use case.
     * Match actions, including future casts, must synchronize on this Game.
     */
    public synchronized void beginNextTurn() {
        if (players.isEmpty()) {
            throw new IllegalStateException("There are no players in this game.");
        }
        turn = (turn + 1) % maxTurn;
        Player owner = getCurrentPlayer();
        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 6; y++) {
                resetOwnedCharacter(board.getTile(x, y).getCharacterCard(), owner);
            }
        }
        // Include off-board cards so replaying one cannot retain stale action flags.
        for (HashMap<Player, Deck> piles : List.of(drawPiles, handPiles, discardPiles)) {
            for (Deck pile : piles.values()) {
                for (Card card : pile.getCards()) {
                    resetOwnedCharacter(card, owner);
                }
            }
        }
    }

    private void resetOwnedCharacter(Card card, Player owner) {
        if (card instanceof CharacterCard character && character.getOwner() == owner) {
            character.resetTurnActions();
        }
    }

    ///

}
