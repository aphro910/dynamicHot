package com.kuma.tools.dynamicHotCompute.mapper;

import com.kuma.tools.dynamicHotCompute.elasticsearch.entity.HotKeyEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotKeyMapper extends ElasticsearchRepository<HotKeyEntity, Long> {
}
