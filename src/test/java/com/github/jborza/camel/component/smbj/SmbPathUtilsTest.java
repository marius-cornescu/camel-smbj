package com.github.jborza.camel.component.smbj;

import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class SmbPathUtilsTest {

    @Test
    public void removeShareNameTest() {
        assertEquals("dir", SmbPathUtils.removeShareName("HiddenShare$" + File.separator + "dir", "HiddenShare$", false));
        assertEquals("dir", SmbPathUtils.removeShareName("NotSoHiddenShare" + File.separator + "dir", "NotSoHiddenShare", false));
    }

    @Test
    public void getWindowsPath() {
        // given
        char dirPathSeparator = '\\';
        String dirPath = "done\\1\\2\\3\\4";

        // when
        Path path = SmbPathUtils.get(dirPath, dirPathSeparator);

        // then
        assertEquals(5, path.getNameCount());
    }

    @Test
    public void getUnixPath() {
        // given
        char dirPathSeparator = '/';
        String dirPath = "done/1/2/3/4";

        // when
        Path path = SmbPathUtils.get(dirPath, dirPathSeparator);

        // then
        assertEquals(5, path.getNameCount());
    }

    @Test
    public void toStringWindowsPath() {
        // given
        char dirPathSeparator = '\\';
        String dirPath = "done\\1\\2\\3\\4";
        Path path = SmbPathUtils.get(dirPath, dirPathSeparator);

        // when
        String stringPath = SmbPathUtils.toString(path, dirPathSeparator);

        // then
        assertEquals("done\\1\\2\\3\\4", stringPath);
    }

    @Test
    public void toStringUnixPath() {
        // given
        char dirPathSeparator = '/';
        String dirPath = "done/1/2/3/4";
        Path path = SmbPathUtils.get(dirPath, dirPathSeparator);

        // when
        String stringPath = SmbPathUtils.toString(path, dirPathSeparator);

        // then
        assertEquals(12, stringPath.length());
    }

}