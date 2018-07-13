package org.cups4j;

import cups4j.TestCups;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link CupsPrinter} class.
 *
 * @author oboehm
 */
public final class CupsPrinterTest {

    private static final Logger LOG = LoggerFactory.getLogger(CupsPrinterTest.class);
    private static CupsPrinter printer;

    @BeforeClass
    public static void setUpPrinter() {
        printer = getPrinter();
        assertNotNull(printer);
        LOG.info("Printer {} was choosen for testing.", printer);
    }

    @Test
    public void testPrintPDF() {
        print(printer, new File("src/test/resources/test.pdf"));
    }

    @Test
    public void testPrintText() {
        print(printer, new File("src/test/resources/test.txt"));
    }

    private PrintRequestResult print(CupsPrinter printer, File file) {
        PrintJob job = createPrintJob(file);
        LOG.info("Print job '{}' will be sent to {}.", job, printer);
        try {
            return printer.print(job);
        } catch (Exception ex) {
            throw new IllegalStateException("print of '" + file + "' failed", ex);
        }
    }

    @Test
    public void testPrintList() throws UnsupportedEncodingException {
        File file = new File("src/test/resources/test.txt");
        PrintJob job = createPrintJob(file);
        printer.print(job, job);
    }

    @Test
    public void testPrintListWithEmtpyJob() throws UnsupportedEncodingException {
        File file = new File("src/test/resources/test.txt");
        PrintJob job = createPrintJob(file);
        PrintJob empty = createPrintJob(new byte[0], "empty");
        printer.print(job, job);
    }

    private PrintJob createPrintJob(File file) {
        String jobname = generateJobnameFor(file);
        try {
            byte[] content = FileUtils.readFileToByteArray(file);
            return createPrintJob(content, jobname);
        } catch (IOException ioe) {
            throw new IllegalArgumentException("cannot read '" + file + "'", ioe);
        }
    }

    private PrintJob createPrintJob(byte[] content, String jobname) {
        String userName = System.getProperty("user.name", CupsClient.DEFAULT_USER);
        return new PrintJob.Builder(content).jobName(jobname).userName(userName).build();
    }

    private static String generateJobnameFor(File file) {
        String basename = file.getName().split("\\.")[0];
        return generateJobNameFor(basename);
    }

    private static String generateJobNameFor(String basename) {
        byte[] epochTime = Base64.encodeBase64(BigInteger.valueOf(System.currentTimeMillis()).toByteArray());
        return basename + new String(epochTime).substring(2);
    }

    /**
     * Gets a printer for testing. This is either the printer defined by the
     * system property 'printer' or the default printer.
     *
     * @return the printer
     */
    public static CupsPrinter getPrinter()  {
        String name = System.getProperty("printer");
        if (name == null) {
            LOG.info("To specify printer please set system property 'printer'.");
            try {
                return TestCups.getCupsClient().getDefaultPrinter();
            } catch (Exception ex) {
                throw new IllegalStateException("no printer configured by system property 'printer'", ex);
            }
        } else {
            return getPrinter(name);
        }
    }

    /**
     * Returns the printer with the given name. The search of the name is
     * not case sensitiv.
     *
     * @param name name of the printer
     * @return printer
     */
    public static CupsPrinter getPrinter(String name) {
        try {
            List<CupsPrinter> printers = TestCups.getCupsClient().getPrinters();
            CupsPrinter printer = null;
            for (CupsPrinter p : printers) {
                if (name.equalsIgnoreCase(p.getName())) {
                    return p;
                }
            }
            throw new IllegalArgumentException("not a valid printer name: " + name);
        } catch (Exception ex) {
            throw new IllegalStateException("cannot get printers", ex);
        }
    }

}
