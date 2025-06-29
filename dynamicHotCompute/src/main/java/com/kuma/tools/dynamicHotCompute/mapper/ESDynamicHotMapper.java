package com.kuma.tools.dynamicHotCompute.mapper;

import com.kuma.tools.dynamicHotCompute.entity.ESDynamicHot;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ESDynamicHotMapper extends ElasticsearchRepository<ESDynamicHot,String> {
}
