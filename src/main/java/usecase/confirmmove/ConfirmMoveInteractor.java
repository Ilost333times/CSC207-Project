package usecase.confirmmove;

import entity.Game;
import entity.Player;
import entity.board.Board;
import entity.board.BoardPosition;
import entity.card.CharacterCard;

import java.util.Objects;
import java.util.Set;

/**
 * Confirms a move for a canonical player supplied by trusted application/session wiring.
 * Validation and mutation share the Game monitor with the turn lifecycle hook.
 */
public class ConfirmMoveInteractor implements ConfirmMoveInputBoundary {
    private final Game game;
    private final Player actingPlayer;
    private final Board board;
    private final ConfirmMoveOutputBoundary presenter;

    public ConfirmMoveInteractor(Game game, Player actingPlayer, ConfirmMoveOutputBoundary presenter) {
        this.game = Objects.requireNonNull(game);
        this.actingPlayer = Objects.requireNonNull(actingPlayer);
        this.board = game.getBoard();
        this.presenter = Objects.requireNonNull(presenter);
    }

    @Override
    public void confirmMove(ConfirmMoveInputData inputData) {
        String failure;
        synchronized (game) {
            failure = validateMove(inputData);
            if (failure == null) {
                CharacterCard character = board.getTile(inputData.sourceX(), inputData.sourceY()).getCharacterCard();
                board.moveTile(inputData.sourceX(), inputData.sourceY(), inputData.destinationX(), inputData.destinationY());
                character.recordMove();
            }
        }
        // Keep external callbacks outside the match lock and emit exactly one outcome.
        if (failure != null) {
            presenter.presentMoveFailure(failure);
        } else {
            presenter.presentMoveConfirmed(new ConfirmMoveOutputData(inputData.sourceX(), inputData.sourceY(),
                    inputData.destinationX(), inputData.destinationY()));
        }
    }

    @Override
    public Set<BoardPosition> getValidDestinations(int sourceX, int sourceY) {
        synchronized (game) {
            if (!board.isInBounds(sourceX, sourceY) || sourceFailure(sourceX, sourceY) != null) {
                return Set.of();
            }
            return board.getReachableDestinations(sourceX, sourceY,
                    board.getTile(sourceX, sourceY).getCharacterCard().getSpeed());
        }
    }

    private String validateMove(ConfirmMoveInputData inputData) {
        if (inputData == null) {
            return "A move request is required.";
        }
        if (!board.isInBounds(inputData.sourceX(), inputData.sourceY())
                || !board.isInBounds(inputData.destinationX(), inputData.destinationY())) {
            return "Source and destination must be within the board.";
        }
        if (inputData.sourceX() == inputData.destinationX()
                && inputData.sourceY() == inputData.destinationY()) {
            return "Source and destination must be different tiles.";
        }
        String failure = sourceFailure(inputData.sourceX(), inputData.sourceY());
        if (failure != null) {
            return failure;
        }
        if (!board.isMovementDestinationEmpty(inputData.destinationX(), inputData.destinationY())) {
            return "Target space is occupied; sorcery-only and spell-only tiles are unavailable pending rule confirmation.";
        }
        if (!getValidDestinations(inputData.sourceX(), inputData.sourceY())
                .contains(new BoardPosition(inputData.destinationX(), inputData.destinationY()))) {
            return "Destination is not reachable within the character's Speed.";
        }
        return null;
    }

    private String sourceFailure(int sourceX, int sourceY) {
        if (game.getCurrentPlayer() == null || game.getCurrentPlayer() != actingPlayer) {
            return "Only the current player can move.";
        }
        CharacterCard character = board.getTile(sourceX, sourceY).getCharacterCard();
        if (character == null) {
            return "The source tile must contain a character.";
        }
        if (character.getOwner() != actingPlayer) {
            return "You can only move your own character.";
        }
        if (character.getHP() <= 0 || character.hasBeenKnockedOut()) {
            return "Movement for KO'd or revived characters awaits rule confirmation.";
        }
        if (board.getTile(sourceX, sourceY).getSorceryCard() != null) {
            return "Movement from a tile containing both a character and sorcery awaits rule confirmation.";
        }
        if (character.hasCastThisTurn()) {
            return "A character cannot move after casting.";
        }
        if (character.hasMovedThisTurn()) {
            return "A character can move at most once per turn.";
        }
        return null;
    }
}
