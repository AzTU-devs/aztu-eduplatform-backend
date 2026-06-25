package com.eduplatform.eduplatform_backend.video.web.mapper;

import com.eduplatform.eduplatform_backend.video.domain.Video;
import com.eduplatform.eduplatform_backend.video.web.dto.VideoDto;
import org.springframework.stereotype.Component;

/** Manual mapper — derives the {@code uploadPath} from the video id. */
@Component
public class VideoMapper {

    public VideoDto toDto(Video v) {
        return new VideoDto(
                v.getId(),
                v.getOwnerUserId(),
                v.getTitle(),
                v.getStorage(),
                v.getMimeType(),
                v.getByteSize(),
                v.getDurationSec(),
                v.getStatus(),
                v.getId() == null ? null : "/api/videos/" + v.getId() + "/content",
                v.getCreatedAt(),
                v.getUpdatedAt());
    }
}
