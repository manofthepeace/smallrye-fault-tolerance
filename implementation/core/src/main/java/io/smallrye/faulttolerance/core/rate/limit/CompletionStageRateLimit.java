package io.smallrye.faulttolerance.core.rate.limit;

import static io.smallrye.faulttolerance.core.rate.limit.RateLimitLogger.LOG;
import static io.smallrye.faulttolerance.core.util.CompletionStages.propagateCompletion;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.smallrye.faulttolerance.api.RateLimitException;
import io.smallrye.faulttolerance.api.RateLimitType;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.clock.Clock;

public class CompletionStageRateLimit<V> extends RateLimit<CompletionStage<V>> {
    public CompletionStageRateLimit(FaultToleranceStrategy<CompletionStage<V>> delegate, String description, int maxInvocations,
            long timeWindowInMillis, long minSpacingInMillis, RateLimitType type, Clock clock) {
        super(delegate, description, maxInvocations, timeWindowInMillis, minSpacingInMillis, type, clock);
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) throws Exception {
        LOG.trace("CompletionStageRateLimit started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("CompletionStageRateLimit finished");
        }
    }

    private CompletionStage<V> doApply(InvocationContext<CompletionStage<V>> ctx) {
        CompletableFuture<V> result = new CompletableFuture<>();

        if (timeWindow.record()) {
            try {
                LOG.trace("Task permitted by rate limit");
                ctx.fireEvent(RateLimitEvents.DecisionMade.PERMITTED);
                propagateCompletion(delegate.apply(ctx), result);
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        } else {
            LOG.debug(description + " rate limit exceeded");
            ctx.fireEvent(RateLimitEvents.DecisionMade.REJECTED);
            result.completeExceptionally(new RateLimitException(description + " rate limit exceeded"));
        }

        return result;
    }
}
