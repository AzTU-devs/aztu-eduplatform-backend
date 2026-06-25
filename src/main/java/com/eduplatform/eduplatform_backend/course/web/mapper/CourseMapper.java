package com.eduplatform.eduplatform_backend.course.web.mapper;

import com.eduplatform.eduplatform_backend.catalog.domain.Category;
import com.eduplatform.eduplatform_backend.catalog.domain.Tag;
import com.eduplatform.eduplatform_backend.common.enums.CourseType;
import com.eduplatform.eduplatform_backend.course.domain.Course;
import com.eduplatform.eduplatform_backend.course.domain.CourseModule;
import com.eduplatform.eduplatform_backend.course.domain.Lesson;
import com.eduplatform.eduplatform_backend.course.domain.OfflineCourseDetails;
import com.eduplatform.eduplatform_backend.course.domain.OnlineCourseDetails;
import com.eduplatform.eduplatform_backend.course.web.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mapping(target = "tutorId", source = "tutor.id")
    @Mapping(target = "tutorDisplayName", expression = "java(course.getTutor() == null ? null : course.getTutor().getUser().getFirstName() + \" \" + course.getTutor().getUser().getLastName())")
    @Mapping(target = "thumbnailMediaId", source = "thumbnail.id")
    @Mapping(target = "totalDurationSec", expression = "java(totalDurationSec(course))")
    CourseSummaryDto toSummaryDto(Course course);

    /**
     * Total course duration in seconds: for ONLINE courses the online details' total video seconds;
     * for OFFLINE courses the total contact hours converted to seconds (× 3600).
     */
    default Integer totalDurationSec(Course course) {
        if (course.getCourseType() == CourseType.ONLINE) {
            OnlineCourseDetails od = course.getOnlineDetails();
            return od == null ? null : od.getTotalVideoSeconds();
        }
        OfflineCourseDetails od = course.getOfflineDetails();
        if (od == null || od.getTotalHours() == null) {
            return null;
        }
        return od.getTotalHours().multiply(java.math.BigDecimal.valueOf(3600)).intValue();
    }

    @Mapping(target = "tutorId", source = "tutor.id")
    @Mapping(target = "tutorDisplayName", expression = "java(course.getTutor() == null ? null : course.getTutor().getUser().getFirstName() + \" \" + course.getTutor().getUser().getLastName())")
    @Mapping(target = "thumbnailMediaId", source = "thumbnail.id")
    @Mapping(target = "trailerMediaId",   source = "trailer.id")
    @Mapping(target = "categoryIds", source = "categories", qualifiedByName = "categoryIds")
    @Mapping(target = "tagIds",      source = "tags",       qualifiedByName = "tagIds")
    @Mapping(target = "onlineDetails",  source = "onlineDetails")
    @Mapping(target = "offlineDetails", source = "offlineDetails")
    @Mapping(target = "modules", expression = "java(toModuleDtos(course.getModules()))")
    CourseDto toDto(Course course);

    OnlineDetailsDto toOnline(OnlineCourseDetails d);
    OfflineDetailsDto toOffline(OfflineCourseDetails d);

    @Mapping(target = "videoMediaId", source = "videoMedia.id")
    LessonDto toLessonDto(Lesson lesson);

    @Mapping(target = "lessons", expression = "java(toLessonDtos(module.getLessons()))")
    ModuleDto toModuleDto(CourseModule module);

    default List<ModuleDto> toModuleDtos(Set<CourseModule> modules) {
        if (modules == null) return List.of();
        return modules.stream()
                .sorted(Comparator.comparingInt(CourseModule::getOrderIndex))
                .map(this::toModuleDto)
                .toList();
    }

    default List<LessonDto> toLessonDtos(Set<Lesson> lessons) {
        if (lessons == null) return List.of();
        return lessons.stream()
                .sorted(Comparator.comparingInt(Lesson::getOrderIndex))
                .map(this::toLessonDto)
                .toList();
    }

    @Named("categoryIds")
    default Set<UUID> categoryIds(Set<Category> categories) {
        return categories == null ? Set.of() : categories.stream().map(Category::getId).collect(Collectors.toSet());
    }

    @Named("tagIds")
    default Set<UUID> tagIds(Set<Tag> tags) {
        return tags == null ? Set.of() : tags.stream().map(Tag::getId).collect(Collectors.toSet());
    }
}
