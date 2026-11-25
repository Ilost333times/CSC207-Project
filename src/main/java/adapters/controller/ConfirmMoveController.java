package adapters.controller;

import entity.board.BoardPosition;
import usecase.confirmmove.ConfirmMoveInputBoundary;
import usecase.confirmmove.ConfirmMoveInputData;

import java.util.Objects;
import java.util.Set;

public class ConfirmMoveController {
    private final ConfirmMoveInputBoundary input;

    public ConfirmMoveController(ConfirmMoveInputBoundary input) {
        this.input = Objects.requireNonNull(input, "Confirm Move input boundary is required.");
    }

    public void onConfirmMoveClicked(int sourceX, int sourceY, int destinationX, int destinationY) {
        input.confirmMove(new ConfirmMoveInputData(sourceX, sourceY, destinationX, destinationY));
    }

    public Set<BoardPosition> getValidDestinations(int sourceX, int sourceY) {
        return input.getValidDestinations(sourceX, sourceY);
    }
}
