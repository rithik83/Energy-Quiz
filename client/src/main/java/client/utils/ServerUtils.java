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
package client.utils;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;

import commons.*;
import org.glassfish.jersey.client.ClientConfig;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;

public class ServerUtils {

    private static final String SERVER = "http://localhost:8080/";

    /*
    public void getQuotesTheHardWay() throws IOException {
        var url = new URL("http://localhost:8080/api/quotes");
        var is = url.openConnection().getInputStream();
        var br = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
    }
    */

    /**
     * Retrieve all players from a session in the DB.
     *
     * @param sessionId the id of the session
     * @return List of all players from a session
     */
    public List<Player> getPlayers(long sessionId) {
        return ClientBuilder.newClient(new ClientConfig())
                .target(SERVER).path("api/sessions/" + sessionId + "/players")
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .get(new GenericType<List<Player>>() {
                });
    }

    /**
     * Adds a player to a game session.
     *
     * @param sessionId id of the session to add the player to
     * @param player    Player object to be added
     * @return The player that has been added
     */
    public Player addPlayer(long sessionId, Player player) {
        return ClientBuilder.newClient(new ClientConfig())
                .target(SERVER).path("api/sessions/" + sessionId + "/players")
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .post(Entity.entity(player, APPLICATION_JSON), Player.class);
    }

    /**
     * Removes a player from a game session.
     *
     * @param sessionId id of the session to remove the player from
     * @param playerId  id of player to be removed
     * @return The response from player removal
     */
    public Player removePlayer(long sessionId, long playerId) {
        return ClientBuilder.newClient(new ClientConfig())
                .target(SERVER).path("api/sessions/" + sessionId + "/players/" + playerId)
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .delete(Player.class);
    }

    /**
     * Retrieves a game session from the DB.
     *
     * @param sessionId id of the session to retrieve
     * @return Game session with the given id
     */
    public GameSession getSession(long sessionId) {
        return ClientBuilder.newClient(new ClientConfig())
                .target(SERVER).path("api/sessions/" + sessionId)
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .get(new GenericType<GameSession>() {
                });
    }

    /**
     * Retrieves an available game session from the DB.
     *
     * @return Available game session
     */
    public GameSession getAvailableSession() {
        return ClientBuilder.newClient(new ClientConfig())
                .target(SERVER).path("api/sessions/join")
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .get(new GenericType<GameSession>() {
                });
    }

    /**
     * Retrieves all game sessions from the DB that are still active.
     *
     * @return All active game sessions
     */
    public List<GameSession> getSessions() {
        return ClientBuilder.newClient(new ClientConfig())
                .target(SERVER).path("api/sessions")
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .get(new GenericType<List<GameSession>>() {
                });
    }

    /**
     * Adds a session to the DB.
     *
     * @param session GameSession object to be added
     * @return The session that has been added
     */
    public GameSession addSession(GameSession session) {
        return ClientBuilder.newClient(new ClientConfig())
                .target(SERVER).path("api/sessions")
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .post(Entity.entity(session, APPLICATION_JSON), GameSession.class);
    }

    /**
     * Removes a session from the DB.
     *
     * @param sessionId Id of session to be removed
     * @return The response from session removal
     */
    public GameSession removeSession(long sessionId) {
        return ClientBuilder.newClient(new ClientConfig())
                .target(SERVER).path("api/sessions/" + sessionId)
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .delete(GameSession.class);
    }

    /**
     * Updates a session status
     *
     * @param session Session to update
     * @param status  new status to be set
     * @return The updated session
     */
    public GameSession updateStatus(GameSession session, String status) {
        return ClientBuilder.newClient(new ClientConfig())
                .target(SERVER).path("api/sessions/" + session.id + "/status")
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .put(Entity.entity(status, APPLICATION_JSON), GameSession.class);
    }

    /**
     * Sets and unsets a player as being ready for a multiplayer game
     *
     * @param sessionId
     * @param isReady   True iff a player must be set as ready
     * @return New count of players that are ready
     */
    public Integer toggleReady(long sessionId, boolean isReady) {
        return ClientBuilder.newClient(new ClientConfig())
                .target(SERVER).path("api/sessions/" + sessionId + "/" + ((isReady) ? "" : "not") + "ready")
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .get(new GenericType<Integer>() {
                });
    }

    public Question fetchOneQuestion(long sessionId) {
        return ClientBuilder.newClient(new ClientConfig())
                .target(SERVER).path("api/questions/" + sessionId)
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .get(new GenericType<Question>() {
                });
    }

    public Evaluation submitAnswer(long sessionId, Answer answer) {
        return ClientBuilder.newClient(new ClientConfig())
                .target(SERVER).path("api/questions/" + sessionId)
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .post(Entity.entity(answer, APPLICATION_JSON), Evaluation.class);
    }

    /**
     * get player from the DB
     * @return whether the getting is successful or not
     */
    public List<Player> getPlayers() {
        return ClientBuilder.newClient(new ClientConfig()) //
                .target(SERVER).path("api/leaderboard/") //
                .request(APPLICATION_JSON) //
                .accept(APPLICATION_JSON) //
                .get(new GenericType<List<Player>>() {
                });
    }

    /**
     * add a new player to the DB
     * @param player the player to be added
     * @return a message to show whether the adding is successful or not
     */
    public Player addPlayer(Player player) {
        return ClientBuilder.newClient(new ClientConfig()) //
                .target(SERVER).path("api/leaderboard") //
                .request(APPLICATION_JSON) //
                .accept(APPLICATION_JSON) //
                .post(Entity.entity(player, APPLICATION_JSON), Player.class);
    }
}