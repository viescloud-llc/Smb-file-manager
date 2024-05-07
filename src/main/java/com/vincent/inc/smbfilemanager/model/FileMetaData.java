package com.vincent.inc.smbfilemanager.model;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vincent.inc.viesspringutils.model.UserModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "file_meta_data")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class FileMetaData extends UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    @Column
    private String originalFilename;
    
    @Column
    private String contentType;
    
    @Column
    private Long size;
    
    @Column(unique = true)
    private String path;

    @Column(columnDefinition = "BIT(1) default false")
    private Boolean publicity;

    @JsonIgnore
    @Transient
    private byte[] data;

    public static FileMetaData fromMultipartFile(MultipartFile file, int userId, byte[] data, boolean publicity) {
        var fileMetaData = FileMetaData.builder()
                           .originalFilename(file.getOriginalFilename())
                           .contentType(file.getContentType())
                           .size(file.getSize())
                           .ownerUserId(userId)
                           .publicity(publicity)
                           .path(String.format("/%s/%s", userId, file.getOriginalFilename()))
                           .data(data)
                           .build();

        return fileMetaData;
    }
}
