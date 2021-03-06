package net.demilich.metastone.game.spells;

import co.paralleluniverse.fibers.Suspendable;
import net.demilich.metastone.game.entities.heroes.HeroClass;
import net.demilich.metastone.game.targeting.Zones;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.entities.Entity;
import net.demilich.metastone.game.entities.heroes.Hero;
import net.demilich.metastone.game.heroes.powers.HeroPowerCard;
import net.demilich.metastone.game.spells.desc.SpellArg;
import net.demilich.metastone.game.spells.desc.SpellDesc;

public class ChangeHeroPowerSpell extends Spell {

	private static Logger logger = LoggerFactory.getLogger(ChangeHeroPowerSpell.class);

	@Suspendable
	protected void changeHeroPower(GameContext context, String heroPowerCardId, Hero hero) {
		HeroPowerCard heroPower = (HeroPowerCard) SpellUtils.getCardFromContextOrDiscover(context, heroPowerCardId);
		heroPower.setId(context.getLogic().getIdFactory().generateId());
		heroPower.setOwner(hero.getOwner());
		logger.debug("{}'s hero power was changed to {}", hero.getName(), heroPower);
		// The old hero power should be removed from play.
		HeroPowerCard oldHeroPower = hero.getHeroPower();
		context.removeTriggersAssociatedWith(oldHeroPower.getReference(), false);
		hero.getHeroPowerZone().move(oldHeroPower, context.getPlayer(hero.getOwner()).getRemovedFromPlay());
		if (heroPower.getHeroClass() == HeroClass.INHERIT) {
			heroPower.setHeroClass(hero.getHeroClass());
		}
		heroPower.moveOrAddTo(context, Zones.HERO_POWER);
	}

	@Override
	@Suspendable
	protected void onCast(GameContext context, Player player, SpellDesc desc, Entity source, Entity target) {
		String heroPowerName = (String) desc.get(SpellArg.CARD);
		changeHeroPower(context, heroPowerName, player.getHero());
	}
}
