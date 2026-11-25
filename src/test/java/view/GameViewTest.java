package view;

import adapters.controller.ConfirmMoveController;
import adapters.viewmodel.GameViewModel;
import adapters.viewmodel.ViewState;
import app.ClientAppBuilder;
import entity.Game;
import entity.Player;
import entity.board.BoardPosition;
import entity.card.CharacterCard;
import entity.card.SpellCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import usecase.confirmmove.ConfirmMoveInputBoundary;
import usecase.confirmmove.ConfirmMoveInputData;
import usecase.confirmmove.ConfirmMoveInteractor;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Component tests run on the EDT without opening windows or using native mouse interaction. */
class GameViewTest {
    private Player player;
    private Player opponent;
    private Game game;
    private CharacterCard character;
    private CharacterCard otherCharacter;
    private ClientAppBuilder.LocalGame local;
    private GameView view;

    @BeforeEach
    void buildLocalMatch() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            player = new Player("Player 1");
            opponent = new Player("Player 2");
            game = new Game(new ArrayList<>(List.of(player, opponent)));
            character = new CharacterCard(player, "Aki", 200, 3);
            otherCharacter = new CharacterCard(player, "Cel", 150, 4);
            game.getBoard().playCard(1, 2, character);
            game.getBoard().playCard(1, 2, new SpellCard(player, "Fireball"));
            game.getBoard().playCard(4, 4, otherCharacter);
            game.getBoard().playCard(2, 2, new CharacterCard(opponent, "Kia", 200, 3));
            var model = new GameViewModel("Player 1's turn", 57, GameViewModel.emptyBoard(),
                    List.of(new GameViewModel.CardViewData("Hand card", GameViewModel.CardType.SPELL, "Spell")),
                    31, 2, new GameViewModel.CardViewData("Discard", GameViewModel.CardType.SORCERY, "Sorcery"));
            local = new ClientAppBuilder().buildLocalGame(game, player, new ViewState<>(model));
            view = local.view();
        });
    }

    @Test
    void selectingCharacterHighlightsExactlyTheAuthoritativeDestinations() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            tile(1, 2).doClick(0);
            var expected = new ConfirmMoveInteractor(game, player, local.presenter()).getValidDestinations(1, 2);
            for (int x = 0; x < 6; x++) {
                for (int y = 0; y < 6; y++) {
                    assertEquals(expected.contains(new BoardPosition(x, y)), highlighted(x, y));
                }
            }
            assertTrue(highlighted(1, 3));
            assertFalse(highlighted(2, 2));
            assertFalse(confirm().isEnabled());
            assertTrue(cancel().isEnabled());
            assertTrue(feedback().contains("B3"));
            assertFalse(character.hasMovedThisTurn());
        });
    }

    @Test
    void ignoresInvalidDestinationClicksWithoutChangingTheChosenTarget() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            tile(1, 2).doClick(0);
            tile(5, 5).doClick(0);
            assertFalse(confirm().isEnabled());
            tile(1, 3).doClick(0);
            tile(5, 5).doClick(0);
            assertTrue(confirm().isEnabled());
            confirm().doClick(0);
            assertSame(character, game.getBoard().getTile(1, 3).getCharacterCard());
            assertNull(game.getBoard().getTile(5, 5).getCharacterCard());
        });
    }

    @Test
    void cancelClearsSelectionWithoutMovingAnything() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            selectTarget();
            cancel().doClick(0);
            assertSelectionCleared();
            assertSame(character, game.getBoard().getTile(1, 2).getCharacterCard());
            assertFalse(character.hasMovedThisTurn());
            assertEquals("Move canceled.", feedback());
        });
    }

    @Test
    void clickingSelectedSourceCancels() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            selectTarget();
            tile(1, 2).doClick(0);
            assertSelectionCleared();
        });
    }

    @Test
    void changingSourceReplacesHighlightsAndRequiresANewTarget() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            selectTarget();
            tile(4, 4).doClick(0);
            assertFalse(confirm().isEnabled());
            assertFalse(highlighted(0, 2));
            assertTrue(highlighted(4, 5));
            tile(4, 5).doClick(0);
            confirm().doClick(0);
            assertSame(otherCharacter, game.getBoard().getTile(4, 5).getCharacterCard());
            assertFalse(character.hasMovedThisTurn());
        });
    }

    @Test
    void successfulConfirmationUpdatesBoardSpellsFeedbackAndPreservesOtherInformation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameViewModel before = local.state().getState();
            selectTarget();
            confirm().doClick(0);
            GameViewModel after = local.state().getState();
            assertNull(after.tileAt(1, 2).occupant());
            assertEquals("Aki", after.tileAt(1, 3).occupant().name());
            assertEquals("Fireball", after.tileAt(1, 3).spells().getFirst().name());
            assertTrue(tile(1, 2).getAccessibleContext().getAccessibleName().contains("Empty"));
            assertTrue(tile(1, 3).getAccessibleContext().getAccessibleName().contains("Aki"));
            assertEquals("Moved B3 to B4.", feedback());
            assertEquals(before.gameStatus(), after.gameStatus());
            assertEquals(before.hand(), after.hand());
            assertEquals(before.drawCount(), after.drawCount());
            assertEquals(before.discardCount(), after.discardCount());
            assertEquals(before.topDiscard(), after.topDiscard());
            assertEquals(before.remainingTurnSeconds(), after.remainingTurnSeconds());
            assertSelectionCleared();
        });
    }

    @Test
    void changedDestinationIsRevalidatedAndRejectionIsDisplayed() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            selectTarget();
            CharacterCard blocker = new CharacterCard(opponent, "Kia", 200, 3);
            synchronized (game) {
                game.getBoard().playCard(1, 3, blocker);
            }
            // Deliberately leave old highlights in place to exercise final authoritative validation.
            confirm().doClick(0);
            assertTrue(feedback().contains("occupied"));
            assertSame(character, game.getBoard().getTile(1, 2).getCharacterCard());
            assertSame(blocker, game.getBoard().getTile(1, 3).getCharacterCard());
            assertFalse(character.hasMovedThisTurn());
            assertEquals("Kia", local.state().getState().tileAt(1, 3).occupant().name());
            assertSelectionCleared();
        });
    }

    @Test
    void preventsDuplicateAndReentrantSubmissions() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger requests = new AtomicInteger();
            ConfirmMoveInteractor delegate = new ConfirmMoveInteractor(game, player, local.presenter());
            ConfirmMoveInputBoundary boundary = new ConfirmMoveInputBoundary() {
                @Override
                public void confirmMove(ConfirmMoveInputData input) {
                    requests.incrementAndGet();
                    assertFalse(confirm().isEnabled());
                    confirm().doClick(0);
                    delegate.confirmMove(input);
                }

                @Override
                public Set<BoardPosition> getValidDestinations(int x, int y) {
                    return delegate.getValidDestinations(x, y);
                }
            };
            view = new GameView(new ConfirmMoveController(boundary));
            local.state().addPropertyChangeListener(view);
            view.render(local.state().getState());
            selectTarget();
            confirm().doClick(0);
            confirm().doClick(0);
            assertEquals(1, requests.get());
            assertTrue(character.hasMovedThisTurn());
            assertSelectionCleared();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"board", "turn", "cast"})
    void refreshInvalidatesSelectionForRelevantStateChanges(String change) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            selectTarget();
            long revision = local.state().getState().movementRevision();
            synchronized (game) {
                switch (change) {
                    case "board" -> game.getBoard().playCard(5, 5, new CharacterCard(opponent, "Kia", 200, 3));
                    case "turn" -> game.beginNextTurn();
                    case "cast" -> character.recordCast();
                }
            }
            local.presenter().refreshGameView();
            assertTrue(local.state().getState().movementRevision() > revision);
            assertSelectionCleared();
            assertFalse(character.hasMovedThisTurn());
        });
    }

    @Test
    void timerTickDoesNotInvalidateSelectionOrOverwriteItsInstructions() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            selectTarget();
            String instructions = feedback();
            local.state().setState(local.state().getState().withRemainingTurnSeconds(56));
            assertTrue(confirm().isEnabled());
            assertTrue(highlighted(1, 3));
            assertEquals(instructions, feedback());
        });
    }

    @Test
    void statusChangeInvalidatesSelectionWithoutChangingTheBoard() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            selectTarget();
            local.state().setState(local.state().getState().withGameStatus("Player 2's turn"));
            assertSelectionCleared();
        });
    }

    @Test
    void backgroundStateNotificationIsRenderedOnTheEventThread() throws Exception {
        SwingUtilities.invokeAndWait(this::selectTarget);
        local.state().setState(local.state().getState().withMovementUpdate(local.state().getState().board(), "State changed"));
        SwingUtilities.invokeAndWait(() -> {
            assertSelectionCleared();
            assertEquals("State changed", feedback());
        });
    }

    @Test
    void hoverInspectionIsIndependentOfMovementSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JButton button = tile(1, 2);
            var event = new MouseEvent(button, MouseEvent.MOUSE_ENTERED, 0, 0, 2, 2, 0, false);
            for (var listener : button.getMouseListeners()) listener.mouseEntered(event);
            assertTrue(component(view, "card-preview", JComponent.class).getAccessibleContext()
                    .getAccessibleName().contains("Aki"));
            assertFalse(cancel().isEnabled());
            assertFalse(character.hasMovedThisTurn());
        });
    }

    @Test
    void opponentSelectionHasNoMovementHighlights() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            tile(2, 2).doClick(0);
            assertSelectionCleared();
            assertTrue(feedback().contains("No movement destinations"));
        });
    }

    private void selectTarget() {
        tile(1, 2).doClick(0);
        tile(1, 3).doClick(0);
        assertTrue(confirm().isEnabled());
    }

    private void assertSelectionCleared() {
        assertFalse(confirm().isEnabled());
        assertFalse(cancel().isEnabled());
        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 6; y++) assertFalse(highlighted(x, y));
        }
    }

    private JButton tile(int x, int y) {
        return component(view, "tile-" + (char) ('A' + x) + (y + 1), JButton.class);
    }

    private JButton confirm() { return component(view, "confirm-move", JButton.class); }
    private JButton cancel() { return component(view, "cancel-move", JButton.class); }
    private String feedback() { return component(view, "move-feedback", JLabel.class).getText(); }
    private boolean highlighted(int x, int y) {
        return "Highlighted tile".equals(tile(x, y).getAccessibleContext().getAccessibleDescription());
    }

    private static <T extends Component> T component(Component root, String name, Class<T> type) {
        if (name.equals(root.getName()) && type.isInstance(root)) return type.cast(root);
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = component(child, name, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
