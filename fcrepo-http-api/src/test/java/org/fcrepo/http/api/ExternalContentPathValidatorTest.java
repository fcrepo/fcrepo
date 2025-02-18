/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import static java.nio.file.StandardOpenOption.APPEND;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.fcrepo.kernel.api.exception.ExternalMessageBodyException;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author bbpennel
 */
public class ExternalContentPathValidatorTest {

    private ExternalContentPathValidator validator;

    @TempDir
    public Path tmpDir;

    private File dataDir;

    private String dataUri;

    private File allowListFile;

    private File goodFile;

    private String goodFileUri;

    @BeforeEach
    public void init() throws Exception {
        allowListFile = Files.createFile(
                tmpDir.resolve("allow-list.txt")
        ).toFile();

        validator = new ExternalContentPathValidator();
        validator.setConfigPath(allowListFile.getAbsolutePath());

        dataDir = Files.createDirectory(
                tmpDir.resolve("data")
        ).toFile();
        dataUri = dataDir.toURI().toString();

        goodFile = new File(dataDir, "file.txt");
        goodFile.createNewFile();
        goodFileUri = goodFile.toURI().toString();
    }

    @AfterEach
    public void after() {
        validator.shutdown();
    }

    @Test
    public void testValidateWithNoAllowList() throws Exception {
        validator.setConfigPath(null);
        validator.init();

        assertThrows(ExternalMessageBodyException.class, () -> validator.validate(goodFileUri));
    }

    @Test
    public void testValidFileUri() throws Exception {
        addAllowedPath(dataUri);

        validator.validate(goodFileUri);
    }

    @Test
    public void testValidHttpUri() throws Exception {
        final String goodPath = "http://example.com/";
        final String extPath = goodPath + "file.txt";

        addAllowedPath(goodPath);

        validator.validate(extPath);
    }

    @Test
    public void testExactFileUri() throws Exception {
        addAllowedPath(goodFileUri);

        validator.validate(goodFileUri);
    }

    @Test
    public void testMultipleMatches() throws Exception {
        new File(dataDir, "file.txt").createNewFile();
        final String extPath = goodFileUri;
        final String anotherPath = Files.createFile(
                dataDir.toPath().resolve("another.txt")
        ).toFile().toURI().toString();

        addAllowedPath(anotherPath);
        addAllowedPath(dataUri);

        validator.validate(extPath);
    }

    @Test
    public void testMultipleSchemes() throws Exception {
        final String httpPath = "http://example.com/";

        addAllowedPath(httpPath);
        addAllowedPath(dataUri);

        validator.validate(goodFileUri);
    }

    @Test
    public void testInvalidFileUri() throws Exception {
        final String extPath = Files.createFile(
                tmpDir.resolve("file.txt")
        ).toFile().toURI().toString();

        addAllowedPath(dataUri);

        assertThrows(ExternalMessageBodyException.class, () -> validator.validate(extPath));
    }

    @Test
    public void testMalformedUri() throws Exception {
        final String goodPath = "http://example.com/";
        final String extPath = ".bad://example.com/file.txt";

        addAllowedPath(goodPath);

        assertThrows(ExternalMessageBodyException.class, () -> validator.validate(extPath));
    }

    @Test
    public void testNonabsoluteUri() throws Exception {
        final String goodPath = "http://example.com/";
        final String extPath = "/example.com/file.txt";

        addAllowedPath(goodPath);

        assertThrows(ExternalMessageBodyException.class, () -> validator.validate(extPath));
    }

    @Test
    public void testInvalidHttpUri() throws Exception {
        final String goodPath = "http://good.example.com/";
        final String extPath = "http://bad.example.com/file.txt";

        addAllowedPath(goodPath);

        assertThrows(ExternalMessageBodyException.class, () -> validator.validate(extPath));
    }

    @Test
    public void testHttpUriMissingSlash() throws Exception {
        // Slash after domain is required
        final String goodPath = "http://good.example.com";
        final String extPath = "http://good.example.com:8080/offlimits";

        addAllowedPath(goodPath);

        assertThrows(ExternalMessageBodyException.class, () -> validator.validate(extPath));
    }

    @Test
    public void testRelativeModifier() throws Exception {
        final String extPath = dataUri + "../sneaky.txt";

        addAllowedPath(dataUri);

        assertThrows(ExternalMessageBodyException.class, () -> validator.validate(extPath));
    }

    @Test
    public void testNoScheme() throws Exception {
        final String extPath = dataDir.getAbsolutePath() + "file.txt";

        addAllowedPath(dataUri);

        assertThrows(ExternalMessageBodyException.class, () -> validator.validate(extPath));
    }

    @Test
    public void testEmptyPath() throws Exception {
        addAllowedPath(dataUri);

        assertThrows(ExternalMessageBodyException.class, () -> validator.validate(""));
    }

    @Test
    public void testNullPath() throws Exception {
        addAllowedPath(dataUri);

        assertThrows(ExternalMessageBodyException.class, () -> validator.validate(null));
    }

    @Test
    public void testListFileDoesNotExist() throws Exception {
        allowListFile.delete();

        assertThrows(IOException.class, () -> validator.init());
    }

