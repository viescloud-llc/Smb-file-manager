package com.vincent.inc.smbfilemanager.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vincent.inc.smbfilemanager.model.FileMetaData;
import com.vincent.inc.smbfilemanager.service.FileMetaDataService;
import com.vincent.inc.viesspringutils.exception.HttpResponseThrowers;

import io.github.techgnious.IVCompressor;
import io.github.techgnious.dto.IVSize;
import io.github.techgnious.dto.ImageFormats;
import io.github.techgnious.dto.ResizeResolution;
import io.github.techgnious.dto.VideoFormats;
import io.github.techgnious.exception.ImageException;
import io.github.techgnious.exception.VideoException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
@Slf4j
public class SmbFileManagerController {

    private static final Map<String, VideoFormats> VIDEO_FORMATS = Map.of(
        "mp4", VideoFormats.MP4,
        "mkv", VideoFormats.MKV,
        "avi", VideoFormats.AVI,
        "mov", VideoFormats.MOV,
        "wmv", VideoFormats.WMV,
        "flv", VideoFormats.FLV
    );

    private static final Map<String, ImageFormats> IMAGE_FORMATS = Map.of(
        "png", ImageFormats.PNG,
        "jpg", ImageFormats.JPG,
        "jpeg", ImageFormats.JPEG
    );

    @Autowired
    private FileMetaDataService fileMetaDataService;

    private IVCompressor compressor = new IVCompressor();

    @GetMapping("file")
    @SuppressWarnings("unchecked")
    public ResponseEntity<byte[]> getFileById(
            @RequestHeader(required = false) String user_id,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height,
            @RequestParam(required = false) ResizeResolution resizeResolution,
            @RequestParam(required = false) VideoFormats videoFormat,
            @RequestParam(required = false) ImageFormats imageFormat) {
        int userId = 0;
        if(!ObjectUtils.isEmpty(user_id))
            userId = Integer.parseInt(user_id);
        var metadata = this.fileMetaDataService.getFileByCriteria(id, path, fileName, userId);

        if(!ObjectUtils.isEmpty(resizeResolution)) {
            width = resizeResolution.getWidth();
            height = resizeResolution.getHeight();
        }
        
        if (!ObjectUtils.isEmpty(width) && !ObjectUtils.isEmpty(height)) {
            if(metadata.getContentType().startsWith("image"))
                resizeImage(metadata, imageFormat, width, height);
            else if(metadata.getContentType().startsWith("video"))
                resizeVideo(metadata, videoFormat, width, height);
        }

        if (!ObjectUtils.isEmpty(metadata))
            return ResponseEntity.ok().header("Content-Type", metadata.getContentType()).body(metadata.getData());
        else
            return (ResponseEntity<byte[]>) HttpResponseThrowers.throwNotFound("File not found");
    }

    private void resizeImage(FileMetaData metadata, ImageFormats imageFormat, int width, int height) {
        IVSize customRes = new IVSize();
        customRes.setWidth(width);
        customRes.setHeight(height);

        if(ObjectUtils.isEmpty(imageFormat)) {
            imageFormat = Optional.of(IMAGE_FORMATS.get(metadata.getContentType().split("/")[1].toLowerCase()))
                                  .orElseThrow(() -> HttpResponseThrowers.throwServerErrorException("Unknown image format"));
        }

        try {
            var result = this.compressor.resizeImageWithCustomRes(metadata.getData(), imageFormat, customRes);
            metadata.setData(result);
        } 
        catch (ImageException e) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
            HttpResponseThrowers.throwServerError("Server experience unknown error when resize image");
        }
    }

    private void resizeVideo(FileMetaData metadata, VideoFormats videoFormat, int width, int height) {
        IVSize customRes = new IVSize();
        customRes.setWidth(width);
        customRes.setHeight(height);

        var currentVideoFormat = Optional.of(VIDEO_FORMATS.get(metadata.getContentType().split("/")[1].toLowerCase()))
                                         .orElseThrow(() -> HttpResponseThrowers.throwServerErrorException("Unknown video format"));
        
        if (ObjectUtils.isEmpty(videoFormat))
            videoFormat = currentVideoFormat;

        try {
            var result = metadata.getData();
            if(currentVideoFormat != videoFormat)
                result = this.compressor.convertVideoFormat(result, currentVideoFormat, videoFormat);

            result = this.compressor.reduceVideoSizeWithCustomRes(result, videoFormat, customRes);
            
            metadata.setData(result);
        } 
        catch (VideoException e) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
            HttpResponseThrowers.throwServerError("Server experience unknown error when resize video");
        }
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
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "publicity", required = false) Boolean publicity)
            throws IOException {
        if(ObjectUtils.isEmpty(user_id))
            HttpResponseThrowers.throwUnauthorized("Unauthorized");
        if(publicity == null)
            publicity = false;
        int userId = Integer.parseInt(user_id);
        var metadata = FileMetaData.fromMultipartFile(file, userId, file.getBytes(), publicity);
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
        this.fileMetaDataService.checkIsOwnByUser(metadata, userId);
        this.fileMetaDataService.delete(metadata.getId());
    }
}
