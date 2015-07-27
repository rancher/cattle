package io.github.ibuildthecloud.gdapi.request.parser;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultApiRequestParserTest {

    private static final String DEFAULT_REQUEST_URL = "http://defaulturl/v1";
    static DefaultApiRequestParser parser;

    HttpServletRequest request;

    @BeforeClass
    public static void setupClass() {
        parser = new DefaultApiRequestParser();
        parser.setAllowClientOverrideHeaders(true);
    }

    @Before
    public void setup() {
        request = mock(HttpServletRequest.class);
        // host header should always be set
        when(request.getHeader(DefaultApiRequestParser.HOST_HEADER)).thenReturn("hostfoo");
        when(request.getRequestURI()).thenReturn("/v1");
        StringBuffer defaultUrl = new StringBuffer(DEFAULT_REQUEST_URL);
        when(request.getRequestURL()).thenReturn(defaultUrl);
    }

    @Test
    public void testXApiRequestUrl() {
        // Test X-API-Request-url basic use case
        when(request.getHeader(DefaultApiRequestParser.DEFAULT_OVERRIDE_URL_HEADER)).thenReturn("http://foo:8080/v1");
        String url = parser.parseRequestUrl(null, request);
        assertEquals("http://foo:8080/v1", url);

        // Test longer request URI
        when(request.getHeader(DefaultApiRequestParser.DEFAULT_OVERRIDE_URL_HEADER)).thenReturn("http://foo:8080/v1/instances");
        url = parser.parseRequestUrl(null, request);
        assertEquals("http://foo:8080/v1/instances", url);
    }

    @Test
    public void testXApiRequestUrlQueryString() {
        // Test X-API-Request-url with query string that needs stripped
        when(request.getHeader(DefaultApiRequestParser.DEFAULT_OVERRIDE_URL_HEADER)).thenReturn("http://foo/v1/instances?bar=true");
        String url = parser.parseRequestUrl(null, request);
        assertEquals("http://foo/v1/instances", url);
    }

    @Test
    public void testXForwardedHost() {
        // Test x-forwarded-proto + x-f-host + x-f-port basic use case
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PROTO_HEADER)).thenReturn("https");
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_HOST_HEADER)).thenReturn("foo");
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PORT_HEADER)).thenReturn("1234");
        String url = parser.parseRequestUrl(null, request);
        assertEquals("https://foo:1234/v1", url);

        // Test x-forwarded-proto + x-f-host (no x-f-port)
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PORT_HEADER)).thenReturn(null);
        url = parser.parseRequestUrl(null, request);
        assertEquals("https://foo/v1", url);

        // Test x-forwarded-proto + x-f-host + x-f-port case where x-f-host also has port and should be overridden
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_HOST_HEADER)).thenReturn("foo:1111");
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PORT_HEADER)).thenReturn("1234");
        url = parser.parseRequestUrl(null, request);
        assertEquals("https://foo:1234/v1", url);

        // Test x-forwarded-proto == https and x-forwarded-port == 443, dont include port in result
        // This is the typical AWS ELB use case
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_HOST_HEADER)).thenReturn("foo");
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PORT_HEADER)).thenReturn("443");
        url = parser.parseRequestUrl(null, request);
        assertEquals("https://foo/v1", url);

        // Test x-forwarded-proto == http and x-forwarded-port == 80
        // This is the typical AWS ELB use case
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PROTO_HEADER)).thenReturn("http");
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PORT_HEADER)).thenReturn("80");
        url = parser.parseRequestUrl(null, request);
        assertEquals("http://foo/v1", url);

        // Test longer request URI
        when(request.getRequestURI()).thenReturn("/v1/instances/");
        url = parser.parseRequestUrl(null, request);
        assertEquals("http://foo/v1/instances/", url);
    }

    @Test
    public void testHost() {
        // Test x-forwarded-proto + Host + x-f-port basic use case
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PROTO_HEADER)).thenReturn("https");
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PORT_HEADER)).thenReturn("1234");
        String url = parser.parseRequestUrl(null, request);
        assertEquals("https://hostfoo:1234/v1", url);

        // Test x-forwarded-proto + Host (no x-f-port)
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PORT_HEADER)).thenReturn(null);
        url = parser.parseRequestUrl(null, request);
        assertEquals("https://hostfoo/v1", url);

        // Test x-forwarded-proto + Host + x-f-port case where x-f-host also has port and should be overridden
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PROTO_HEADER)).thenReturn("https");
        when(request.getHeader(DefaultApiRequestParser.HOST_HEADER)).thenReturn("hostfoo:1111");
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PORT_HEADER)).thenReturn("1234");
        url = parser.parseRequestUrl(null, request);
        assertEquals("https://hostfoo:1234/v1", url);

        // Test longer request URI
        when(request.getRequestURI()).thenReturn("/v1/instances/");
        url = parser.parseRequestUrl(null, request);
        assertEquals("https://hostfoo:1234/v1/instances/", url);
    }

    @Test
    public void testNoXForwardedProto() {
        // Test no x-forwarded-proto present but x-f-host/Host is present
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_HOST_HEADER)).thenReturn("foo");
        String url = parser.parseRequestUrl(null, request);
        assertEquals(DEFAULT_REQUEST_URL, url);

        // Test no x-forwarded-proto present but x-f-host/Host is present
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_HOST_HEADER)).thenReturn(null);
        url = parser.parseRequestUrl(null, request);
        assertEquals(DEFAULT_REQUEST_URL, url);
    }

    @Test
    public void testIPv6() {
        // Test for x-f-host == [::1] (no port in host header)
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PROTO_HEADER)).thenReturn("https");
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_HOST_HEADER)).thenReturn("[::1]");
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PORT_HEADER)).thenReturn("1234");
        String url = parser.parseRequestUrl(null, request);
        assertEquals("https://[::1]:1234/v1", url);

        // Same thing, but with host header
        when(request.getHeader(DefaultApiRequestParser.HOST_HEADER)).thenReturn("[::1]");
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_HOST_HEADER)).thenReturn(null);
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PORT_HEADER)).thenReturn("1234");
        url = parser.parseRequestUrl(null, request);
        assertEquals("https://[::1]:1234/v1", url);

        // Properly override the port that's in the x-f-host header
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_HOST_HEADER)).thenReturn("[::1]:1111");
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PORT_HEADER)).thenReturn("1234");
        url = parser.parseRequestUrl(null, request);
        assertEquals("https://[::1]:1234/v1", url);

        // Same thing, but with host header
        when(request.getHeader(DefaultApiRequestParser.HOST_HEADER)).thenReturn("[::1]:1111");
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_HOST_HEADER)).thenReturn(null);
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PORT_HEADER)).thenReturn("1234");
        url = parser.parseRequestUrl(null, request);
        assertEquals("https://[::1]:1234/v1", url);

        // No x-f-port header
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_HOST_HEADER)).thenReturn("[::1]");
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_PORT_HEADER)).thenReturn(null);
        url = parser.parseRequestUrl(null, request);
        assertEquals("https://[::1]/v1", url);

        // Same thing, but with host header
        when(request.getHeader(DefaultApiRequestParser.HOST_HEADER)).thenReturn("[::1]");
        when(request.getHeader(DefaultApiRequestParser.FORWARDED_HOST_HEADER)).thenReturn(null);
        url = parser.parseRequestUrl(null, request);
        assertEquals("https://[::1]/v1", url);
    }
}
