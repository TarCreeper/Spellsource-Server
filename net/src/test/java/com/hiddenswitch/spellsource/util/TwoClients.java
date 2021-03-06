package com.hiddenswitch.spellsource.util;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import com.hiddenswitch.spellsource.impl.GamesImpl;
import com.hiddenswitch.spellsource.client.RemoteGameContext;
import com.hiddenswitch.spellsource.common.ClientConnectionConfiguration;
import com.hiddenswitch.spellsource.impl.ServiceTest;
import com.hiddenswitch.spellsource.models.MatchmakingResponse;
import com.hiddenswitch.spellsource.impl.util.ServerGameContext;
import com.hiddenswitch.spellsource.models.CreateGameSessionRequest;
import com.hiddenswitch.spellsource.models.CreateGameSessionResponse;
import com.hiddenswitch.spellsource.impl.server.PregamePlayerConfiguration;
import com.hiddenswitch.spellsource.models.StartGameResponse;
import net.demilich.metastone.game.cards.CardCatalogue;
import net.demilich.metastone.game.cards.CardParseException;
import net.demilich.metastone.game.decks.Deck;
import net.demilich.metastone.game.decks.DeckCatalogue;
import net.demilich.metastone.game.decks.DeckFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

import static net.demilich.metastone.game.GameContext.PLAYER_1;
import static net.demilich.metastone.game.GameContext.PLAYER_2;

/**
 * Created by bberman on 11/18/16.
 */
@Deprecated
public class TwoClients {
	private RemoteGameContext playerContext1;
	private RemoteGameContext playerContext2;
	private Thread thread1;
	private Thread thread2;
	private String gameId;
	private GamesImpl service;
	private Logger logger = LoggerFactory.getLogger(TwoClients.class);
	private PregamePlayerConfiguration pregame1;
	private PregamePlayerConfiguration pregame2;
	private ClientConnectionConfiguration configurationForPlayer1;
	private ClientConnectionConfiguration configurationForPlayer2;

	public ServerGameContext getServerGameContext() {
		return service.getGameContext(gameId);
	}

	public RemoteGameContext getPlayerContext1() {
		return playerContext1;
	}

	public RemoteGameContext getPlayerContext2() {
		return playerContext2;
	}

	public Thread getThread1() {
		return thread1;
	}

	public Thread getThread2() {
		return thread2;
	}

	public TwoClients invoke(GamesImpl service) throws SuspendExecution, InterruptedException {
		return invoke(service, 120000L);
	}

	@Suspendable
	public TwoClients invoke(GamesImpl service, long noActivityTimeout) throws SuspendExecution, InterruptedException {
		this.service = service;
		CardCatalogue.loadCardsFromPackage();

		AIPlayer player1 = new AIPlayer();
		AIPlayer player2 = new AIPlayer();
		pregame1 = new PregamePlayerConfiguration(player1.getConfiguredDeck(), "Player 1").withPlayer(player1.withUserId("userId1"));
		pregame2 = new PregamePlayerConfiguration(player2.getConfiguredDeck(), "Player 2").withPlayer(player2.withUserId("userId2"));

		CreateGameSessionRequest request = new CreateGameSessionRequest();
		request.setPregame1(pregame1);
		request.setPregame2(pregame2);
		request.setGameId(RandomStringUtils.randomAlphanumeric(8));
		request.setNoActivityTimeout(noActivityTimeout);

		CreateGameSessionResponse response = service.createGameSession(request);
		this.gameId = response.getGameId();
		// Manually override the player in the configurations
		configurationForPlayer1 = response.getConfigurationForPlayer1();
		configurationForPlayer2 = response.getConfigurationForPlayer2();
		connect(PLAYER_1);
		connect(PLAYER_2);
		return this;
	}

