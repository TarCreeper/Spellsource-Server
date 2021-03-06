package com.blizzard.hearthstone;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.DiscoverAction;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardCatalogue;
import net.demilich.metastone.game.cards.MinionCard;
import net.demilich.metastone.game.entities.heroes.HeroClass;
import net.demilich.metastone.game.entities.minions.Minion;
import net.demilich.metastone.game.heroes.powers.HeroPowerCard;
import net.demilich.metastone.game.targeting.Zones;
import net.demilich.metastone.tests.util.TestBase;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class LeagueOfExplorersTests extends TestBase {
	@Test(description =
			"Tests Sir Finley Mrrgglton and also confirms that players can do stuff to discovered cards besides receive them.")
	public void testSirFinleyMrrgglton() {
		GameContext context = createContext(HeroClass.WHITE, HeroClass.WHITE);
		Player player = context.getActivePlayer();
		int oldId = player.getHero().getHeroPower().getId();
		final DiscoverAction[] action = {null};
		final HeroPowerCard[] discoveryCard = new HeroPowerCard[1];
		final int[] handSize = new int[1];
		// Set up a trick to catch the discover action.
		player.setBehaviour(new TestBehaviour() {
			boolean shouldIntercept = true;

			@Override
			public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
				if (shouldIntercept) {
					Assert.assertTrue(validActions.stream()
							.allMatch(ga -> ga.getActionType() == ActionType.DISCOVER));
					action[0] = (DiscoverAction) validActions.get(0);
					HeroPowerCard original = (HeroPowerCard) action[0].getCard();
					discoveryCard[0] = original;
					handSize[0] = player.getHand().size();
				}
				// We should only intercept once
				shouldIntercept = false;
				return super.requestAction(context, player, validActions);
			}
		});
		MinionCard sirFinley = (MinionCard) CardCatalogue.getCardById("minion_sir_finley_mrrgglton");
		playCard(context, player, sirFinley);
		// Control flow will first go to request action above, then proceed.
		Assert.assertEquals(player.getHand().size(), handSize[0],
				"Nothing should be added to the hand.");
		Assert.assertEquals(player.getDiscoverZone().size(), 0,
				"The discover zone should be empty");
		Assert.assertEquals(player.getGraveyard().size(), 1,
				"The graveyard should only Sir Finley's source card.");
		Assert.assertEquals(discoveryCard[0].getZone(), Zones.REMOVED_FROM_PLAY,
				"The discovered card should be removed from play");
		HeroPowerCard currentHeroPower = player.getHeroPowerZone().get(0);
		Assert.assertEquals(discoveryCard[0].getCardId(), currentHeroPower.getCardId(),
				"But the hero power card should be the discovered hero power.");
		Assert.assertNotEquals(currentHeroPower.getId(), oldId,
				"The old hero power should not be the current one");
	}

	@Test
	public void testSummoningStone() {
		GameContext context = createContext(HeroClass.BLACK, HeroClass.RED);
		Player player = context.getPlayer1();

		playCard(context, player, CardCatalogue.getCardById("minion_summoning_stone"));
		playCard(context, player, CardCatalogue.getCardById("spell_preparation"));
		playCard(context, player, CardCatalogue.getCardById("secret_ice_block"));

		Assert.assertEquals(player.getMinions().size(), 3);
		for (Minion minion : player.getMinions()) {
			if (minion.getSourceCard().getCardId().equalsIgnoreCase("minion_summoning_stone")) {
				continue;
			}

			Assert.assertEquals(minion.getSourceCard().getBaseManaCost(), 0);
		}
	}


	@Test
	public void testCurseOfRafaam() {
		GameContext context = createContext(HeroClass.RED, HeroClass.VIOLET);

		Player player = context.getPlayer1();
		Card koboldGeomancerCard = CardCatalogue.getCardById("minion_kobold_geomancer");
		playCard(context, player, koboldGeomancerCard);
		context.endTurn();

		Player opponent = context.getPlayer2();
		Card curseOfRafaamCard = CardCatalogue.getCardById("spell_curse_of_rafaam");
		playCard(context, opponent, curseOfRafaamCard);
		context.endTurn();

		final int CURSE_OF_RAFAAM_DAMAGE = 2;
		// first player should take exactly 2 damage (NOT 3, because the spell
		// damage should not be applied)
		Assert.assertEquals(player.getHero().getHp(), player.getHero().getMaxHp() - CURSE_OF_RAFAAM_DAMAGE);

	}
}
