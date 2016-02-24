package org.amc.docker.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.io.IOException;

@Controller
public class ComposeController {

    @RequestMapping("/compose/{filePath}")
    public ResponseEntity<byte[]> composeDown(@PathVariable("filePath") String filePath) throws IOException {

        System.out.println(filePath);
        ClassPathResource resource = new ClassPathResource("static/compose/" + filePath);
        File downFile = resource.getFile();

        //默认文件名称
        String downFileName = resource.getFilename();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", downFileName);

        return new ResponseEntity<>(FileCopyUtils.copyToByteArray(downFile), headers, HttpStatus.OK);
    }

}
