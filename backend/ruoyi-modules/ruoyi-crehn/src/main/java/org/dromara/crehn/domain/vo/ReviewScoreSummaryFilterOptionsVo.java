package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Distinct dynamic-form filter values for the score-summary view. */
@Data
public class ReviewScoreSummaryFilterOptionsVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<String> programForms = new ArrayList<>();
    private List<String> groupOrNatures = new ArrayList<>();
}
