package io.github.chr1sps.rars.util;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/*
Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * Utility class to perform necessary file-related search
 * operations. One is to find file names in JAR file,
 * another is to find names of files in given directory
 * of normal file system.
 *
 * @author Pete Sanderson
 * @version October 2006
 */
public class FilenameFinder {
    private static final String JAR_EXTENSION = ".jar";
    private static final String FILE_URL = "file:";
    private static final String JAR_URI_PREFIX = "jar:";
    private static final boolean NO_DIRECTORIES = false;
    /**
     * Constant <code>MATCH_ALL_EXTENSIONS="*"</code>
     */
    public static final String MATCH_ALL_EXTENSIONS = "*";

    /**
     * Locate files and return list of file names. Given a known relative directory
     * path,
     * it will locate it and build list of all names of files in that directory
     * having the given file extension. If the "known file path" doesn't work
     * because RARS is running from an executable JAR file, it will locate the
     * directory in the JAR file and proceed from there. NOTE: since this uses
     * the class loader to get the resource, the directory path needs to be
     * relative to classpath, not absolute. To work with an arbitrary file system,
     * use the other version of this overloaded method. Will NOT match directories
     * that happen to have the desired extension.
     *
     * @param classLoader   class loader to use
     * @param directoryPath Search will be confined to this directory. Use "/" as
     *                      separator but do NOT include starting or ending "/"
     *                      (e.g. com.chrisps.rars/tools)
     * @param fileExtension Only files with this extension will be added
     *                      to the list. Do NOT include the "." in extension.
     * @return array list of matching file names as Strings. If none, list is empty.
     */
    public static ArrayList<String> getFilenameList(final ClassLoader classLoader,
                                                    final String directoryPath,
                                                    String fileExtension) {
        fileExtension = FilenameFinder.checkFileExtension(fileExtension);
        final ArrayList<String> filenameList = new ArrayList<>();
        // Modified by DPS 10-July-2008 to better handle path containing space
        // character (%20) and to hopefully handle path containing non-ASCII
        // characters. The "toURI()" approach was suggested by MARS user
        // Felipe Lessa and worked for him when running 'java Mars' but it did
        // not work when executing from a jar file 'java -jar Mars.jar'. I
        // took it from there and discovered that in the latter situation,
        // "toURI()" created a URI prefixed with "jar:" and the "getPath()" in
        // that case returns null! If you strip the "jar:" prefix and create a
        // new URI from the resulting string, it works! Thanks Felipe!
        //
        // NOTE 5-Sep-2008: "toURI()" was introduced in Java 1.5. To maintain
        // 1.4 compatibility, I need to change it to call URI constructor with
        // string argument, as documented in Sun API.
        //
        // Modified by Ingo Kofler 24-Sept-2009 to handle multiple JAR files.
        // This requires use of ClassLoader getResources() instead of
        // getResource(). The former will look in all JAR files listed in
        // in the java command.
        // TODO: update to use toURI
        URI uri;
        try {
            final Enumeration<URL> urls = classLoader.getResources(directoryPath);

            while (urls.hasMoreElements()) {
                uri = new URI(urls.nextElement().toString());
                if (uri.toString().indexOf(FilenameFinder.JAR_URI_PREFIX) == 0) {
                    uri = new URI(uri.toString().substring(FilenameFinder.JAR_URI_PREFIX.length()));
                }

                final File f = new File(uri.getPath());
                final File[] files = f.listFiles();
                if (files == null) {
                    if (f.toString().toLowerCase().indexOf(FilenameFinder.JAR_EXTENSION) > 0) {
                        // Must be running from a JAR file. Use ZipFile to find files and create list.
                        // Modified 12/28/09 by DPS to add results to existing filenameList instead of
                        // overwriting it.
                        filenameList
                                .addAll(FilenameFinder.getListFromJar(FilenameFinder.extractJarFilename(f.toString()), directoryPath, fileExtension));
                    }
                } else { // have array of File objects; convert to names and add to list
                    final FileFilter filter = FilenameFinder.getFileFilter(fileExtension, "", FilenameFinder.NO_DIRECTORIES);
                    for (final File file : files) {
                        if (filter.accept(file)) {
                            filenameList.add(file.getName());
                        }
                    }
                }
            }
            return filenameList;

        } catch (final URISyntaxException | IOException e) {
            e.printStackTrace();
            return filenameList;
        }

        // 3/4/2019 - Removed original commented out implementation - Benjamin Landers
    }

