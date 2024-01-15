package com.vincent.inc.smbfilemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.RestController;

import com.vincent.inc.viesspringutils.ViesApplication;

@SpringBootApplication
@EnableDiscoveryClient
@RestController
public class SmbFileManagerApplication extends ViesApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmbFileManagerApplication.class, args);
	}

	@Override
	public String getApplicationName() {
		return "SMB File Manager";
	}
}
