package net.demilich.metastone.game.events;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.entities.Entity;

public class TargetAcquisitionEvent extends GameEvent {

	private final Entity target;
	private final ActionType actionType;

	public TargetAcquisitionEvent(GameContext context, ActionType actionType, Entity target) {
		super(context);
		this.actionType = actionType;
		this.target = target;
	}

	public ActionType getActionType() {
		return actionType;
	}

	@Override
	public Entity getEventTarget() {
		return getTarget();
	}

	@Override
	public GameEventType getEventType() {
		return GameEventType.TARGET_ACQUISITION;
	}

	public Entity getTarget() {
		return target;
	}

}