    /**
     * Locate files and return list of file names. Given a known relative directory
     * path,
     * it will locate it and build list of all names of files in that directory
     * having the given file extension. If the "known file path" doesn't work
     * because RARS is running from an executable JAR file, it will locate the
     * directory in the JAR file and proceed from there. NOTE: since this uses
     * the class loader to get the resource, the directory path needs to be
     * relative to classpath, not absolute. To work with an arbitrary file system,
     * use the other version of this overloaded method.
     *
     * @param classLoader    class loader to use
     * @param directoryPath  Search will be confined to this directory. Use "/" as
     *                       separator but do NOT include starting or ending "/"
     *                       (e.g. com.chrisps.rars/tools)
     * @param fileExtensions ArrayList of Strings containing file extensions.
     *                       Only files with an extension in this list will be added
     *                       to the list.
     *                       Do NOT include the ".", eg "class" not ".class". If
     *                       Arraylist or
     *                       extension null or empty, all files are added.
     * @return array list of matching file names as Strings. If none, list is empty.
     */
    public static ArrayList<String> getFilenameList(final ClassLoader classLoader,
                                                    final String directoryPath,
                                                    final ArrayList<String> fileExtensions) {
        ArrayList<String> filenameList = new ArrayList<>();
        if (fileExtensions == null || fileExtensions.isEmpty()) {
            filenameList = FilenameFinder.getFilenameList(classLoader, directoryPath, "");
        } else {
            for (final String fileExtension : fileExtensions) {
                filenameList.addAll(FilenameFinder.getFilenameList(classLoader, directoryPath, fileExtension));
            }
        }
        return filenameList;
    }

    /**
     * Locate files and return list of file names. Given a known directory path,
     * it will locate it and build list of all names of files in that directory
     * having the given file extension. If file extenion is null or empty, all
     * filenames are returned. Returned list contains absolute filename paths.
     *
     * @param directoryPath Search will be confined to this directory.
     * @param fileExtension Only files with this extension will be added to the
     *                      list.
     *                      Do NOT include "." in extension.
     *                      If null or empty string, all files are added.
     * @return array list of matching file names (absolute path). If none, list is
     * empty.
     */
    public static ArrayList<String> getFilenameList(final String directoryPath, String fileExtension) {
        fileExtension = FilenameFinder.checkFileExtension(fileExtension);
        final ArrayList<String> filenameList = new ArrayList<>();
        final File directory = new File(directoryPath);
        if (directory.isDirectory()) {
            final File[] allFiles = directory.listFiles();
            final FileFilter filter = FilenameFinder.getFileFilter(fileExtension, "", FilenameFinder.NO_DIRECTORIES);
            for (final File file : allFiles) {
                if (filter.accept(file)) {
                    filenameList.add(file.getAbsolutePath());
                }
            }
        }
        return filenameList;
    }

    /**
     * Locate files and return list of file names. Given a known directory path,
     * it will locate it and build list of all names of files in that directory
     * having the given file extension. If file extenion is null or empty, all
     * filenames are returned. Returned list contains absolute filename paths.
     *
     * @param directoryPath  Search will be confined to this directory.
     * @param fileExtensions ArrayList of Strings containing file extensions.
     *                       Only files with an extension in this list will be added
     *                       to the list. Do NOT include the "." in extensions. If
     *                       Arraylist or
     *                       extension null or empty, all files are added.
     * @return array list of matching file names (absolute path). If none, list is
     * empty.
     */
    public static ArrayList<String> getFilenameList(final String directoryPath, final ArrayList<String> fileExtensions) {
        ArrayList<String> filenameList = new ArrayList<>();
        if (fileExtensions == null || fileExtensions.isEmpty()) {
            filenameList = FilenameFinder.getFilenameList(directoryPath, "");
        } else {
            for (final String fileExtension : fileExtensions) {
                filenameList.addAll(FilenameFinder.getFilenameList(directoryPath, fileExtension));
            }
        }
        return filenameList;
    }

