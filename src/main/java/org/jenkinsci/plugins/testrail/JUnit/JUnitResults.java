package org.jenkinsci.plugins.testrail.JUnit;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import hudson.util.DirScanner;
import hudson.util.FileVisitor;
import jenkins.MasterToSlaveFileCallable;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

public class JUnitResults {
    private final FilePath baseDir;
    private final PrintStream logger;
    private final LinkedList<TestSuite> Suites;

    public JUnitResults(FilePath baseDir, String fileMatchers, PrintStream logger) throws IOException, JAXBException, InterruptedException {
        this.Suites = new LinkedList<>();
        this.baseDir = baseDir;
        this.logger = logger;
        slurpTestResults(fileMatchers);
    }

    public void slurpTestResults(String fileMatchers) throws IOException, JAXBException, InterruptedException {
        JAXBContext jaxbSuiteContext = JAXBContext.newInstance(TestSuite.class);
        JAXBContext jaxbSuitesContext = JAXBContext.newInstance(TestSuites.class);
        final Unmarshaller jaxbSuiteUnmarshaller = jaxbSuiteContext.createUnmarshaller();
        final Unmarshaller jaxbSuitesUnmarshaller = jaxbSuitesContext.createUnmarshaller();

        final DirScanner scanner = new DirScanner.Glob(fileMatchers, null);
        logger.println("Scanning " + baseDir);

        baseDir.act(new MasterToSlaveFileCallable<Void>() {
            private static final long serialVersionUID = 1L;

            public Void invoke(File f, VirtualChannel channel) throws IOException {
                scanner.scan(f, new FileVisitor() {
                    @Override
                    public void visit(File file, String s) {
                        LinkedList<TestSuite> resultsForFile = new LinkedList<>();
                        logger.println("processing " + file.getName());

                        try {
                            TestSuites suites = (TestSuites) jaxbSuitesUnmarshaller.unmarshal(file);
                            if (suites.hasSuites()) {
                                resultsForFile.addAll(suites.getSuites());
                            }
                        } catch (ClassCastException e) {
                            logger.println(e.getMessage());
                            try {
                                TestSuite suite = (TestSuite) jaxbSuiteUnmarshaller.unmarshal(file);
                                resultsForFile.add(suite);
                            } catch (JAXBException ex) {
                                logger.println(ex.getMessage());
                            }
                        } catch (JAXBException exc) {
                            logger.println(exc.getMessage());
                        }
                        if (resultsForFile.size() == 0) {
                            logger.println("Results NOT parsed from JUnit file " + file);
                        }
                        Suites.addAll(resultsForFile);
                    }
                });
                return null;
            }
        });
    }

    public List<TestSuite> getSuites() {
        return this.Suites;
    }

    //public String[] getFiles() { return this.Files.clone(); }
}
