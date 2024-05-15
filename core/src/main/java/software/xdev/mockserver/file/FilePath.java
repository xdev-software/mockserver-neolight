/*
 * Copyright © 2024 XDEV Software (https://xdev.software)
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
package software.xdev.mockserver.file;

import org.apache.commons.lang3.StringUtils;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.file.FileVisitResult.CONTINUE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.event.Level.ERROR;

public class FilePath {

    public static String absolutePathFromClassPathOrPath(String filename) {
        URL resource = FileReader.class.getClassLoader().getResource(filename);
        if (resource != null) {
            return resource.getPath();
        }
        return new File(filename).getAbsolutePath();
    }

    public static List<String> expandFilePathGlobs(String filePath) {
        if (isNotBlank(filePath) && filePath.contains(".")) {
            if (filePath.contains("*") || filePath.contains("?")) {
                List<String> expandedFilePaths = new ArrayList<>();
                PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + filePath);
                Finder finder = new Finder(filePath, pathMatcher);
                try {
                    String startingDir = filePath.contains("/") ? StringUtils.substringBeforeLast(StringUtils.substringBefore(filePath, "*"), "/") : ".";
                    if (new File(startingDir).exists()) {
                        Files.walkFileTree(
                            new File(startingDir).toPath(),
                            EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                            Integer.MAX_VALUE,
                            finder
                        );
                        expandedFilePaths.addAll(finder.getMatchingPaths());
                    } else {
                        new MockServerLogger(FileReader.class).logEvent(
                            new LogEntry()
                                .setLogLevel(ERROR)
                                .setMessageFormat("can't find directory:{}for file path:{}")
                                .setArguments(startingDir, filePath)
                        );
                    }
                } catch (Throwable throwable) {
                    new MockServerLogger(FileReader.class).logEvent(
                        new LogEntry()
                            .setLogLevel(ERROR)
                            .setMessageFormat("exception finding files for file path:{}")
                            .setArguments(filePath)
                            .setThrowable(throwable)
                    );
                    throw new RuntimeException(throwable);
                }
                return expandedFilePaths.stream().sorted().collect(Collectors.toList());
            } else {
                return Collections.singletonList(filePath);
            }
        } else {
            return Collections.emptyList();
        }
    }

    public static class Finder extends SimpleFileVisitor<Path> {

        private final PathMatcher matcher;
        private final String pattern;
        private final List<String> matchingPaths = new ArrayList<>();

        public List<String> getMatchingPaths() {
            return matchingPaths;
        }

        Finder(String pattern, PathMatcher matcher) {
            this.pattern = pattern;
            this.matcher = matcher;
        }

        void find(Path file) {
            if (file != null && matcher.matches(file)) {
                matchingPaths.add(file.toFile().getAbsolutePath());
            }
        }

        // test pattern on each file
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            find(file);
            return CONTINUE;
        }

        // test pattern on each directory
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            find(dir);
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exception) {
            new MockServerLogger(FileReader.class).logEvent(
                new LogEntry()
                    .setLogLevel(ERROR)
                    .setMessageFormat("exception while findings file matching for pattern " + pattern + " for file " + file + " - " + exception.getMessage())
                    .setThrowable(exception)
            );
            return CONTINUE;
        }
    }

}
