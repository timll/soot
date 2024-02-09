package soot.util;

import org.junit.Test;
import soot.FoundFile;
import soot.IFoundFile;

import java.io.IOException;
import java.io.InputStream;

public class FoundFileTest {
    @Test
    public void testSharedZipFile1() {
        IFoundFile f = new FoundFile("./src/test/resources/LineNumberAdderTest/C.jar", "C.class");
        try (InputStream is = f.inputStream()) {
            // Should be ok
            System.out.println(is.available());
        } catch (IOException e) {
            // ok
        }
        // Should be ok
        f.getZipFile().size();
        f.close();
    }

    @Test
    public void testSharedZipFile2() {
        IFoundFile f = new FoundFile("./src/test/resources/LineNumberAdderTest/C.jar", "C.class");
        InputStream is = f.inputStream();
        InputStream is2 = f.inputStream();
        try {
            System.out.println(is.available());
            System.out.println(is2.available());
            is.close();
            System.out.println(is2.available());
            is2.close();
        } catch (IOException e) {
            // ok
        }
        f.close();
    }

    @Test
    public void testSharedZipFile3() throws IOException {
        // Check that no one closes the zip file before
        IFoundFile f = new FoundFile("./src/test/resources/LineNumberAdderTest/C.jar", "C.class");
        InputStream is = f.inputStream();
        is.close();
        f.close();
        IFoundFile f2 = new FoundFile("./src/test/resources/LineNumberAdderTest/C.jar", "C.class");
        f2.inputStream();
    }
}
