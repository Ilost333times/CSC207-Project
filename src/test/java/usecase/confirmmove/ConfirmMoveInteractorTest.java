package usecase.confirmmove;

import entity.Game;
import entity.Player;
import entity.board.Board;
import entity.board.BoardPosition;
import entity.card.CharacterCard;
import entity.card.SorceryCard;
import entity.card.SpellCard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfirmMoveInteractorTest {
    private final Player player = new Player("Player 1");
    private final Player opponent = new Player("Player 2");
    private final Game game = new Game(new ArrayList<>(List.of(player, opponent)));
    private final Board board = game.getBoard();
    private final CharacterCard character = new CharacterCard(player, "Aki", 200, 3);
    private final SpellCard spell = new SpellCard(player, "Fireball");
    private final RecordingPresenter presenter = new RecordingPresenter();
    private final ConfirmMoveInteractor interactor = new ConfirmMoveInteractor(game, player, presenter);

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsInvalidRequestsWithoutDestroyingCharacterOrSpells(
            ConfirmMoveInputData request, String expectedReason) {
        placeCharacterAndSpell();
        var attachedSpells = board.getTile(1, 2).getSpells();
        CharacterCard destinationCharacter = new CharacterCard(player, "Kia", 200, 3);
        board.playCard(3, 2, destinationCharacter);

        assertRejected(request, expectedReason);
        assertSame(character, board.getTile(1, 2).getCharacterCard());
        assertSame(attachedSpells, board.getTile(1, 2).getSpells());
        assertEquals(List.of(spell), attachedSpells);
        assertSame(destinationCharacter, board.getTile(3, 2).getCharacterCard());
    }

    static Stream<Arguments> invalidRequests() {
        String boundsFailure = "Source and destination must be within the board.";
        return Stream.of(
                Arguments.of(null, "A move request is required."),
                Arguments.of(new ConfirmMoveInputData(1, 2, 1, 2),
                        "Source and destination must be different tiles."),
                Arguments.of(new ConfirmMoveInputData(-1, 2, 3, 2), boundsFailure),
                Arguments.of(new ConfirmMoveInputData(6, 2, 3, 2), boundsFailure),
                Arguments.of(new ConfirmMoveInputData(1, -1, 3, 2), boundsFailure),
                Arguments.of(new ConfirmMoveInputData(1, 6, 3, 2), boundsFailure),
                Arguments.of(new ConfirmMoveInputData(1, 2, -1, 2), boundsFailure),
                Arguments.of(new ConfirmMoveInputData(1, 2, 6, 2), boundsFailure),
                Arguments.of(new ConfirmMoveInputData(1, 2, 3, -1), boundsFailure),
                Arguments.of(new ConfirmMoveInputData(1, 2, 3, 6), boundsFailure),
                Arguments.of(new ConfirmMoveInputData(Integer.MIN_VALUE, 2, 3, 2), boundsFailure),
                Arguments.of(new ConfirmMoveInputData(1, 2, Integer.MAX_VALUE, 2), boundsFailure)
        );
    }

    @Test
    void rejectsEmptySourceWithoutChangingEitherTile() {
        assertRejected(new ConfirmMoveInputData(1, 2, 3, 2), "The source tile must contain a character.");
        assertTrue(board.getTile(1, 2).isEmpty());
        assertTrue(board.getTile(3, 2).isEmpty());
    }

    @Test
    void rejectsSorceryOnlySourceWithoutRemovingIt() {
        SorceryCard sorcery = new SorceryCard(player, "Mirror", true);
        board.playCard(1, 2, sorcery);

        assertRejected(new ConfirmMoveInputData(1, 2, 3, 2), "The source tile must contain a character.");
        assertSame(sorcery, board.getTile(1, 2).getSorceryCard());
        assertTrue(board.getTile(3, 2).isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"2, 2, 3, 2", "2, 2, 1, 2", "2, 2, 2, 3", "2, 2, 2, 1",
            "0, 0, 3, 0", "5, 5, 2, 5", "0, 5, 0, 2", "5, 0, 5, 3", "1, 1, 2, 3"})
    void newlyPlayedCharacterMovesInAllDirectionsAndAroundCorners(
            int sourceX, int sourceY, int destinationX, int destinationY) {
        board.playCard(sourceX, sourceY, character);
        board.playCard(sourceX, sourceY, spell);

        assertFalse(character.hasMovedThisTurn());
        assertFalse(character.hasCastThisTurn());
        assertSuccessfulMove(sourceX, sourceY, destinationX, destinationY);
    }

    @Test
    void movesAroundCharacterObstacleUsingAnExactSpeedPath() {
        CharacterCard fast = new CharacterCard(player, "Cel", 150, 4);
        board.playCard(1, 2, fast);
        board.playCard(2, 2, new CharacterCard(opponent, "Kia", 200, 3));

        interactor.confirmMove(new ConfirmMoveInputData(1, 2, 3, 2));

        assertEquals(List.of(new ConfirmMoveOutputData(1, 2, 3, 2)), presenter.successes);
        assertTrue(presenter.failures.isEmpty());
        assertSame(fast, board.getTile(3, 2).getCharacterCard());
        assertTrue(fast.hasMovedThisTurn());
        assertTrue(board.getTile(1, 2).isEmpty());
    }

    @Test
    void rejectsDetourLongerThanSpeedEvenWhenManhattanDistanceFits() {
        placeCharacterAndSpell();
        board.playCard(2, 2, new CharacterCard(opponent, "Kia", 200, 3));

        assertRejected(new ConfirmMoveInputData(1, 2, 3, 2),
                "Destination is not reachable within the character's Speed.");
    }

    @Test
    void rejectsDistanceGreaterThanSpeed() {
        placeCharacterAndSpell();
        assertRejected(new ConfirmMoveInputData(1, 2, 5, 2),
                "Destination is not reachable within the character's Speed.");
    }

    @Test
    void rejectsCompletelyBlockedCharacterAndHighlightsNothing() {
        placeCharacterAndSpell();
        for (BoardPosition position : List.of(new BoardPosition(0, 2), new BoardPosition(2, 2),
                new BoardPosition(1, 1), new BoardPosition(1, 3))) {
            board.playCard(position.x(), position.y(), new CharacterCard(opponent, "Kia", 200, 3));
        }
        assertTrue(interactor.getValidDestinations(1, 2).isEmpty());
        assertRejected(new ConfirmMoveInputData(1, 2, 3, 2),
                "Destination is not reachable within the character's Speed.");
    }

    @Test
    void preventsMovingTwiceAndRetainsAttachedSpellInstances() {
        placeCharacterAndSpell();
        SpellCard secondSpell = new SpellCard(opponent, "Magic Missiles");
        board.playCard(1, 2, secondSpell);
        assertSuccessfulMove(1, 2, 2, 2);
        assertEquals(List.of(spell, secondSpell), board.getTile(2, 2).getSpells());
        assertTrue(interactor.getValidDestinations(2, 2).isEmpty());
        assertRejected(new ConfirmMoveInputData(2, 2, 3, 2), "A character can move at most once per turn.");
    }

    @Test
    void preventsMovementAfterCasting() {
        placeCharacterAndSpell();
        character.recordCast();
        assertTrue(interactor.getValidDestinations(1, 2).isEmpty());
        assertRejected(new ConfirmMoveInputData(1, 2, 2, 2), "A character cannot move after casting.");
    }

    @Test
    void permitsCastingAfterMovement() {
        placeCharacterAndSpell();
        assertSuccessfulMove(1, 2, 2, 2);
        character.recordCast();
        assertTrue(character.hasMovedThisTurn());
        assertTrue(character.hasCastThisTurn());
    }

    @Test
    void canMoveAgainOnlyWhenOwnersNextTurnBegins() {
        placeCharacterAndSpell();
        assertSuccessfulMove(1, 2, 2, 2);
        character.recordCast();
        game.beginNextTurn();
        assertTrue(character.hasMovedThisTurn());
        assertTrue(character.hasCastThisTurn());
        assertRejected(new ConfirmMoveInputData(2, 2, 3, 2), "Only the current player can move.");

        game.beginNextTurn();

        assertFalse(character.hasMovedThisTurn());
        assertFalse(character.hasCastThisTurn());
        presenter.failures.clear();
        assertSuccessfulMove(2, 2, 3, 2);
    }

    @Test
    void rejectsGameWithoutAnActivePlayerInsteadOfBypassingTurnChecks() {
        Game emptyGame = new Game(new ArrayList<>());
        ConfirmMoveInteractor withoutTurn = new ConfirmMoveInteractor(emptyGame, player, presenter);
        CharacterCard card = new CharacterCard(player, "Aki", 200, 3);
        emptyGame.getBoard().playCard(1, 2, card);

        withoutTurn.confirmMove(new ConfirmMoveInputData(1, 2, 2, 2));

        assertFailureOnly("Only the current player can move.");
        assertSame(card, emptyGame.getBoard().getTile(1, 2).getCharacterCard());
        assertTrue(emptyGame.getBoard().getTile(2, 2).isEmpty());
        assertFalse(card.hasMovedThisTurn());
        assertTrue(withoutTurn.getValidDestinations(1, 2).isEmpty());
    }

    @Test
    void rejectsOpponentsCharacter() {
        board.playCard(1, 2, new CharacterCard(opponent, "Kia", 200, 3));
        assertTrue(interactor.getValidDestinations(1, 2).isEmpty());
        assertRejected(new ConfirmMoveInputData(1, 2, 2, 2), "You can only move your own character.");
    }

    @Test
    void rejectsRequestWhenInjectedActorIsNotCurrentPlayer() {
        placeCharacterAndSpell();
        game.beginNextTurn();
        assertTrue(interactor.getValidDestinations(1, 2).isEmpty());
        assertRejected(new ConfirmMoveInputData(1, 2, 2, 2), "Only the current player can move.");
    }

    @Test
    void rejectsReconstructedPlayerWithSameName() {
        placeCharacterAndSpell();
        ConfirmMoveInteractor untrusted = new ConfirmMoveInteractor(game, new Player(player.getName()), presenter);
        var before = snapshot();
        untrusted.confirmMove(new ConfirmMoveInputData(1, 2, 2, 2));
        assertFailureOnly("Only the current player can move.");
        assertEquals(before, snapshot());
        assertTrue(untrusted.getValidDestinations(1, 2).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"character", "sorcery", "spells"})
    void rejectsOccupiedDestinationsConservatively(String contents) {
        placeCharacterAndSpell();
        switch (contents) {
            case "character" -> board.playCard(2, 2, new CharacterCard(opponent, "Kia", 200, 3));
            case "sorcery" -> board.playCard(2, 2, new SorceryCard(opponent, "Mirror", true));
            case "spells" -> board.getTile(2, 2).addSpell(new SpellCard(opponent, "Fireball"));
        }
        assertFalse(interactor.getValidDestinations(1, 2).contains(new BoardPosition(2, 2)));
        assertRejected(new ConfirmMoveInputData(1, 2, 2, 2),
                "Target space is occupied; sorcery-only and spell-only tiles are unavailable pending rule confirmation.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"KO", "revive", "heal"})
    void conservativelyRejectsKoAndRevivalUntilConfirmed(String state) {
        placeCharacterAndSpell();
        character.takeDamage(200);
        if (state.equals("revive")) character.revive();
        if (state.equals("heal")) character.takeDamage(-100);
        assertTrue(interactor.getValidDestinations(1, 2).isEmpty());
        assertRejected(new ConfirmMoveInputData(1, 2, 2, 2),
                "Movement for KO'd or revived characters awaits rule confirmation.");
    }

    @Test
    void conservativelyRejectsMixedCharacterAndSorcerySource() {
        placeCharacterAndSpell();
        board.getTile(1, 2).setSorceryCard(new SorceryCard(player, "Mirror", true));
        assertTrue(interactor.getValidDestinations(1, 2).isEmpty());
        assertRejected(new ConfirmMoveInputData(1, 2, 2, 2),
                "Movement from a tile containing both a character and sorcery awaits rule confirmation.");
    }

    @ParameterizedTest
    @MethodSource("allDestinations")
    void highlightedDestinationsExactlyMatchAcceptedMoves(int x, int y) {
        board.playCard(2, 2, character);
        board.playCard(2, 1, new CharacterCard(opponent, "Kia", 200, 3));
        board.playCard(3, 2, new CharacterCard(opponent, "Kia", 200, 3));
        var before = snapshot();
        Set<BoardPosition> destinations = interactor.getValidDestinations(2, 2);
        assertEquals(before, snapshot());
        assertThrows(UnsupportedOperationException.class, () -> destinations.add(new BoardPosition(5, 5)));

        interactor.confirmMove(new ConfirmMoveInputData(2, 2, x, y));

        if (destinations.contains(new BoardPosition(x, y))) {
            assertEquals(List.of(new ConfirmMoveOutputData(2, 2, x, y)), presenter.successes);
            assertTrue(presenter.failures.isEmpty());
            assertSame(character, board.getTile(x, y).getCharacterCard());
            assertTrue(character.hasMovedThisTurn());
        } else {
            assertEquals(1, presenter.failures.size());
            assertTrue(presenter.successes.isEmpty());
            assertEquals(before, snapshot());
        }
    }

    static Stream<Arguments> allDestinations() {
        return IntStream.range(0, 6).boxed().flatMap(x ->
                IntStream.range(0, 6).mapToObj(y -> Arguments.of(x, y)));
    }

    @Test
    void invalidOrEmptyHighlightSelectionReturnsEmptyWithoutCallbacks() {
        assertTrue(interactor.getValidDestinations(-1, 2).isEmpty());
        assertTrue(interactor.getValidDestinations(6, 2).isEmpty());
        assertTrue(interactor.getValidDestinations(1, 2).isEmpty());
        assertTrue(presenter.successes.isEmpty());
        assertTrue(presenter.failures.isEmpty());
    }

    private void assertSuccessfulMove(int sourceX, int sourceY, int destinationX, int destinationY) {
        var spells = board.getTile(sourceX, sourceY).getSpells();
        interactor.confirmMove(new ConfirmMoveInputData(sourceX, sourceY, destinationX, destinationY));
        assertEquals(List.of(new ConfirmMoveOutputData(sourceX, sourceY, destinationX, destinationY)), presenter.successes);
        assertTrue(presenter.failures.isEmpty());
        assertSame(character, board.getTile(destinationX, destinationY).getCharacterCard());
        assertSame(spells, board.getTile(destinationX, destinationY).getSpells());
        assertTrue(board.getTile(sourceX, sourceY).isEmpty());
        assertNull(board.getTile(sourceX, sourceY).getSpells());
        assertTrue(character.hasMovedThisTurn());
    }

    private void assertRejected(ConfirmMoveInputData request, String reason) {
        var before = snapshot();
        Player currentPlayer = game.getCurrentPlayer();
        presenter.successes.clear();
        presenter.failures.clear();
        interactor.confirmMove(request);
        assertFailureOnly(reason);
        var after = snapshot();
        assertEquals(before, after);
        for (int index = 0; index < before.size(); index++) {
            assertSame(before.get(index).spellReference(), after.get(index).spellReference());
        }
        assertSame(currentPlayer, game.getCurrentPlayer());
    }

    private List<TileState> snapshot() {
        List<TileState> states = new ArrayList<>();
        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 6; y++) {
                var tile = board.getTile(x, y);
                var card = tile.getCharacterCard();
                states.add(new TileState(card, tile.getSpells(),
                        tile.getSpells() == null ? null : List.copyOf(tile.getSpells()), tile.getSorceryCard(),
                        card != null && card.hasMovedThisTurn(), card != null && card.hasCastThisTurn(),
                        card == null ? 0 : card.getHP()));
            }
        }
        return states;
    }

    private record TileState(CharacterCard character, List<SpellCard> spellReference, List<SpellCard> spells,
                             SorceryCard sorcery, boolean moved, boolean cast, int hp) { }

    private void placeCharacterAndSpell() {
        board.playCard(1, 2, character);
        board.playCard(1, 2, spell);
    }

    private void assertFailureOnly(String expectedReason) {
        assertEquals(List.of(expectedReason), presenter.failures);
        assertTrue(presenter.successes.isEmpty());
    }

    private static class RecordingPresenter implements ConfirmMoveOutputBoundary {
        private final List<String> failures = new ArrayList<>();
        private final List<ConfirmMoveOutputData> successes = new ArrayList<>();

        @Override
        public void presentMoveConfirmed(ConfirmMoveOutputData outputData) {
            successes.add(outputData);
        }

        @Override
        public void presentMoveFailure(String reason) {
            failures.add(reason);
        }
    }
}