    /**
     * Return list of file names. Given a list of file names, it will return the
     * list
     * of all having the given file extension. If file extenion is null or empty,
     * all
     * filenames are returned. Returned list contains absolute filename paths.
     *
     * @param nameList      ArrayList of String containing file names.
     * @param fileExtension Only files with this extension will be added to the
     *                      list.
     *                      If null or empty string, all files are added. Do NOT
     *                      include "." in extension.
     * @return array list of matching file names (absolute path). If none, list is
     * empty.
     */
    public static ArrayList<String> getFilenameList(final ArrayList<String> nameList, String fileExtension) {
        fileExtension = FilenameFinder.checkFileExtension(fileExtension);
        final ArrayList<String> filenameList = new ArrayList<>();
        final FileFilter filter = FilenameFinder.getFileFilter(fileExtension, "", FilenameFinder.NO_DIRECTORIES);
        for (final String name : nameList) {
            final File file = new File(name);
            if (filter.accept(file)) {
                filenameList.add(file.getAbsolutePath());
            }
        }
        return filenameList;
    }

    /**
     * Return list of file names. Given a list of file names, it will return the
     * list
     * of all having the given file extension. If file extenion is null or empty,
     * all
     * filenames are returned. Returned list contains absolute filename paths.
     *
     * @param nameList       ArrayList of String containing file names.
     * @param fileExtensions ArrayList of Strings containing file extensions.
     *                       Only files with an extension in this list will be added
     *                       to the list. Do NOT include the "." in extensions. If
     *                       Arraylist or
     *                       extension null or empty, all files are added.
     * @return array list of matching file names (absolute path). If none, list is
     * empty.
     */
    public static ArrayList<String> getFilenameList(final ArrayList<String> nameList, final ArrayList<String> fileExtensions) {
        ArrayList<String> filenameList = new ArrayList<>();
        String fileExtension;
        if (fileExtensions == null || fileExtensions.isEmpty()) {
            filenameList = FilenameFinder.getFilenameList(nameList, "");
        } else {
            for (final String ext : fileExtensions) {
                fileExtension = FilenameFinder.checkFileExtension(ext);
                filenameList.addAll(FilenameFinder.getFilenameList(nameList, fileExtension));
            }
        }
        return filenameList;
    }

