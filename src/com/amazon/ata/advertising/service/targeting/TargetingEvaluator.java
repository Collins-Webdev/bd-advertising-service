package com.amazon.ata.advertising.service.targeting;

import com.amazon.ata.advertising.service.model.RequestContext;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicate;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicateResult;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Evaluates TargetingPredicates for a given RequestContext.
 */
public class TargetingEvaluator {
    public static final boolean IMPLEMENTED_STREAMS = true;
    public static final boolean IMPLEMENTED_CONCURRENCY = true; // Modifié à true
    private final RequestContext requestContext;
    private final ExecutorService executorService;

    /**
     * Creates an evaluator for targeting predicates.
     * @param requestContext Context that can be used to evaluate the predicates.
     */
    public TargetingEvaluator(RequestContext requestContext) {
        this.requestContext = requestContext;
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Evaluate a TargetingGroup to determine if all of its TargetingPredicates are TRUE or not for the given
     * RequestContext.
     * @param targetingGroup Targeting group for an advertisement, including TargetingPredicates.
     * @return TRUE if all of the TargetingPredicates evaluate to TRUE against the RequestContext, FALSE otherwise.
     */
    public TargetingPredicateResult evaluate(TargetingGroup targetingGroup) {
        List<TargetingPredicate> targetingPredicates = targetingGroup.getTargetingPredicates();

        try {
            // Soumettre toutes les évaluations en parallèle
            List<Future<TargetingPredicateResult>> futures = targetingPredicates.stream()
                    .map(predicate -> executorService.submit(() -> predicate.evaluate(requestContext)))
                    .collect(Collectors.toList());

            // Vérifier que tous les résultats sont TRUE
            for (Future<TargetingPredicateResult> future : futures) {
                if (!future.get().isTrue()) {
                    return TargetingPredicateResult.FALSE;
                }
            }
            return TargetingPredicateResult.TRUE;

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            return TargetingPredicateResult.INDETERMINATE;
        }
    }
}