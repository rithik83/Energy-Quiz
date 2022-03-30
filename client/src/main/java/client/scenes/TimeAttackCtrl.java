package client.scenes;

import client.utils.*;
import commons.Answer;
import commons.Question;
import jakarta.ws.rs.BadRequestException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;

import javax.inject.Inject;
import java.util.Timer;
import java.util.TimerTask;

public class TimeAttackCtrl extends SingleplayerCtrl {

    private long initialTime;
    private TimeUtils roundTimer;

    @Inject
    public TimeAttackCtrl(WebSocketsUtils webSocketsUtils, GameSessionUtils gameSessionUtils,
                          LeaderboardUtils leaderboardUtils, QuestionUtils questionUtils, MainCtrl mainCtrl) {
        super(webSocketsUtils, gameSessionUtils, leaderboardUtils, questionUtils, mainCtrl);


        this.initialTime = 60;
    }

    /**
     * Displays the question screen attributes.
     */
    @Override
    public void removeMidGameLeaderboard() {
        answerArea.setOpacity(1);
        questionPrompt.setOpacity(1);
        submitButton.setOpacity(1);
    }

    /**
     * Resets all fields and the screen for a new game.
     */
    @Override
    public void reset() {
        this.initialTime = 60L;
        super.reset();
    }

    /**
     * Loads a question and starts reading time.
     */
    public void loadQuestion() {
        disableButton(submitButton, false);
        this.answerArea.getChildren().clear();

        try {
            Question q = this.questionUtils.fetchOneQuestion(this.sessionId);
            this.currentQuestion = q;
            renderGeneralInformation(q);
            renderQuestionCount();
            loadAnswer();
        } catch (BadRequestException ignore) { /* happens when session is removed before question is loaded */ }
    }

    /**
     * Loads the answers of the current question and updates the timer after reading time is over
     */
    @Override
    public void loadAnswer() {
        Question q = this.currentQuestion;
        if (q == null) return;
        renderAnswerFields(q);

        disableButton(submitButton, false);
        imageHover();
    }

    /**
     * Submit button click event handler
     */
    @Override
    public void submitAnswerButton() {
        submitAnswer(false);
    }

    /**
     * Submit an answer to the server
     */
    public void submitAnswer(boolean initiatedByTimer) {
        Answer ans = new Answer(currentQuestion.type);

        switch (currentQuestion.type) {
            case MULTIPLE_CHOICE:
            case COMPARISON:
            case EQUIVALENCE:
                for (int i = 0; i < multiChoiceAnswers.size(); ++i) {
                    if (multiChoiceAnswers.get(i).isSelected()) {
                        ans.addAnswer(i);
                    }
                }
                break;
            case RANGE_GUESS:
                try {
                    ans.addAnswer(Long.parseLong(estimationAnswer.getText()));
                } catch (NumberFormatException ex) {
                    System.out.println("Invalid answer yo");
                    if (!initiatedByTimer) {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Invalid answer");
                        alert.setHeaderText("Invalid answer");
                        alert.setContentText("You should only enter an integer number");
                        alert.show();
                        return;
                    } else {
                        ans.addAnswer(0);
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported question type when parsing answer");
        }

        disableButton(submitButton, true);

        this.evaluation = questionUtils.submitAnswer(sessionId, playerId, ans);

        gameSessionUtils.toggleReady(sessionId, true);
        startEvaluation();
    }

    /**
     * Gets the user's answer, starts the evaluation and loads a new question or ends the game.
     */
    @Override
    public void startEvaluation() {
        if (this.evaluation == null) return;
        updatePoints();
        renderCorrectAnswer();
        this.evaluation = null;

        // TODO disable button while waiting
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (currentQuestion == null) return; // happens if shutdown is called before triggering
                    rounds++;
                    if (initialTime <= 0) {
                        handleGameEnd();
                    } else {
                        handleNextRound();
                    }
                });
            }
        }, GAME_ROUND_DELAY * 500);
    }

    /**
     * refresh the screen to show the leaderboard.
     */
    @Override
    public void refresh() {
        var players = leaderboardUtils.getPlayerTimeAttackScore();
        data = FXCollections.observableList(players);
        allPlayers.setItems(data);
    }

    /**
     * Initiates the timer at the beginning of the game and loads a question.
     */
    public void startTimer() {
        roundTimer = new TimeUtils(initialTime, TIMER_UPDATE_INTERVAL_MS);
        roundTimer.setTimeBooster(this::getTimeJokers);
        roundTimer.setOnSucceeded((event) -> Platform.runLater(() -> {
            System.out.println("roundTimer is done");
            this.initialTime = 0;
            gameSessionUtils.setQuestionCounter(sessionId, Integer.MAX_VALUE);
            submitAnswer(true);
        }));

        timeProgress.progressProperty().bind(roundTimer.progressProperty());
        this.timerThread = new Thread(roundTimer);
        this.timerThread.start();

        loadQuestion();
    }

}