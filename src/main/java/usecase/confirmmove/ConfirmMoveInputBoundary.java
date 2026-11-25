package usecase.confirmmove;

import entity.board.BoardPosition;

import java.util.Set;

public interface ConfirmMoveInputBoundary {
    void confirmMove(ConfirmMoveInputData inputData);

    /** Authoritative, read-only destinations for future highlighting. Invalid selections return empty. */
    Set<BoardPosition> getValidDestinations(int sourceX, int sourceY);
}
