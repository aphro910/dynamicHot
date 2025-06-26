package com.kuma.tools.dynamicHot.record;

import com.kuma.tools.dynamicHot.context.HotKeyContext;
import com.kuma.tools.dynamicHot.entity.ESDynamicHot;
import com.kuma.tools.dynamicHot.utils.SnowFlakeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@ConditionalOnProperty(name = "spring.dynamic.hotkey.record.type", havingValue = "elastic")
public class ElasticRecord implements HotRecord {

    @Autowired
    HotKeyContext hotKeyContext;
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Override
    @Async
    public void record(Map<String, Integer> map) {
        List<ESDynamicHot> batchList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            ESDynamicHot esDynamicHot = new ESDynamicHot();
            esDynamicHot.setId(SnowFlakeGenerator.nextId());
            esDynamicHot.setKey(entry.getKey());
            esDynamicHot.setCount(entry.getValue());
            esDynamicHot.setTime(new Date(hotKeyContext.timestamp));
            batchList.add(esDynamicHot);
        }
        elasticsearchRestTemplate.save(batchList);
    }
}
