package net.demilich.metastone.game.spells.desc.filter;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.entities.Entity;
import net.demilich.metastone.game.spells.SpellUtils;

public class ManaCostFilter extends EntityFilter {

	public ManaCostFilter(FilterDesc desc) {
		super(desc);
	}

	@Override
	protected boolean test(GameContext context, Player player, Entity entity, Entity host) {
		Card card = entity.getSourceCard();
		int mana = desc.getValue(FilterArg.VALUE, context, player, null, host, 0);
		Operation operation = (Operation) desc.get(FilterArg.OPERATION);
		int actualValue = context.getLogic().getModifiedManaCost(player, card);
		return SpellUtils.evaluateOperation(operation, actualValue, mana);
	}
}
