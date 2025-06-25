package com.kuma.tools.dynamicHotNotice.timer;

import cn.hutool.json.JSONUtil;
import com.kuma.tools.dynamicHotNotice.entity.ESDynamicHot;
import com.kuma.tools.dynamicHotNotice.notify.MQNotify;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.SumAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.BucketSelectorPipelineAggregationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;


@Component
public class HotKeyNotice {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;
    @Autowired
    MQNotify mqNotify;
    @Value("${spring.dynamic.hotkey.detect.timerange:60}")
    private String timerange;
    @Value("${spring.dynamic.hotkey.detect.mincount:5}")
    private String minCount;

    private Map<String, Object> params = new HashMap<>();

    @PostConstruct
    public void init() {
        // 创建桶选择器管道聚合（用于过滤）
        params.put("threshold", Double.valueOf(minCount)); // 设置阈值
    }

    @Scheduled(initialDelay = 10000, fixedRate = 10000)
    public void collect() {
        TermsAggregationBuilder aggregation = AggregationBuilders
                .terms("data_row_change")  // 聚合名称
                .field("key")
                .minDocCount(Long.parseLong(minCount));          // 分组字段
        // 创建子聚合：对每个分组中的 "count" 字段求和
        SumAggregationBuilder sumAggregation = AggregationBuilders
                .sum("total_count")        // 子聚合名称（自定义）
                .field("count");           // 求和的字段

        // 将子聚合添加到主聚合中
        aggregation.subAggregation(sumAggregation);

        BucketSelectorPipelineAggregationBuilder bucketSelector =
                PipelineAggregatorBuilders.bucketSelector(
                        "count_filter", // 管道聚合名称
                        org.elasticsearch.common.collect.Map.of("total_count", "total_count"), // 引用子聚合结果
                        new Script(
                                ScriptType.INLINE,
                                "painless",
                                "params.total_count > params.threshold", // 过滤条件脚本
                                params
                        )
                );
        aggregation.subAggregation(bucketSelector);
        // 2. 构建查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.rangeQuery("time").format("epoch_millis").gte(System.currentTimeMillis()-Long.parseLong(timerange)*1000));//X秒内的变更数据
        Query query = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .addAggregation(aggregation)
                .withPageable(PageRequest.of(0, 1))
                .build();
        SearchHits<ESDynamicHot> searchHits = elasticsearchRestTemplate.search(query, ESDynamicHot.class, IndexCoordinates.of("dynamic_hot"));

        // 3. 解析聚合结果
        // 获取主聚合结果
        Terms terms = searchHits.getAggregations().get("data_row_change");

        Set<String> set = new HashSet<>();
        for (Terms.Bucket bucket : terms.getBuckets()) {
            String key = bucket.getKeyAsString();
            set.add(key);
        }

        if (!set.isEmpty()) {
            mqNotify.notice(JSONUtil.toJsonStr(set));
        }
    }
}
