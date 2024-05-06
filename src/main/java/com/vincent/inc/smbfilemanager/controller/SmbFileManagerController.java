package com.vincent.inc.smbfilemanager.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vincent.inc.smbfilemanager.model.FileMetaData;
import com.vincent.inc.smbfilemanager.service.FileMetaDataService;
import com.vincent.inc.viesspringutils.exception.HttpResponseThrowers;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class SmbFileManagerController {

    @Autowired
    private FileMetaDataService fileMetaDataService;

    @GetMapping("file")
    @SuppressWarnings("unchecked")
    public ResponseEntity<byte[]> getFileById(
            @RequestHeader(required = false) String user_id,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String id) {
        int userId = 0;
        if(!ObjectUtils.isEmpty(user_id))
            userId = Integer.parseInt(user_id);
        var metadata = this.fileMetaDataService.getFileByCriteria(id, path, fileName, userId);
        if (!ObjectUtils.isEmpty(metadata))
            return ResponseEntity.ok().header("Content-Type", metadata.getContentType()).body(metadata.getData());
        else
            return (ResponseEntity<byte[]>) HttpResponseThrowers.throwNotFound("File not found");
    }

    @GetMapping("metadata/all")
    public List<FileMetaData> getMetadata(@RequestHeader(required = false) String user_id) {

        if(ObjectUtils.isEmpty(user_id))
            HttpResponseThrowers.throwUnauthorized("Unauthorized");

        int userId = Integer.parseInt(user_id);

        return this.fileMetaDataService.getAll(userId);
    }

    @GetMapping("metadata")
    public FileMetaData getMetadata(
            @RequestHeader(required = false) String user_id,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String id) {
        if(ObjectUtils.isEmpty(user_id))
            HttpResponseThrowers.throwUnauthorized("Unauthorized");
        int userId = Integer.parseInt(user_id);
        var metadata = this.fileMetaDataService.getFileMetaDataByCriteria(id, path, fileName, userId);
        if (!ObjectUtils.isEmpty(metadata))
            return metadata;
        else
            return (FileMetaData) HttpResponseThrowers.throwNotFound("Metadata not found");
    }

    @PostMapping("file")
    public FileMetaData uploadFile(
        @RequestHeader(required = false) String user_id, 
        @RequestParam("file") MultipartFile file)
            throws IOException {
        if(ObjectUtils.isEmpty(user_id))
            HttpResponseThrowers.throwUnauthorized("Unauthorized");
        int userId = Integer.parseInt(user_id);
        var metadata = FileMetaData.fromMultipartFile(file, userId, file.getBytes());
        return this.fileMetaDataService.post(metadata);
    }

    @PatchMapping("metadata")
    public FileMetaData patchMetaData(
            @RequestHeader(required = false) String user_id,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String id,
            @RequestBody FileMetaData fileMetaData) {
        if(ObjectUtils.isEmpty(user_id))
            HttpResponseThrowers.throwUnauthorized("Unauthorized");
        int userId = Integer.parseInt(user_id);
        var metadata = this.fileMetaDataService.getFileMetaDataByCriteria(id, path, fileName, userId);
        if (ObjectUtils.isEmpty(metadata))
            HttpResponseThrowers.throwBadRequest("No file metadata found");
        return this.fileMetaDataService.patchFileMetaData(metadata, fileMetaData);
    }

    @DeleteMapping("file")
    public void deleteFile(
            @RequestHeader(required = false) String user_id,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String id) {
        if(ObjectUtils.isEmpty(user_id))
            HttpResponseThrowers.throwUnauthorized("Unauthorized");
        int userId = Integer.parseInt(user_id);
        var metadata = this.fileMetaDataService.getFileMetaDataByCriteria(id, path, fileName, userId);
        if (ObjectUtils.isEmpty(metadata))
            HttpResponseThrowers.throwBadRequest("No file metadata found");
        this.fileMetaDataService.checkIsFileBelongToUserOwner(metadata, userId);
        this.fileMetaDataService.delete(metadata.getId());
    }
}
