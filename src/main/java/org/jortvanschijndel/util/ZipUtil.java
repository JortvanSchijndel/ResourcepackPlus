package org.jortvanschijndel.util;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * General-purpose ZIP helper.
 */
public class ZipUtil {

    /**
     * Repackages a GitHub zipball by removing the top-level folder structure.
     * Creates a new ZIP file where pack.mcmeta and other files are at the root level.
     */
    public static void repackageZipball(File inputZip, File outputZip) throws IOException {
        String rootFolder = null;

        // First pass: find the folder that contains pack.mcmeta
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(inputZip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith("pack.mcmeta")) {
                    String path = entry.getName();
                    int lastSlash = path.lastIndexOf('/');
                    if (lastSlash >= 0) {
                        rootFolder = path.substring(0, lastSlash + 1);
                    } else {
                        rootFolder = "";
                    }
                    break;
                }
            }
        }

        if (rootFolder == null) {
            throw new IOException("pack.mcmeta not found in zip!");
        }

        // Second pass: create new ZIP with corrected structure
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(inputZip));
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputZip))) {

            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // Skip entries that don't start with our root folder
                if (!entryName.startsWith(rootFolder)) {
                    continue;
                }

                // Remove the root folder prefix
                String newEntryName = entryName.substring(rootFolder.length());

                // Skip empty paths (the root folder itself)
                if (newEntryName.isEmpty()) {
                    continue;
                }

                // Create new entry with corrected path
                ZipEntry newEntry = new ZipEntry(newEntryName);
                newEntry.setTime(entry.getTime()); // Preserve timestamp

                zos.putNextEntry(newEntry);

                // Copy file data if it's not a directory
                if (!entry.isDirectory()) {
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                }

                zos.closeEntry();
            }
        }
    }

    /**
     * Unzips a zip file to target directory (assumes proper structure already).
     */
    public static void unzipToDirectory(File zipFile, File targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(targetDir, entry.getName());

                if (entry.isDirectory()) {
                    if (!outFile.exists()) {
                        outFile.mkdirs();
                    }
                } else {
                    // Ensure parent directories exist
                    File parentDir = outFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }

                    // Extract the file
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        int len;
                        while ((len = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
            }
        }
    }
}