package usecase.confirmmove;

/** Zero-based board coordinates. Validation belongs to the use case. */
public record ConfirmMoveInputData(int sourceX, int sourceY, int destinationX, int destinationY) {
}
