package net.demilich.metastone.game.spells;

import java.util.Map;
import java.util.stream.Stream;

import co.paralleluniverse.fibers.Suspendable;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.cards.MinionCard;
import net.demilich.metastone.game.entities.Entity;
import net.demilich.metastone.game.entities.EntityType;
import net.demilich.metastone.game.entities.minions.Minion;
import net.demilich.metastone.game.spells.desc.SpellArg;
import net.demilich.metastone.game.spells.desc.SpellDesc;
import net.demilich.metastone.game.spells.trigger.Trigger;
import net.demilich.metastone.game.targeting.EntityReference;

public class SummonCopySpell extends Spell {

	public static SpellDesc create(EntityReference target) {
		Map<SpellArg, Object> arguments = SpellDesc.build(SummonCopySpell.class);
		arguments.put(SpellArg.TARGET, target);
		return new SpellDesc(arguments);
	}

	@Override
	@Suspendable
	protected void onCast(GameContext context, Player player, SpellDesc desc, Entity source, Entity target) {
		Minion template;
		int boardPosition = SpellUtils.getBoardPosition(context, player, desc, source);
		if (target.getEntityType() == EntityType.CARD) {
			template = ((MinionCard) target.getSourceCard()).summon();
		} else {
			template = (Minion) target;
		}
		int value = desc.getValue(SpellArg.VALUE, context, player, target, source, 1);
		for (int i = 0; i < value; i++) {
			Stream<SpellDesc> subSpells = desc.subSpells();
			Minion clone = template.getCopy();
			clone.clearEnchantments();

			boolean summoned = context.getLogic().summon(player.getId(), clone, null, boardPosition, false);
			if (!summoned) {
				return;
			}
			for (Trigger trigger : context.getTriggersAssociatedWith(template.getReference())) {
				Trigger triggerClone = trigger.clone();
				context.getLogic().addGameEventListener(player, triggerClone, clone);
			}

			subSpells.forEach(subSpell -> {
				SpellUtils.castChildSpell(context, player, subSpell, source, clone);
			});
		}
	}

}
