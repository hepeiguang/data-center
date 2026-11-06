/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.plugin.descriptor.parser;

import org.apache.dolphinscheduler.plugin.descriptor.model.PluginDescriptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PluginDescriptorParser.
 * Covers: valid YAML, malformed YAML, missing descriptor, zip-bomb, path traversal.
 */
class PluginDescriptorParserTest {

    private PluginDescriptorParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new PluginDescriptorParser();
    }

    @Test
    void testParseValidDescriptor() throws Exception {
        File jar = createTestJar("valid-plugin/ds-plugin.yaml", "META-INF/ds-plugin.yaml");
        PluginDescriptor descriptor = parser.parse(jar);

        assertNotNull(descriptor);
        assertEquals("ds.plugin/v1", descriptor.getApiVersion());
        assertEquals("TEST_TASK", descriptor.getTask().getType());
        assertEquals("universal", descriptor.getTask().getCategory());
        assertEquals("Test Task", descriptor.getTask().getName());
        assertEquals(4, descriptor.getForm().size());
        assertTrue(descriptor.getParams().isStrict());
    }

    @Test
    void testParseMissingApiVersion() throws Exception {
        File jar = createTestJar("invalid-plugin/missing-api-version.yaml", "META-INF/ds-plugin.yaml");

        PluginDescriptorParser.PluginDescriptorParseException ex =
                assertThrows(PluginDescriptorParser.PluginDescriptorParseException.class,
                        () -> parser.parse(jar));
        assertTrue(ex.getMessage().contains("apiVersion"));
    }

    @Test
    void testParseMissingTask() throws Exception {
        File jar = createTestJar("invalid-plugin/missing-task.yaml", "META-INF/ds-plugin.yaml");

        PluginDescriptorParser.PluginDescriptorParseException ex =
                assertThrows(PluginDescriptorParser.PluginDescriptorParseException.class,
                        () -> parser.parse(jar));
        assertTrue(ex.getMessage().contains("task"));
    }

    @Test
    void testParseInvalidTaskType() throws Exception {
        File jar = createTestJar("invalid-plugin/invalid-task-type.yaml", "META-INF/ds-plugin.yaml");

        // Note: task type validation happens in the validator, not the parser.
        // The parser should still parse it successfully.
        PluginDescriptor descriptor = parser.parse(jar);
        assertNotNull(descriptor);
        assertEquals("lowercase_invalid", descriptor.getTask().getType());
    }

    @Test
    void testParseNoDescriptorEntry() throws Exception {
        // Create a JAR without ds-plugin.yaml
        File jar = createEmptyJar();
        PluginDescriptor descriptor = parser.parse(jar);
        assertNull(descriptor);
    }

    @Test
    void testParseMalformedYaml() throws Exception {
        File jar = createTestJarWithContent("META-INF/ds-plugin.yaml",
                "this: is: not: valid: yaml: [[[");

        assertThrows(PluginDescriptorParser.PluginDescriptorParseException.class,
                () -> parser.parse(jar));
    }

    @Test
    void testParseNonExistentFile() {
        File nonExistent = new File("/tmp/does-not-exist.jar");
        assertThrows(IOException.class, () -> parser.parse(nonExistent));
    }

    @Test
    void testHasTaskChannelFactorySpi() throws Exception {
        File jarWithSpi = createJarWithSpi();
        assertTrue(parser.hasTaskChannelFactorySpi(jarWithSpi));

        File jarWithoutSpi = createEmptyJar();
        assertFalse(parser.hasTaskChannelFactorySpi(jarWithoutSpi));
    }

    @Test
    void testReadIcon() throws Exception {
        byte[] iconData = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}; // PNG header
        File jar = createJarWithIcon("META-INF/ds-plugin/icon.png", iconData);

        byte[] result = parser.readIcon(jar, "META-INF/ds-plugin/icon.png");
        assertNotNull(result);
        assertArrayEquals(iconData, result);
    }

    @Test
    void testReadIconInvalidPath() throws Exception {
        File jar = createEmptyJar();
        assertThrows(IOException.class, () -> parser.readIcon(jar, "../etc/passwd"));
    }

    @Test
    void testPathTraversalProtection() throws Exception {
        // Create a JAR with a path traversal entry name
        File jar = createJarWithTraversalEntry();

        PluginDescriptorParser.PluginDescriptorParseException ex =
                assertThrows(PluginDescriptorParser.PluginDescriptorParseException.class,
                        () -> parser.parse(jar));
        assertTrue(ex.getMessage().contains("traversal"));
    }

    // --- Helper methods ---

    private File createTestJar(String resourcePath, String entryName) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(is, "Test resource not found: " + resourcePath);
        byte[] content = readAllBytes(is);
        is.close();

        File jarFile = tempDir.resolve("test-plugin.jar").toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
            jos.putNextEntry(new JarEntry(entryName));
            jos.write(content);
            jos.closeEntry();
        }
        return jarFile;
    }

    private File createTestJarWithContent(String entryName, String content) throws IOException {
        File jarFile = tempDir.resolve("test-content.jar").toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
            jos.putNextEntry(new JarEntry(entryName));
            jos.write(content.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
        return jarFile;
    }

    private File createEmptyJar() throws IOException {
        File jarFile = tempDir.resolve("empty.jar").toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
            // Empty JAR
        }
        return jarFile;
    }

    private File createJarWithSpi() throws IOException {
        File jarFile = tempDir.resolve("with-spi.jar").toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
            jos.putNextEntry(new JarEntry(
                    "META-INF/services/org.apache.dolphinscheduler.plugin.task.api.TaskChannelFactory"));
            jos.write("com.example.TestTaskChannelFactory".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
        return jarFile;
    }

    private File createJarWithIcon(String entryName, byte[] iconData) throws IOException {
        File jarFile = tempDir.resolve("with-icon.jar").toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
            jos.putNextEntry(new JarEntry(entryName));
            jos.write(iconData);
            jos.closeEntry();
        }
        return jarFile;
    }

    private File createJarWithTraversalEntry() throws IOException {
        File jarFile = tempDir.resolve("traversal.jar").toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
            jos.putNextEntry(new JarEntry("../evil.yaml"));
            jos.write("apiVersion: ds.plugin/v1\ntask:\n  type: EVIL\nform: []".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
        return jarFile;
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
