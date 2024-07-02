package com.vincent.inc.smbfilemanager.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.springframework.integration.smb.session.SmbSession;
import org.springframework.integration.smb.session.SmbSessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.vincent.inc.smbfilemanager.dao.FileMetaDataDao;
import com.vincent.inc.smbfilemanager.model.FileMetaData;
import com.vincent.inc.viesspringutils.exception.HttpResponseThrowers;
import com.vincent.inc.viesspringutils.service.ViesServiceWithUser;
import com.vincent.inc.viesspringutils.util.DatabaseCall;
import com.vincent.inc.viesspringutils.util.ReflectionUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileMetaDataService extends ViesServiceWithUser<FileMetaData, Integer, FileMetaDataDao> {

    private static final String REMOVE_FILE_PATH = "/Trash";

    private final SmbSession smbSession;

    public FileMetaDataService(DatabaseCall<FileMetaData, Integer> databaseCall, FileMetaDataDao repositoryDao,
            SmbSessionFactory smbSessionFactory) {
        super(databaseCall, repositoryDao);
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

    public List<FileMetaData> getAll(int userId) {
        return this.repositoryDao.findAllByUserId("/" + userId + "/%");
    }

    public FileMetaData getFileMetaDataById(int id) {
        var object = this.databaseCall.getAndExpire(id);
        if (ObjectUtils.isEmpty(object)) {
            HttpResponseThrowers.throwBadRequest(String.format("%s Id not found", this.T_TYPE.getSimpleName()));
        }

        return object;
    }

    public FileMetaData getFileMetaDataByPath(String path) {
        var metadatas = this.repositoryDao.findAllByPath(path);

        for (var metadata : metadatas) {
            if (metadata.getPath().equals(path))
                return metadata;
        }

        return null;
    }

    private FileMetaData getFileMetaDataByPathWithTry(String path, int userId, int numTry) {
        if (numTry >= 20)
            return (FileMetaData) HttpResponseThrowers
                    .throwServerError("Server experience unknown error when get by criteria");

        var metadata = this.getFileMetaDataByPath(path);

        if (!ObjectUtils.isEmpty(metadata)) {
            this.checkIsRelatedToUser(metadata, userId);
            return metadata;
        } else if (this.isFileExist(path)) {
            numTry++;
            return getFileMetaDataByPathWithTry(path, userId, numTry);
        } else
            return null;
    }

    public FileMetaData getFileMetaDataByPath(String path, int userId) {
        return getFileMetaDataByPathWithTry(path, userId, 0);
    }

    public FileMetaData getFileMetaDataByName(String fileName, int userId) {
        return this.getFileMetaDataByPath(String.format("/%s/%s", userId, fileName), userId);
    }

    public FileMetaData getFileByPath(String path) {
        var metadata = this.getFileMetaDataByPath(path);
        return ObjectUtils.isEmpty(metadata) ? null : this.processingGetOutput(metadata);
    }

    public FileMetaData getFileByPath(String path, int userId) {
        return getFileByPathWithTry(path, userId, 0);
    }

    private FileMetaData getFileByPathWithTry(String path, int userId, int numTry) {
        var metadata = this.getFileMetaDataByPathWithTry(path, userId, numTry);
        return ObjectUtils.isEmpty(metadata) ? null : this.processingGetOutput(metadata);
    }

    public FileMetaData getFileByName(String fileName, int userId) {
        return this.getFileByPath(String.format("/%s/%s", userId, fileName), userId);
    }

    public FileMetaData getFileMetaDataByCriteria(String id, String path, String fileName, int userId) {

        if (!ObjectUtils.isEmpty(id)) {
            var metadata = this.getFileMetaDataById(Integer.parseInt(id));
            this.checkIsRelatedToUser(metadata, userId);
            return metadata;
        }

        if (!ObjectUtils.isEmpty(path)) {
            var metadata = this.getFileMetaDataByPath(path, userId);
            if (!ObjectUtils.isEmpty(metadata))
                return metadata;
        }

        if (!ObjectUtils.isEmpty(fileName)) {
            var metadata = this.getFileMetaDataByName(fileName, userId);
            if (!ObjectUtils.isEmpty(metadata))
                return metadata;
        }

        return null;
    }

    public FileMetaData getFileByCriteria(String id, String path, String fileName, int userId) {
        var metadata = this.getFileMetaDataByCriteria(id, path, fileName, userId);
        return ObjectUtils.isEmpty(metadata) ? null : this.processingGetOutput(metadata);
    }

    public FileMetaData patchFileMetaData(FileMetaData originalMetadata, FileMetaData newMetaData) {
        isOwnByUser(originalMetadata);
        String newName = newMetaData.getOriginalFilename();
        String originalPath = originalMetadata.getPath();
        int ownerId = originalMetadata.getOwnerUserId();
        String newPath = ObjectUtils.isEmpty(newName) ? null : String.format("/%s/%s", ownerId, newName);
        String newType = ObjectUtils.isEmpty(newName) ? null : this.getContentTypeFromPath(newPath);

        if (!ObjectUtils.isEmpty(newType) && !newType.equals(originalMetadata.getContentType()))
            HttpResponseThrowers.throwBadRequest("New type can't be difference from old type");

        if (!ObjectUtils.isEmpty(newName) && this.isFileExist(newPath))
            HttpResponseThrowers.throwBadRequest("New file name or path already exist");

        newMetaData.setOriginalFilename(newName);
        newMetaData.setPath(newPath);
        ReflectionUtils.patchValue(originalMetadata, newMetaData);
        originalMetadata.setOwnerUserId(ownerId);

        if (!ObjectUtils.isEmpty(newName)) {
            try {
                this.smbSession.rename(originalPath, newPath);
            } catch (IOException e1) {
                log.error(e1.getMessage(), e1);
                try {
                    this.smbSession.remove(newPath);
                } catch (IOException e2) {
                    log.error(e2.getMessage(), e2);
                }
                HttpResponseThrowers.throwServerError("Server experience unexpected error when renaming file");
            }
        }

        return this.databaseCall.saveAndExpire(originalMetadata);
    }

    @Override
    public void delete(Integer id) {
        var fileMetaData = this.getFileMetaDataById(id);
        var ownerId = this.getOwnerUserIdFromPath(fileMetaData.getPath());
        var moveFilePath = String.format("%s/%s/%s", REMOVE_FILE_PATH, ownerId, fileMetaData.getOriginalFilename());
        
        try {
            this.checkIfFileDirectoryExist(moveFilePath);
            
            var toMoveFilePath = moveFilePath;
            int count = 0;
            while(this.smbSession.isFile(toMoveFilePath)) {
                count++;
                toMoveFilePath = addCountToFilePath(moveFilePath, count);
            }
            
            this.smbSession.rename(fileMetaData.getPath(), toMoveFilePath);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            HttpResponseThrowers.throwServerError("Server Experience Unexpected error");
        }

        super.delete(id);
    }

    private String addCountToFilePath(String path, int count) {
        List<String> splits = new ArrayList<>();
        splits.addAll(Arrays.stream(path.split("\\.")).toList());

        if(splits.size() >= 2) {
            int index = splits.size() - 2;
            splits.set(index, String.format("%s (%s)", splits.get(index), count));
        }

        return splits.stream().collect(Collectors.joining("."));
    }

    public boolean isFileExist(String path) {
        try {
            var exist = this.smbSession.isFile(path);

            if (exist) {
                var raw = this.smbSession.readRaw(path);
                var data = IOUtils.toByteArray(raw);
                var contentType = this.getContentTypeFromPath(path);
                var fileName = this.getFileNameFromPath(path);
                var userId = this.getOwnerUserIdFromPath(path);
                long size = data.length;
                var metadata = FileMetaData.builder().publicity(false).contentType(contentType).originalFilename(fileName).path(path).size(size).ownerUserId(userId).build();
                this.databaseCall.saveAndExpire(metadata);
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

    public int getOwnerUserIdFromPath(String path) {
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

    @Override
    public boolean isRelatedToUser(FileMetaData fileMetaData, int userId) {
        return (fileMetaData.getPublicity() != null && fileMetaData.getPublicity() == true) || super.isRelatedToUser(fileMetaData, userId);
    }

    public void checkIfFileDirectoryExist(String path) {
        path = getDirectoryPathFromFilePath(path);
        try {
            if(!this.smbSession.isDirectory(path))
                this.smbSession.mkdir(path);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public String getDirectoryPathFromFilePath(String filePath) {
        List<String> splits = Arrays.stream(filePath.split("/")).toList();
        String lastElement = splits.get(splits.size() - 1);
        if(lastElement.contains("."))
            splits = splits.subList(0, splits.size() - 1);
        
        return splits.stream().collect(Collectors.joining("/"));
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
    protected FileMetaData processingGetOutput(FileMetaData object) {

        try {
            var stream = this.smbSession.readRaw(object.getPath());
            var data = IOUtils.toByteArray(stream);
            object.setData(data);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            HttpResponseThrowers.throwServerError("Server experience unexpected error");
        }

        return object;
    }

    @Override
    protected FileMetaData processingPostInput(FileMetaData object) {
        if (ObjectUtils.isEmpty(object.getData()))
            HttpResponseThrowers.throwBadRequest("File is empty");

        if (this.isFileExist(object.getPath()))
            HttpResponseThrowers.throwBadRequest("File name is already exist");

        return object;
    }

    @Override
    protected FileMetaData processingPostOutput(FileMetaData object) {
        try {
            this.checkIfFileDirectoryExist(object.getPath());
            this.smbSession.write(object.getData(), object.getPath());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            HttpResponseThrowers.throwServerError("Server Experience Unexpected error");
        }

        return object;
    }
}
