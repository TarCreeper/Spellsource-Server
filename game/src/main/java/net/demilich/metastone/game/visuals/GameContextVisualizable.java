package net.demilich.metastone.game.visuals;

import net.demilich.metastone.game.shared.GameNotification;
import net.demilich.metastone.game.shared.NotificationProxy;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.decks.DeckFormat;
import net.demilich.metastone.game.events.GameEvent;
import net.demilich.metastone.game.logic.GameLogic;

import java.util.ArrayList;
import java.util.List;

public class GameContextVisualizable extends GameContext implements GameContextVisuals {
	private final List<GameEvent> gameEvents = new ArrayList<>();

	private boolean blockedByAnimation;

	public GameContextVisualizable(Player player1, Player player2, GameLogic logic, DeckFormat deckFormat) {
		super(player1, player2, logic, deckFormat);
	}

	protected boolean acceptAction(GameAction nextAction) {
		if (!ignoreEvents()) {
			return true;
		}
		while (ignoreEvents()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		return false;
	}

	protected void onGameStateChanged() {
		if (ignoreEvents()) {
			return;
		}

		setBlockedByAnimation(true);
		NotificationProxy.sendNotification(GameNotification.GAME_STATE_UPDATE, this);

		while (blockedByAnimation) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		NotificationProxy.sendNotification(GameNotification.GAME_STATE_LATE_UPDATE, this);
	}


	@Override
	public void fireGameEvent(GameEvent gameEvent) {
		if (ignoreEvents()) {
			return;
		}
		super.fireGameEvent(gameEvent);
		getGameEvents().add(gameEvent);
	}

	public synchronized List<GameEvent> getGameEvents() {
		return gameEvents;
	}

	@Override
	public int getLocalPlayerId() {
		return 0;
	}

	public boolean isBlockedByAnimation() {
		return blockedByAnimation;
	}

	public void setBlockedByAnimation(boolean blockedByAnimation) {
		this.blockedByAnimation = blockedByAnimation;
	}

}
