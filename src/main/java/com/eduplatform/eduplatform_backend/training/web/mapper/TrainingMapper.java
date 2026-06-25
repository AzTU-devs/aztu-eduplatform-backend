package com.eduplatform.eduplatform_backend.training.web.mapper;

import com.eduplatform.eduplatform_backend.training.domain.Training;
import com.eduplatform.eduplatform_backend.training.web.dto.TrainingDto;
import org.springframework.stereotype.Component;

/** Manual mapper — flattens the {@code tutor} association to {@code tutorId}. */
@Component
public class TrainingMapper {

    public TrainingDto toDto(Training t) {
        return new TrainingDto(
                t.getId(),
                t.getTutor() == null ? null : t.getTutor().getId(),
                t.getTitle(),
                t.getDescription(),
                t.getCity(),
                t.getAddressLine(),
                t.getStartDate(),
                t.getEndDate(),
                t.getCapacity(),
                t.getEnrolledCount(),
                t.getPrice(),
                t.getCurrency(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getUpdatedAt());
    }
}
