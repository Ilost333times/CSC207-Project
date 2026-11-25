package adapters.presenter;

import adapters.viewmodel.GameViewModel;
import adapters.viewmodel.ViewState;
import entity.Game;
import entity.GameResult;
import entity.Player;
import entity.card.CharacterCard;
import entity.card.SpellCard;
import org.junit.jupiter.api.Test;
import usecase.confirmmove.ConfirmMoveInputData;
import usecase.confirmmove.ConfirmMoveInteractor;
import usecase.startgame.StartGameInteractor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GamePresenterTest {
    private final Player player = new Player("Player 1");
    private final Player opponent = new Player("Player 2");
    private final Game game = new Game(new ArrayList<>(List.of(player, opponent)));
    private final ViewState<GameViewModel> state = new ViewState<>(new GameViewModel("Player 1's turn", 42,
            GameViewModel.emptyBoard(), List.of(new GameViewModel.CardViewData("Hand", GameViewModel.CardType.SPELL, "Spell")),
            30, 2, new GameViewModel.CardViewData("Discard", GameViewModel.CardType.SORCERY, "Sorcery")));
    private final GamePresenter presenter = new GamePresenter(state, player);

    @Test
    void publishesImmutableBoardInColumnRowCoordinatesAndCorrectPlayerPerspective() {
        CharacterCard card = new CharacterCard(player, "Aki", 200, 3);
        game.getBoard().playCard(1, 3, card);
        game.getBoard().playCard(1, 3, new SpellCard(opponent, "Fireball"));
        game.getBoard().playCard(4, 2, new CharacterCard(opponent, "Kia", 200, 3));

        presenter.presentGameStarted(game);

        var snapshot = state.getState();
        assertSame(game, presenter.getCurrentGame());
        assertEquals("Aki", snapshot.tileAt(1, 3).occupant().name());
        assertEquals("Fireball", snapshot.tileAt(1, 3).spells().getFirst().name());
        assertNull(snapshot.tileAt(3, 1).occupant());
        assertFalse(snapshot.tileAt(1, 3).opponentControlled());
        assertTrue(snapshot.tileAt(4, 2).opponentControlled());
        assertThrows(UnsupportedOperationException.class, snapshot.board()::clear);
        assertThrows(UnsupportedOperationException.class, snapshot.tileAt(1, 3).spells()::clear);
        card.takeDamage(100);
        game.getBoard().getTile(1, 3).clearTile();
        assertEquals(200, snapshot.tileAt(1, 3).occupant().currentHp());
        assertEquals("Fireball", snapshot.tileAt(1, 3).spells().getFirst().name());
    }

    @Test
    void successfulMovePublishesNewBoardAndPreservesUnrelatedState() {
        game.getBoard().playCard(1, 3, new CharacterCard(player, "Aki", 200, 3));
        presenter.presentGameStarted(game);
        var before = state.getState();

        new ConfirmMoveInteractor(game, player, presenter).confirmMove(new ConfirmMoveInputData(1, 3, 2, 3));

        var after = state.getState();
        assertNull(after.tileAt(1, 3).occupant());
        assertEquals("Aki", after.tileAt(2, 3).occupant().name());
        assertEquals("Moved B4 to C4.", after.moveFeedback());
        assertEquals(before.gameStatus(), after.gameStatus());
        assertEquals(before.hand(), after.hand());
        assertEquals(before.drawCount(), after.drawCount());
        assertEquals(before.discardCount(), after.discardCount());
        assertEquals(before.topDiscard(), after.topDiscard());
        assertEquals(before.remainingTurnSeconds(), after.remainingTurnSeconds());
        assertTrue(after.movementRevision() > before.movementRevision());
    }

    @Test
    void repeatedRejectionsPublishFeedbackAndInvalidateSelectionEvenWithUnchangedBoard() {
        presenter.presentGameStarted(game);
        var before = state.getState();
        AtomicInteger notifications = new AtomicInteger();
        state.addPropertyChangeListener(event -> notifications.incrementAndGet());
        presenter.presentMoveFailure("Destination became occupied.");
        presenter.presentMoveFailure("Destination became occupied.");
        assertEquals(2, notifications.get());
        assertEquals(before.board(), state.getState().board());
        assertEquals(before.gameStatus(), state.getState().gameStatus());
        assertEquals("Destination became occupied.", state.getState().moveFeedback());
    }

    @Test
    void originalStartGameAndResultPresentationRemainCompatible() {
        GamePresenter original = new GamePresenter();
        AtomicInteger refreshes = new AtomicInteger();
        original.setOnGameChanged(refreshes::incrementAndGet);
        new StartGameInteractor(original).startGame("First", "Second");
        Game startedGame = original.getCurrentGame();
        assertNotNull(startedGame);
        assertEquals(1, refreshes.get());
        GameResult result = new GameResult("First", "Second", 100, 50, "First", LocalDateTime.now());
        original.presentSaveSuccess("slot");
        original.presentSaveFailure("slot", "Failure");
        original.presentResultLoaded(result);
        original.presentLoadFailure("missing", "Missing");
        original.presentStartFailure("Invalid names");
        assertSame(result, original.getLastLoadedResult());
        assertSame(startedGame, original.getCurrentGame());
        assertEquals(1, refreshes.get());
    }
}
