package com.hiddenswitch.spellsource.impl.server;

import co.paralleluniverse.fibers.Suspendable;
import com.hiddenswitch.spellsource.Games;
import com.hiddenswitch.spellsource.Logic;
import com.hiddenswitch.spellsource.client.models.Emote;
import com.hiddenswitch.spellsource.common.Client;
import com.hiddenswitch.spellsource.common.ClientConnectionConfiguration;
import com.hiddenswitch.spellsource.common.NetworkBehaviour;
import com.hiddenswitch.spellsource.impl.util.ServerGameContext;
import com.hiddenswitch.spellsource.util.Rpc;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.DoNothingBehaviour;
import net.demilich.metastone.game.cards.CardSet;
import net.demilich.metastone.game.decks.DeckFormat;
import net.demilich.metastone.game.events.TouchingNotification;
import net.demilich.metastone.game.gameconfig.PlayerConfig;
import net.demilich.metastone.game.targeting.IdFactory;
import net.demilich.metastone.game.utils.Attribute;
import net.demilich.metastone.game.utils.AttributeMap;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static net.demilich.metastone.game.targeting.IdFactory.PLAYER_1;
import static net.demilich.metastone.game.targeting.IdFactory.PLAYER_2;

public class GameSessionImpl implements GameSession {
	private String host;
	private int websocketPort;
	private Client c1;
	private Client c2;
	private PregamePlayerConfiguration pregamePlayerConfiguration1;
	private PregamePlayerConfiguration pregamePlayerConfiguration2;
	private ServerGameContext gameContext;
	private Player player1;
	private Player player2;
	private final String gameId;
	private final Map<String, String> secretForUserId = new HashMap<>();
	private Logger logger = LoggerFactory.getLogger(GameSessionImpl.class);
	private long noActivityTimeout = Games.getDefaultNoActivityTimeout();
	private final HashSet<Handler<GameSessionImpl>> gameOverHandlers = new HashSet<>();
	private final Vertx vertx;

	public GameSessionImpl(String host, int websocketPort, PregamePlayerConfiguration p1, PregamePlayerConfiguration p2, String gameId, Vertx vertx, long noActivityTimeout) {
		setHost(host);
		this.pregamePlayerConfiguration1 = p1;
		this.pregamePlayerConfiguration2 = p2;
		this.gameId = gameId;
		this.vertx = vertx;
		this.websocketPort = websocketPort;
		if (p1.getUserId().equals(p2.getUserId())) {
			throw new RuntimeException();
		}
		for (String userId : new String[]{p1.getUserId(), p2.getUserId()}) {
			if (userId == null) {
				throw new RuntimeException();
			}
			this.secretForUserId.put(userId, RandomStringUtils.randomAlphanumeric(40));
		}
		this.noActivityTimeout = noActivityTimeout;
	}

	private ClientConnectionConfiguration getConfigurationFor(PregamePlayerConfiguration player, int id) {
		// TODO: It's obviously insecure to allow the client to specify things like their player object
		Player tempPlayer = player.getPlayer();
		if (tempPlayer == null) {
			PlayerConfig playerConfig = new PlayerConfig(player.getDeck(), new DoNothingBehaviour());
			tempPlayer = new Player(playerConfig);
		}
		tempPlayer.setId(id);
		return new ClientConnectionConfiguration(
				getUrl(), player.getUserId(), getSecret(player.getUserId()));
	}

	public String getSecret(String userId) {
		return secretForUserId.getOrDefault(userId, null);
	}

	@Override
	@Suspendable
	public void onPlayerConnected(Player player, Client client) {
		logger.debug("Receive connections from {}", player.toString());
		if (player.getId() == PLAYER_1) {
			if (getPlayer1() != null) {
				throw new RuntimeException("Two players tried to connect to the same player slot.");
			}
			setClient1(client);
			setPlayer1(player);
		} else if (player.getId() == IdFactory.PLAYER_2) {
			if (getPlayer2() != null) {
				throw new RuntimeException("Two players tried to connect to the same player slot.");
			}
			setClient2(client);
			setPlayer2(player);
		} else {
			throw new RuntimeException("A player without an ID set has attempted to connect.");
		}

		if (isGameReady()) {
			startGame();
		}
	}

	@Override
	@Suspendable
	public void onPlayerReconnected(Player player, Client client) {
		checkContext();
		if (player.getId() == PLAYER_1) {
			if (getClient1() != null) {
				getClient1().close();
			}
			setClient1(client);
		} else if (player.getId() == IdFactory.PLAYER_2) {
			if (getClient2() != null) {
				getClient2().close();
			}

			setClient2(client);
		} else {
			throw new RuntimeException("A player without an ID set has attempted to connect.");
		}

		getGameContext().onPlayerReconnected(player, client);
	}

