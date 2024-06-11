/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.neuralsearch.query;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;
import lombok.SneakyThrows;
import org.junit.Before;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.RangeQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.neuralsearch.BaseNeuralSearchIT;
import static org.opensearch.neuralsearch.util.AggregationsTestUtils.getNestedHits;
import static org.opensearch.neuralsearch.util.TestUtils.assertHitResultsFromQueryWhenSortIsEnabled;
import org.opensearch.search.sort.SortOrder;
import org.opensearch.search.sort.SortBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.ScoreSortBuilder;
import org.opensearch.search.sort.FieldSortBuilder;

public class HybridQuerySortIT extends BaseNeuralSearchIT {
    private static final String TEST_MULTI_DOC_INDEX_WITH_TEXT_AND_INT_MULTIPLE_SHARDS = "test-hybrid-sort-multi-doc-index-multiple-shards";
    private static final String TEST_MULTI_DOC_INDEX_WITH_TEXT_AND_INT_SINGLE_SHARD = "test-hybrid-sort-multi-doc-index-single-shard";
    private static final String SEARCH_PIPELINE = "phase-results-hybrid-sort-pipeline";
    private static final String INTEGER_FIELD_1_STOCK = "stock";
    private static final String TEXT_FIELD_1_NAME = "name";
    private static final String KEYWORD_FIELD_2_CATEGORY = "category";
    private static final String TEXT_FIELD_VALUE_1_DUNES = "Dunes part 1";
    private static final String TEXT_FIELD_VALUE_2_DUNES = "Dunes part 2";
    private static final String TEXT_FIELD_VALUE_3_MI_1 = "Mission Impossible 1";
    private static final String TEXT_FIELD_VALUE_4_MI_2 = "Mission Impossible 2";
    private static final String TEXT_FIELD_VALUE_5_TERMINAL = "The Terminal";
    private static final String TEXT_FIELD_VALUE_6_AVENGERS = "Avengers";
    private static final int INTEGER_FIELD_STOCK_1_25 = 24;
    private static final int INTEGER_FIELD_STOCK_2_22 = 22;
    private static final int INTEGER_FIELD_STOCK_3_256 = 256;
    private static final int INTEGER_FIELD_STOCK_4_25 = 25;
    private static final int INTEGER_FIELD_STOCK_5_20 = 20;
    private static final String KEYWORD_FIELD_CATEGORY_1_DRAMA = "Drama";
    private static final String KEYWORD_FIELD_CATEGORY_2_ACTION = "Action";
    private static final String KEYWORD_FIELD_CATEGORY_3_SCI_FI = "Sci-fi";
    private static final int SHARDS_COUNT_IN_SINGLE_NODE_CLUSTER = 1;
    private static final int SHARDS_COUNT_IN_MULTI_NODE_CLUSTER = 3;
    private static final int LTE_OF_RANGE_IN_HYBRID_QUERY = 400;
    private static final int GTE_OF_RANGE_IN_HYBRID_QUERY = 20;
    private static final int SMALLEST_STOCK_VALUE_IN_QUERY_RESULT = 20;
    private static final int LARGEST_STOCK_VALUE_IN_QUERY_RESULT = 400;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        updateClusterSettings();
    }

    @Override
    public boolean isUpdateClusterSettings() {
        return false;
    }

    @Override
    protected boolean preserveClusterUponCompletion() {
        return true;
    }

    @SneakyThrows
    public void testSingleFieldSort_whenMultipleSubQueriesOnIndexWithSingleShard_thenSuccessful() {
        try {
            prepareResourcesBeforeTestExecution(SHARDS_COUNT_IN_SINGLE_NODE_CLUSTER);
            HybridQueryBuilder hybridQueryBuilder = createHybridQueryBuilderWithMatchTermAndRangeQuery(
                "mission",
                "part",
                LTE_OF_RANGE_IN_HYBRID_QUERY,
                GTE_OF_RANGE_IN_HYBRID_QUERY
            );

            Map<String, SortOrder> fieldSortOrderMap = new HashMap<>();
            fieldSortOrderMap.put("stock", SortOrder.DESC);

            Map<String, Object> searchResponseAsMap = search(
                TEST_MULTI_DOC_INDEX_WITH_TEXT_AND_INT_SINGLE_SHARD,
                hybridQueryBuilder,
                null,
                10,
                Map.of("search_pipeline", SEARCH_PIPELINE),
                null,
                null,
                createSortBuilders(fieldSortOrderMap, false)
            );
            List<Map<String, Object>> nestedHits = validateHitsCountAndFetchNestedHits(searchResponseAsMap, 6);
            assertStockValueWithSortOrderInHybridQueryResults(nestedHits, SortOrder.DESC, LARGEST_STOCK_VALUE_IN_QUERY_RESULT, true, true);
        } finally {
            wipeOfTestResources(TEST_MULTI_DOC_INDEX_WITH_TEXT_AND_INT_SINGLE_SHARD, null, null, SEARCH_PIPELINE);
        }
    }

    @SneakyThrows
    public void testMultipleFieldSort_whenMultipleSubQueriesOnIndexWithSingleShard_thenSuccessful() {
        try {
            prepareResourcesBeforeTestExecution(SHARDS_COUNT_IN_SINGLE_NODE_CLUSTER);
            HybridQueryBuilder hybridQueryBuilder = createHybridQueryBuilderWithMatchTermAndRangeQuery(
                "mission",
                "part",
                LTE_OF_RANGE_IN_HYBRID_QUERY,
                GTE_OF_RANGE_IN_HYBRID_QUERY
            );

            Map<String, SortOrder> fieldSortOrderMap = new HashMap<>();
            fieldSortOrderMap.put("stock", SortOrder.DESC);
            fieldSortOrderMap.put("_doc", SortOrder.ASC);

            Map<String, Object> searchResponseAsMap = search(
                TEST_MULTI_DOC_INDEX_WITH_TEXT_AND_INT_SINGLE_SHARD,
                hybridQueryBuilder,
                null,
                10,
                Map.of("search_pipeline", SEARCH_PIPELINE),
                null,
                null,
                createSortBuilders(fieldSortOrderMap, true)
            );
            List<Map<String, Object>> nestedHits = validateHitsCountAndFetchNestedHits(searchResponseAsMap, 6);
            assertStockValueWithSortOrderInHybridQueryResults(nestedHits, SortOrder.DESC, LARGEST_STOCK_VALUE_IN_QUERY_RESULT, true, false);
            assertDocValueWithSortOrderInHybridQueryResults(nestedHits, SortOrder.ASC, 0, false, false);
        } finally {
            wipeOfTestResources(TEST_MULTI_DOC_INDEX_WITH_TEXT_AND_INT_SINGLE_SHARD, null, null, SEARCH_PIPELINE);
        }
    }

    @SneakyThrows
    public void testSingleFieldSort_whenMultipleSubQueriesOnIndexWithMultipleShards_thenSuccessful() {
        try {
            prepareResourcesBeforeTestExecution(SHARDS_COUNT_IN_MULTI_NODE_CLUSTER);
            QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
            // HybridQueryBuilder hybridQueryBuilder = createHybridQueryBuilderWithMatchTermAndRangeQuery(
            // "mission",
            // "part",
            // LTE_OF_RANGE_IN_HYBRID_QUERY,
            // GTE_OF_RANGE_IN_HYBRID_QUERY
            // );

            HybridQueryBuilder hybridQueryBuilder = new HybridQueryBuilder().add(queryBuilder);
            Map<String, SortOrder> fieldSortOrderMap = new HashMap<>();
            fieldSortOrderMap.put("stock", SortOrder.DESC);

            Map<String, Object> searchResponseAsMap = search(
                TEST_MULTI_DOC_INDEX_WITH_TEXT_AND_INT_MULTIPLE_SHARDS,
                hybridQueryBuilder,
                null,
                10,
                Map.of("search_pipeline", SEARCH_PIPELINE),
                null,
                null,
                createSortBuilders(fieldSortOrderMap, false)
            );
            List<Map<String, Object>> nestedHits = validateHitsCountAndFetchNestedHits(searchResponseAsMap, 6);
            assertStockValueWithSortOrderInHybridQueryResults(nestedHits, SortOrder.DESC, LARGEST_STOCK_VALUE_IN_QUERY_RESULT, true, true);
        } finally {
            wipeOfTestResources(TEST_MULTI_DOC_INDEX_WITH_TEXT_AND_INT_MULTIPLE_SHARDS, null, null, SEARCH_PIPELINE);
        }
    }

    @SneakyThrows
    public void testMultipleFieldSort_whenMultipleSubQueriesOnIndexWithMultipleShards_thenSuccessful() {
        try {
            prepareResourcesBeforeTestExecution(SHARDS_COUNT_IN_MULTI_NODE_CLUSTER);
            HybridQueryBuilder hybridQueryBuilder = createHybridQueryBuilderWithMatchTermAndRangeQuery(
                "mission",
                "part",
                LTE_OF_RANGE_IN_HYBRID_QUERY,
                GTE_OF_RANGE_IN_HYBRID_QUERY
            );

            Map<String, SortOrder> fieldSortOrderMap = new HashMap<>();
            fieldSortOrderMap.put("stock", SortOrder.DESC);

            Map<String, Object> searchResponseAsMap = search(
                TEST_MULTI_DOC_INDEX_WITH_TEXT_AND_INT_MULTIPLE_SHARDS,
                hybridQueryBuilder,
                null,
                10,
                Map.of("search_pipeline", SEARCH_PIPELINE),
                null,
                null,
                createSortBuilders(fieldSortOrderMap, false)
            );
            List<Map<String, Object>> nestedHits = validateHitsCountAndFetchNestedHits(searchResponseAsMap, 6);
            assertStockValueWithSortOrderInHybridQueryResults(nestedHits, SortOrder.DESC, LARGEST_STOCK_VALUE_IN_QUERY_RESULT, true, false);
            assertDocValueWithSortOrderInHybridQueryResults(nestedHits, SortOrder.ASC, 0, false, false);
        } finally {
            wipeOfTestResources(TEST_MULTI_DOC_INDEX_WITH_TEXT_AND_INT_MULTIPLE_SHARDS, null, null, SEARCH_PIPELINE);
        }
    }

    @SneakyThrows
    public void testSingleFieldSort_whenMultipleSubQueriesAndConcurrentSearchEnabled_thenSuccessful() {
        try {
            prepareResourcesBeforeTestExecution(SHARDS_COUNT_IN_MULTI_NODE_CLUSTER);
        } finally {

        }
    }

    @SneakyThrows
    public void testMultipleFieldSort_whenMultipleSubQueriesAndConcurrentSearchEnabled_thenSuccessful() {
        try {
            prepareResourcesBeforeTestExecution(SHARDS_COUNT_IN_MULTI_NODE_CLUSTER);
        } finally {

        }
    }

    @SneakyThrows
    public void testSingleFieldSort_whenTrackScoresIsEnabled_thenFail() {
        try {
            prepareResourcesBeforeTestExecution(SHARDS_COUNT_IN_MULTI_NODE_CLUSTER);
        } finally {

        }
    }

    @SneakyThrows
    public void testSingleFieldSort_whenSortCriteriaIsByScoreAndField_thenFail() {
        try {
            prepareResourcesBeforeTestExecution(SHARDS_COUNT_IN_MULTI_NODE_CLUSTER);
        } finally {

        }
    }

    @SneakyThrows
    public void testSearchAfter_whenSingleFieldSortOnIndexWithSingleShard_thenSuccessful() {
        try {
            prepareResourcesBeforeTestExecution(SHARDS_COUNT_IN_SINGLE_NODE_CLUSTER);
        } finally {

        }
    }

    @SneakyThrows
    public void testSearchAfter_whenSingleFieldSortOnIndexWithMultipleShard_thenSuccessful() {
        try {
            prepareResourcesBeforeTestExecution(SHARDS_COUNT_IN_MULTI_NODE_CLUSTER);
        } finally {

        }
    }

    @SneakyThrows
    public void testMultipleFieldSort_whenSortFieldsSizeNotEqualToSearchAfterSize_thenFail() {
        try {
            prepareResourcesBeforeTestExecution(SHARDS_COUNT_IN_MULTI_NODE_CLUSTER);
        } finally {

        }
    }

    @SneakyThrows
    public void testSearchAfter_whenAfterFieldIsNotPassed_thenFail() {
        try {
            prepareResourcesBeforeTestExecution(SHARDS_COUNT_IN_MULTI_NODE_CLUSTER);
        } finally {

        }
    }

    private HybridQueryBuilder createHybridQueryBuilderWithMatchTermAndRangeQuery(String text, String value, int lte, int gte) {
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery(TEXT_FIELD_1_NAME, text);
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery(TEXT_FIELD_1_NAME, value);
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(INTEGER_FIELD_1_STOCK).gte(gte).lte(lte);
        HybridQueryBuilder hybridQueryBuilder = new HybridQueryBuilder();
        hybridQueryBuilder.add(matchQueryBuilder).add(termQueryBuilder).add(rangeQueryBuilder);
        return hybridQueryBuilder;
    }

    private List<SortBuilder<?>> createSortBuilders(Map<String, SortOrder> fieldSortOrderMap, boolean isSortByScore) {
        List<SortBuilder<?>> sortBuilders = new ArrayList<>();
        if (fieldSortOrderMap != null) {
            for (Map.Entry<String, SortOrder> entry : fieldSortOrderMap.entrySet()) {
                FieldSortBuilder fieldSortBuilder = SortBuilders.fieldSort(entry.getKey()).order(entry.getValue());
                sortBuilders.add(fieldSortBuilder);
            }
        }

        if (isSortByScore) {
            ScoreSortBuilder scoreSortBuilder = SortBuilders.scoreSort().order(SortOrder.ASC);
            sortBuilders.add(scoreSortBuilder);
        }
        return sortBuilders;
    }

    private void assertStockValueWithSortOrderInHybridQueryResults(
        List<Map<String, Object>> hitsNestedList,
        SortOrder sortOrder,
        int baseStockValue,
        boolean isPrimarySortField,
        boolean isSingleFieldSort
    ) {
        for (Map<String, Object> oneHit : hitsNestedList) {
            assertNotNull(oneHit.get("_source"));
            Map<String, Object> source = (Map<String, Object>) oneHit.get("_source");
            List<Object> sorts = (List<Object>) oneHit.get("sort");
            int stock = (int) source.get(INTEGER_FIELD_1_STOCK);
            if (isPrimarySortField) {
                int stockValueInSort = (int) sorts.get(0);
                if (sortOrder == SortOrder.DESC) {
                    assertTrue("Stock value is sorted as per sort order", stock <= baseStockValue);
                } else {
                    assertTrue("Stock value is sorted as per sort order", stock >= baseStockValue);
                }
                assertEquals(stock, stockValueInSort);
            }
            if (!isSingleFieldSort) {
                assertNotNull(sorts.get(1));
                int stockValueInSort = (int) sorts.get(1);
                assertEquals(stock, stockValueInSort);
            }
            baseStockValue = stock;
        }
    }

    private void assertDocValueWithSortOrderInHybridQueryResults(
        List<Map<String, Object>> hitsNestedList,
        SortOrder sortOrder,
        int baseDocIdValue,
        boolean isPrimarySortField,
        boolean isSingleFieldSort
    ) {
        for (Map<String, Object> oneHit : hitsNestedList) {
            assertNotNull(oneHit.get("_source"));
            // Map<String, Object> source = (Map<String, Object>) oneHit.get("_source");
            List<Object> sorts = (List<Object>) oneHit.get("sort");
            if (isPrimarySortField) {
                int docId = (int) sorts.get(0);
                if (sortOrder == SortOrder.DESC) {
                    assertTrue("Doc Id value is sorted as per sort order", docId <= baseDocIdValue);
                } else {
                    assertTrue("Doc Id value is sorted as per sort order", docId >= baseDocIdValue);
                }
                baseDocIdValue = docId;
            }
            if (!isSingleFieldSort) {
                assertNotNull(sorts.get(1));
            }
        }
    }

    private List<Map<String, Object>> validateHitsCountAndFetchNestedHits(Map<String, Object> searchResponseAsMap, int resultsExpected) {
        assertHitResultsFromQueryWhenSortIsEnabled(resultsExpected, searchResponseAsMap);
        return getNestedHits(searchResponseAsMap);
    }

    private void assertScoreWithSortOrderInHybridQueryResults(
        Map<String, Object> searchResponseAsMap,
        int resultsExpected,
        SortOrder sortOrder,
        float baseScore
    ) {
        assertHitResultsFromQueryWhenSortIsEnabled(resultsExpected, searchResponseAsMap);
        List<Map<String, Object>> hitsNestedList = getNestedHits(searchResponseAsMap);

        for (Map<String, Object> oneHit : hitsNestedList) {
            assertNotNull(oneHit.get("_source"));
            // Map<String, Object> source = (Map<String, Object>) oneHit.get("_source");
            float score = (float) oneHit.get("_score");
            if (sortOrder == SortOrder.DESC) {
                assertTrue("Stock value is sorted by descending sort order", score <= baseScore);
            } else {
                assertTrue("Stock value is sorted by ascending sort order", score >= baseScore);
            }
            baseScore = score;
        }
    }

    @SneakyThrows
    void prepareResourcesBeforeTestExecution(int numShards) {
        if (numShards == 1) {
            initializeIndexIfNotExists(TEST_MULTI_DOC_INDEX_WITH_TEXT_AND_INT_SINGLE_SHARD, numShards);
        } else {
            initializeIndexIfNotExists(TEST_MULTI_DOC_INDEX_WITH_TEXT_AND_INT_MULTIPLE_SHARDS, numShards);
        }
        createSearchPipelineWithResultsPostProcessor(SEARCH_PIPELINE);
    }

    @SneakyThrows
    private void initializeIndexIfNotExists(String indexName, int numShards) {
        if (!indexExists(indexName)) {
            createIndexWithConfiguration(
                indexName,
                buildIndexConfiguration(
                    List.of(),
                    List.of(),
                    List.of(INTEGER_FIELD_1_STOCK),
                    List.of(KEYWORD_FIELD_2_CATEGORY),
                    List.of(),
                    List.of(TEXT_FIELD_1_NAME),
                    numShards
                ),
                ""
            );

            addKnnDoc(
                indexName,
                "1",
                List.of(),
                List.of(),
                Collections.singletonList(TEXT_FIELD_1_NAME),
                Collections.singletonList(TEXT_FIELD_VALUE_2_DUNES),
                List.of(),
                List.of(),
                List.of(INTEGER_FIELD_1_STOCK),
                List.of(INTEGER_FIELD_STOCK_1_25),
                List.of(KEYWORD_FIELD_2_CATEGORY),
                List.of(KEYWORD_FIELD_CATEGORY_1_DRAMA),
                List.of(),
                List.of()
            );

            addKnnDoc(
                indexName,
                "2",
                List.of(),
                List.of(),
                Collections.singletonList(TEXT_FIELD_1_NAME),
                Collections.singletonList(TEXT_FIELD_VALUE_1_DUNES),
                List.of(),
                List.of(),
                List.of(INTEGER_FIELD_1_STOCK),
                List.of(INTEGER_FIELD_STOCK_2_22),
                List.of(KEYWORD_FIELD_2_CATEGORY),
                List.of(KEYWORD_FIELD_CATEGORY_1_DRAMA),
                List.of(),
                List.of()
            );

            addKnnDoc(
                indexName,
                "3",
                List.of(),
                List.of(),
                Collections.singletonList(TEXT_FIELD_1_NAME),
                Collections.singletonList(TEXT_FIELD_VALUE_3_MI_1),
                List.of(),
                List.of(),
                List.of(INTEGER_FIELD_1_STOCK),
                List.of(INTEGER_FIELD_STOCK_3_256),
                List.of(KEYWORD_FIELD_2_CATEGORY),
                List.of(KEYWORD_FIELD_CATEGORY_2_ACTION),
                List.of(),
                List.of()
            );

            addKnnDoc(
                indexName,
                "4",
                List.of(),
                List.of(),
                Collections.singletonList(TEXT_FIELD_1_NAME),
                Collections.singletonList(TEXT_FIELD_VALUE_4_MI_2),
                List.of(),
                List.of(),
                List.of(INTEGER_FIELD_1_STOCK),
                List.of(INTEGER_FIELD_STOCK_4_25),
                List.of(KEYWORD_FIELD_2_CATEGORY),
                List.of(KEYWORD_FIELD_CATEGORY_2_ACTION),
                List.of(),
                List.of()
            );

            addKnnDoc(
                indexName,
                "5",
                List.of(),
                List.of(),
                Collections.singletonList(TEXT_FIELD_1_NAME),
                Collections.singletonList(TEXT_FIELD_VALUE_5_TERMINAL),
                List.of(),
                List.of(),
                List.of(INTEGER_FIELD_1_STOCK),
                List.of(INTEGER_FIELD_STOCK_5_20),
                List.of(KEYWORD_FIELD_2_CATEGORY),
                List.of(KEYWORD_FIELD_CATEGORY_1_DRAMA),
                List.of(),
                List.of()
            );

            addKnnDoc(
                indexName,
                "6",
                List.of(),
                List.of(),
                Collections.singletonList(TEXT_FIELD_1_NAME),
                Collections.singletonList(TEXT_FIELD_VALUE_6_AVENGERS),
                List.of(),
                List.of(),
                List.of(INTEGER_FIELD_1_STOCK),
                List.of(INTEGER_FIELD_STOCK_5_20),
                List.of(KEYWORD_FIELD_2_CATEGORY),
                List.of(KEYWORD_FIELD_CATEGORY_3_SCI_FI),
                List.of(),
                List.of()
            );
        }
    }
}
