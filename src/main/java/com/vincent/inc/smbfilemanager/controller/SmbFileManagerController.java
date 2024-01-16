package com.vincent.inc.smbfilemanager.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vincent.inc.smbfilemanager.model.FileMetaData;
import com.vincent.inc.smbfilemanager.service.FileMetaDataService;
import com.vincent.inc.viesspringutils.exception.HttpResponseThrowers;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("smb")
public class SmbFileManagerController {

    @Autowired
    private FileMetaDataService fileMetaDataService;

    @GetMapping("file")
    @SuppressWarnings("unchecked")
    public ResponseEntity<byte[]> getFileById(
            @RequestHeader int user_id,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String id) {

        var metadata = this.fileMetaDataService.getByCriteria(id, path, fileName, user_id);

        if(!ObjectUtils.isEmpty(metadata))
            return ResponseEntity.ok().header("Content-Type", metadata.getContentType()).body(metadata.getData());
        else
            return (ResponseEntity<byte[]>) HttpResponseThrowers.throwNotFound("File not found");
    }

    @GetMapping("metadata")
    public FileMetaData getMetadata(
            @RequestHeader int user_id,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String id) {
        var metadata = this.fileMetaDataService.getByCriteria(id, path, fileName, user_id);
        if(!ObjectUtils.isEmpty(metadata))
            return metadata;
        else
            return (FileMetaData) HttpResponseThrowers.throwNotFound("Metadata not found");
    }

    @PostMapping("file")
    public FileMetaData uploadFile(@RequestHeader int user_id, @RequestParam("file") MultipartFile file)
            throws IOException {
        var metadata = FileMetaData.fromMultipartFile(file, user_id, file.getBytes());
        this.fileMetaDataService.create(metadata);
        return metadata;
    }

    @PatchMapping("metadata")
    public FileMetaData patchMetaData(
        @RequestHeader int user_id,
        @RequestParam(required = false) String path,
        @RequestParam(required = false) String fileName,
        @RequestParam(required = false) String id,
        @RequestBody FileMetaData fileMetaData) {
        var metadata = this.fileMetaDataService.getByCriteria(id, path, fileName, user_id);

        if(ObjectUtils.isEmpty(metadata))
            HttpResponseThrowers.throwBadRequest("No file metadata found");

        return this.fileMetaDataService.patchMetaData(metadata, fileMetaData);
    }

    @DeleteMapping("file")
    public void deleteFile(
        @RequestHeader int user_id,
        @RequestParam(required = false) String path,
        @RequestParam(required = false) String fileName,
        @RequestParam(required = false) String id) {

        if (!ObjectUtils.isEmpty(id)) {
            var metadata = this.fileMetaDataService.getById(Integer.parseInt(id));
            this.fileMetaDataService.checkIsFileBelongToUser(metadata, user_id);
            this.fileMetaDataService.delete(metadata.getId());
        }

        if (!ObjectUtils.isEmpty(path)) {
            var metadata = this.fileMetaDataService.getByPath(path, user_id);
            if (!ObjectUtils.isEmpty(metadata))
                this.fileMetaDataService.delete(metadata.getId());
        }

        if (!ObjectUtils.isEmpty(fileName)) {
            var metadata = this.fileMetaDataService.getByFileName(fileName, user_id);
            if (!ObjectUtils.isEmpty(metadata))
                this.fileMetaDataService.delete(metadata.getId());
        }
    }
}
