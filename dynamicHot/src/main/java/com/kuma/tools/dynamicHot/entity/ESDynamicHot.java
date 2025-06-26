package com.kuma.tools.dynamicHot.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

@Data
@Document(indexName = "dynamic_hot")
public class ESDynamicHot {
    @Id
    private long id;
    @Field(type = FieldType.Keyword)
    private String key;
    @Field(type = FieldType.Integer)
    private int count;
    @Field(type = FieldType.Date)
    private Date time;

}
