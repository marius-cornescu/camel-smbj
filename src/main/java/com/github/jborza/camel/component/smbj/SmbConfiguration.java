/*
 *  Copyright [2018] [Juraj Borza]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.jborza.camel.component.smbj;

import org.apache.camel.component.file.GenericFileConfiguration;
import org.apache.camel.spi.UriParam;

import java.net.URI;
import java.util.regex.Pattern;

import static com.github.jborza.camel.component.smbj.utils.StringHelper.after;
import static com.github.jborza.camel.component.smbj.utils.StringHelper.before;
import static com.github.jborza.camel.component.smbj.SmbConstants.WINDOWS_PATH_SEPARATOR;

public class SmbConfiguration extends GenericFileConfiguration {

    private static final String DOMAIN_SEPARATOR = ";";
    private static final String USER_PASS_SEPARATOR = ":";
    private static final int DEFAULT_SMB_PORT = 445;

    private String domain;
    private String username;
    private String password;
    private String host;
    private String path;

    @UriParam(label = "consumer")
    private boolean streamDownload;

    private String share;
    private int port;

    @UriParam(defaultValue = "\\", description = "Path separator for the target samba server")
    private char pathSeparator = WINDOWS_PATH_SEPARATOR;

    public SmbConfiguration(URI uri) {
        configure(uri);
    }

    @Override
    public void configure(URI uri) {
        super.configure(uri);
        String userInfo = uri.getUserInfo();

        if (userInfo != null) {
            if (userInfo.contains(DOMAIN_SEPARATOR)) {
                setDomain(before(userInfo, DOMAIN_SEPARATOR));
                userInfo = after(userInfo, DOMAIN_SEPARATOR);
            }
            if (userInfo.contains(USER_PASS_SEPARATOR)) {
                setUsername(before(userInfo, USER_PASS_SEPARATOR));
                setPassword(after(userInfo, USER_PASS_SEPARATOR));
            } else {
                setUsername(userInfo);
            }
        }

        setHost(uri.getHost());
        setPort(uri.getPort() <= 0 ? DEFAULT_SMB_PORT : uri.getPort()); // set port to default if none if provided by the endpoint config
        setPath(uri.getPath());
        String[] segments = uri.getPath().split("/");
        if(segments.length > 1){//first one is "/"
            setShare(segments[1]);
        } else {
            setShare("");
        }

        setPath(removeShareFromPath(uri));
    }

    private String removeShareFromPath(URI uri) {
        return uri.getPath().replaceFirst("/"+ Pattern.quote(getShare()) + "[/]{0,1}","");
    }

    public String getSmbHostPath() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("smb://");
        buffer.append(getHost());
        if (!isDefaultPort()) {
            buffer.append(":").append(getPort());
        }
        buffer.append("/");
        return buffer.toString();
    }

    public boolean isDefaultPort() {
        return false;
    }

    public String getShare() { return share; }

    public void setShare(String share) { this.share = share; }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public boolean isStreamDownload() {
        return streamDownload;
    }

    public void setStreamDownload(boolean streamDownload) {
        this.streamDownload = streamDownload;
    }

    public char getPathSeparator() {
        return pathSeparator;
    }

    public void setPathSeparator(char pathSeparator) {
        this.pathSeparator = pathSeparator;
    }
}

