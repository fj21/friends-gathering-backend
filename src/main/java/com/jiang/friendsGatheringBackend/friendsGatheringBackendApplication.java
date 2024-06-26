package com.jiang.friendsGatheringBackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.jiang.friendsGatheringBackend.mapper")
@EnableScheduling
public class friendsGatheringBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(friendsGatheringBackendApplication.class, args);
	}

}
