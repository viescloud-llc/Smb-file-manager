package com.vincent.inc.smbfilemanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.smb.session.SmbSessionFactory;
import jcifs.DialectVersion;

@Configuration
public class BeanConfig {
    @Bean
    public SmbSessionFactory smbSessionFactory() {
        SmbSessionFactory smbSession = new SmbSessionFactory();
        smbSession.setHost("10.24.24.2");
        smbSession.setPort(445);
        smbSession.setDomain("cyberpower");
        smbSession.setUsername("smb_file_manager");
        smbSession.setPassword("smb_file_manager");
        smbSession.setShareAndDir("Business/Smb_file_manager");
        smbSession.setSmbMinVersion(DialectVersion.SMB210);
        smbSession.setSmbMaxVersion(DialectVersion.SMB311);
        return smbSession;
    }
}
