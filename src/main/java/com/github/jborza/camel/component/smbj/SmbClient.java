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

import org.apache.camel.Exchange;
import org.apache.camel.spi.Synchronization;
import org.apache.camel.util.IOHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class SmbClient {
    private final SmbShareFactory smbShareFactory;

    public SmbClient(SmbShareFactory factory) {
        this.smbShareFactory = factory;
    }

    private SmbShare makeSmbShare() {
        return smbShareFactory.makeSmbShare();
    }

    public List<SmbFile> listFiles(String path) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            return share.listFiles(path);
        }
    }

    public void storeFile(String name, InputStream inputStream) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            share.storeFile(name, inputStream);
        }
    }

    public void appendFile(String name, InputStream inputStream) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            share.appendFile(name, inputStream);
        }
    }

    public void retrieveFile(String name, OutputStream os) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            share.retrieveFile(name, os);
        } finally {
            IOHelper.close(os, "retrieve: " + name);
        }
    }

    /**
     * The inputstream is directly returned in order to stream the body directly from the file system
     * When the file is moved and thus the exchange is completed the stream and thus the share needs to
     * closed. By adding the onCompletion
     * @param name
     * @param exchange
     * @return
     * @throws IOException
     */
    public InputStream retrieveFileAsStream(String name, Exchange exchange) throws IOException {

        SmbShare share = makeSmbShare();
        exchange.addOnCompletion(smbSynchronization(share));

        return share.retrieveFileStream(name);

    }

    public boolean fileExists(String name) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            return share.fileExists(name);
        }
    }

    public void deleteFile(String name) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            share.deleteFile(name);
        }
    }

    public void renameFile(String from, String to) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            share.rename(from, to);
        }
    }

    public boolean mkdirs(String directory) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            return share.mkdirs(directory);
        }
    }

    private Synchronization smbSynchronization(SmbShare share) {
        return new Synchronization() {
            @Override
            public void onComplete(Exchange exchange) {
                closeShare();
            }

            @Override
            public void onFailure(Exchange exchange) {
                closeShare();
            }

            private void closeShare() {
                try {
                    share.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
