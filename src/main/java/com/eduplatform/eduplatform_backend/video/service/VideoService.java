package com.eduplatform.eduplatform_backend.video.service;

import com.eduplatform.eduplatform_backend.common.error.Errors;
import com.eduplatform.eduplatform_backend.media.storage.StorageService;
import com.eduplatform.eduplatform_backend.video.domain.Video;
import com.eduplatform.eduplatform_backend.video.repo.VideoRepository;
import com.eduplatform.eduplatform_backend.video.web.dto.VideoInitRequest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** Owner-scoped video library: register metadata, upload bytes, finalise, stream, delete. */
@Service
public class VideoService {

    private final VideoRepository repo;
    private final StorageService storage;

    public VideoService(VideoRepository repo, StorageService storage) {
        this.repo = repo;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public Page<Video> list(UUID ownerId, Pageable pageable) {
        return repo.findAllByOwnerUserIdOrderByCreatedAtDesc(ownerId, pageable);
    }

    @Transactional
    public Video init(VideoInitRequest req, UUID ownerId) {
        Video v = Video.builder()
                .ownerUserId(ownerId)
                .title(req.title())
                .storage(storage.type().name())
                .mimeType(req.mimeType())
                .byteSize(req.sizeBytes())
                .status("PENDING")
                .build();
        v.setId(UUID.randomUUID());
        return repo.save(v);
    }

    @Transactional
    public Video uploadContent(UUID id, MultipartFile file, UUID ownerId) {
        Video v = ownedOrThrow(id, ownerId);
        // Replace any previously stored object (best-effort) before writing the new one.
        if (v.getObjectKey() != null) {
            storage.delete(v.getObjectKey());
        }
        StorageService.Stored stored = storage.store(file);
        String mime = file.getContentType() == null || file.getContentType().isBlank()
                ? "application/octet-stream"
                : file.getContentType();
        v.setObjectKey(stored.objectKey());
        v.setByteSize(stored.size());
        v.setMimeType(mime);
        v.setStorage(storage.type().name());
        v.setStatus("UPLOADED");
        return repo.save(v);
    }

    @Transactional
    public Video complete(UUID id, Integer durationSec, UUID ownerId) {
        Video v = ownedOrThrow(id, ownerId);
        if (durationSec != null) {
            v.setDurationSec(durationSec);
        }
        v.setStatus("READY");
        return repo.save(v);
    }

    @Transactional(readOnly = true)
    public Content loadContent(UUID id, UUID ownerId) {
        Video v = ownedOrThrow(id, ownerId);
        if (v.getObjectKey() == null) {
            throw Errors.notFound("VIDEO_CONTENT_NOT_FOUND", "Video has no uploaded content");
        }
        Resource resource = storage.load(v.getObjectKey());
        String mime = v.getMimeType() == null || v.getMimeType().isBlank()
                ? "application/octet-stream"
                : v.getMimeType();
        return new Content(resource, mime, v.getByteSize());
    }

    @Transactional
    public void delete(UUID id, UUID ownerId) {
        Video v = ownedOrThrow(id, ownerId);
        if (v.getObjectKey() != null) {
            storage.delete(v.getObjectKey());   // best-effort
        }
        repo.delete(v);   // @SQLDelete → soft delete (sets deleted_at)
    }

    private Video ownedOrThrow(UUID id, UUID ownerId) {
        Video v = repo.findById(id)
                .orElseThrow(() -> Errors.notFound("VIDEO_NOT_FOUND", "Video does not exist"));
        if (!ownerId.equals(v.getOwnerUserId())) {
            throw Errors.forbidden("VIDEO_FORBIDDEN", "You do not own this video");
        }
        return v;
    }

    /** A loaded video resource plus the headers needed to serve it. */
    public record Content(Resource resource, String mimeType, long byteSize) {}
}
