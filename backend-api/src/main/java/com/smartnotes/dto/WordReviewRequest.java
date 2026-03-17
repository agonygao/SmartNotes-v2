package com.smartnotes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordReviewRequest {

    private Long bookId;
    private List<Long> wordIds;
    @Builder.Default
    private String mode = "REVIEW";
    @Builder.Default
    private Integer pageSize = 20;
}
