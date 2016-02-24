package org.amc.docker;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = DockerResourcesApplication.class)
@WebAppConfiguration
public class DockerResourcesApplicationTests {

	@Test
	public void contextLoads() {
	}

}
