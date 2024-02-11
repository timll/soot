package soot.util;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ZipFileCache {
    private final ConcurrentHashMap<String, ZipFile> map = new ConcurrentHashMap<>();

    public ZipFile get(String path) {
        return map.computeIfAbsent(path, (k) -> {
            try {
                return new ZipFile(k) {
                    @Override
                    public void close() {
                        //
                    }
                };
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void invalidateAll() {
        map.replaceAll((k, v) -> {
            try {
                v.close();
            } catch (IOException e) {
                //
            }
            return null;
        });
    }
}
