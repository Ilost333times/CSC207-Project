package usecase.confirmmove;

public interface ConfirmMoveOutputBoundary {
    void presentMoveConfirmed(ConfirmMoveOutputData outputData);
    void presentMoveFailure(String reason);
}
