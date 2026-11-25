package adapters.viewmodel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameViewModelTest {
    @Test
    void movementUpdateCopiesBoardAndRetainsExistingGameInformation() {
        var original = new GameViewModel("Opponent's turn", 50, GameViewModel.emptyBoard(), List.of(), 20, 3, null);
        var board = new ArrayList<>(original.board());
        var card = new GameViewModel.CardViewData("Aki", GameViewModel.CardType.CHARACTER, "Character", 200);
        board.set(GameViewModel.tileIndex(1, 3), new GameViewModel.TileViewData(card, List.of()));
        var updated = original.withMovementUpdate(board, "Moved");
        board.clear();
        assertEquals(36, updated.board().size());
        assertEquals("Aki", updated.tileAt(1, 3).occupant().name());
        assertNull(updated.tileAt(3, 1).occupant());
        assertEquals(original.gameStatus(), updated.gameStatus());
        assertEquals(original.remainingTurnSeconds(), updated.remainingTurnSeconds());
        assertEquals(original.drawCount(), updated.drawCount());
        assertEquals(original.discardCount(), updated.discardCount());
        assertEquals(1, updated.movementRevision());
        assertEquals("Moved", updated.moveFeedback());
        assertNull(original.tileAt(1, 3).occupant());
    }

    @Test
    void timerAndStatusCopiesPreserveMovementFeedbackAndPresentationRevision() {
        var original = GameViewModel.empty().withMovementUpdate(GameViewModel.emptyBoard(), "Rejected");
        var updated = original.withRemainingTurnSeconds(10).withGameStatus("Next turn");
        assertEquals(original.movementRevision(), updated.movementRevision());
        assertEquals("Rejected", updated.moveFeedback());
        assertEquals(10, updated.remainingTurnSeconds());
        assertEquals("Next turn", updated.gameStatus());
    }
}
