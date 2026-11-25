package usecase.confirmmove;

/** Coordinates of a successfully completed move; never emitted for a rejected request. */
public record ConfirmMoveOutputData(int sourceX, int sourceY, int destinationX, int destinationY) {
}
