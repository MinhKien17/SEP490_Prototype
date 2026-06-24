package com.evidencepilot.config.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Ensures the upload root directory and its subdirectories exist before any
 * HTTP request is processed.
 *
 * <p>This {@link ApplicationRunner} fires synchronously during Spring Boot
 * startup — after the application context is fully refreshed but before
 * the embedded Tomcat begins accepting requests.  A crash here (e.g. due to
 * a read-only filesystem) will halt the application cleanly with a clear
 * error message rather than an obscure {@code FileNotFoundException} buried
 * inside the first multipart upload.</p>
 *
 * <p>Directory structure created:
 * <pre>
 *   ${app.upload.dir}/          ← root  (default: /app/uploads)
 *   ${app.upload.dir}/sources/  ← uploaded source files
 *   ${app.upload.dir}/papers/   ← uploaded paper PDFs
 * </pre>
 * </p>
 *
 * <p><b>Permissions:</b> On POSIX systems (Linux containers) the directories
 * are created with {@code rwxr-xr-x} (755).  On Windows (local dev) the
 * POSIX call is silently skipped; the directories are created with whatever
 * the OS default is.</p>
 */
@Slf4j
@Component
public class UploadsInitializer implements ApplicationRunner {

    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDirValue;

    /** Subdirectories that the file-upload controllers write to. */
    private static final String[] SUBDIRS = {"sources", "papers"};

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Path root = Paths.get(uploadDirValue).toAbsolutePath().normalize();

        createDirectoryWithLogging(root);

        for (String sub : SUBDIRS) {
            createDirectoryWithLogging(root.resolve(sub));
        }

        log.info("Upload directories ready at: {}", root);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private void createDirectoryWithLogging(Path dir) throws IOException {
        if (Files.exists(dir)) {
            if (!Files.isDirectory(dir)) {
                throw new IOException(
                        "Upload path exists but is a file, not a directory: " + dir);
            }
            if (!Files.isWritable(dir)) {
                throw new IOException(
                        "Upload directory is not writable by the application process: " + dir);
            }
            log.debug("Upload directory already exists: {}", dir);
            return;
        }

        Files.createDirectories(dir);
        applyPosixPermissions(dir);
        log.info("Created upload directory: {}", dir);
    }

    /**
     * Sets {@code rwxr-xr-x} (755) on POSIX systems.
     * Silently no-ops on Windows where POSIX attributes are unsupported.
     */
    private void applyPosixPermissions(Path dir) {
        try {
            Set<PosixFilePermission> perms =
                    PosixFilePermissions.fromString("rwxr-xr-x");
            Files.setPosixFilePermissions(dir, perms);
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX filesystem (Windows local dev) — permissions not applicable
        } catch (IOException e) {
            log.warn("Could not set permissions on {}: {}", dir, e.getMessage());
        }
    }
}
