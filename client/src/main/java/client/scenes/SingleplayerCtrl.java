package client.scenes;

import client.utils.GameSessionUtils;
import client.utils.LeaderboardUtils;
import client.utils.QuestionUtils;
import client.utils.WebSocketsUtils;
import commons.Player;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class SingleplayerCtrl extends GameCtrl {

    private ObservableList<Player> data;
    @FXML
    private TableView<Player> allPlayers;
    @FXML
    private TableColumn<Player, String> colName;
    @FXML
    private TableColumn<Player, String> colPoint;
    @FXML
    private Label jokerUsage;

    @Inject
    public SingleplayerCtrl(WebSocketsUtils webSocketsUtils, GameSessionUtils gameSessionUtils,
                            LeaderboardUtils leaderboardUtils, QuestionUtils questionUtils, MainCtrl mainCtrl) {
        super(webSocketsUtils, gameSessionUtils, leaderboardUtils, questionUtils, mainCtrl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colName.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().username));
        colPoint.setCellValueFactory(q -> new SimpleStringProperty(String.valueOf(q.getValue().bestSingleScore)));
        jokerUsage.setText("");
    }

    /**
     * Submit an answer to the server and start evaluation
     */
    @Override
    public void submitAnswer(boolean initiatedByTimer) {
        super.submitAnswer(initiatedByTimer);
        startEvaluation(bestSingleScore);
    }

    /**
     * Enables the doublePoints joker if still available and calls the overridden method
     */
    @Override
    public void loadAnswer() {
        if (decreaseTimeJoker) {
            disableButton(decreaseTimeButton, false);
        }
        if (doublePointsJoker) {
            disableButton(doublePointsButton, false);
        }
        super.loadAnswer();
    }

    /**
     * Empty method because singleplayer mode does not have an end screen.
     */
    @Override
    public void showEndScreen() {
        return;
    }

    /**
     * Reverts the player to the splash screen and remove him from the current game session.
     */
    @Override
    public void back() {
        super.back();
    }

    /**
     * If the joker is active make the time joker -0.5 so the booster will work at half the normal speed
     * for a singleplayer game
     *
     * @return -0.5 if the joker has been used, or 0 otherwise
     */
    @Override
    public double getTimeJokers() {
        int number = gameSessionUtils.getSession(sessionId).getTimeJokers();
        return (number == 1) ? -0.5 : 0;
    }

    /**
     * This is an equivalent method to decreaseTime for Multiplayer
     */
    public void increaseTime() {
        decreaseTimeJoker = false;
        disableButton(decreaseTimeButton, true);
        gameSessionUtils.updateTimeJokers(sessionId, 1);
        displayJokerUsage(playerId, "IncreaseTimeJoker");
    }

    /**
     * refresh the screen to show the leaderboards
     */
    public void refresh() {
        var players = leaderboardUtils.getPlayerSingleScore();
        data = FXCollections.observableList(players);
        allPlayers.setItems(data);
    }

    /**
     * the method to update score for single player mode
     * @param playerId    the id of the player
     * @param points      the points of the player
     * @param isBestScore the flag of the best score of the player
     */
    @Override
    public void updateScore(long playerId, int points, boolean isBestScore) {
        leaderboardUtils.updateSingleScore(playerId, points, isBestScore);
    }

    /**
     * the method to display joker usage
     * @param playerId the id of the player who has used the joker
     * @param jokerName the name of the joker used
     */
    @Override
    public void displayJokerUsage(long playerId, String jokerName) {
        String username = leaderboardUtils.getPlayerByIdInLeaderboard(playerId).getUsername();
        jokerUsage.setText(username + " has used " + jokerName);
    }
}
