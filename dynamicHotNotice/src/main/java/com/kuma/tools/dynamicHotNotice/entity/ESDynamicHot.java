package com.kuma.tools.dynamicHotNotice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

@Data
@Document(indexName = "comment")
public class ESDynamicHot {
    @Id
    private long id;
    @Field(type = FieldType.Keyword)
    private String key;
    @Field(type = FieldType.Keyword)
    private int count;
    @Field(type = FieldType.Date)
    private Date time;

}
