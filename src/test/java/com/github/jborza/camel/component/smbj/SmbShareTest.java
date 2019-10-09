package com.github.jborza.camel.component.smbj;

import java.io.IOException;

import org.apache.camel.util.FileUtil;
import org.junit.Before;
import org.junit.Test;

import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SmbShareTest {

    private Connection connection;
    private SMBClient client;
    private Session session;
    private ConnectionCache connectionCache;
    private SmbConfiguration config;

    private SmbShare underTest;

    @Before
    public void setUp() throws IOException {
        client = mock(SMBClient.class);
        connectionCache = mock(ConnectionCache.class);
        connection = mock(Connection.class);
        when(connectionCache.getClient()).thenReturn(client);
        when(connectionCache.getConnection("hostname", 1234)).thenReturn(connection);

        session = mock(Session.class);
        when(connection.authenticate(any(AuthenticationContext.class))).thenReturn(session);

        config = mock(SmbConfiguration.class);
        when(config.getShare()).thenReturn("fs/Country/City/Street");
        when(config.getUsername()).thenReturn("user");
        when(config.getPassword()).thenReturn("secret");
        when(config.getHost()).thenReturn("hostname");
        when(config.getPort()).thenReturn(1234);

        when(config.getPathSeparator()).thenReturn('\\');

        underTest = createNonDfs(connectionCache, config);
    }

    @Test
    public void listFiles() {
        // given
        DiskShare diskShare = mock(DiskShare.class);
        when(session.connectShare("fs/Country/City/Street")).thenReturn(diskShare);

        // when
        underTest.mkdirs("Archive");

        // then

    }

    private static SmbShare createNonDfs(ConnectionCache connectionCache, SmbConfiguration config) {
        return new SmbShare(connectionCache, config, false, FileUtil.BUFFER_SIZE);
    }
}