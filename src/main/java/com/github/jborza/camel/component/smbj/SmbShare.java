/*
 * Copyright [2018] [Juraj Borza]
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.jborza.camel.component.smbj;

import com.github.jborza.camel.component.smbj.dfs.DfsResolutionResult;
import com.github.jborza.camel.component.smbj.dfs.DfsResolver;
import com.github.jborza.camel.component.smbj.exceptions.AttemptedRenameAcrossSharesException;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.common.SmbPath;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import org.apache.camel.util.IOHelper;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.github.jborza.camel.component.smbj.SmbConstants.CURRENT_DIRECTORY;
import static com.github.jborza.camel.component.smbj.SmbConstants.PARENT_DIRECTORY;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class SmbShare implements AutoCloseable {
    private final ConnectionCache connectionCache;
    private final SmbConfiguration config;
    private final boolean dfs;
    private final int bufferSize;

    private Session session;
    private String path;
    private DiskShare share;

    public SmbShare(SMBClient client, SmbConfiguration config, boolean dfs, int bufferSize) {
        this(new ConnectionCache(client), config, dfs, bufferSize);
    }

    SmbShare(ConnectionCache connectionCache, SmbConfiguration config, boolean dfs, int bufferSize) {
        this.config = config;
        this.dfs = dfs;
        this.bufferSize = bufferSize;
        this.connectionCache = connectionCache;
    }

    private void connect(String targetPath) {
        session = connectSession();
        DfsResolutionResult pathResolutionResult = resolvePlainPath(targetPath);
        String resolvedPath = pathResolutionResult.getSmbPath().getPath();
        if (resolvedPath == null)
            resolvedPath = "";
        path = removeLeadingBackslash(resolvedPath);
        share = pathResolutionResult.getDiskShare();
    }

    private String removeLeadingBackslash(String path) {
        return path.replaceAll("^\\\\", "");
    }

    private DfsResolutionResult resolvePlainPath(String targetPath) {
        String actualPath = SmbPathUtils.removeShareName(SmbPathUtils.convertToBackslashes(targetPath), config.getShare(), true);
        SmbPath targetSmbPath = new SmbPath(config.getHost(), config.getShare(), actualPath);
        return resolvePath(session, targetSmbPath);
    }

    @Override
    public void close() throws IOException {
        if (share != null) {
            share.close();
        }
        //connection.close should close the active sessions
        for (Connection connection : connectionCache.getConnections()) {
            if (connection != null && connection.isConnected()) {
                connection.close(true);
            }
        }
    }

    /**
     * @return The connected share (either resolved by DFS or directly connected)
     */
    public DiskShare getShare() {
        return share;
    }

    /**
     * @return The DFS resolved path, if DFS is used. Otherwise the supplied path is returned
     */
    public String getPath() {
        return path;
    }

    private boolean isDfs() {
        return dfs;
    }

    private Session connectSession() {
        return connectSession(config.getHost(), config.getPort());
    }

    private Session connectSession(String host, int port) {
        try {
            Connection cachedConnection = connectionCache.getConnection(host, port);
            return cachedConnection.authenticate(getAuthenticationContext());
        } catch (IOException e) {
            throw new SmbConnectionException(e);
        }
    }

    private Session connectSession(String host) {
        return connectSession(host, SMBClient.DEFAULT_PORT);
    }

    private AuthenticationContext getAuthenticationContext() {
        String username = config.getUsername();
        String domain = config.getDomain();
        String password = config.getPassword();
        if (username == null)
            throw new IllegalArgumentException("Username cannot be null!");
        if (password == null)
            throw new IllegalArgumentException("Password cannot be null!");
        return new AuthenticationContext(username, password.toCharArray(), domain);
    }

    private DfsResolutionResult resolvePath(Session session, SmbPath path) {
        if (isDfs()) {
            return connectDfsShare(session, path);
        } else {
            return connectNonDfsShare(session, path);
        }
    }

    private DfsResolutionResult connectDfsShare(Session session, SmbPath path) {
        DfsResolver resolver = new DfsResolver();
        SmbPath resolvedPath = resolver.resolve(connectionCache.getClient(), session, path);
        DiskShare diskShare = getDfsShare(session, resolvedPath);
        return new DfsResolutionResult(diskShare, resolvedPath);
    }

    private DfsResolutionResult connectNonDfsShare(Session session, SmbPath path) {
        DiskShare diskShare = (DiskShare) session.connectShare(path.getShareName());
        return new DfsResolutionResult(diskShare, path);
    }

    private DiskShare getDfsShare(Session session, SmbPath resolvedPath) {
        if (isOnSameHost(session, resolvedPath)) {
            return (DiskShare) session.connectShare(resolvedPath.getShareName());
        } else {
            Session newSession = connectSession(resolvedPath.getHostname());
            return (DiskShare) newSession.connectShare(resolvedPath.getShareName());
        }
    }

    private boolean isOnSameHost(Session session, SmbPath path) {
        return session.getConnection().getRemoteHostname().equals(path.getHostname());
    }

    private long getFileSize() {
        return getShare().getFileInformation(getPath()).getStandardInformation().getEndOfFile();
    }

    public void rename(String from, String to) {
        session = connectSession();
        DfsResolutionResult resolvedFrom = resolvePlainPath(from);
        DfsResolutionResult resolvedTo = resolvePlainPath(to);
        if (!resolvedFrom.getSmbPath().isOnSameShare(resolvedTo.getSmbPath())) {
            throw new AttemptedRenameAcrossSharesException("Rename operation failed, " + from + " and " + to + " are on different shares!");
        }
        DiskShare diskShare = resolvedFrom.getDiskShare();
        EnumSet<AccessMask> renameAttributes = EnumSet.of(AccessMask.FILE_READ_ATTRIBUTES, AccessMask.DELETE, AccessMask.SYNCHRONIZE);
        try (File file = diskShare.openFile(resolvedFrom.getSmbPath().getPath(), renameAttributes, null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null)) {
            file.rename(resolvedTo.getSmbPath().getPath());
        }
    }

    public void storeFile(String path, InputStream inputStream) throws IOException {
        connect(path);
        try (File file = openForWrite(getShare(), getPath());
             OutputStream outputStream = file.getOutputStream()
        ) {
            IOUtils.copy(inputStream, outputStream, bufferSize);
        }
    }

    public void appendFile(String path, InputStream inputStream) throws IOException {
        connect(path);
        try (File file = openForAppend(getShare(), getPath())) {
            long fileOffset = getFileSize();
            int length;

            final byte[] buffer = new byte[4096];
            final int EOF = -1;

            while (EOF != (length = inputStream.read(buffer))) {
                file.write(buffer, fileOffset, 0, length);
                fileOffset += length;
            }
        }
    }

    public List<SmbFile> listFiles(String path) {
        connect(path);
        List<SmbFile> files = new ArrayList<>();
        for (FileIdBothDirectoryInformation f : getShare().list(getPath())) {
            boolean isDirectory = FileDirectoryAttributes.isDirectory(f);
            if (isDirectory) {
                //skip special directories . and ..
                if (f.getFileName().equals(CURRENT_DIRECTORY) || f.getFileName().equals(PARENT_DIRECTORY))
                    continue;
            }
            files.add(new SmbFile(isDirectory, f.getFileName(), f.getEndOfFile(), FileDirectoryAttributes.getLastModified(f),
                    FileDirectoryAttributes.isArchive(f), FileDirectoryAttributes.isHidden(f), FileDirectoryAttributes.isReadOnly(f), FileDirectoryAttributes.isSystem(f)));
        }
        return files;
    }

    public void retrieveFile(String path, OutputStream os) throws IOException {
        connect(path);
        File f = openForRead(getShare(), getPath());
        InputStream is = f.getInputStream();
        IOHelper.copyAndCloseInput(is, os, bufferSize);
    }

    public InputStream retrieveFileStream(String path) {
        connect(path);
        File f = openForRead(getShare(), getPath());
        return f.getInputStream();
    }


    public boolean fileExists(String path) {
        connect(path);
        return getShare().fileExists(getPath());
    }

    public void deleteFile(String path) {
        connect(path);
        if(getShare().fileExists(getPath())) {
            getShare().rm(getPath());
        }
    }

    public boolean mkdirs(String directory) {
        connect(directory);
        Path path = SmbPathUtils.get(getPath(), config.getPathSeparator());
        mkdirs(path);
        return true;
    }

    private void mkdirs(Path path) {
        if (isNull(path)) {
            return;
        }

        Path parent = path.getParent();
        String parentStringPath = SmbPathUtils.toString(parent, config.getPathSeparator());

        if (nonNull(parent) && !getShare().folderExists(parentStringPath)) {
            mkdirs(parent);
        }

        String stringPath = SmbPathUtils.toString(path, config.getPathSeparator());
        if (!getShare().folderExists(stringPath)) {
            getShare().mkdir(stringPath);
        }
    }

    private static File openForWrite(DiskShare share, String name) {
        return share.openFile(name, EnumSet.of(AccessMask.GENERIC_WRITE), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_CREATE, EnumSet.of(SMB2CreateOptions.FILE_SEQUENTIAL_ONLY));
    }

    private static File openForAppend(DiskShare share, String name) {
        return share.openFile(name, EnumSet.of(AccessMask.GENERIC_WRITE), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN_IF, EnumSet.of(SMB2CreateOptions.FILE_SEQUENTIAL_ONLY));
    }

    private static File openForRead(DiskShare share, String name) {
        //NB https://msdn.microsoft.com/en-us/library/cc246502.aspx - SMB2 CREATE Request
        // ShareAccess.ALL means that other opens are allowed to read, but not write or delete the file
        return share.openFile(name, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
    }

}
