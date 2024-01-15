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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
        @RequestParam(required = false) String id
        ) throws IOException {

        if(!ObjectUtils.isEmpty(id)) {
            var metadata = this.fileMetaDataService.getById(Integer.parseInt(id));
            this.fileMetaDataService.checkIsFileBelongToUser(metadata, user_id);
            return ResponseEntity.ok().header("Content-Type", metadata.getContentType()).body(metadata.getData());
        }

        if(!ObjectUtils.isEmpty(path)) {
            var metadata = this.fileMetaDataService.getByPath(path, user_id);
            if(!ObjectUtils.isEmpty(metadata))
                return ResponseEntity.ok().header("Content-Type", metadata.getContentType()).body(metadata.getData());
        }

        if(!ObjectUtils.isEmpty(fileName)) {
            var metadata = this.fileMetaDataService.getByFileName(fileName, user_id);
            if(!ObjectUtils.isEmpty(metadata))
                return ResponseEntity.ok().header("Content-Type", metadata.getContentType()).body(metadata.getData());
        }

        return (ResponseEntity<byte[]>) HttpResponseThrowers.throwNotFound("File not found");
    }
    
    @PostMapping("file")
    public FileMetaData uploadFile(@RequestHeader int user_id, @RequestParam("file") MultipartFile file) throws IOException {
        var metadata = FileMetaData.fromMultipartFile(file, user_id, file.getBytes());
        this.fileMetaDataService.create(metadata);
        return metadata;
    } 
}
