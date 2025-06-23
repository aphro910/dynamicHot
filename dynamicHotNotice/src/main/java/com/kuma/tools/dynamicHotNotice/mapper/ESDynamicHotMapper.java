package com.kuma.tools.dynamicHotNotice.mapper;

import com.kuma.tools.dynamicHotNotice.entity.ESDynamicHot;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ESDynamicHotMapper extends ElasticsearchRepository<ESDynamicHot,String> {
}