	@Override
	@Suspendable
	public void onActionReceived(String id, int actionIndex) {
		onActionReceived(id, getActionForMessage(id, actionIndex));
	}

	@Override
	@Suspendable
	public void onActionReceived(String id, GameAction action) {
		checkContext();
		getGameContext().onActionReceived(id, action);
	}

	@Override
	public boolean isGameReady() {
		return (isAgainstAI()
				&& ((player1 != null && player2 == null)
				|| (player1 == null && player2 != null)))
				|| player1 != null
				&& player2 != null;
	}

	/**
	 * This is where a game is actually started in the networked engine.
	 */
	@Suspendable
	private void startGame() {
		logger.debug("Starting game...");
		DeckFormat simpleFormat = new DeckFormat().withCardSets(CardSet.BASIC,
				CardSet.CLASSIC,
				CardSet.BLACKROCK_MOUNTAIN,
				CardSet.GOBLINS_VS_GNOMES,
				CardSet.LEAGUE_OF_EXPLORERS,
				CardSet.MEAN_STREETS_OF_GADGETZHAN,
				CardSet.NAXXRAMAS,
				CardSet.ONE_NIGHT_IN_KARAZHAN,
				CardSet.PROMO,
				CardSet.REWARD,
				CardSet.THE_GRAND_TOURNAMENT,
				CardSet.THE_OLD_GODS,
				CardSet.JOURNEY_TO_UNGORO,
				CardSet.KNIGHTS_OF_THE_FROZEN_THRONE);

		// Configure the network behaviours on the players
		if (isAgainstAI()) {
			if (pregamePlayerConfiguration1.isAI()) {
				setPlayer1(createAIPlayer(pregamePlayerConfiguration1, PLAYER_1));
			} else if (pregamePlayerConfiguration2.isAI()) {
				setPlayer2(createAIPlayer(pregamePlayerConfiguration2, PLAYER_2));
			}
		}

		Player player1 = getPlayer1();
		Player player2 = getPlayer2();
		player1.setBehaviour(new NetworkBehaviour());
		player2.setBehaviour(new NetworkBehaviour());
		this.gameContext = new ServerGameContext(player1, player2, simpleFormat, getGameId(), Rpc.connect(Logic.class, vertx.eventBus()), new VertxTimers(vertx));
		final Client listener1;
		final Client listener2;


		if (isAgainstAI()) {
			if (pregamePlayerConfiguration1.isAI()) {
				listener1 = new AIServiceConnection(getGameContext(), vertx.eventBus(), PLAYER_1);
				setClient1(listener1);
				listener2 = getPlayerListener(PLAYER_2);
			} else {
				listener1 = getPlayerListener(PLAYER_1);
				listener2 = new AIServiceConnection(getGameContext(), vertx.eventBus(), PLAYER_2);
				setClient2(listener2);
			}
		} else {
			listener1 = getPlayerListener(PLAYER_1);
			listener2 = getPlayerListener(PLAYER_2);
		}

		// Merge in attributes
		for (int i = 0; i < 2; i++) {
			PregamePlayerConfiguration config = new PregamePlayerConfiguration[]{pregamePlayerConfiguration1, pregamePlayerConfiguration2}[i];
			final AttributeMap attributes = config.getAttributes();
			if (attributes == null) {
				continue;
			}
			for (Map.Entry<Attribute, Object> kv : attributes.entrySet()) {
				getGameContext().getPlayer(i).getAttributes().put(kv.getKey(), kv.getValue());
			}
		}


		getGameContext().setUpdateListener(player1, listener1);
		getGameContext().setUpdateListener(player2, listener2);
		getGameContext().handleEndGame(sgc -> {
			gameOverHandlers.forEach(h -> {
				h.handle(this);
			});
		});
		getGameContext().networkPlay();
	}

	@Override
	public ClientConnectionConfiguration getConfigurationForPlayer1() {
		return getConfigurationFor(pregamePlayerConfiguration1, PLAYER_1);
	}

	@Override
	public ClientConnectionConfiguration getConfigurationForPlayer2() {
		return getConfigurationFor(pregamePlayerConfiguration2, PLAYER_2);
	}

	private Client getPlayerListener(int player) {
		if (player == 0) {
			return getClient1();
		} else {
			return getClient2();
		}
	}

	@Suspendable
	@Override
	public void kill() {
		// The game never started if this were null
		if (getGameContext() != null) {
			getGameContext().kill();
		}

		if (getClient1() != null) {
			getClient1().close();
		}
		if (getClient2() != null) {
			getClient2().close();
		}
	}

