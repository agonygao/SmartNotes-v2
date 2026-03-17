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
public class WordReviewResponse {

    private List<WordReviewItem> words;
    private Boolean hasMore;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordReviewItem {

        private Long wordId;
        private String word;
        private String phonetic;
        private String meaning;
        private String exampleSentence;
        private Integer masteryLevel;
    }
}
