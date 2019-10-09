package com.github.jborza.camel.component.smbj;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class SmbPathUtilsSpecTest {

    @Test
    public void shouldCorrectlyRemoveShareWithWindowsSeparatorTest() {

        assertEquals("", SmbPathUtils.removeShareName("share", "share", true));
        assertEquals("dir\\file.ext", SmbPathUtils.removeShareName("share\\dir\\file.ext", "share", true));
        assertEquals("file.ext", SmbPathUtils.removeShareName("share\\file.ext", "share", true));
        assertEquals("dir\\subdir\\file.ext", SmbPathUtils.removeShareName("share\\dir\\subdir\\file.ext", "share", true));

    }

    @Test
    public void objectShouldNotRemoveShareNameOnMismatchTest() {
        assertEquals("share\\file.ext", SmbPathUtils.removeShareName("share\\file.ext", "asd", true));
    }

    @Test
    public void objectShouldConvertToBackSlashesTest() {
        assertEquals("dir\\dirdeel2\\filename.txt", SmbPathUtils.convertToBackslashes("dir/dirdeel2/filename.txt"));
        assertEquals("dir", SmbPathUtils.convertToBackslashes("dir"));

        assertEquals("\\dir\\\\", SmbPathUtils.convertToBackslashes("\\dir//"));
    }


}
