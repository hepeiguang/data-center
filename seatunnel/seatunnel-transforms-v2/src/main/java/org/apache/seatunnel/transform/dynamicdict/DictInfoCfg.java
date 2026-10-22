package org.apache.seatunnel.transform.dynamicdict;


import lombok.Data;

import java.io.Serializable;

@Data
public class DictInfoCfg implements Serializable {

    private String id;

    private String itemType;
    private String itemCode;

    private String itemCnName;
    private String itemEnName;

    private String dictRemark;

    private String refTopicId;

    private String  businessMeaning;
    private String  topicName;


}

