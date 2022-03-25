package server.api;

import commons.Joker;
import commons.Player;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class JokerController {
    private LeaderboardController leaderboard;

    public JokerController(LeaderboardController leaderboard) {
        this.leaderboard = leaderboard;
    }

    /**
     * the API for joker usage
     * @param playerId the Id of the player who has used the joker
     * @param jokerName the name of the player who has used the joker
     * @return a Joker which has been used
     */
    @MessageMapping("/joker/{sessionId}/send/{playerId}")
    @SendTo("/updates/joker/{sessionId}")
    public Joker sendJoker(@DestinationVariable("playerId") long playerId, String jokerName) {
        ResponseEntity<Player> playerInfo = leaderboard.getPlayerById(playerId);
        if (playerInfo.getStatusCode() != HttpStatus.OK || playerInfo.getBody() == null) return null;
        String username = playerInfo.getBody().username;
        System.out.println("sendJoker ready");
        return new Joker(username, jokerName);
    }
}
