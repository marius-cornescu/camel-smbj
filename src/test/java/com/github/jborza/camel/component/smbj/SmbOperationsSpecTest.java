package com.github.jborza.camel.component.smbj;

import com.hierynomus.smbj.SmbConfig;
import org.apache.camel.Message;
import org.apache.camel.component.file.FileComponent;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SmbOperationsSpecTest {

    private SmbOperations ops;
    private SmbClient mockSmbClient;

    @Before
    public void setup() throws Exception {
        SmbConfig config = SmbConfig.builder().build();
        ops = new SmbOperations(config);
        mockSmbClient = mock(SmbClient.class);
        SmbEndpoint endpoint = mock(SmbEndpoint.class);
        endpoint.setConfiguration(new SmbConfiguration(new URI("smb2://host/dir")));
        ops.setEndpoint(endpoint);
        ops.setSmbClient(mockSmbClient);
    }

    @Test
    public void listFilesShouldinvokeKistFilesOnClientTest() throws Exception {
        ops.listFiles("directory");

        verify(mockSmbClient, times(1)).listFiles(Matchers.same("directory"));
    }

    @Test
    public void deleteFileShouldInvokeDeleteOnClientTest() throws Exception {
        String input = "delte/this";
        ops.deleteFile(input);

        verify(mockSmbClient, times(1)).deleteFile(Matchers.same(input));
    }

    @Test
    public void existsFileTest() throws Exception {
        String input = "delte/this";
        ops.existsFile(input);

        verify(mockSmbClient, times(1)).fileExists(Matchers.same(input));
    }

    @Test
    public void storeFileTest() throws Exception {
        DefaultCamelContext ctx = new DefaultCamelContext();
        DefaultExchange exchange = new DefaultExchange(ctx);
        Message message = exchange.getIn();
        InputStream stream = IOUtils.toInputStream("Hello camel-smbj!", Charset.defaultCharset());
        message.setBody(stream);
        exchange.setIn(message);

        String input = "path/to/file";

        ops.storeFile(input, exchange);

        verify(mockSmbClient, times(1)).storeFile(Matchers.same(input), any(InputStream.class));
    }

    @Test
    public void retrieveFileTest() throws Exception {
        DefaultCamelContext ctx = new DefaultCamelContext();
        DefaultExchange exchange = new DefaultExchange(ctx);
        GenericFile<SmbFile> gf = new GenericFile<>();
        exchange.setProperty(FileComponent.FILE_EXCHANGE_FILE, gf);
        String uri = "smb2://user@server.example.com/sharename?password=secret";
        SmbComponent component = mock(SmbComponent.class);
        SmbConfiguration config = new SmbConfiguration(new URI(uri));
        SmbEndpoint endpoint = new SmbEndpoint(uri, component, config);
        ops.setEndpoint(endpoint);
        String input = "path/to/file";
        ops.retrieveFile(input, exchange);

        verify(mockSmbClient, times(1)).retrieveFile(Matchers.same(input), any(OutputStream.class));
    }


    @Test
    public void retrieveFileStreamingTest() throws Exception {
        DefaultCamelContext ctx = new DefaultCamelContext();
        DefaultExchange exchange = new DefaultExchange(ctx);
        GenericFile<SmbFile> gf = new GenericFile<>();
        exchange.setProperty(FileComponent.FILE_EXCHANGE_FILE, gf);
        String uri = "smb2://user@server.example.com/sharename?password=secret";
        SmbComponent component = mock(SmbComponent.class);
        SmbConfiguration config = new SmbConfiguration(new URI(uri));
        config.setStreamDownload(true);

        SmbEndpoint endpoint = new SmbEndpoint(uri, component, config);

        ops.setEndpoint(endpoint);
        String input = "path/to/file";
        ops.retrieveFile(input, exchange);

        verify(mockSmbClient, times(1)).retrieveFileAsStream(Matchers.same(input), eq(exchange));
    }

    @Test
    public void renameFileTest() throws Exception {
        String oldName = "path/to/file/old.txt";
        String newName = "path/to/file/new.txt";
        ops.renameFile(oldName, newName);

        verify(mockSmbClient, times(1)).renameFile(Matchers.same(oldName), same(newName));
    }

    @Test
    public void makeShareTest() {
        SmbShare share = ops.makeSmbShare();
        assertNotNull(share);
    }

    @Test
    public void buildDirTest() throws Exception {
        String input = "share/level1/level2";
        ops.buildDirectory(input, true);
        verify(mockSmbClient, times(1)).mkdirs(Matchers.same(input));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void listFilesTest() {
        ops.listFiles();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getCurrentDirectoryTest() {
        ops.getCurrentDirectory();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void changeToParentDirectory() {
        ops.changeToParentDirectory();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void changeCurrentDirectory() {
        ops.changeCurrentDirectory("blaat");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void deleteFileWithExceptionTest() {
        ops.changeCurrentDirectory("blaat");
    }

    @Test(expected = GenericFileOperationFailedException.class)
    public void deleteFileWithExceptionPartIITest() throws IOException {
        doThrow(new IOException("KABOOM")).when(mockSmbClient).deleteFile(same("file"));
        ops.deleteFile("file");
    }

    @Test(expected = GenericFileOperationFailedException.class)
    public void existsFileWithException() throws IOException {
        doThrow(new IOException("KABOOM")).when(mockSmbClient).fileExists(same("file"));
        ops.existsFile("file");
    }

    @Test(expected = GenericFileOperationFailedException.class)
    public void renameFileWithException() throws IOException {
        doThrow(new IOException("KABOOM")).when(mockSmbClient).renameFile(same("file"), same("newFile"));
        ops.renameFile("file", "newFile");
    }

    @Test(expected = GenericFileOperationFailedException.class)
    public void listFilesWithException() throws IOException {
        doThrow(new IOException("KABOOM")).when(mockSmbClient).listFiles(same("file"));
        ops.listFiles("file");
    }

    @Test(expected = GenericFileOperationFailedException.class)
    public void buildDirectoryWithException() throws IOException {
        doThrow(new IOException("KABOOM")).when(mockSmbClient).mkdirs(same("file"));
        ops.buildDirectory("file", true);
    }

    @Test(expected = GenericFileOperationFailedException.class)
    public void retrieveFileWithException() throws Exception {

        DefaultCamelContext ctx = new DefaultCamelContext();
        DefaultExchange exchange = new DefaultExchange(ctx);
        GenericFile<SmbFile> gf = new GenericFile<>();
        exchange.setProperty(FileComponent.FILE_EXCHANGE_FILE, gf);
        doThrow(new IOException("KABOOM")).when(mockSmbClient).retrieveFile(same("file"), any(OutputStream.class));
        String uri = "smb2://user@server.example.com/sharename?password=secret";
        SmbComponent component = mock(SmbComponent.class);
        SmbConfiguration config = new SmbConfiguration(new URI(uri));
        SmbEndpoint endpoint = new SmbEndpoint(uri, component, config);
        ops.setEndpoint(endpoint);
        ops.retrieveFile("file", exchange);
    }

    @Test(expected = GenericFileOperationFailedException.class)
    public void storeFileWithException() throws Exception {
        DefaultCamelContext ctx = new DefaultCamelContext();
        DefaultExchange exchange = new DefaultExchange(ctx);
        Message message = exchange.getIn();
        InputStream stream = IOUtils.toInputStream("Hello camel-smbj!", Charset.defaultCharset());
        message.setBody(stream);
        exchange.setIn(message);
        String uri = "smb2://user@server.example.com/sharename?password=secret";
        SmbComponent component = mock(SmbComponent.class);
        SmbConfiguration config = new SmbConfiguration(new URI(uri));
        SmbEndpoint endpoint = new SmbEndpoint(uri, component, config);
        ops.setEndpoint(endpoint);
        doThrow(new IOException("KABOOM")).when(mockSmbClient).storeFile(same("path/to/file"), any(InputStream.class));
        ops.storeFile("path/to/file", exchange);
    }

}
