/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package com.stacksync.desktop.util;

import com.stacksync.syncservice.models.ObjectMetadata;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import com.stacksync.desktop.db.models.CloneChunk;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.Workspace;
import com.stacksync.desktop.repository.Update;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class StringUtil {

    private static int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    /**
     * @see
     * http://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
     * @param str1
     * @param str2
     * @return
     */
    public static int computeLevenshteinDistance(CharSequence str1, CharSequence str2) {
        int[][] distance = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            distance[i][0] = i;
        }
        for (int j = 0; j <= str2.length(); j++) {
            distance[0][j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                distance[i][j] = minimum(
                        distance[i - 1][j] + 1,
                        distance[i][j - 1] + 1,
                        distance[i - 1][j - 1]
                        + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1));
            }
        }

        return distance[str1.length()][str2.length()];
    }

    public static String join(Collection<String> strings, String delimiter) {
        return join((String[]) strings.toArray(new String[0]), delimiter);
    }

    public static String join(Object[] objects, String delimiter) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < objects.length; i++) {
            result.append(objects[i]);

            if (i != objects.length - 1) {
                result.append(delimiter);
            }
        }

        return result.toString();
    }

    public static String toCamelCase(String str) {
        StringBuilder sb = new StringBuilder();

        for (String s : str.split("_")) {
            sb.append(Character.toUpperCase(s.charAt(0)));

            if (s.length() > 1) {
                sb.append(s.substring(1, s.length()).toLowerCase());
            }
        }

        return sb.toString();
    }

    public static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);

        return result.toString();
    }

    public static String toHex(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "x", bi);
    }

    public static String readStream(InputStream in, String charsetName) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        // Performance tests say 4K is the fastest (sschellh)
        byte[] buf = new byte[4096];

        int len;
        while ((len = in.read(buf)) > 0) {
            bos.write(buf, 0, len);
        }
        String result = bos.toString(charsetName);
        bos.close();

        return result;
    }

    public static Integer parseInt(String value, Integer defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String getSHA1Hash(String message) {
        String hash = "";
        byte[] buffer = message.getBytes();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.update(buffer);
            byte[] digest = md.digest();

            for (byte aux : digest) {
                int b = aux & 0xff;
                if (Integer.toHexString(b).length() == 1) {
                    hash += "0";
                }
                hash += Integer.toHexString(b);
            }
        } catch (NoSuchAlgorithmException ex) {
            hash = message;
        }

        return hash;
    }

    public static Update parseJson2Update(JsonObject objMetadata, Workspace workspace) throws NullPointerException {
        Update update = new Update();

        update.setServerUploaded(true);
        update.setServerUploadedAck(true);
        update.setServerUploadedTime(new Date());

        update.setRootId(objMetadata.get("rootId").getAsString());
        update.setFileId(objMetadata.get("fileId").getAsLong());
        update.setVersion(objMetadata.get("version").getAsLong());

        update.setUpdated(new Date(objMetadata.get("serverDateModified").getAsLong()));
        update.setLastModified(new Date(objMetadata.get("clientDateModified").getAsLong()));

        update.setStatus(CloneFile.Status.valueOf(objMetadata.get("status").getAsString()));
        update.setChecksum(objMetadata.get("checksum").getAsLong());
        update.setMimeType(objMetadata.get("mimetype").getAsString());
        //update.setClientName(objMetadata.get("clientName").getAsString());
        update.setFileSize(objMetadata.get("fileSize").getAsLong());
        update.setFolder(objMetadata.get("isFolder").getAsBoolean());

        update.setName(objMetadata.get("fileName").getAsString());
        update.setPath(objMetadata.get("filePath").getAsString());

        // Parent
        if (objMetadata.get("parentFileId").getAsString() != null && !objMetadata.get("parentFileId").getAsString().isEmpty()) {
            update.setParentRootId(objMetadata.get("parentRootId").getAsString());
            update.setParentFileId(objMetadata.get("parentFileId").getAsLong());
            update.setParentFileVersion(objMetadata.get("parentFileVersion").getAsLong());
        }

        // Parse chunks-value{
        JsonArray chunksCsv = objMetadata.get("chunks").getAsJsonArray();

        List<String> chunks = new ArrayList<String>();
        for (int j = 0; j < chunksCsv.size(); j++) {
            String chunk = chunksCsv.get(j).getAsString();
            if (chunk.isEmpty()) {
                continue;
            }

            chunks.add(chunk);
        }

        update.setChunks(chunks);
        update.setWorkpace(workspace);

        return update;
    }

    public static Update parseUpdate(ObjectMetadata objMetadata, Workspace workspace) throws NullPointerException {
        Update update = new Update();

        update.setServerUploaded(true);
        update.setServerUploadedAck(true);
        update.setServerUploadedTime(new Date());

        update.setRootId(objMetadata.getRootId());
        update.setFileId(objMetadata.getFileId());
        update.setVersion(objMetadata.getVersion());

        update.setUpdated(objMetadata.getServerDateModified());
        update.setLastModified(objMetadata.getClientDateModified());

        update.setStatus(CloneFile.Status.valueOf(objMetadata.getStatus()));
        update.setChecksum(objMetadata.getChecksum());
        update.setMimeType(objMetadata.getMimetype());
        //update.setClientName(objMetadata.get("clientName").getAsString());
        update.setFileSize(objMetadata.getFileSize());
        update.setFolder(objMetadata.isFolder());

        update.setName(objMetadata.getFileName());
        update.setPath(objMetadata.getFilePath());

        // Parent
        if (objMetadata.getParentFileId() != null && !objMetadata.getParentFileId().toString().isEmpty()) {
            update.setParentRootId(objMetadata.getParentRootId());
            update.setParentFileId(objMetadata.getParentFileId());
            if (objMetadata.getParentFileVersion() != null) {
                update.setParentFileVersion(objMetadata.getParentFileVersion());
            }
        }

        update.setChunks(objMetadata.getChunks());
        update.setWorkpace(workspace);

        return update;
    }

    public static ObjectMetadata parseJson2Update(CloneFile cloneFile) throws NullPointerException {
        ObjectMetadata object = new ObjectMetadata();

        object.setRootId(cloneFile.getRootId());
        object.setFileId(cloneFile.getFileId());
        object.setVersion(cloneFile.getVersion());
        
        object.setServerDateModified(cloneFile.getUpdated());
        object.setClientDateModified(cloneFile.getLastModified());

        
        object.setStatus(cloneFile.getStatus().toString());
        object.setChecksum(cloneFile.getChecksum());
        object.setMimetype(cloneFile.getMimetype());
        object.setClientName(cloneFile.getClientName());
        
        object.setFileSize(cloneFile.getFileSize());
        object.setFolder(cloneFile.isFolder());
        
        object.setFileName(cloneFile.getName());
        object.setFilePath(cloneFile.getPath());

        // Parent
        if (cloneFile.getParent() != null) {
            object.setParentFileId(cloneFile.getParent().getFileId());            
            object.setParentFileVersion(cloneFile.getParent().getVersion());
            object.setParentRootId(cloneFile.getParent().getRootId());            
        } else{
            object.setParentFileId(null);            
            object.setParentFileVersion(null);
            object.setParentRootId(null);
        }
        
        List<String> chunks = new ArrayList<String>();        
        for(CloneChunk chunk: cloneFile.getChunks()){
            chunks.add(chunk.getChecksum());
        }
        
        object.setChunks(chunks);        
        return object;
    }
}