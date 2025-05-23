package org.wgd.community.model.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Document(indexName = "order")
public class Order implements Serializable {
    @Id
    private Integer id;

    @Field(type = FieldType.Keyword)
    private Long orderNo;

    @Field(type = FieldType.Integer)
    private Integer orderType;

    @Field(type = FieldType.Long)
    private Long orderAmount;

    @Field(type = FieldType.Text, analyzer = "ik_smart", searchAnalyzer = "ik_max_word")
    private String orderDesc;

    @Field(type = FieldType.Keyword, analyzer = "ik_smart", searchAnalyzer = "ik_max_word")
    private String username;

    @Field(type = FieldType.Keyword, analyzer = "ik_smart", searchAnalyzer = "ik_max_word")
    private String userPhone;

    private Map<String, List<String>> highlights;
}
