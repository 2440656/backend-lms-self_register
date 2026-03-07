package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class TenantFeatureFlagsDto {
    private String pk;
    private String sk;
    private String tenant;
    private String name;
    //private featureFlags featureFlags;
  private Map<String, Boolean> featureFlags;


    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Data
    public static class featureFlags
    {
        private boolean aiAssistant;
        private boolean assessmentReport;
        private boolean evaluate;
        private boolean externalCredentials;
        private boolean featuredLeadershipDevelopmentCourses;
        private boolean gamificationBadges;
        private boolean interviewPrep;
        private boolean learningPaths;
        private boolean learningReels;
        private boolean managerInsights;
        private boolean mentoringMarketplace;
        private boolean notifications;
        private boolean profileBasedLearning;
        private boolean quizzes;
        private boolean recommendedCourses;
        private boolean searchEngineFromMLS;
        private boolean selfReportedTraining;
        private boolean skillUpLearningGoals;
        private boolean skillUpMyTeam;
    }


}
