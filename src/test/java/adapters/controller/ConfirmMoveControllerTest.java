package adapters.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import entity.board.BoardPosition;
import usecase.confirmmove.ConfirmMoveInputBoundary;
import usecase.confirmmove.ConfirmMoveInputData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfirmMoveControllerTest {
    @Test
    void rejectsNullInputBoundaryAtConstruction() {
        assertThrows(NullPointerException.class, () -> new ConfirmMoveController(null));
    }

    @ParameterizedTest
    @CsvSource({"1, 2, 3, 2", "1, 2, 1, 2", "-1, 2, 6, 2"})
    void forwardsCoordinatesToInputBoundaryWithoutApplyingMovementRules(
            int sourceX, int sourceY, int destinationX, int destinationY) {
        List<ConfirmMoveInputData> requests = new ArrayList<>();
        ConfirmMoveController controller = new ConfirmMoveController(new ConfirmMoveInputBoundary() {
            @Override
            public void confirmMove(ConfirmMoveInputData inputData) { requests.add(inputData); }

            @Override
            public Set<BoardPosition> getValidDestinations(int x, int y) { return Set.of(); }
        });

        controller.onConfirmMoveClicked(sourceX, sourceY, destinationX, destinationY);

        assertEquals(List.of(new ConfirmMoveInputData(sourceX, sourceY, destinationX, destinationY)), requests);
    }

    @Test
    void delegatesHighlightQueryToTheAuthoritativeBoundary() {
        ConfirmMoveController controller = new ConfirmMoveController(new ConfirmMoveInputBoundary() {
            @Override
            public void confirmMove(ConfirmMoveInputData inputData) { throw new AssertionError("Unexpected move"); }

            @Override
            public Set<BoardPosition> getValidDestinations(int x, int y) {
                assertEquals(1, x);
                assertEquals(2, y);
                return Set.of(new BoardPosition(2, 2));
            }
        });

        assertEquals(Set.of(new BoardPosition(2, 2)), controller.getValidDestinations(1, 2));
    }
}
