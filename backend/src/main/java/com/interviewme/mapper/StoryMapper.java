package com.interviewme.mapper;

import com.interviewme.dto.experience.CreateStoryRequest;
import com.interviewme.dto.experience.StoryResponse;
import com.interviewme.dto.experience.UpdateStoryRequest;
import com.interviewme.model.Story;

public class StoryMapper {

    public static Story toEntity(CreateStoryRequest request) {
        Story story = new Story();
        story.setTitle(request.title());
        story.setSituation(request.situation());
        story.setTask(request.task());
        story.setAction(request.action());
        story.setResult(request.result());
        story.setMetrics(request.metrics());
        story.setVisibility(request.visibility() != null ? request.visibility() : "private");
        return story;
    }

    public static StoryResponse toResponse(Story entity) {
        return new StoryResponse(
            entity.getId(),
            entity.getExperienceProjectId(),
            entity.getTitle(),
            entity.getSituation(),
            entity.getTask(),
            entity.getAction(),
            entity.getResult(),
            entity.getMetrics(),
            entity.getVisibility(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion()
        );
    }

    public static void updateEntity(Story entity, UpdateStoryRequest request) {
        entity.setTitle(request.title());
        entity.setSituation(request.situation());
        entity.setTask(request.task());
        entity.setAction(request.action());
        entity.setResult(request.result());
        entity.setMetrics(request.metrics());
        if (request.visibility() != null) {
            entity.setVisibility(request.visibility());
        }
    }
}
