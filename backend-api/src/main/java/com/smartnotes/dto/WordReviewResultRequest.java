package com.smartnotes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordReviewResultRequest {

    @NotNull(message = "单词ID不能为空")
    private Long wordId;

    @NotNull(message = "答题结果不能为空")
    private Boolean isCorrect;

    @NotBlank(message = "复习模式不能为空")
    private String mode;
}
