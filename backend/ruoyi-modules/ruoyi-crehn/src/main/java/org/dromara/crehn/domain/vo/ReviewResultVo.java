package org.dromara.crehn.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.ReviewResult;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ReviewResult.class)
public class ReviewResultVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long activityId;
    @ExcelProperty("活动")
    private String activityName;
    private Long categoryId;
    @ExcelProperty("类别")
    private String categoryName;
    private Long projectId;
    @ExcelProperty("项目编号")
    private String projectNo;
    @ExcelProperty("项目名称")
    private String projectName;
    private Long schoolId;
    @ExcelProperty("单位")
    private String schoolName;
    @ExcelProperty("评分数")
    private Integer scoreCount;
    @ExcelProperty("总分")
    private BigDecimal totalScore;
    @ExcelProperty("平均分")
    private BigDecimal averageScore;
    @ExcelProperty("等级")
    private String finalGrade;
    @ExcelProperty("奖项")
    private String awardLevel;
    @ExcelProperty("奖项备注")
    private String awardRemark;
    @ExcelProperty("排名")
    private Integer rankNo;
    private String scoreSummaryJson;
    @ExcelProperty("发布状态")
    private String resultStatus;
    private Boolean showScore;
    private Boolean showRank;
    private Boolean showComment;
    private Long publishedBy;
    @ExcelProperty("发布时间")
    private Date publishedAt;
    @ExcelProperty("备注")
    private String remark;
    private Date createTime;
}
