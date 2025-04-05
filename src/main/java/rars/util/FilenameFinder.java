package rars.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class to perform necessary file-related search
 * operations. One is to find file names in JAR file,
 * another is to find names of files in given directory
 * of normal file system.
 *
 * @author Pete Sanderson
 * @version October 2006
 */
public final class FilenameFinder {

    public static final @NotNull FileFilter RARS_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(final File f) {
            if (f.isDirectory()) return true;
            return isFileExtensionMatch(f, Globals.FILE_EXTENSIONS);
        }

        @Override
        public String getDescription() {
            return "Assembler files (%s)".formatted(String.join(", ", Globals.FILE_EXTENSIONS));
        }
    };

    private FilenameFinder() {
    }

    /**
     * Locate files and return list of file names. Given a known directory path,
     * it will locate it and build list of all names of files in that directory
     * having the given file extension. If file extenion is null or empty, all
     * filenames are returned. Returned list contains absolute file paths.
     *
     * @param directory
     *     Search will be confined to this directory.
     * @param fileExtensions
     *     ArrayList of Strings containing file extensions.
     *     Only files with an extension in this list will be added
     *     to the list. Do NOT include the "." in extensions. If
     *     Arraylist or
     *     extension null or empty, all files are added.
     * @return array list of matching file names (absolute path). If none, list is
     * empty.
     */
    public static @NotNull List<? extends @NotNull File> getFilenameListForDirectory(
        final @NotNull File directory,
        final @NotNull List<@NotNull String> fileExtensions
    ) {
        final List<? extends @NotNull File> allFiles = Arrays.asList(directory.listFiles());
        return filterFilesByExtensions(allFiles, fileExtensions);
    }

    public static @NotNull List<? extends @NotNull File> filterFilesByExtensions(
        final @NotNull List<? extends @NotNull File> files,
        final @NotNull List<@NotNull String> fileExtensions
    ) {
        return files.stream().filter(file -> isFileExtensionMatch(file, fileExtensions)).toList();
    }

    /**
     * Returns true if a file's name matches one of the given extensions.
     * Also returns true if the extensions list is empty.
     */
    private static boolean isFileExtensionMatch(
        final @NotNull File file,
        final @NotNull List<@NotNull String> extensions
    ) {
        if (extensions.isEmpty()) {
            return true;
        }
        final var fileExtension = FilenameFinder.getFileExtension(file);
        if (fileExtension != null) {
            for (final var extension : extensions) {
                final var trimmed = FilenameFinder.removePrecedingDotIfPresent(extension);
                if (extension.equals("*") || fileExtension.equals(trimmed)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the file extension of the specified File.
     *
     * @param file
     *     the File object representing the file of interest
     * @return The file extension (everything that follows
     * last '.' in file) or null if none.
     */
    private static @Nullable String getFileExtension(final @NotNull File file) {
        @Nullable String ext = null;
        final String s = file.getName();
        final int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    private static @NotNull String removePrecedingDotIfPresent(final @NotNull String fileExtension) {
        return (!fileExtension.startsWith("."))
            ? fileExtension
            : fileExtension.substring(1);
    }
}
