package com.vincent.inc.smbfilemanager.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.vincent.inc.smbfilemanager.model.FileMetaData;

@Repository
public interface FileMetaDataDao extends JpaRepository<FileMetaData, Integer> {
    public List<FileMetaData> findAllByOriginalFilename(String originalFilename);

	public FileMetaData findByContentType(String contentType);
	public List<FileMetaData> findAllByContentType(String contentType);

	public FileMetaData findByPath(String path);
	public List<FileMetaData> findAllByPath(String path);

	public FileMetaData findByPublicity(boolean publicity);
	public List<FileMetaData> findAllByPublicity(boolean publicity);

	@Query(value = "select * from file_meta_data f where f.path like ?1", nativeQuery = true)
	public List<FileMetaData> findAllByUserId(String id);
}
