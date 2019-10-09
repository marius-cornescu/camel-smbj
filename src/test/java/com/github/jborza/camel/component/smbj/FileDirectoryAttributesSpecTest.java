package com.github.jborza.camel.component.smbj;

import com.hierynomus.msdtyp.FileTime;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import org.junit.Test;

import static junit.framework.TestCase.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class FileDirectoryAttributesSpecTest {

    @Test
    public void isDirectoryTest(){
        FileIdBothDirectoryInformation mock = mock(FileIdBothDirectoryInformation.class);

        when(mock.getFileAttributes()).thenReturn(SmbConstants.FILE_ATTRIBUTE_DIRECTORY);
        assertTrue(FileDirectoryAttributes.isDirectory(mock));
        when(mock.getFileAttributes()).thenReturn(0L);
        assertFalse(FileDirectoryAttributes.isDirectory(mock));
    }

    @Test
    public void getLastModifiedTest(){
        FileIdBothDirectoryInformation mock = mock(FileIdBothDirectoryInformation.class);
        when(mock.getLastWriteTime()).thenReturn(FileTime.ofEpochMillis(1261207195000L));
        assertEquals(1261207195000L, FileDirectoryAttributes.getLastModified(mock));
    }

}
