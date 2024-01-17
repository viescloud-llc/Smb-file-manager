package com.vincent.inc.smbfilemanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.smb.session.SmbSessionFactory;

import jcifs.DialectVersion;

@Configuration
public class BeanConfig {
    @Bean
    public SmbSessionFactory smbSessionFactory(
            @Value("${smb.host}") String host,
            @Value("${smb.port}") int port,
            @Value("${smb.domain}") String domain,
            @Value("${smb.username}") String username,
            @Value("${smb.password}") String password,
            @Value("${smb.shareAndDir}") String shareAndDir) {
        SmbSessionFactory smbSessionFactory = new SmbSessionFactory();
        smbSessionFactory.setHost(host);
        smbSessionFactory.setPort(port);
        smbSessionFactory.setDomain(domain);
        smbSessionFactory.setUsername(username);
        smbSessionFactory.setPassword(password);
        smbSessionFactory.setShareAndDir(shareAndDir);
        smbSessionFactory.setSmbMinVersion(DialectVersion.SMB210);
        smbSessionFactory.setSmbMaxVersion(DialectVersion.SMB311);
        return smbSessionFactory;
    }
}
