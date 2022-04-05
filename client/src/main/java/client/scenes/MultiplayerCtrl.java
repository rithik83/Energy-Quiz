/*
 * Copyright 2021 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package client.scenes;

import client.utils.*;
import com.google.inject.Inject;
import commons.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import org.springframework.messaging.simp.stomp.StompSession;

import java.net.URL;
import java.nio.file.Path;
import java.util.*;

public class MultiplayerCtrl extends GameCtrl {

    @FXML
    private ImageView emojiFunny;
    @FXML
    private ImageView emojiSad;
    @FXML
    private ImageView emojiAngry;
    @FXML
    private Button backButton;
    @FXML
    private Button leaveButton;
    @FXML
    private Button playAgain;
    @FXML
    private Label status;
    @FXML
    private Label removedPlayers;
    @FXML
    private Label jokerUsage;
    @FXML
    private Pane emojiArea;

    private int lastDisconnectIndex;
    private int previousPlayerCount;
    private Timer disconnectTimer;
    private int lastJokerIndex;
    private Timer jokerTimer;
    private Timer endGameTimer;
    private TimeUtils endGameCountdown;
    private StompSession.Subscription channel;
    private boolean playingAgain;
    private int waitingSkip = 0;
    private final static long END_GAME_TIME = 60L;
    private final ObservableList<Emoji> sessionEmojis;
    private final List<Image> emojiImages;
    private final GameAnimation gameAnimation;
    private List<Joker> usedJokers;

    @Inject
    public MultiplayerCtrl(WebSocketsUtils webSocketsUtils, GameSessionUtils gameSessionUtils,
                           LeaderboardUtils leaderboardUtils, QuestionUtils questionUtils,
                           GameAnimation gameAnimation, MainCtrl mainCtrl) {
        super(webSocketsUtils, gameSessionUtils, leaderboardUtils, questionUtils, mainCtrl);
        this.gameAnimation = gameAnimation;

        sessionEmojis = FXCollections.observableArrayList();
        emojiImages = new ArrayList<Image>();
        String[] emojiFileNames = {"funny", "sad", "angry"};
        ClassLoader cl = getClass().getClassLoader();
        for (String fileName : emojiFileNames) {
            URL location = cl.getResource(
                    Path.of("", "client", "scenes", "emojis", fileName + ".png").toString());

            emojiImages.add(new Image(location.toString()));
        }
        usedJokers = new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     */
    public void initialize(URL url, ResourceBundle res) {
        colUserName.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().username));
        colPoints.setCellValueFactory(p -> new SimpleIntegerProperty(p.getValue().currentPoints).asObject());

        colRank.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Player, Integer> call(TableColumn<Player, Integer> param) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(Integer item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty) setText(this.getTableRow().getIndex() + 1 + "");
                        else setText("");
                    }
                };
            }
        });

        leaderboard.setPrefWidth(IN_GAME_LEADERBOARD_WIDTH);
        colUserName.setPrefWidth(IN_GAME_COLUSERNAME_WIDTH);
        leaderboard.setOpacity(1);

        leaveButton.setOpacity(0);
        backButton.setOpacity(1);
        playAgain.setOpacity(0);
        status.setOpacity(0);

        emojiFunny.setImage(emojiImages.get(0));
        emojiSad.setImage(emojiImages.get(1));
        emojiAngry.setImage(emojiImages.get(2));
    }

    /**
     * Initialize an ImageView node for an emoji
     * @param e Emoji to use for an imageview
     * @param dimension Size of imageview (even dimensions for width and height)
     * @return An ImageView node
     */
    public ImageView emojiToImage(Emoji e, int dimension) {
        Image picture;
        switch (e.emoji) {
            case FUNNY -> picture = emojiImages.get(0);
            case SAD -> picture = emojiImages.get(1);
            default -> picture = emojiImages.get(2);
        }

        ImageView iv = new ImageView(picture);
        iv.setFitHeight(dimension);
        iv.setFitWidth(dimension);
        return iv;
    }

    /**
     * Checks the server periodically for players who disconnected. If so, displays text on the game screen
     */
    public void scanForDisconnect() {
        lastDisconnectIndex = -1;
        disconnectTimer = new Timer();
        disconnectTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                try {
                    List<Player> allRemoved = gameSessionUtils.getRemovedPlayers(sessionId);
                    List<Player> newRemoved = new ArrayList<Player>();
                    for (int i = lastDisconnectIndex + 1; i < allRemoved.size(); i++) {
                        newRemoved.add(allRemoved.get(i));
                    }
                    Platform.runLater(() -> disconnectedText(newRemoved));
                    lastDisconnectIndex = allRemoved.size() - 1;
                } catch (Exception e) {
                    cancel();
                }
            }
        }, 0, 2000);
    }

    /**
     * Scans for players joining in the end game screen
     */
    public void scanForEndGameAddition() {
        previousPlayerCount = -1;
        endGameTimer = new Timer();
        endGameTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                int playerCount = gameSessionUtils.getSession(sessionId).players.size();
                if (previousPlayerCount < playerCount) {
                    endGameCountdown.resetTimer();
                    Platform.runLater(() -> displayLeaderboard());
                }
                previousPlayerCount = playerCount;
            }
        }, 0, 500);
    }

    /**
     * Displays the player(s) who got disconnected
     *
     * @param players Players who got disconnected
     */
    public void disconnectedText(List<Player> players) {
        if (players.size() == 0) {
            removedPlayers.setOpacity(0.0);
            return;
        }
        String req = "";
        for (int i = 0; i < players.size(); i++) {
            req += players.get(i).username + ", ";
        }
        req = req.substring(0, req.length() - 2);
        removedPlayers.setText(String.format("%s" + ": DISCONNECTED...", req));
        removedPlayers.setOpacity(1.0);
    }

    /**
     * Refreshes the multiplayer player board to check whether the evaluation can start or refreshes the board to check
     * how many players want to play again.
     */
    public void refresh() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    try {
                        if (gameSessionUtils.getSession(sessionId).sessionStatus
                                == GameSession.SessionStatus.PAUSED) {
                            startEvaluation();
                            cancel();
                        }
                        if (gameSessionUtils.getSession(sessionId).sessionStatus
                                == GameSession.SessionStatus.PLAY_AGAIN) {
                            if (gameSessionUtils.getSession(sessionId).players.size() ==
                                    gameSessionUtils.getSession(sessionId).playersReady.get()) {
                                //Speed the timer up
                                waitingSkip = 4;
                            } else {
                                //Slow the timer down
                                waitingSkip = 0;
                            }
                            status.setText(gameSessionUtils.getSession(sessionId).playersReady.get() + " / " +
                                    gameSessionUtils.getSession(sessionId).players.size()
                                    + " players want to play again");
                        }
                        if (gameSessionUtils.getSession(sessionId).sessionStatus
                                == GameSession.SessionStatus.TRANSFERRING) {
                            cancel();
                        }
                    } catch (Exception e) {
                        cancel();
                    }
                });
            }

            @Override
            public boolean cancel() {
                return super.cancel();
            }
        }, 0, 100);
    }

    /**
     * Renders the leaderboard at the start of a question and renders the rest of the general information
     *
     * @param q the question to be rendered
     */
    @Override
    public void renderGeneralInformation(Question q) {
        renderLeaderboard();
        super.renderGeneralInformation(q);
    }

    /**
     * Renders the correct answer and updates the leaderboard
     */
    @Override
    public void renderCorrectAnswer() {
        super.renderCorrectAnswer();
        renderLeaderboard();
    }

    /**
     * Resizes the leaderboard and displays the question screen attributes
     */
    @Override
    public void removeMidGameLeaderboard() {
        countdown.setOpacity(1);
        leaderboard.setPrefWidth(IN_GAME_LEADERBOARD_WIDTH);
        colUserName.setPrefWidth(IN_GAME_COLUSERNAME_WIDTH);
        super.removeMidGameLeaderboard();
    }

    /**
     * Interrupts the timer, disables the submit button, sends the user's answer for evaluation and pauses the game
     * until everyone has answered or the timer has terminated.
     */
    public void submitAnswer(boolean initiatedByTimer) {
        super.submitAnswer(initiatedByTimer);
        if (!initiatedByTimer && this.evaluation == null) return;

        //enable jokers that can be used after submitting an answer
        disableButton(decreaseTimeButton, !decreaseTimeJoker);
        disableButton(doublePointsButton, !doublePointsJoker);

        refresh();
    }

    @Override
    public void shutdown() {
        if (submitButton.isDisabled() &&
                gameSessionUtils.getSession(sessionId).sessionStatus != GameSession.SessionStatus.PLAY_AGAIN) {
            gameSessionUtils.toggleReady(sessionId, false);
        }
        if (gameSessionUtils.getSession(sessionId).sessionStatus == GameSession.SessionStatus.PLAY_AGAIN &&
                playAgain.getText().equals("Don't play again")) {
            playAgain();
        }
        channel.unsubscribe();
        super.shutdown();
        disconnectTimer.cancel();
        if (endGameTimer != null) endGameTimer.cancel();
        lastDisconnectIndex = -1;
        jokerTimer.cancel();
        lastJokerIndex = -1;
    }

    /**
     * Register the client to receive emoji reactions from other players
     */
    public void registerForEmojiUpdates() {
        channel = this.webSocketsUtils.registerForEmojiUpdates(emoji -> {
            Platform.runLater(() -> gameAnimation.startEmojiAnimation(
                    emojiToImage(emoji, 60), emoji.username, emojiArea));
        }, this.sessionId);
    }

    /**
     * Method that calls the parent class' back method when the endgame back button is pressed and calls reset.
     */
    public void leaveGame() {
        if (playAgain.getText().equals("Don't play again")) playAgain();
        super.back();
    }

    /**
     * Reset method that resets multiplayer only attributes.
     */
    @Override
    public void reset() {
        playAgain.setText("Play again");
        playAgain.setOpacity(0);
        leaveButton.setOpacity(0);
        leaveButton.setDisable(true);
        backButton.setOpacity(1);
        backButton.setDisable(false);
        status.setText("[Status]");
        status.setOpacity(0);
        setPlayingAgain(false);
        waitingSkip = 0;
        leaderboard.setOpacity(0);
        super.reset();
    }

    /**
     * Toggles between want to play again and don't want to play again, modifying playAgain button and stores whether
     * the player wants to play again.
     */
    public void playAgain() {
        switch (playAgain.getText()) {
            case "Play again" -> {
                playAgain.setText("Don't play again");
                questionCount.setText("Waiting for game to start...");
                gameSessionUtils.toggleReady(sessionId, true);
                setPlayingAgain(true);
            }
            case "Don't play again" -> {
                playAgain.setText("Play again");
                questionCount.setText("End of game! Play again or go back to main.");
                gameSessionUtils.toggleReady(sessionId, false);
                setPlayingAgain(false);
            }
        }
    }

    /**
     * Show leaderboard at the end of the game and reveals back and play again buttons. Starts time and after 20 seconds
     * a new game starts if enough players want to play again.
     *
     * @param sentFromGame - True iff the user is sent to the end screen after a game, false otherwise
     */
    @Override
    public void showEndScreen(boolean sentFromGame) {
        displayLeaderboard();
        countdown.setOpacity(0);
        backButton.setOpacity(0);
        backButton.setDisable(true);
        leaveButton.setOpacity(1);
        leaveButton.setDisable(false);
        playAgain.setOpacity(1);
        status.setOpacity(1);
        status.setText("");
        waitingSkip = 0;
        questionCount.setText("End of game! Play again or go back to main.");
        if (sentFromGame) {
            gameSessionUtils.toggleReady(sessionId, false);
        }
        endGameCountdown = new TimeUtils(END_GAME_TIME, TIMER_UPDATE_INTERVAL_MS);
        endGameCountdown.setTimeBooster(() -> (double) waitingSkip);
        endGameCountdown.setOnSucceeded((event) -> {
            endGameTimer.cancel();
            gameSessionUtils.updateStatus(gameSessionUtils.getSession(sessionId),
                    GameSession.SessionStatus.TRANSFERRING);
            Platform.runLater(() -> {
                if (isPlayingAgain()) {
                    startGame();
                } else {
                    leaveGame();
                }
            });
        });

        timeProgress.progressProperty().bind(endGameCountdown.progressProperty());
        timerThread = new Thread(endGameCountdown);
        timerThread.start();
        scanForEndGameAddition();
        refresh();
    }

    /**
     * Checks whether there are enough players in the session after the clients had time to remove the players that
     * quit.
     */
    public void startGame() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (gameSessionUtils.getPlayers(sessionId).size() >= 2 && isPlayingAgain()) {
                        GameSession session = gameSessionUtils.toggleReady(sessionId, false);
                        if (session.playersReady.get() == 0) {
                            gameSessionUtils.updateStatus(session, GameSession.SessionStatus.ONGOING);
                        }
                        reset();
                        loadQuestion();
                    } else {
                        leaveGame();
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Unable to start new game!");
                        alert.setHeaderText("There are too few people to play again:");
                        alert.setContentText("Please join a fresh game to play with more people!");
                        mainCtrl.addCSS(alert);
                        alert.showAndWait();
                    }
                });
            }
        }, 1000);
    }

    /**
     * Getter for playingAgain field.
     *
     * @return whether the player wants to play again.
     */
    public boolean isPlayingAgain() {
        return playingAgain;
    }

    /**
     * Setter for playingAgain field
     *
     * @param playingAgain parameter that shows if a player wants to play again.
     */
    public void setPlayingAgain(boolean playingAgain) {
        this.playingAgain = playingAgain;
    }

    /**
     * the method to deal with the joker usage in the game
     */
    public void scanForJokerUsage() {
        lastJokerIndex = -1;
        jokerTimer = new Timer();
        jokerTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                Platform.runLater(() -> {
                    List<Joker> allUsed = gameSessionUtils.getUsedJoker(sessionId);
                    List<Joker> newlyUsed = new ArrayList<>();
                    for (int i = lastJokerIndex + 1; i < allUsed.size(); i++) {
                        newlyUsed.add(allUsed.get(i));
                    }
                    displayJokerUsage(newlyUsed);
                    lastJokerIndex = allUsed.size() - 1;
                });
            }
        }, 0, 2000);
    }

    /**
     * the method to display joker usage
     *
     * @param jokers a list of jokers which has been used
     */
    public void displayJokerUsage(List<Joker> jokers) {
        if (jokers.size() == 0) {
            jokerUsage.setOpacity(0.0);
            return;
        }
        String temp = "";
        for (int i = 0; i < jokers.size(); i++) {
            temp += jokers.get(i).username() + " has used " + jokers.get(i).jokerName() + ", ";
        }
        temp = temp.substring(0, temp.length() - 2);
        jokerUsage.setText(temp);
        jokerUsage.setOpacity(1.0);
    }
}