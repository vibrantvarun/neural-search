/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.neuralsearch.bwc;

import java.util.Locale;
import java.util.Optional;
import org.junit.Before;
import org.opensearch.common.settings.Settings;
import org.opensearch.neuralsearch.BaseNeuralSearchIT;
import static org.opensearch.neuralsearch.TestUtils.CLIENT_TIMEOUT_VALUE;
import static org.opensearch.neuralsearch.TestUtils.RESTART_UPGRADE_OLD_CLUSTER;
import static org.opensearch.neuralsearch.TestUtils.BWC_VERSION;
import static org.opensearch.neuralsearch.TestUtils.NEURAL_SEARCH_BWC_PREFIX;
import org.opensearch.test.rest.OpenSearchRestTestCase;

public abstract class AbstractRestartUpgradeRestTestCase extends BaseNeuralSearchIT {
    protected static String testIndex;

    @Before
    protected void setIndex() {
        // Creating index name by concatenating "knn-bwc-" prefix with test method name
        // for all the tests in this sub-project
        testIndex = NEURAL_SEARCH_BWC_PREFIX + getTestName().toLowerCase(Locale.ROOT);
    }

    @Override
    protected final boolean preserveIndicesUponCompletion() {
        return true;
    }

    @Override
    protected final boolean preserveReposUponCompletion() {
        return true;
    }

    @Override
    protected boolean preserveTemplatesUponCompletion() {
        return true;
    }

    @Override
    protected final Settings restClientSettings() {
        return Settings.builder()
                .put(super.restClientSettings())
                // increase the timeout here to 90 seconds to handle long waits for a green
                // cluster health. the waits for green need to be longer than a minute to
                // account for delayed shards
                .put(OpenSearchRestTestCase.CLIENT_SOCKET_TIMEOUT, CLIENT_TIMEOUT_VALUE)
                .build();
    }

    protected static final boolean isRunningAgainstOldCluster() {
        return Boolean.parseBoolean(System.getProperty(RESTART_UPGRADE_OLD_CLUSTER));
    }

    protected final Optional<String> getBWCVersion() {
        return Optional.ofNullable(System.getProperty(BWC_VERSION, null));
    }
}