    /**
     * Get the filename extension of the specified File.
     *
     * @param file the File object representing the file of interest
     * @return The filename extension (everything that follows
     * last '.' in filename) or null if none.
     */
    // Source code from Sun Microsystems "The Java Tutorials : How To Use File
    // Choosers"
    public static String getExtension(final File file) {
        String ext = null;
        final String s = file.getName();
        final int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    /**
     * Get a FileFilter that will filter files based on the given list of filename
     * extensions.
     *
     * @param extensions        ArrayList of Strings, each string is acceptable
     *                          filename extension.
     * @param description       String containing description to be added in
     *                          parentheses after list of extensions.
     * @param acceptDirectories boolean value true if directories are accepted by
     *                          the filter, false otherwise.
     * @return a FileFilter object that accepts files with given extensions, and
     * directories if so indicated.
     */
    public static FileFilter getFileFilter(final ArrayList<String> extensions, final String description,
                                           final boolean acceptDirectories) {
        return new RarsFileFilter(extensions, description, acceptDirectories);
    }

    /**
     * Get a FileFilter that will filter files based on the given filename
     * extension.
     *
     * @param extension         String containing acceptable filename extension.
     * @param description       String containing description to be added in
     *                          parentheses after list of extensions.
     * @param acceptDirectories boolean value true if directories are accepted by
     *                          the filter, false otherwise.
     * @return a FileFilter object that accepts files with given extensions, and
     * directories if so indicated.
     */
    public static FileFilter getFileFilter(final String extension, final String description, final boolean acceptDirectories) {
        final ArrayList<String> extensions = new ArrayList<>();
        extensions.add(extension);
        return new RarsFileFilter(extensions, description, acceptDirectories);
    }

    /**
     * Determine if given filename ends with given extension.
     *
     * @param name      A String containing the file name
     * @param extension A String containing the file extension. Leading period is
     *                  optional.
     * @return Returns true if filename ends with given extension, false otherwise.
     */
    // For assured results, make sure extension starts with "." (will add it if not
    // there)
    public static boolean fileExtensionMatch(final String name, final String extension) {
        return (extension == null || extension.isEmpty()
                || name.endsWith(((extension.startsWith(".")) ? "" : ".") + extension));
    }

    // return list of file names in specified folder inside JAR
    private static ArrayList<String> getListFromJar(final String jarName, final String directoryPath, String fileExtension) {
        fileExtension = FilenameFinder.checkFileExtension(fileExtension);
        final ArrayList<String> nameList = new ArrayList<>();
        if (jarName == null) {
            return nameList;
        }
        try {
            final ZipFile zf = new ZipFile(new File(jarName));
            final Enumeration<? extends ZipEntry> list = zf.entries();
            while (list.hasMoreElements()) {
                final ZipEntry ze = list.nextElement();
                if (ze.getName().startsWith(directoryPath + "/") &&
                        FilenameFinder.fileExtensionMatch(ze.getName(), fileExtension)) {
                    nameList.add(ze.getName().substring(ze.getName().lastIndexOf('/') + 1));
                }
            }
        } catch (final Exception e) {
            System.out.println("Exception occurred reading Filename list from JAR: " + e);
        }
        return nameList;
    }

    // Given pathname, extract and return JAR file name (must be only element
    // containing ".jar")
    // 5 Dec 2007 DPS: Modified to return file path of JAR file, not just its name.
    // This was
    // by request of Zachary Kurmas of Grant Valley State, who got errors trying
    // to run the Mars.jar file from a different working directory. He helpfully
    // pointed out what the error is and where it occurs. Originally, it would
    // work only if the JAR file was in the current working directory (as would
    // be the case if executed from a GUI by double-clicking the jar icon).
    private static String extractJarFilename(String path) {
        final StringTokenizer findTheJar = new StringTokenizer(path, "\\/");
        if (path.toLowerCase().startsWith(FilenameFinder.FILE_URL)) {
            path = path.substring(FilenameFinder.FILE_URL.length());
        }
        final int jarPosition = path.toLowerCase().lastIndexOf(FilenameFinder.JAR_EXTENSION);
        return (jarPosition >= 0) ? path.substring(0, jarPosition + FilenameFinder.JAR_EXTENSION.length()) : path;
    }

    // make sure file extension, if it is real, does not start with '.' -- remove
    // it.
    private static String checkFileExtension(final String fileExtension) {
        return (fileExtension == null || !fileExtension.startsWith("."))
                ? fileExtension
                : fileExtension.substring(1);
    }

    ///////////////////////////////////////////////////////////////////////////
    // FileFilter subclass to be instantiated by the getFileFilter method above.
    // This extends javax.swing.filechooser.FileFilter

    private static class RarsFileFilter extends FileFilter {

        private final ArrayList<String> extensions;
        private final String fullDescription;
        private final boolean acceptDirectories;

        private RarsFileFilter(final ArrayList<String> extensions, final String description, final boolean acceptDirectories) {
            this.extensions = extensions;
            this.fullDescription = this.buildFullDescription(description, extensions);
            this.acceptDirectories = acceptDirectories;
        }

        // User provides descriptive phrase to be parenthesized.
        // We will attach it to description of the extensions. For example, if the
        // extensions
        // given are s and asm and the description is "Assembler Programs" the full
        // description
        // generated here will be "Assembler Programs (*.s; *.asm)"
        private String buildFullDescription(final String description, final ArrayList<String> extensions) {
            String result = (description == null) ? "" : description;
            if (!extensions.isEmpty()) {
                result += "  (";
            }
            for (int i = 0; i < extensions.size(); i++) {
                final String extension = extensions.get(i);
                if (extension != null && !extension.isEmpty()) {
                    result += ((i == 0) ? "" : "; ") + "*" + ((extension.charAt(0) == '.') ? "" : ".") + extension;
                }
            }
            if (!extensions.isEmpty()) {
                result += ")";
            }
            return result;
        }

        // required by the abstract superclass
        @Override
        public String getDescription() {
            return this.fullDescription;
        }

        // required by the abstract superclass.
        @Override
        public boolean accept(final File file) {
            if (file.isDirectory()) {
                return this.acceptDirectories;
            }
            final String fileExtension = FilenameFinder.getExtension(file);
            if (fileExtension != null) {
                for (final String ext : this.extensions) {
                    final String extension = FilenameFinder.checkFileExtension(ext);
                    if (extension.equals(FilenameFinder.MATCH_ALL_EXTENSIONS) ||
                            fileExtension.equals(extension)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
