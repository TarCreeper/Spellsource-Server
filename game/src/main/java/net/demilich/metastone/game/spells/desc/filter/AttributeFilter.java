package net.demilich.metastone.game.spells.desc.filter;

import net.demilich.metastone.game.utils.Attribute;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.entities.Actor;
import net.demilich.metastone.game.entities.Entity;
import net.demilich.metastone.game.entities.weapons.Weapon;
import net.demilich.metastone.game.spells.SpellUtils;
import net.demilich.metastone.game.targeting.EntityReference;

import java.util.List;

public class AttributeFilter extends EntityFilter {

	public AttributeFilter(FilterDesc desc) {
		super(desc);
	}

	@Override
	protected boolean test(GameContext context, Player player, Entity entity, Entity host) {
		EntityReference targetReference = (EntityReference) desc.get(FilterArg.TARGET);
		List<Entity> entities = null;
		if (targetReference != null) {
			entities = context.resolveTarget(player, host, targetReference);
		}
		Attribute attribute = (Attribute) desc.get(FilterArg.ATTRIBUTE);
		Operation operation = (Operation) desc.get(FilterArg.OPERATION);
		if (operation == Operation.HAS) {
			return entity.hasAttribute(attribute);
		}

		int targetValue;
		if (entities == null) {
			targetValue = desc.getInt(FilterArg.VALUE);
		} else {
			targetValue = desc.getValue(FilterArg.VALUE, context, player, entities.get(0), null, 0);
		}

		int actualValue = -1;
		if (attribute == Attribute.ATTACK) {
			if (entity instanceof Weapon) {
				actualValue = ((Weapon) entity).getWeaponDamage();
			} else {
				actualValue = ((Actor) entity).getAttack();
			}
		} else if (attribute == Attribute.HP) {
			if (entity instanceof Weapon) {
				actualValue = ((Weapon) entity).getDurability();
			} else {
				actualValue = ((Actor) entity).getHp();
			}
		} else {
			actualValue = entity.getAttributeValue(attribute);
		}

		return SpellUtils.evaluateOperation(operation, actualValue, targetValue);
	}

}