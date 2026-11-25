package app;

import adapters.controller.ConfirmMoveController;
import adapters.presenter.GamePresenter;
import adapters.viewmodel.GameViewModel;
import adapters.viewmodel.ViewState;
import entity.Game;
import entity.Player;
import usecase.confirmmove.ConfirmMoveInteractor;
import view.GameView;

import javax.swing.SwingUtilities;
import java.util.Objects;

public class ClientAppBuilder {
    /**
     * Builds one local match on the EDT. The caller supplies the authoritative Game and
     * canonical player; network session authentication is deliberately not inferred here.
     */
    public LocalGame buildLocalGame(Game game, Player localPlayer, ViewState<GameViewModel> state) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Build the local game on the Swing event dispatch thread.");
        }
        Objects.requireNonNull(game);
        GamePresenter presenter = new GamePresenter(state, localPlayer);
        ConfirmMoveController controller = new ConfirmMoveController(new ConfirmMoveInteractor(game, localPlayer, presenter));
        GameView view = new GameView(controller);
        state.addPropertyChangeListener(view);
        presenter.presentGameStarted(game);
        return new LocalGame(view, presenter, state);
    }

    /** The presenter refresh hook invalidates selections after external local game changes. */
    public record LocalGame(GameView view, GamePresenter presenter, ViewState<GameViewModel> state) { }
}