    @Test
    public void testAllowAny() throws Exception {
        addAllowedPath("file:///");
        addAllowedPath("http://");
        addAllowedPath("https://");

        final String path1 = goodFileUri;
        validator.validate(path1);
        final String path2 = "http://example.com/file";
        validator.validate(path2);
        final String path3 = "https://example.com/file";
        validator.validate(path3);
    }

    @Test
    public void testCaseInsensitiveFileScheme() throws Exception {
        final String goodPath = "FILE://" + dataDir.toURI().getPath();
        final String extPath = dataUri + "file.txt";

        addAllowedPath(goodPath);

        validator.validate(extPath);
    }

    @Test
    public void testVaryingSensitiveFilePath() throws Exception {
        final File subfolder = Files.createDirectory(
                tmpDir.resolve("CAPSLOCK")
        ).toFile();
        final File file = new File(subfolder, "OoOoOooOOooo.txt");
        file.createNewFile();

        final String goodPath = subfolder.toURI().toString();
        final String extPath = file.toURI().toString();

        addAllowedPath(goodPath);

        validator.validate(extPath);
    }

    @Test
    public void testVaryingCaseHttpUri() throws Exception {
        final String goodPath = "http://sensitive.example.com/PATH/to/";
        final String extPath = "http://sensitive.example.com/PATH/to/stuff";

        addAllowedPath(goodPath);

        validator.validate(extPath);
    }

    @Test
    public void testFileSlashes() throws Exception {
        final File oneFolder = Files.createDirectory(
                tmpDir.resolve("one")
        ).toFile();
        final File twoFolder = Files.createDirectory(
                tmpDir.resolve("two")
        ).toFile();
        final File threeFolder = Files.createDirectory(
                tmpDir.resolve("three")
        ).toFile();
        final File manyFolder = Files.createDirectory(
                tmpDir.resolve("toomany")
        ).toFile();
        new File(oneFolder, "file").createNewFile();
        new File(twoFolder, "file").createNewFile();
        new File(threeFolder, "file").createNewFile();
        new File(manyFolder, "file").createNewFile();

        addAllowedPath("file:" + oneFolder.toURI().getPath() + "/");
        addAllowedPath("file:/" + twoFolder.toURI().getPath() + "/");
        addAllowedPath("file://" + threeFolder.toURI().getPath() + "/");
        addAllowedPath("file:///" + manyFolder.toURI().getPath() + "/");

        validator.validate("file:" + oneFolder.toURI().getPath() + "/file");
        validator.validate("file:/" + oneFolder.toURI().getPath() + "/file");
        validator.validate("file://" + oneFolder.toURI().getPath() + "/file");

        validator.validate("file:" + twoFolder.toURI().getPath() + "/file");
        validator.validate("file:/" + twoFolder.toURI().getPath() + "/file");
        validator.validate("file://" + twoFolder.toURI().getPath() + "/file");

        validator.validate("file:" + threeFolder.toURI().getPath() + "/file");
        validator.validate("file:/" + threeFolder.toURI().getPath() + "/file");
        validator.validate("file://" + threeFolder.toURI().getPath() + "/file");

        assertThrows(ExternalMessageBodyException.class,
                () -> validator.validate("file:" + manyFolder.toURI().getPath() + "file")
        );
    }

    @Test
    public void testDisallowDirectoryUriWithoutSlash() throws Exception {
        final String allowDir = dataUri.substring(0, dataUri.length() - 1);
        addAllowedPath(allowDir);

        final String extPath = dataUri + "file.txt";
        assertThrows(ExternalMessageBodyException.class, () -> validator.validate(extPath));
    }

    @Test
    public void testDisallowFileUriWithSlash() throws Exception {
        new File(dataDir, "file").createNewFile();
        final String extPath = dataUri + "file/";
        final String allowExactPath = extPath + "/";

        addAllowedPath(allowExactPath);

        assertThrows(ExternalMessageBodyException.class, () -> validator.validate(extPath));
    }

    /*
     * Test ignored because it takes around 10+ seconds to poll for events on MacOS:
     * httpshttps://bugs.openjdk.org/browse/JDK-8293067 Can be enabled for one off testing
     */
    @Disabled("Test is ignored due to file event timing")
    @Test
    public void testDetectModification() throws Exception {
        validator.setMonitorForChanges(true);

        final String anotherPath = Files.createDirectory(
                tmpDir.resolve("other")
        ).toFile().toURI().toString();
        addAllowedPath(anotherPath);

        final String path = dataUri + "file";
        assertThrows(ExternalMessageBodyException.class, () -> validator.validate(path));

        // Wait to ensure that the watch service is watching...
        Thread.sleep(5000);

        // Add a new allowed path
        try (final BufferedWriter writer = Files.newBufferedWriter(allowListFile.toPath(), APPEND)) {
            writer.write(dataUri + System.lineSeparator());
        }

        Awaitility.await().atMost(Duration.ofSeconds(20))
            .ignoreException(ExternalMessageBodyException.class)
            .until(() -> {
                validator.validate(path);
                return true;
            });
    }

    private void addAllowedPath(final String allowed) throws Exception {
        try (final BufferedWriter writer = Files.newBufferedWriter(allowListFile.toPath(), APPEND)) {
            writer.write(allowed + System.lineSeparator());
        }
        validator.init();
    }
}