	private void checkContext() {
		if (getGameContext() == null) {
			throw new NullPointerException(String.format("The game context for this game session is null. gameId: %s", getGameId()));
		}
	}

	private boolean isAgainstAI() {
		return pregamePlayerConfiguration1.isAI()
				|| pregamePlayerConfiguration2.isAI();
	}

	private Player createAIPlayer(PregamePlayerConfiguration pregame, int id) {
		PlayerConfig playerConfig = new PlayerConfig(pregame.getDeck(), new DoNothingBehaviour());
		Player player = new Player(playerConfig);
		player.setId(id);
		return player;
	}

	public String getHost() {
		return host;
	}

	private void setHost(String host) {
		this.host = host;
	}

	public Client getClient1() {
		return c1;
	}

	private void setClient1(Client c1) {
		this.c1 = c1;
	}

	public Client getClient2() {
		return c2;
	}

	private void setClient2(Client c2) {
		this.c2 = c2;
	}

	@Override
	public ServerGameContext getGameContext() {
		return gameContext;
	}

	@Override
	public Player getPlayer(String userId) {
		if (getPlayer1() != null
				&& getPlayer1().getUserId().equals(userId)) {
			return getPlayer1();
		} else if (getPlayer2() != null
				&& getPlayer2().getUserId().equals(userId)) {
			return getPlayer2();
		} else {
			if (pregamePlayerConfiguration1.getUserId().equals(userId)) {
				if (pregamePlayerConfiguration1.getPlayer() != null) {
					return pregamePlayerConfiguration1.getPlayer();
				} else {
					return Player.forUser(userId, IdFactory.PLAYER_1, pregamePlayerConfiguration1.getDeck());
				}
			} else if (pregamePlayerConfiguration2.getUserId().equals(userId)) {
				if (pregamePlayerConfiguration2.getPlayer() != null) {
					return pregamePlayerConfiguration2.getPlayer();
				} else {
					return Player.forUser(userId, IdFactory.PLAYER_2, pregamePlayerConfiguration2.getDeck());
				}
			}
		}
		throw new RuntimeException();
	}

	@Override
	public GameAction getActionForMessage(String messageId, int actionIndex) {
		return getGameContext().getActionForMessage(messageId, actionIndex);
	}

	@Override
	public int getPlayerIdForSocket(Object socket) {
		if (getClient1() != null
				&& getClient1().getPrivateSocket().equals(socket)) {
			return GameContext.PLAYER_1;
		} else if (getClient2() != null
				&& getClient2().getPrivateSocket().equals(socket)) {
			return GameContext.PLAYER_2;
		}
		// TODO: We probably should be able to concede a player that hasn't connected
		throw new RuntimeException("Cannot get a player that hasn't connected.");
	}

	@Override
	@Suspendable
	public void onMulliganReceived(String messageId, List<Integer> discardedCardIndices) {
		getGameContext().onMulliganReceived(messageId, discardedCardIndices);
	}

	@Override
	public void onEmote(int entityId, Emote.MessageEnum message) {
		if (getClient1() != null) {
			getClient1().onEmote(entityId, message);
		}

		if (getClient2() != null) {
			getClient2().onEmote(entityId, message);
		}
	}

	@Override
	public void onConcede(int playerId) {
		getGameContext().concede(playerId);
	}

	@Override
	public void onTouch(int playerId, int entityId) {
		getPlayerListener(getOpponent(playerId))
				.onNotification(
						new TouchingNotification(playerId, entityId, true),
						getGameContext().getGameState());
	}

	@Override
	public void onUntouch(int playerId, int entityId) {
		getPlayerListener(getOpponent(playerId))
				.onNotification(
						new TouchingNotification(playerId, entityId, false),
						getGameContext().getGameState());
	}

	private Player getPlayer1() {
		return player1;
	}

	private void setPlayer1(Player player1) {
		this.player1 = player1;
	}

	private Player getPlayer2() {
		return player2;
	}

	private void setPlayer2(Player player2) {
		this.player2 = player2;
	}

	public String getGameId() {
		return gameId;
	}

	@Override
	public long getNoActivityTimeout() {
		return noActivityTimeout;
	}

	public void handleGameOver(Handler<GameSessionImpl> handler) {
		gameOverHandlers.add(handler);
	}

	public String getUrl() {
		return "ws://" + getHost() + ":" + Integer.toString(websocketPort) + "/" + Games.WEBSOCKET_PATH;
	}

	private int getOpponent(int playerId) {
		return playerId == PLAYER_1 ? PLAYER_2 : PLAYER_1;
	}
}
