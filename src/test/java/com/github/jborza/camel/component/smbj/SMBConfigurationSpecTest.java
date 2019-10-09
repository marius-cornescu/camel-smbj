package com.github.jborza.camel.component.smbj;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SMBConfigurationSpecTest {

    @Test
    public void processDomainUsernameAndPasswordTest() throws Exception {
        SmbConfiguration smbConfiguration = new SmbConfiguration(new URI("smb2://mydomain;user:password@127.0.0.1"));
        assertEquals("user", smbConfiguration.getUsername());
        assertEquals("password", smbConfiguration.getPassword());
        assertEquals("mydomain", smbConfiguration.getDomain());

        smbConfiguration = new SmbConfiguration(new URI("smb2://user:password@127.0.0.1"));
        assertEquals("user", smbConfiguration.getUsername());
        assertEquals("password", smbConfiguration.getPassword());
        assertNull(smbConfiguration.getDomain());

        smbConfiguration = new SmbConfiguration(new URI("smb2://user@127.0.0.1"));
        assertEquals("user", smbConfiguration.getUsername());
        assertNull(smbConfiguration.getPassword());
        assertNull(smbConfiguration.getDomain());
    }

    @Test
    public void processPortTest() throws Exception {
        SmbConfiguration smbConfiguration = new SmbConfiguration(new URI("smb2://127.0.0.1"));
        assertEquals(445, smbConfiguration.getPort());
        smbConfiguration = new SmbConfiguration(new URI("smb2://127.0.0.1:139"));
        assertEquals(139, smbConfiguration.getPort());
        smbConfiguration = new SmbConfiguration(new URI("smb2://user:password@127.0.0.1:139"));
        assertEquals(139, smbConfiguration.getPort());
        smbConfiguration = new SmbConfiguration(new URI("smb2://a.computer.name:445"));
        assertEquals(445, smbConfiguration.getPort());
    }


    @Test
    public void processHostnameTest() throws Exception {
        SmbConfiguration smbConfiguration = new SmbConfiguration(new URI("smb2://127.0.0.1"));
        assertEquals("127.0.0.1", smbConfiguration.getHost());
        smbConfiguration = new SmbConfiguration(new URI("smb2://a.hostname:123"));
        assertEquals("a.hostname", smbConfiguration.getHost());
        smbConfiguration = new SmbConfiguration(new URI("smb2://user:password@host:445/share/subdir"));
        assertEquals("host", smbConfiguration.getHost());
    }

    @Test
    public void processHiddenSharTest() throws Exception {

        SmbConfiguration smbConfiguration = new SmbConfiguration(new URI("smb2://user:password@host:445/share&/subdir"));
        assertEquals("host", smbConfiguration.getHost());
    }

    @Test
    public void processShareWithSameNameAsSubDirTest() throws Exception {
        SmbConfiguration smbConfiguration = new SmbConfiguration(new URI("smb2://user:password@host:445/share/share/dir"));
        assertEquals("host", smbConfiguration.getHost());
        assertEquals("share/dir", smbConfiguration.getPath());

        smbConfiguration = new SmbConfiguration(new URI("smb2://user:password@host:445/share$/share/dir"));
        assertEquals("host", smbConfiguration.getHost());
        assertEquals("share/dir", smbConfiguration.getPath());

        smbConfiguration = new SmbConfiguration(new URI("smb2://user:password@host:445/share/SHARE/dir"));
        assertEquals("host", smbConfiguration.getHost());
        assertEquals("SHARE/dir", smbConfiguration.getPath());
    }

    @Test
    public void processShareAndPathTest() throws Exception {
        SmbConfiguration smbConfiguration = new SmbConfiguration(new URI("smb2://127.0.0.1"));
        assertEquals("", smbConfiguration.getShare());
        assertEquals("", smbConfiguration.getPath());
        smbConfiguration = new SmbConfiguration(new URI("smb2://127.0.0.1/share"));
        assertEquals("share", smbConfiguration.getShare());
        assertEquals("", smbConfiguration.getPath());
        smbConfiguration = new SmbConfiguration(new URI("smb2://127.0.0.1/share/sub/dir"));
        assertEquals("share", smbConfiguration.getShare());
        assertEquals("sub/dir", smbConfiguration.getPath());
        smbConfiguration = new SmbConfiguration(new URI("smb2://127.0.0.1/hidden_share$/sub/dir"));
        assertEquals("hidden_share$", smbConfiguration.getShare());
        assertEquals("sub/dir", smbConfiguration.getPath());
    }

    @Test
    public void processSmbHostAndPathTest() throws Exception {
        SmbConfiguration smbConfiguration = new SmbConfiguration(new URI("smb2://host"));
        assertEquals("smb://host:445/", smbConfiguration.getSmbHostPath());

        smbConfiguration = new SmbConfiguration(new URI("smb2://host:1321/folder"));
        assertEquals("smb://host:1321/", smbConfiguration.getSmbHostPath());

        smbConfiguration = new SmbConfiguration(new URI("smb2://username:password@host:1321/folder"));
        assertEquals("smb://host:1321/", smbConfiguration.getSmbHostPath());

        smbConfiguration = new SmbConfiguration(new URI("smb2://doman:username:password@a.host.name:1321/folder"));
        assertEquals("smb://a.host.name:1321/", smbConfiguration.getSmbHostPath());
    }

}
