package com.github.jborza.camel.component.smbj;

import org.junit.Test;

import java.net.URI;


import static junit.framework.TestCase.*;
import static org.mockito.Mockito.mock;

public class SmbEndpointSpecTest {

    @Test
    public void shouldReturnCorrectMetadataTest() throws Exception {
        String uri = "smb2://user@server.example.com/sharename?password=secret";
        SmbComponent component = mock(SmbComponent.class);
        SmbConfiguration config = new SmbConfiguration(new URI(uri));
        SmbEndpoint endpoint = new SmbEndpoint(uri, component, config);

        assertEquals("smb2", endpoint.getScheme());
        assertEquals("/".charAt(0), endpoint.getFileSeparator());
        assertTrue(endpoint.isAbsolute(uri));
        assertFalse(endpoint.isSingleton());
    }

    @Test
    public void shouldCreateOperationsTest() throws Exception {
        String uri = "smb2://user@server.example.com/sharename?password=secret";
        SmbComponent component = mock(SmbComponent.class);
        SmbConfiguration config = new SmbConfiguration(new URI(uri));
        SmbEndpoint endpoint = new SmbEndpoint(uri, component, config);
        SmbOperations operations = endpoint.createSmbOperations();

        assertNotNull(operations);
    }

    @Test
    public void dfsAttributeIsSetUpTest() throws Exception {
        String uri = "smb2://server/share?dfs=true";
        SmbComponent component = mock(SmbComponent.class);
        SmbConfiguration config = new SmbConfiguration(new URI(uri));
        SmbEndpoint endpoint = new SmbEndpoint(uri, component, config);
        endpoint.setDfs(true);
        assertTrue(endpoint.isDfs());

        endpoint.setDfs(false);
        assertFalse(endpoint.isDfs());
    }

}
