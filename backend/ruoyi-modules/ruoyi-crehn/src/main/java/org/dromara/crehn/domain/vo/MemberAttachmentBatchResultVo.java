package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class MemberAttachmentBatchResultVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String fieldKey;
    private Integer matchedCount = 0;
    private Integer skippedCount = 0;
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String fileName;
        private Long memberId;
        private String matchedValue;
        private String status;
        private String message;
    }
}
