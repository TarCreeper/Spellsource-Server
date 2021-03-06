package net.demilich.metastone.game.actions;

import co.paralleluniverse.fibers.Suspendable;
import net.demilich.metastone.game.cards.CardType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.demilich.metastone.game.utils.Attribute;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.SpellCard;
import net.demilich.metastone.game.entities.Entity;
import net.demilich.metastone.game.targeting.CardReference;

import java.util.Collections;
import java.util.List;

public abstract class PlayCardAction extends GameAction {

	public static Logger logger = LoggerFactory.getLogger(PlayCardAction.class);

	protected CardReference cardReference;
	private Integer chooseOneOptionIndex = null;

	protected PlayCardAction() {
	}

	public PlayCardAction(CardReference cardReference) {
		this.cardReference = cardReference;
	}

	@Override
	public boolean canBeExecutedOn(GameContext context, Player player, Entity entity) {
		Card card = context.resolveCardReference(getCardReference());
		if (card instanceof SpellCard) {
			SpellCard spellCard = (SpellCard) card;
			return spellCard.canBeCastOn(context, player, entity);
		}

		return true;
	}

	@Override
	@Suspendable
	public void execute(GameContext context, int playerId) {
		Card card = context.resolveCardReference(getCardReference());
		context.setPendingCard(card);

		context.getLogic().playCard(playerId, getCardReference());
		// card was countered, do not actually resolve its effects
		if (!card.hasAttribute(Attribute.COUNTERED)) {
			play(context, playerId);
		}

		context.getLogic().afterCardPlayed(playerId, getCardReference());
		context.setPendingCard(null);
	}

	public CardReference getCardReference() {
		return cardReference;
	}

	public Integer getChooseOneOptionIndex() {
		return chooseOneOptionIndex;
	}

	@Suspendable
	protected abstract void play(GameContext context, int playerId);

	public void setChooseOneOptionIndex(Integer chooseOneOptionIndex) {
		this.chooseOneOptionIndex = chooseOneOptionIndex;
	}

	@Override
	public String toString() {
		return String.format("%s Card: %s Target: %s", getActionType(), cardReference, getTargetReference());
	}

	@Override
	public Entity getSource(GameContext context) {
		return context.resolveCardReference(getCardReference());
	}

	@Override
	public List<Entity> getTargets(GameContext context, int player) {
		final List<Entity> entities = context.resolveTarget(context.getPlayer(player), getSource(context), getTargetReference());
		return entities == null ? Collections.emptyList() : entities;
	}

	@Override
	public String getDescription(GameContext context, int playerId) {
		Card playedCard = context.resolveCardReference(getCardReference());
		String cardName = playedCard != null ? playedCard.getName() : "an unknown card";
		if (playedCard.getCardType() == CardType.SPELL
				&& playedCard.hasAttribute(Attribute.SECRET)) {
			cardName = "a secret";
		}
		return String.format("%s played %s.", context.getActivePlayer().getName(), cardName);
	}
}
