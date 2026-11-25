package app;

import adapters.viewmodel.GameViewModel;
import adapters.viewmodel.ViewState;
import entity.Game;
import entity.Player;
import entity.card.CharacterCard;
import entity.card.SpellCard;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/** Standalone local movement preview, independent of unfinished lobby/session networking. */
public final class LocalMoveMain {
    private LocalMoveMain() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Player local = new Player("Local player");
            Player opponent = new Player("Opponent");
            Game game = new Game(new ArrayList<>(List.of(local, opponent)));
            game.getBoard().playCard(1, 2, new CharacterCard(local, "Aki", 200, 3));
            game.getBoard().playCard(1, 2, new SpellCard(local, "Fireball"));
            game.getBoard().playCard(4, 3, new CharacterCard(local, "Cel", 150, 4));
            game.getBoard().playCard(2, 2, new CharacterCard(opponent, "Kia", 200, 3));
            var state = new ViewState<>(GameViewModel.empty().withGameStatus("Local player's turn · movement preview"));
            var localGame = new ClientAppBuilder().buildLocalGame(game, local, state);
            JFrame frame = new JFrame("Spells to Save the World · Local Confirm Move");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(localGame.view());
            frame.setSize(1120, 800);
            frame.setMinimumSize(new Dimension(860, 650));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
