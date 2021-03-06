package com.hiddenswitch.spellsource.client;

import java.util.List;

import com.hiddenswitch.spellsource.common.ClientToServerMessage;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.cards.Card;

public interface SendToServer {
	void sendAction(String id, GameAction action);

	void sendMulligan(String id, Player player, List<Card> discardedCards);

	void sendGenericMessage(ClientToServerMessage message);
}
