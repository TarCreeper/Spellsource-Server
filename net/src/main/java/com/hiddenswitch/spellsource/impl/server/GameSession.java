package com.hiddenswitch.spellsource.impl.server;

import co.paralleluniverse.fibers.Suspendable;
import com.hiddenswitch.spellsource.common.ClientConnectionConfiguration;
import com.hiddenswitch.spellsource.common.Server;
import com.hiddenswitch.spellsource.common.Client;
import com.hiddenswitch.spellsource.impl.util.ServerGameContext;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;

import java.util.List;

/**
 * A game session extends a server with information needed for clients to connect to the specific session.
 */
public interface GameSession extends Server {

	boolean isGameReady();

	/**
	 * Returns the information the client needs to know whom to connect to and what message to send.
	 *
	 * @return {ClientConnectionConfiguration} Data for player 1.
	 */
	ClientConnectionConfiguration getConfigurationForPlayer1();

	/**
	 * Returns the information the client needs to know whom to connect to and what message to send.
	 *
	 * @return {ClientConnectionConfiguration} Data for player 2.
	 */
	ClientConnectionConfiguration getConfigurationForPlayer2();

	@Suspendable
	void kill();

	/**
	 * An identifier the matchmaking API can use to identify this particular game session for management tasks, like
	 * cleaning up a game (closing its sockets) or sending in-game messages.
	 *
	 * @return The ID
	 */
	String getGameId();

	/**
	 * How long should this session last before it is closed due to inactivity?
	 *
	 * @return The timeout, in milliseconds.
	 */
	long getNoActivityTimeout();

	/**
	 * Gets a reference to the {@link ServerGameContext} hosted by this session.
	 *
	 * @return A {@link ServerGameContext}
	 * @see net.demilich.metastone.game.GameContext for more about game contexts.
	 */
	ServerGameContext getGameContext();

	/**
	 * Gets a {@link Player} entity inside the game that corresponds to the specified user ID.
	 *
	 * @param userId The user ID to look up.
	 * @return A {@link Player} entity, by reference, that corresponds to the specified user ID.
	 */
	Player getPlayer(String userId);

	/**
	 * Gets the {@link GameAction} that corresponds to the specified {@code actionIndex} that the connected {@link
	 * Client} was prompted for.
	 *
	 * @param messageId   The message where a {@link net.demilich.metastone.game.behaviour.Behaviour#requestAction(GameContext,
	 *                    Player, List)} was made.
	 * @param actionIndex The index that the player chose.
	 * @return The corresponding {@link GameAction}.
	 * @see net.demilich.metastone.game.logic.GameLogic#performGameAction(int, GameAction) for more about game actions.
	 */
	GameAction getActionForMessage(String messageId, int actionIndex);

	int getPlayerIdForSocket(Object socket);
}
