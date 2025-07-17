package com.kuma.tools.dynamicHotCompute.elasticsearch.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "hotkey")
public class HotKeyEntity {
    @Id
    private String id;
    @Field(analyzer="ik_max_word", type = FieldType.Text)
    private String key;
    @Field(type = FieldType.Long)
    private long timestamp;
    @Field(type = FieldType.Integer)
    private int count;
}
