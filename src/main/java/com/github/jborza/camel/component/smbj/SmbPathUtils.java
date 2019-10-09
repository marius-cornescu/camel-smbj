/*
 * Copyright [2018] [Juraj Borza]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.jborza.camel.component.smbj;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.github.jborza.camel.component.smbj.SmbConstants.UNIX_PATH_SEPARATOR;

public final class SmbPathUtils {

    public static String convertToBackslashes(String path) {
        return path.replace('/', '\\');
    }

    public static String removeShareName(String path, String shareName, boolean forceWindowsSeparator) {
        String separator = forceWindowsSeparator ? "\\" : File.separator;
        return removeShareName(path, shareName, separator);
    }

    private static String removeShareName(String path, String shareName, String separator) {
        if (path.equals(shareName))
            return "";
        String sharePathElementPattern = "^" + Pattern.quote(shareName) + Pattern.quote(separator);
        return path.replaceFirst(sharePathElementPattern, "");
    }

    /**
     * added to make the Path component OS specific
     */
    public static Path get(String path, char pathSeparator) {
        if (Objects.isNull(path)) {
            return null;
        }

        if (UNIX_PATH_SEPARATOR != pathSeparator) {
            // java Path will treat unix separator as path separator on windows and unix ...
            // but the windows path separator is treated as part of the dir name on unix ... so a dir can be named "a\b\c"
            path = path.replace(pathSeparator, UNIX_PATH_SEPARATOR);
        }

        return Paths.get(path);
    }

    /**
     * added to make the Path component OS specific
     */
    public static String toString(Path path, char pathSeparator) {
        if (Objects.isNull(path)) {
            return null;
        }

        String pathString = path.toString();

        if (UNIX_PATH_SEPARATOR != pathSeparator) {
            pathString = pathString.replace(UNIX_PATH_SEPARATOR, pathSeparator);
        }

        return pathString;
    }

}
