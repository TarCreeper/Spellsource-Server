package net.demilich.metastone.game.actions;

import co.paralleluniverse.fibers.Suspendable;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.spells.desc.SpellDesc;
import net.demilich.metastone.game.targeting.EntityReference;
import net.demilich.metastone.game.targeting.TargetSelection;

public class PlayChooseOneCardAction extends PlayCardAction implements HasChoiceCard {
	private SpellDesc spell;
	protected final EntityReference cardReference;
	protected final String chosenCard;

	public PlayChooseOneCardAction(SpellDesc spell, Card chooseOneCard, String chosenCard, TargetSelection targetSelection) {
		super(chooseOneCard.getCardReference());
		setActionType(ActionType.SPELL);
		setTargetRequirement(targetSelection);
		this.setSpell(spell);
		this.cardReference = chooseOneCard.getReference();
		this.chosenCard = chosenCard;
	}

	@Override
	@Suspendable
	public void play(GameContext context, int playerId) {
		context.getLogic().castChooseOneSpell(playerId, spell, cardReference, getTargetReference(), chosenCard);
	}

	public SpellDesc getSpell() {
		return spell;
	}

	public void setSpell(SpellDesc spell) {
		this.spell = spell;
	}

	@Override
	public String getChoiceCardId() {
		return chosenCard;
	}
}
