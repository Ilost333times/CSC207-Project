package entity.board;

import entity.Player;
import entity.card.CharacterCard;
import entity.card.SorceryCard;
import entity.card.SpellCard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardMovementTest {
    private final Board board = new Board();
    private final Player player = new Player("Player 1");

    @Test
    void oneStepMeansOnlyFourOrthogonalNeighbors() {
        board.playCard(2, 2, new CharacterCard(player, "Aki", 200, 1));
        assertEquals(Set.of(new BoardPosition(1, 2), new BoardPosition(3, 2),
                new BoardPosition(2, 1), new BoardPosition(2, 3)), board.getReachableDestinations(2, 2, 1));
    }

    @Test
    void cornerNeighborsStayOnBoardAndExcludeSource() {
        board.playCard(0, 0, new CharacterCard(player, "Aki", 200, 1));
        assertEquals(Set.of(new BoardPosition(1, 0), new BoardPosition(0, 1)),
                board.getReachableDestinations(0, 0, 1));
    }

    @Test
    void bendsAroundObstacleUsingShortestPathInsteadOfManhattanDistance() {
        board.playCard(1, 2, new CharacterCard(player, "Aki", 200, 4));
        board.playCard(2, 2, new CharacterCard(player, "Kia", 200, 3));
        assertFalse(board.getReachableDestinations(1, 2, 3).contains(new BoardPosition(3, 2)));
        assertTrue(board.getReachableDestinations(1, 2, 4).contains(new BoardPosition(3, 2)));
        assertFalse(board.getReachableDestinations(1, 2, 4).contains(new BoardPosition(2, 2)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sorcery", "spells"})
    void specialOnlyTilesConservativelyBlockTraversalAsWellAsLanding(String contents) {
        board.playCard(0, 0, new CharacterCard(player, "Aki", 200, 3));
        board.playCard(0, 1, new CharacterCard(player, "Kia", 200, 3));
        if (contents.equals("sorcery")) {
            board.playCard(1, 0, new SorceryCard(player, "Mirror", true));
        } else {
            board.getTile(1, 0).addSpell(new SpellCard(player, "Fireball"));
        }
        assertTrue(board.getReachableDestinations(0, 0, 3).isEmpty());
    }

    @Test
    void emptySpellListDoesNotBlockMovement() {
        board.playCard(0, 0, new CharacterCard(player, "Aki", 200, 1));
        SpellCard spell = new SpellCard(player, "Fireball");
        board.getTile(1, 0).addSpell(spell);
        board.getTile(1, 0).removeSpell(spell);
        assertTrue(board.getReachableDestinations(0, 0, 1).contains(new BoardPosition(1, 0)));
    }

    @Test
    void noSourceOrNoSpeedHasNoReachableDestinations() {
        assertTrue(board.getReachableDestinations(0, 0, 3).isEmpty());
        assertTrue(board.getReachableDestinations(-1, 0, 3).isEmpty());
        board.playCard(0, 0, new CharacterCard(player, "Aki", 200, 0));
        assertTrue(board.getReachableDestinations(0, 0, 0).isEmpty());
        assertTrue(board.getReachableDestinations(0, 0, -1).isEmpty());
    }
}
