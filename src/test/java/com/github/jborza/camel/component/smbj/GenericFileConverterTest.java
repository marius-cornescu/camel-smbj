package com.github.jborza.camel.component.smbj;

import org.apache.camel.component.file.GenericFile;
import org.apache.camel.util.FileUtil;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;

public class GenericFileConverterTest {

    @Test
    public void extractTheCorrectRelativePath() throws Exception {
        // given
        String endpoint = "share/dir/relative/path";
        SmbFile file = new SmbFile(false, "iron-maiden.txt", 100, 000,
                false, false, false, false);
        SmbConfiguration configuration = new SmbConfiguration(new URI("smb2://domain;username:password@server/share/dir"));
        String endpointPath = configuration.getShare() + "\\" + configuration.getPath();

        // when
        GenericFile<SmbFile> smbFileGenericFile = new GenericFileConverter().asGenericFile(endpoint, file, endpointPath, "");

        // then
        assertEquals(FileUtil.normalizePath("relative\\path\\iron-maiden.txt"), smbFileGenericFile.getRelativeFilePath());
    }

    @Test
    public void extractTheCorrectRelativePathEmptyEndpoint() throws Exception {
        // given
        String endpoint = "";
        SmbFile file = new SmbFile(false, "iron-maiden.txt", 100, 000,
                false, false, false, false);
        SmbConfiguration configuration = new SmbConfiguration(new URI("smb2://domain;username:password@server/share/dir"));
        String endpointPath = configuration.getShare() + "\\" + configuration.getPath();

        // when
        GenericFile<SmbFile> smbFileGenericFile = new GenericFileConverter().asGenericFile(endpoint, file, endpointPath, "");

        // then
        assertEquals("iron-maiden.txt", smbFileGenericFile.getRelativeFilePath());
    }

    @Test
    public void extractTheCorrectRelativePathEmptyNoRelativePath() throws Exception {
        // given
        String endpoint = "share\\dir";
        SmbFile file = new SmbFile(false, "iron-maiden.txt", 100, 000,
                false, false, false, false);
        SmbConfiguration configuration = new SmbConfiguration(new URI("smb2://domain;username:password@server/share/dir"));
        String endpointPath = configuration.getShare() + "\\" + configuration.getPath();

        // when
        GenericFile<SmbFile> smbFileGenericFile = new GenericFileConverter().asGenericFile(endpoint, file, endpointPath, "");

        // then
        assertEquals("iron-maiden.txt", smbFileGenericFile.getRelativeFilePath());
    }

}