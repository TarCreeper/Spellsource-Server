package com.hiddenswitch.spellsource.util;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import com.hiddenswitch.spellsource.models.MigrationRequest;
import com.hiddenswitch.spellsource.models.MigrationToResponse;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import static io.vertx.ext.sync.Sync.awaitFiber;

public interface Migrator {
	Migrator add(MigrationRequest request);

	void migrateTo(final int version, final Handler<AsyncResult<MigrationToResponse>> response);

	@Suspendable
	default MigrationToResponse migrateTo(int version) throws SuspendExecution, InterruptedException {
		return awaitFiber(h -> migrateTo(version, then -> h.handle(then.succeeded() ? Future.succeededFuture(then.result()) : Future.failedFuture(then.cause()))));
	}
}
