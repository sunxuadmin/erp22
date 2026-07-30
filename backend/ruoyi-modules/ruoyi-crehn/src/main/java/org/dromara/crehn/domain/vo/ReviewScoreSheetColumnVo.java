package org.dromara.crehn.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewScoreSheetColumnVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String key;
    private String label;
    private Boolean visible;
}