	@Suspendable
	public TwoClients invoke(GamesImpl service, boolean aiOpponent) throws IOException, URISyntaxException, CardParseException, SuspendExecution, InterruptedException {
		this.service = service;
		CardCatalogue.loadCardsFromPackage();

		AIPlayer player1 = new AIPlayer();
		pregame1 = new PregamePlayerConfiguration(player1.getConfiguredDeck(), "Player 1").withPlayer(player1.withUserId("localPlayerUserId1"));
		pregame2 = new PregamePlayerConfiguration(DeckFactory.getRandomDeck(), "Player 2").withAI(true);

		CreateGameSessionRequest request = new CreateGameSessionRequest();
		request.setPregame1(pregame1);
		request.setPregame2(pregame2);
		request.setGameId(RandomStringUtils.randomAlphanumeric(8));
		request.setNoActivityTimeout(120000L);

		CreateGameSessionResponse response = service.createGameSession(request);
		this.gameId = response.getGameId();
		// Manually override the player in the configurations
		configurationForPlayer1 = response.getConfigurationForPlayer1();
		connect(PLAYER_1);

		return this;
	}

	@Suspendable
	public TwoClients invoke(MatchmakingResponse response1, Deck deck1, MatchmakingResponse response2, Deck deck2, String gameId, GamesImpl service) {
		throw new UnsupportedOperationException();
	}

	@Suspendable
	public TwoClients invoke(GamesImpl service, CreateGameSessionResponse cgsr, StartGameResponse sgr, String userId1, String userId2) {
		throw new UnsupportedOperationException();
	}

	public void connect(int playerId) {
		if (playerId == PLAYER_1) {
			playerContext1 = createRemoteGameContext(configurationForPlayer1);
			thread1 = new Thread(playerContext1::play);
		} else {
			playerContext2 = createRemoteGameContext(configurationForPlayer2);
			thread2 = new Thread(playerContext2::play);
		}
	}

	public void disconnect(int playerId) {
		if (playerId == PLAYER_1) {
			if (thread1 != null) {
				thread1.interrupt();
			}
			if (playerContext1 != null) {
				playerContext1.dispose();
			}

		} else {
			if (thread2 != null) {
				thread2.interrupt();
			}
			if (playerContext2 != null) {
				playerContext2.dispose();
			}
		}
	}

	private RemoteGameContext createRemoteGameContext(ClientConnectionConfiguration configuration) {
		try {
			DeckCatalogue.loadDecksFromPackage();
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}

		return new RemoteGameContext(configuration).withIgnoreEventOverride(true);
	}

	public void play() {
		play(PLAYER_1);
		play(PLAYER_2);
	}

	public void play(int playerId) {
		if (playerId == PLAYER_1) {
			thread1.start();
		} else {
			thread2.start();
		}
	}

	public boolean isInterrupted() {
		return getThread1().isInterrupted() || getThread2().isInterrupted();
	}

	public boolean gameDecided() {
		return getPlayerContext1().updateAndGetGameOver()
				&& (getPlayerContext2() == null || getPlayerContext2().updateAndGetGameOver());
	}

	public void assertGameOver() {
		if (!gameDecided()
				|| isTimedOut()) {
			// Print some diagnostic information
			log("A match was not decided in this test by the deadline. Game information:");
		} else {
			logger.info("TwoClients match complete.");
		}
		ServiceTest.getContext().assertTrue(gameDecided());
		ServiceTest.getContext().assertFalse(isTimedOut());
		if (playerContext2 != null) {
			ServiceTest.getContext().assertTrue(playerContext1.getWinningPlayerId() == playerContext2.getWinningPlayerId());
		}

		this.dispose();
	}

	public void log(String message) {
		logger.error(message);
		final ServerGameContext serverGameContext = getServerGameContext();
		if (serverGameContext == null) {
			logger.error("The server game context is null, so an error cannot be logged. Client errors:");
			logger.error(getPlayerContext1().toLongString());
		} else {
			logger.error(serverGameContext.toLongString());
			logger.error("Panic Dump:");
			serverGameContext.getLogic().logDebugHistory();
		}
	}

	public void dispose() {
		disconnect(PLAYER_1);
		disconnect(PLAYER_2);
	}

	public boolean isTimedOut() {
		return isTimedOut((long) 40e9);
	}

	public boolean isTimedOut(long time) {
		return !gameDecided()
				&& playerContext1.isTimedOut(time)
				&& (playerContext2 == null || playerContext2.isTimedOut(time));
	}

	public String getGameId() {
		return gameId;
	}
}
