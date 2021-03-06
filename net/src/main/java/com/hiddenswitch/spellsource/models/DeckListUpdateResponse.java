package com.hiddenswitch.spellsource.models;

import java.io.Serializable;

public class DeckListUpdateResponse implements Serializable {
	private final long updatedCount;

	public DeckListUpdateResponse(long updatedCount) {
		this.updatedCount = updatedCount;
	}

	public long getUpdatedCount() {
		return updatedCount;
	}
}
