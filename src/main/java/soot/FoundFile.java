package soot;

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 1997 - 2018 Raja Vallée-Rai and others
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.util.SharedResource;

public class FoundFile implements IFoundFile {
  private static final Logger logger = LoggerFactory.getLogger(FoundFile.class);

  protected final List<InputStream> openedInputStreams = new ArrayList<>();

  protected Path path;

  protected File file;
  protected String entryName;

  protected SharedResource<ZipFile> zipFile;
  protected ZipEntry zipEntry;

  public FoundFile(String archivePath, String entryName) {
    if (archivePath == null || entryName == null) {
      throw new IllegalArgumentException("Error: The archive path and entry name cannot be null.");
    }
    this.file = new File(archivePath);
    this.entryName = entryName;
  }

  public FoundFile(File file) {
    if (file == null) {
      throw new IllegalArgumentException("Error: The file cannot be null.");
    }
    this.file = file;
    this.entryName = null;
  }

  public FoundFile(Path path) {
    this.path = path;
  }

  @Override
  public String getFilePath() {
    if (file != null)
      return file.getPath();
    if (path != null)
      return path.toFile().getPath();
    return null;
  }

  @Override
  public boolean isZipFile() {
    return entryName != null;
  }

  @Override
  public ZipFile getZipFile() {
    return zipFile != null ? zipFile.get() : null;
  }

  @Override
  public File getFile() {
    return file;
  }

  @Override
  public String getAbsolutePath() {
    try {
      return file != null ? file.getCanonicalPath() : path.toRealPath().toString();
    } catch (IOException ex) {
      return file != null ? file.getAbsolutePath() : path.toAbsolutePath().toString();
    }
  }

  @Override
  public InputStream inputStream() {
    InputStream ret = null;
    if (path != null) {
      try {
        ret = Files.newInputStream(path);
      } catch (IOException e) {
        throw new RuntimeException(
            "Error: Failed to open a InputStream for the file at path '" + path.toAbsolutePath().toString() + "'.", e);
      }
    } else if (!isZipFile()) {
      try {
        ret = new FileInputStream(file);
      } catch (Exception e) {
        throw new RuntimeException("Error: Failed to open a InputStream for the file at path '" + file.getPath() + "'.", e);
      }
    } else {
      if (zipFile == null) {
        try {
          zipFile = SourceLocator.v().archivePathToZip.get(file.getPath());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }

        zipEntry = zipFile.get().getEntry(entryName);
        if (zipEntry == null) {
          silentClose();
          throw new RuntimeException(
                  "Error: Failed to find entry '" + entryName + "' in the archive file at path '" + file.getPath() + "'.");
        }
      }
      try (InputStream stream = zipFile.get().getInputStream(zipEntry)) {
        // Converts the input stream to a ByteArrayInputStream
        ret = doJDKBugWorkaround(stream, zipEntry.getSize());
      } catch (Exception e) {
        throw new RuntimeException("Error: Failed to open a InputStream for the entry '" + zipEntry.getName()
            + "' of the archive at path '" + zipFile.get().getName() + "'.", e);
      }
    }

    openedInputStreams.add(ret);
    return ret;
  }

  @Override
  public void close() {
    List<Throwable> errs = new ArrayList<>();
    for (InputStream is : openedInputStreams) {
      try {
        is.close();
      } catch (Exception e) {
        errs.add(e);
      }
    }

    try {
      if (zipFile != null)
        zipFile.close();
    } catch (Exception e) {
      errs.add(e);
    }

    zipFile = null;
    zipEntry = null;

    if (!errs.isEmpty()) {
      String msg = null;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (PrintStream ps = new PrintStream(baos, true, "utf-8")) {
        ps.println("Error: Failed to close all opened resources. The following exceptions were thrown in the process: ");
        int i = 0;
        for (Throwable t : errs) {
          ps.print("Exception ");
          ps.print(i++);
          ps.print(": ");
          logger.error(t.getMessage(), t);
        }
        msg = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      } catch (UnsupportedEncodingException e) {
        //
      }
      throw new RuntimeException(msg);
    }
  }

  private static InputStream doJDKBugWorkaround(InputStream is, long size) throws IOException {
    int sz = (int) size;
    final byte[] buf = new byte[sz];
    final int N = 1024;
    for (int ln = 0, count = 0; sz > 0 && (ln = is.read(buf, count, Math.min(N, sz))) != -1;) {
      count += ln;
      sz -= ln;
    }
    return new ByteArrayInputStream(buf);
  }
}
