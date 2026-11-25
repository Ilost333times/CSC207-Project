package entity;

import entity.card.CharacterCard;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {
    @Test
    void nextTurnResetsOnlyIncomingOwnersActionsAndRetainsCanonicalPlayers() {
        Player first = new Player("Player 1");
        Player second = new Player("Player 2");
        Game game = new Game(new ArrayList<>(List.of(first, second)));
        CharacterCard firstCharacter = new CharacterCard(first, "Aki", 200, 3);
        CharacterCard secondCharacter = new CharacterCard(second, "Kia", 200, 3);
        game.getBoard().playCard(0, 0, firstCharacter);
        game.getBoard().playCard(5, 5, secondCharacter);
        firstCharacter.recordMove();
        firstCharacter.recordCast();
        secondCharacter.recordMove();
        secondCharacter.recordCast();
        assertSame(first, game.getCurrentPlayer());

        game.beginNextTurn();

        assertSame(second, game.getCurrentPlayer());
        assertTrue(firstCharacter.hasMovedThisTurn());
        assertTrue(firstCharacter.hasCastThisTurn());
        assertFalse(secondCharacter.hasMovedThisTurn());
        assertFalse(secondCharacter.hasCastThisTurn());
        secondCharacter.recordMove();

        game.beginNextTurn();

        assertSame(first, game.getCurrentPlayer());
        assertFalse(firstCharacter.hasMovedThisTurn());
        assertFalse(firstCharacter.hasCastThisTurn());
        assertTrue(secondCharacter.hasMovedThisTurn());
    }

    @Test
    void finishingAnEmptyMatchReportsADraw() {
        Player first = new Player("Player 1");
        Player second = new Player("Player 2");
        Game game = new Game(new ArrayList<>(List.of(first, second)));

        GameResult result = game.finish();

        assertTrue(result.isDraw());
        assertEquals(0, result.getPlayer1Score());
        assertEquals(0, result.getPlayer2Score());
        assertEquals(Set.of("Player 1", "Player 2"), Set.of(result.getPlayer1Name(), result.getPlayer2Name()));
    }
}
