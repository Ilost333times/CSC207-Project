package adapters.presenter;

import entity.Game;
import entity.GameResult;
import entity.Player;
import entity.card.Card;
import entity.card.CharacterCard;
import entity.card.SpellCard;
import adapters.viewmodel.GameViewModel;
import adapters.viewmodel.GameViewModel.CardViewData;
import adapters.viewmodel.GameViewModel.CardType;
import adapters.viewmodel.GameViewModel.TileViewData;
import adapters.viewmodel.ViewState;
import usecase.confirmmove.ConfirmMoveOutputBoundary;
import usecase.confirmmove.ConfirmMoveOutputData;
import usecase.loadgame.LoadGameOutputBoundary;
import usecase.savegame.SaveGameOutputBoundary;
import usecase.startgame.StartGameOutputBoundary;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GamePresenter implements StartGameOutputBoundary,
        SaveGameOutputBoundary,
        LoadGameOutputBoundary, ConfirmMoveOutputBoundary {

    private Game currentGame;
    private GameResult lastLoadedResult;
    private Runnable onGameChanged;
    private final ViewState<GameViewModel> gameState;
    private final Player localPlayer;

    public GamePresenter() {
        gameState = null;
        localPlayer = null;
    }

    public GamePresenter(ViewState<GameViewModel> gameState, Player localPlayer) {
        this.gameState = Objects.requireNonNull(gameState);
        this.localPlayer = Objects.requireNonNull(localPlayer);
    }

    public void setOnGameChanged(Runnable callback) { this.onGameChanged = callback; }
    public Game getCurrentGame() { return currentGame; }
    public GameResult getLastLoadedResult() { return lastLoadedResult; }

    @Override
    public void presentGameStarted(Game game) {
        this.currentGame = game;
        publishMovement("");
        refresh();
    }

    @Override
    public void presentMoveConfirmed(ConfirmMoveOutputData outputData) {
        publishMovement("Moved " + coordinate(outputData.sourceX(), outputData.sourceY()) + " to "
                + coordinate(outputData.destinationX(), outputData.destinationY()) + ".");
        refresh();
    }

    @Override
    public void presentMoveFailure(String reason) {
        publishMovement(reason);
    }

    /** Call on the local UI event thread after relevant board, turn or action changes. */
    public void refreshGameView() {
        publishMovement("");
    }

    private void publishMovement(String feedback) {
        if (gameState == null || currentGame == null) {
            return;
        }
        List<TileViewData> tiles = new ArrayList<>();
        synchronized (currentGame) {
            for (int row = 0; row < GameViewModel.BOARD_SIZE; row++) {
                for (int column = 0; column < GameViewModel.BOARD_SIZE; column++) {
                    var tile = currentGame.getBoard().getTile(column, row);
                    Card occupant = tile.getCharacterCard() != null ? tile.getCharacterCard() : tile.getSorceryCard();
                    List<CardViewData> spells = tile.getSpells() == null ? List.of()
                            : tile.getSpells().stream().map(this::cardViewData).toList();
                    tiles.add(new TileViewData(occupant == null ? null : cardViewData(occupant), spells,
                            occupant != null && occupant.getOwner() != localPlayer));
                }
            }
        }
        // Publish outside the game lock. Local composition serializes actions/publication on the EDT.
        gameState.setState(gameState.getState().withMovementUpdate(tiles, feedback));
    }

    private CardViewData cardViewData(Card card) {
        if (card instanceof CharacterCard character) {
            return new CardViewData(card.getName(), CardType.CHARACTER,
                    "Character · Speed " + character.getSpeed(), character.getHP());
        }
        if (card instanceof SpellCard spell) {
            return new CardViewData(card.getName(), CardType.SPELL,
                    "Spell · " + spell.getDamage() + " damage · Range " + spell.getRange());
        }
        return new CardViewData(card.getName(), CardType.SORCERY, "Sorcery");
    }

    private String coordinate(int x, int y) {
        return String.valueOf((char) ('A' + x)) + (y + 1);
    }

    @Override
    public void presentSaveSuccess(String slotName) {
        System.out.println("[Presenter] Result saved to slot: " + slotName);
    }

    @Override
    public void presentSaveFailure(String slotName, String reason) {
        System.err.println("[Presenter] Save failed (" + slotName + "): " + reason);
    }

    @Override
    public void presentResultLoaded(GameResult result) {
        this.lastLoadedResult = result;
        System.out.println("[Presenter] Loaded result: "
                + result.getPlayer1Name() + " " + result.getPlayer1Score()
                + " vs "
                + result.getPlayer2Name() + " " + result.getPlayer2Score()
                + " | winner = "
                + (result.isDraw() ? "DRAW" : result.getWinnerName())
                + " | endedAt = " + result.getEndedAt());
    }

    @Override
    public void presentLoadFailure(String slotName, String reason) {
        System.err.println("[Presenter] Load failed (" + slotName + "): " + reason);
    }

    @Override
    public void presentStartFailure(String reason) {
        System.err.println("[Presenter] Cannot start: " + reason);
    }

    private void refresh() { if (onGameChanged != null) onGameChanged.run(); }
}
