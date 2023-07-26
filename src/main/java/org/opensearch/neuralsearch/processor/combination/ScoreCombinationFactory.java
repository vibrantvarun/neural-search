/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.neuralsearch.processor.combination;

import java.util.Map;
import java.util.Optional;

import org.opensearch.OpenSearchParseException;

/**
 * Abstracts creation of exact score combination method based on technique name
 */
public class ScoreCombinationFactory {

    public static final ScoreCombinationTechnique DEFAULT_METHOD = new ArithmeticMeanScoreCombinationTechnique();

    private final Map<String, ScoreCombinationTechnique> scoreCombinationMethodsMap = Map.of(
        ArithmeticMeanScoreCombinationTechnique.TECHNIQUE_NAME,
        new ArithmeticMeanScoreCombinationTechnique()
    );

    /**
     * Get score combination method by technique name
     * @param technique name of technique
     * @return instance of ScoreCombinationTechnique for technique name
     */
    public ScoreCombinationTechnique createCombination(final String technique) {
        return Optional.ofNullable(scoreCombinationMethodsMap.get(technique))
            .orElseThrow(() -> new OpenSearchParseException("provided combination technique is not supported"));
    }
}