package com.github.jborza.camel.component.smbj;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class SmbFileSpecTest {

    @Test
    public void shouldcorrectlyProcessConstructor() {
        SmbFile file = new SmbFile(true, "file.fil", 12345678910L, 1261207195000L,
                false, false, false, false);
        assertTrue(file.isDirectory());
        assertEquals("file.fil", file.getFileName());
        assertEquals(12345678910L, file.getFileLength());
        assertEquals(1261207195000L, file.getLastModified());
        assertFalse(file.isArchive());
        assertFalse(file.isHidden());
        assertFalse(file.isReadOnly());
        assertFalse(file.isSystem());
    }

}
