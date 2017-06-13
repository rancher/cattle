package io.cattle.platform.docker.client;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class DockerImageTest {
    @Test
    public void testParsingImage(){
        Map<String, String> testCase = new HashMap<>();
        testCase.put("test.com/repo:tag", "test.com");
        testCase.put("test:5000/repo:tag", "test:5000");
        testCase.put("test:5000/repo", "test:5000");
        testCase.put("test:5000/repo@sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "test:5000");
        testCase.put("test:5000/repo:tag@sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "test:5000");
        testCase.put("sub-dom1.foo.com/bar/baz/quux", "sub-dom1.foo.com");
        testCase.put("sub-dom1.foo.com/bar/baz/quux:some-long-tag", "sub-dom1.foo.com");
        testCase.put("b.gcr.io/test.example.com/my-app:test.example.com", "b.gcr.io");
        testCase.put("xn--n3h.com/myimage:xn--n3h.com", "xn--n3h.com");
        testCase.put("foo/foo_bar.com:8080", "index.docker.io");
        testCase.put("foo/foo_bar.com", "index.docker.io");
        testCase.put("xn--7o8h.com/myimage:xn--7o8h.com@sha512:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "xn--7o8h.com");
        for (String key : testCase.keySet()) {
            DockerImage image = DockerImage.parse(key);
            assertEquals(image.getServer(), testCase.get(key));
        }
        Map<String, String> invalidTestCase = new HashMap<>();
        invalidTestCase.put("IamAbadImageWithalonglonglonglonglonglonglonglonglonglonglonglonglonglonglongNameAndColonEnd:", "index.docker.io");
        invalidTestCase.put("IamAbadImageWithalonglonglonglonglonglonglonglonglonglonglonglonglonglonglongNameAndQuestionMarkEnd?", "index.docker.io");
        invalidTestCase.put("IamAbadImageWithalonglonglonglonglonglonglonglonglonglonglonglonglonglonglongNameAndCommaEnd,", "index.docker.io");
        invalidTestCase.put("IamAbadImageWithalonglonglonglonglonglonglonglonglonglonglonglonglonglonglongNameAndSemicolonEnd;", "index.docker.io");
        invalidTestCase.put("IamAbadImageWithalonglonglonglonglonglonglonglonglonglonglonglonglonglonglongNameAndPeriodEnd.", "index.docker.io");
        invalidTestCase.put("IamAbadImageWithalonglonglonglonglonglonglonglonglonglonglonglonglonglonglongNameAndApostropheEnd`", "index.docker.io");
        invalidTestCase.put("IamAbadImageWithalonglonglonglonglonglonglonglonglonglonglonglonglonglonglongNameAndAtEnd@", "index.docker.io");
        invalidTestCase.put("IamAbadImageWithalonglonglonglonglonglonglonglonglonglonglonglonglonglonglongNameAndSpaceEnd ", "index.docker.io");
        invalidTestCase.put("IamAbadImageWithalonglonglonglonglonglonglonglonglonglonglonglonglonglonglongNameAndDoubleSemicolon::tag", "index.docker.io");
        for (String key : invalidTestCase.keySet()) {
            DockerImage image = DockerImage.parse(key);
            assertEquals(image.getServer(), invalidTestCase.get(key));
        }
    }
}