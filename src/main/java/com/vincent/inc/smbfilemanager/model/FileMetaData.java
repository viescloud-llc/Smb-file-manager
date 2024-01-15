package com.vincent.inc.smbfilemanager.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_meta_data")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileMetaData implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    @Column(unique = true)
    private String originalFilename;
    
    @Column
    private String contentType;
    
    @Column
    private Long size;
    
    @Builder.Default
    @Column
    private List<Integer> userIds = new ArrayList<>();
    
    @Column(unique = true)
    private String path;

    @Builder.Default
    @Column
    private boolean publicity = false;

    private byte[] data;

    public static FileMetaData fromMultipartFile(MultipartFile file, int userId, byte[] data) {
        return FileMetaData.builder()
                           .originalFilename(file.getOriginalFilename())
                           .contentType(file.getContentType())
                           .size(file.getSize())
                           .userIds(List.of(userId))
                           .path(String.format("/%s/%s", userId, file.getOriginalFilename()))
                           .data(data)
                           .build();
    }
}
