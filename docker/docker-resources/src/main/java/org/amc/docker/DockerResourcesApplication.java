package org.amc.docker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@SpringBootApplication
public class DockerResourcesApplication {



	public static void main(String[] args) {
		SpringApplication.run(DockerResourcesApplication.class, args);
	}
}
