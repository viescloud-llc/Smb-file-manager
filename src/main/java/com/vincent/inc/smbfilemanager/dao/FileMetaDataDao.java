package com.vincent.inc.smbfilemanager.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

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

	@Query(value = "select * from  as  where .originalFilename = :originalFilename and .contentType = :contentType and .path = :path and .publicity = :publicity", nativeQuery = true)
	public List<FileMetaData> getAllByMatchAll(@Param("originalFilename") String originalFilename, @Param("contentType") String contentType, @Param("path") String path, @Param("publicity") boolean publicity);

	@Query(value = "select * from  as  where .originalFilename = :originalFilename or .contentType = :contentType or .path = :path or .publicity = :publicity", nativeQuery = true)
	public List<FileMetaData> getAllByMatchAny(@Param("originalFilename") String originalFilename, @Param("contentType") String contentType, @Param("path") String path, @Param("publicity") boolean publicity);
}
