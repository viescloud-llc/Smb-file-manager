package com.vincent.inc.smbfilemanager.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.integration.smb.session.SmbSession;
import org.springframework.integration.smb.session.SmbSessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.vincent.inc.smbfilemanager.dao.FileMetaDataDao;
import com.vincent.inc.smbfilemanager.model.FileMetaData;
import com.vincent.inc.viesspringutils.exception.HttpResponseThrowers;
import com.vincent.inc.viesspringutils.service.ViesService;
import com.vincent.inc.viesspringutils.util.DatabaseUtils;
import com.vincent.inc.viesspringutils.util.ReflectionUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileMetaDataService extends ViesService<FileMetaData, Integer, FileMetaDataDao> {

    private final SmbSessionFactory smbSessionFactory;

    private final SmbSession smbSession;

    public FileMetaDataService(DatabaseUtils<FileMetaData, Integer> databaseUtils, FileMetaDataDao repositoryDao,
            SmbSessionFactory smbSessionFactory) {
        super(databaseUtils, repositoryDao);
        this.smbSessionFactory = smbSessionFactory;
        this.smbSession = smbSessionFactory.getSession();
    }

    @Override
    protected FileMetaData newEmptyObject() {
        return new FileMetaData();
    }

    @Override
    @Deprecated
    public List<FileMetaData> getAll() {
        throw new UnsupportedOperationException("method getAll() should not be use");
    }

    public FileMetaData getByPath(String path) {
        var metadatas = this.repositoryDao.findAllByPath(path);

        for (var metadata : metadatas) {
            if (metadata.getPath().equals(path))
                return this.processingAnyGet(metadata);
        }

        return null;
    }

    public FileMetaData getByCriteria(String id, String path, String fileName, int UserId) {

        if (!ObjectUtils.isEmpty(id)) {
            var metadata = this.getById(Integer.parseInt(id));
            this.checkIsFileBelongToUser(metadata, UserId);
            return metadata;
        }

        if (!ObjectUtils.isEmpty(path)) {
            var metadata = this.getByPath(path, UserId);
            if (!ObjectUtils.isEmpty(metadata))
                return metadata;
        }

        if (!ObjectUtils.isEmpty(fileName)) {
            var metadata = this.getByFileName(fileName, UserId);
            if (!ObjectUtils.isEmpty(metadata))
                return metadata;
        }

        return null;
    }

    public FileMetaData getByPath(String path, int userId) {
        var metadata = this.getByPath(path);

        if (!ObjectUtils.isEmpty(metadata)) {
            this.checkIsFileBelongToUser(metadata, userId);
            return metadata;
        } else if (this.isFileExist(path)) {
            return getByPath(path, userId);
        } else
            return null;
    }

    public FileMetaData getByFileName(String fileName, int userId) {
        return this.getByPath(String.format("/%s/%s", userId, fileName), userId);
    }

    public FileMetaData patchMetaData(FileMetaData originalMetadata, FileMetaData newMetaData) {
        String newName = newMetaData.getOriginalFilename();
        String originalPath = originalMetadata.getPath();
        int ownerId = this.getUserIdFromPath(originalMetadata.getPath());
        String newPath = ObjectUtils.isEmpty(newName) ? null : String.format("/%s/%s", ownerId, newName);
        String newType = ObjectUtils.isEmpty(newName) ? null : this.getContentTypeFromPath(newPath);

        if (!ObjectUtils.isEmpty(newType) && !newType.equals(originalMetadata.getContentType()))
            HttpResponseThrowers.throwBadRequest("New type can't be difference from old type");

        if (!ObjectUtils.isEmpty(newName) && this.isFileExist(newPath))
            HttpResponseThrowers.throwBadRequest("New file name already exist");

        newMetaData = FileMetaData.builder().path(newPath).userIds(newMetaData.getUserIds())
                .publicity(newMetaData.isPublicity()).originalFilename(newName).build();

        if (!newMetaData.getUserIds().stream().anyMatch(e -> e == ownerId))
            newMetaData.getUserIds().add(ownerId);

        ReflectionUtils.patchValue(originalMetadata, newMetaData);

        var saved = this.databaseUtils.saveAndExpire(originalMetadata);

        if (!ObjectUtils.isEmpty(newName)) {
            try {
                this.smbSession.rename(originalPath, newPath);
            } catch (IOException e1) {
                log.error(e1.getMessage(), e1);
            }
        }

        return saved;
    }

    @Override
    public void delete(Integer id) {
        var fileMetaData = this.getById(id);

        try {
            this.smbSession.remove(fileMetaData.getPath());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            HttpResponseThrowers.throwServerError("Server Experience Unexpected error");
        }

        super.delete(id);
    }

    public boolean isFileExist(String path) {
        try {
            var exist = this.smbSession.isFile(path);

            if (exist) {
                var raw = this.smbSession.readRaw(path);
                var data = IOUtils.toByteArray(raw);
                var contentType = this.getContentTypeFromPath(path);
                var fileName = this.getFileNameFromPath(path);
                var userId = this.getUserIdFromPath(path);
                long size = data.length;
                var metadata = FileMetaData.builder().contentType(contentType).originalFilename(fileName).path(path)
                        .size(size).userIds(List.of(userId)).build();
                this.databaseUtils.saveAndExpire(metadata);
            }

            return exist;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public String getFileNameFromPath(String path) {
        var splits = path.split("/");
        return splits[splits.length - 1];
    }

    public int getUserIdFromPath(String path) {
        var splits = path.split("/");
        return Integer.parseInt(splits[1]);
    }

    public String getContentTypeFromPath(String path) {
        try {
            return Files.probeContentType(new File(getFileNameFromPath(path)).toPath());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public boolean isFileExist(String fileName, int userId) {
        String path = String.format("/%s/%s", userId, fileName);
        return isFileExist(path);
    }

    public void checkIsFileBelongToUser(FileMetaData fileMetaData, int userId) {
        if (!fileMetaData.isPublicity() && !fileMetaData.getUserIds().stream().anyMatch(e -> e == userId)) {
            HttpResponseThrowers.throwForbidden("User not allow to access this file");
        }
    }

    public byte[] getData(FileMetaData fileMetaData) {
        InputStream raw;
        byte[] byteArray = null;

        try {
            raw = smbSession.readRaw(fileMetaData.getPath());
            byteArray = IOUtils.toByteArray(raw);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return byteArray;
    }

    @Override
    protected FileMetaData processingAnyGet(FileMetaData object) {

        try {
            var stream = this.smbSession.readRaw(object.getPath());
            var data = IOUtils.toByteArray(stream);
            object.setData(data);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            HttpResponseThrowers.throwServerError("Server experience unexpected error");
        }

        return super.processingAnyGet(object);
    }

    @Override
    protected FileMetaData preProcessingAnyModification(FileMetaData object) {
        if (ObjectUtils.isEmpty(object.getData()))
            HttpResponseThrowers.throwBadRequest("File is empty");

        if (this.isFileExist(object.getPath()))
            HttpResponseThrowers.throwBadRequest("File name is already exist");

        return super.preProcessingAnyModification(object);
    }

    @Override
    protected FileMetaData processingAnyModification(FileMetaData object) {
        try {
            this.smbSession.write(object.getData(), object.getPath());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            HttpResponseThrowers.throwServerError("Server Experience Unexpected error");
        }

        return super.processingAnyModification(object);
    }
}
