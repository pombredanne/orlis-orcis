/*
 * SicMu Player - Lightweight music player for Android
 * Copyright (C) 2015  Mathieu Souchaud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package souch.smp;

import java.util.ArrayList;

public class Path {
    public final static char separatorChar = System.getProperty("file.separator", "/").charAt(0);
    public static String rootFolder = "";

    /*
     @param path must not be null
     rootFolder is removed from the beginning of the path
     if rootFolder = "" and path = /mnt/sdcard/toto/tata.mp3 -> return /mnt/sdcard/toto
     if rootFolder = "/mnt/sdcard" and path = /mnt/sdcard/toto/tata.mp3 -> return toto
     if rootFolder = "/mnt/sdcard/" and path = /mnt/sdcard/toto/tata.mp3 -> return toto
    */
    static public String getFolder(String path) {
        String folder;

        // remove rootFolder
        if (rootFolder != null &&
                rootFolder.length() <= path.length() &&
                rootFolder.equals(path.substring(0, rootFolder.length()))) {
            int rootFolderSize = rootFolder.length();
            // remove / remaining at the beginning of path
            if (path.length() > rootFolderSize && path.charAt(rootFolderSize) == separatorChar)
                rootFolderSize++;

            folder = path.substring(rootFolderSize, path.length());
        }
        else {
            folder = path;
        }

        // remove filename
        int index = folder.lastIndexOf(separatorChar);
        if (index == -1) // no folder: remove everything
            index = 0;
        folder = folder.substring(0, index);

        // no folder get the name "."
        if (folder.equals(""))
            folder = ".";

        return folder;
    }

    /*
     path = "toto/tata" -> return {"toto", "tata"}
     */
    static public ArrayList<String> tokenizeFolder(String path) {
        ArrayList<String> folders = new ArrayList<>();
        int beg = 0;
        boolean folderFound = false;
        for(int i = 0; i < path.length(); i++) {
            if(path.charAt(i) == separatorChar) {
                if (folderFound) {
                    folders.add(path.substring(beg, i));
                    folderFound = false;
                }
                beg = i + 1;
            }
            else {
                folderFound = true;
            }
        }
        // path do not finish by /
        if (folderFound) {
            folders.add(path.substring(beg, path.length()));
        }

        return folders;
    }

    /*
     up "toto/tata" down "toto"  -> tata
     up "toto/tata" down ""      -> toto/tata
     up "toto/tata" toto/down "" -> ''
     */
    static public String cutFolder(String up, String down) {
        int i = 0;
        while(i < down.length() && i < up.length()  &&  up.charAt(i) == down.charAt(i))
            i++;
        return up.substring(i, up.length());
    }


    /**
     * From Android String.java
     *
     * modify compareToIgnoreCase in order to put shorter group to the end e.g.
     * normal compareToIgnoreCase order
     * /toto
     * /toto/tata
     * /toto/titi
     *
     * modified order (here)
     * /toto/tata
     * /toto/titi
     * /toto
     *
     * Compares this string to the given string, ignoring case differences.
     *
     * The drawback of this method being outside of String.java is that it is slower as it does not
     * play with internal string data (especially charAt calls). Rows initialization lose 15% of speed.
     */
    public static int compareToIgnoreCaseShorterFolderLast(String string1, String string2) {
        int o1 = 0, o2 = 0, result;
        int end = (string1.length() < string2.length() ? string1.length() : string2.length());
        char c1, c2;
        while (o1 < end) {
            if ((c1 = string1.charAt(o1++)) == (c2 = string2.charAt(o2++))) {
                continue;
            }
            c1 = foldCase(c1);
            c2 = foldCase(c2);
            if ((result = c1 - c2) != 0) {
                return result;
            }
        }
        return string2.length() - string1.length(); // modified here
    }
    /**
     * useful for compareToIgnoreCaseShorterFolderLast
     */
    private static char foldCase(char ch) {
        if (ch < 128) {
            if ('A' <= ch && ch <= 'Z') {
                return (char) (ch + ('a' - 'A'));
            }
            return ch;
        }
        return Character.toLowerCase(Character.toUpperCase(ch));
    }
}
