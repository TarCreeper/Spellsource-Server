package net.demilich.metastone.game.spells;

import co.paralleluniverse.fibers.Suspendable;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardCatalogue;
import net.demilich.metastone.game.cards.CardList;
import net.demilich.metastone.game.cards.CardArrayList;
import net.demilich.metastone.game.entities.Entity;
import net.demilich.metastone.game.spells.desc.SpellArg;
import net.demilich.metastone.game.spells.desc.SpellDesc;
import net.demilich.metastone.game.spells.desc.filter.EntityFilter;
import net.demilich.metastone.game.spells.desc.source.CardSource;
import net.demilich.metastone.game.targeting.Zones;

public class ReceiveCardAndDoSomethingSpell extends Spell {

	@Suspendable
	private void castSomethingSpell(GameContext context, Player player, SpellDesc spell, Entity source, Card card) {
		card = card.getCopy();
		context.getLogic().receiveCard(player.getId(), card);
		// card may be null (i.e. try to draw from deck, but already in
		// fatigue)
		if (card == null || card.getZone() == Zones.GRAVEYARD) {
			return;
		}
		context.setEventCard(card);
		SpellUtils.castChildSpell(context, player, spell, source, card);
		context.setEventCard(null);
	}

	@Override
	@Suspendable
	protected void onCast(GameContext context, Player player, SpellDesc desc, Entity source, Entity target) {
		EntityFilter cardFilter = (EntityFilter) desc.get(SpellArg.CARD_FILTER);
		SpellDesc cardEffectSpell = (SpellDesc) desc.get(SpellArg.SPELL);
		CardSource cardSource = (CardSource) desc.get(SpellArg.CARD_SOURCE);
		CardList cards = CardCatalogue.query(context.getDeckFormat());
		int count = desc.getValue(SpellArg.VALUE, context, player, target, source, 1);
		if (cardSource != null) {
			cards = cardSource.getCards(context, player).getCopy();
		}
		if (cardFilter != null) {
			CardList result = new CardArrayList();
			String replacementCard = (String) desc.get(SpellArg.CARD);
			for (Card card : cards) {
				if (cardFilter.matches(context, player, card, source)) {
					result.addCard(card);
				}
			}
			for (int i = 0; i < count; i++) {
				Card card = null;
				if (!result.isEmpty()) {
					card = result.getRandom();
				} else if (replacementCard != null) {
					card = CardCatalogue.getCardById(replacementCard);
				}
				if (card != null) {
					castSomethingSpell(context, player, cardEffectSpell, source, card);
				}
			}
		} else {
			for (Card card : SpellUtils.getCards(context, desc)) {
				for (int i = 0; i < count; i++) {
					castSomethingSpell(context, player, cardEffectSpell, source, card);
				}
			}
		}
		
	}

}
