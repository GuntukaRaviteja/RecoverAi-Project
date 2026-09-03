package com.recoverai.backend.dto;

import java.util.List;

public class RecentActivityResponse {

    private List<RecentActivityItem> activities;

    public RecentActivityResponse() {
    }

    public RecentActivityResponse(
            List<RecentActivityItem> activities
    ) {
        this.activities = activities;
    }

    public List<RecentActivityItem> getActivities() {
        return activities;
    }

    public void setActivities(
            List<RecentActivityItem> activities
    ) {
        this.activities = activities;
    }
}