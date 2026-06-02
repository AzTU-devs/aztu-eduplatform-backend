package com.eduplatform.eduplatform_backend.media.service;

import com.eduplatform.eduplatform_backend.common.enums.MediaStatus;
import com.eduplatform.eduplatform_backend.common.enums.MediaVisibility;
import com.eduplatform.eduplatform_backend.common.error.Errors;
import com.eduplatform.eduplatform_backend.media.domain.MediaFile;
import com.eduplatform.eduplatform_backend.media.repo.MediaFileRepository;
import com.eduplatform.eduplatform_backend.media.storage.StorageService;
import com.eduplatform.eduplatform_backend.media.web.dto.MediaFileDto;
import com.eduplatform.eduplatform_backend.media.web.mapper.MediaMapper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** Upload + retrieval of binary media (lesson videos, PDFs, etc.). */
@Service
public class MediaService {

    private final MediaFileRepository media;
    private final StorageService storage;
    private final MediaMapper mapper;

    public MediaService(MediaFileRepository media, StorageService storage, MediaMapper mapper) {
        this.media = media;
        this.storage = storage;
        this.mapper = mapper;
    }

    @Transactional
    public MediaFileDto upload(MultipartFile file, UUID ownerId) {
        StorageService.Stored stored = storage.store(file);
        String mime = file.getContentType() == null || file.getContentType().isBlank()
                ? "application/octet-stream"
                : file.getContentType();

        MediaFile m = MediaFile.builder()
                .ownerUserId(ownerId)
                .storage(storage.type())
                .objectKey(stored.objectKey())
                .mimeType(mime)
                .byteSize(stored.size())
                .checksumSha256(stored.sha256())
                .status(MediaStatus.READY)
                .visibility(MediaVisibility.PRIVATE)
                .build();
        m.setId(UUID.randomUUID());
        media.save(m);
        return mapper.toDto(m);
    }

    @Transactional(readOnly = true)
    public Content loadContent(UUID id) {
        MediaFile m = media.findById(id)
                .orElseThrow(() -> Errors.notFound("MEDIA_NOT_FOUND", "Media does not exist"));
        Resource resource = storage.load(m.getObjectKey());
        return new Content(resource, m.getMimeType(), m.getByteSize());
    }

    /** A loaded media resource plus the headers needed to serve it. */
    public record Content(Resource resource, String mimeType, long byteSize) {}
}
