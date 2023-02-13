package org.jenkinsci.plugins.testrail;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.testrail.JUnit.Failure;
import org.jenkinsci.plugins.testrail.JUnit.JUnitResults;
import org.jenkinsci.plugins.testrail.JUnit.TestCase;
import org.jenkinsci.plugins.testrail.JUnit.TestSuite;
import org.jenkinsci.plugins.testrail.TestRail.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import static hudson.model.Result.FAILURE;

public class TestRailNotifier extends Notifier implements SimpleBuildStep {

    private int testrailProject;
    private int testrailSuite;
    private int testrailTestPlan;
    private String junitResultsGlob;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public TestRailNotifier(int testrailProject, int testrailSuite, int testrailTestPlan, String junitResultsGlob) {
        this.testrailProject = testrailProject;
        this.testrailSuite = testrailSuite;
        this.testrailTestPlan = testrailTestPlan;
        this.junitResultsGlob = junitResultsGlob;
    }

    public int getTestrailProject() {
        return this.testrailProject;
    }

    @DataBoundSetter
    public void setTestrailProject(int project) {
        this.testrailProject = project;
    }

    public int getTestrailSuite() {
        return this.testrailSuite;
    }

    @DataBoundSetter
    public void setTestrailSuite(int suite) {
        this.testrailSuite = suite;
    }

    public int getTestrailTestPlan() {
        return this.testrailTestPlan;
    }

    @DataBoundSetter
    public void setTestrailTestPlan(int plan) {
        this.testrailTestPlan = plan;
    }

    public String getJunitResultsGlob() {
        return this.junitResultsGlob;
    }

    @DataBoundSetter
    public void setJunitResultsGlob(String glob) {
        this.junitResultsGlob = glob;
    }

    @Override
    public void perform(@Nonnull hudson.model.Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        TestRailClient testrail = getDescriptor().getTestrailInstance();
        testrail.setHost(getDescriptor().getTestrailHost());
        testrail.setUser(getDescriptor().getTestrailUser());
        testrail.setPassword(getDescriptor().getTestrailPassword());
        PrintStream logger = taskListener.getLogger();

        ExistingTestCases testCases = getTestCasesFromTestRail(run, testrail, logger);
        List<TestSuite> junitTestResults = parseJUnitTestResults(run, workspace, logger);
        TestRailResults trResults = convertJUnitResultsToTestRailFormat(testCases, junitTestResults, logger);

        String runComment = "Automated results from Jenkins: " + workspace.toURI();

        int runId = -1;
        TestRailResponse response = null;
        try {
            if (testCases != null) {
                if (getTestrailTestPlan() == 0) {
                    runId = testrail.addRun(testCases.getProjectId(), testCases.getSuiteId(), runComment);
                } else {
                    runId = testrail.addPlanEntry(getTestrailTestPlan(), testCases.getSuiteId(), runComment);
                }
            }

            if (trResults != null && trResults.getResults() != null && trResults.getResults().size() > 0) {
                logger.println(" Start to push results to the test run id " + runId);
                response = testrail.addResultsForCases(runId, trResults);
            } else {
                logger.println("Results is empty");
            }
        } catch (TestRailException e) {
            logger.println("Error pushing results to TestRail");
            logger.println(e.getMessage());
            run.setResult(FAILURE);
        }

        boolean buildResult;
        if (response != null) {
            buildResult = (200 == response.getStatus());

            if (buildResult) {
                logger.println("Successfully uploaded test results to Test Rail");
            } else {
                logger.println("Failed to add results to TestRail");
                logger.println("status: " + response.getStatus());
                logger.println("body :\n" + response.getBody());
            }
        }
    }

    public ExistingTestCases getTestCasesFromTestRail(hudson.model.Run<?, ?> run, TestRailClient testrail, PrintStream logger) {
        ExistingTestCases testCases = null;

        try {
            testCases = new ExistingTestCases(testrail, getTestrailProject(), getTestrailSuite());
        } catch (ElementNotFoundException | IOException e) {
            logger.println("Cannot find project or suite on TestRail server. Please check your Jenkins job and system configurations");
            run.setResult(FAILURE);
        }

        String[] caseNames = new String[]{};
        try {
            if (testCases != null) {
                caseNames = testCases.listTestCases();
            }
            logger.println("TestRail test cases: ");
            for (String caseName : caseNames) {
                logger.println("  " + caseName);
            }
        } catch (ElementNotFoundException e) {
            logger.println("Failed to list TestRail test cases");
            logger.println("Element not found:" + e.getMessage());
        }

        if (testCases == null) {
            logger.println("Couldn't get test cases from Test Rail");
        }

        return testCases;
    }

    public FilePath copyJUnitResultsToTempDir(hudson.model.Run<?, ?> run, FilePath workspace, PrintStream logger) throws IOException {
        logger.println("Start to copy JUnit test results to the temp dir");
        FilePath tempDir = new FilePath(Util.createTempDir());

        try {
            workspace.copyRecursiveTo(getJunitResultsGlob(), "", tempDir);
            logger.println("JUnit test results successfully copied to the temp dir");
        } catch (Exception e) {
            logger.println("Error trying to copy files to Jenkins master: " + e.getMessage());
            run.setResult(FAILURE);
        }

        return tempDir;
    }

    public List<TestSuite> parseJUnitTestResults(hudson.model.Run<?, ?> run, FilePath workspace, PrintStream logger) {
        LinkedList<TestSuite> junitTestSuites = new LinkedList<>();
        try {
            FilePath tempDir = copyJUnitResultsToTempDir(run, workspace, logger);
            junitTestSuites.addAll(new JUnitResults(tempDir, getJunitResultsGlob(), logger).getSuites());
        } catch (IOException | JAXBException | InterruptedException e) {
            logger.println(e.getMessage());
            run.setResult(FAILURE);
        }

        if (junitTestSuites.size() == 0) {
            logger.println("Couldn't parse JUnit results from xml");
        }

        return junitTestSuites;
    }


    public TestRailResults convertJUnitResultsToTestRailFormat(ExistingTestCases testCases, List<TestSuite> junitTestResults, PrintStream logger) {
        TestRailResults trResults = new TestRailResults();
        try {
            if (testCases != null && junitTestResults != null) {
                for (TestSuite junitSuite : junitTestResults) {
                    trResults.merge(addSuite(junitSuite, null, testCases, logger));
                }
            }
        } catch (Exception e) {
            logger.println("Failed to convert JUnit test suites to the TestRail test suites");
            logger.println("EXCEPTION: " + e.getMessage());
        }

        if (trResults.getResults().size() == 0) {
            logger.println("Test Rail results is empty");
        }

        return trResults;
    }

    public TestRailResults addSuite(TestSuite suite, String parentId, ExistingTestCases
            existingCases, PrintStream logger) throws IOException, TestRailException {
        //figure out TR sectionID
        int sectionId;
        try {
            sectionId = existingCases.getSectionId(suite.getName());
        } catch (ElementNotFoundException e1) {
            try {
                sectionId = existingCases.addSection(suite.getName(), parentId);
                logger.println("Section added to Test Rail run" + suite.getName());
            } catch (ElementNotFoundException e) {
                logger.println("Unable to add test section " + suite.getName());
                logger.println(e.getMessage());
                return null;
            }
        }

        //if we have any subsections - process them
        TestRailResults results = new TestRailResults();

        if (suite.getSuites() != null && suite.getSuites().size() > 0) {
            for (TestSuite subSuite : suite.getSuites()) {
                results.merge(addSuite(subSuite, String.valueOf(sectionId), existingCases, logger));
            }
        } else {
            logger.println("JUnit sub suites are empty for suite " + suite.getName());
        }

        if (suite.getCases() != null && suite.getCases().size() > 0) {
            for (TestCase testcase : suite.getCases()) {
                int caseId;
                try {
                    caseId = existingCases.getCaseId(suite.getName(), testcase.getName());
                    logger.println("JUnit test case " + testcase.getName() + " already exists in Test Rail run and will be updated");
                } catch (ElementNotFoundException e) {
                    caseId = existingCases.addCase(testcase, sectionId);
                    logger.println("JUnit test case " + testcase.getName() + " not exists in Test Rail run but will be added");
                }
                CaseStatus caseStatus;
                Float caseTime = testcase.getTime();
                String caseComment = null;
                Failure caseFailure = testcase.getFailure();
                if (caseFailure != null) {
                    caseStatus = CaseStatus.FAILED;
                    caseComment = (caseFailure.getMessage() == null) ? caseFailure.getText() : caseFailure.getMessage() + "\n" + caseFailure.getText();
                } else if (testcase.getSkipped() != null) {
                    caseStatus = CaseStatus.UNTESTED;
                } else {
                    caseStatus = CaseStatus.PASSED;
                }

                if (caseStatus != CaseStatus.UNTESTED) {
                    results.addResult(new TestRailResult(caseId, caseStatus, caseComment, caseTime));
                }
            }
        } else {
            logger.println("JUnit test cases are empty for suite " + suite.getName());
        }

        return results;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE; //null;
    }

    @Symbol("testRail")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private String testrailHost = "";
        private String testrailUser = "";
        private String testrailPassword = "";
        private TestRailClient testrail = new TestRailClient("", "", "");

        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public FormValidation doCheckTestrailProject(@QueryParameter int value)
                throws IOException, ServletException {
            testrail.setHost(getTestrailHost());
            testrail.setUser(getTestrailUser());
            testrail.setPassword(getTestrailPassword());
            if (getTestrailHost().isEmpty() || getTestrailUser().isEmpty() || getTestrailPassword().isEmpty() || !testrail.serverReachable() || testrail.isNotAuthenticationWorks()) {
                return FormValidation.warning("Please fix your TestRail configuration in Manage Jenkins -> Configure System.");
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillTestrailProjectItems() {
            testrail.setHost(getTestrailHost());
            testrail.setUser(getTestrailUser());
            testrail.setPassword(getTestrailPassword());

            ListBoxModel items = new ListBoxModel();
            try {
                for (Project prj : testrail.getProjects()) {
                    items.add(prj.getName(), prj.getStringId());
                }
            } catch (ElementNotFoundException | IOException ignored) {
            }

            return items;
        }

        public ListBoxModel doFillTestrailSuiteItems(@QueryParameter int testrailProject) {
            testrail.setHost(getTestrailHost());
            testrail.setUser(getTestrailUser());
            testrail.setPassword(getTestrailPassword());

            ListBoxModel items = new ListBoxModel();
            try {
                for (Suite suite : testrail.getSuites(testrailProject)) {
                    items.add(suite.getName(), suite.getStringId());
                }
            } catch (ElementNotFoundException | IOException ignored) {
            }

            return items;
        }

        public FormValidation doCheckTestrailSuite(@QueryParameter String value)
                throws IOException, ServletException {
            testrail.setHost(getTestrailHost());
            testrail.setUser(getTestrailUser());
            testrail.setPassword(getTestrailPassword());

            if (getTestrailHost().isEmpty() || getTestrailUser().isEmpty() || getTestrailPassword().isEmpty() || !testrail.serverReachable() || testrail.isNotAuthenticationWorks()) {
                return FormValidation.warning("Please fix your TestRail configuration in Manage Jenkins -> Configure System.");
            }

            return FormValidation.ok();
        }

        public ListBoxModel doFillTestrailTestPlanItems(@QueryParameter int testrailProject) {
            testrail.setHost(getTestrailHost());
            testrail.setUser(getTestrailUser());
            testrail.setPassword(getTestrailPassword());

            ListBoxModel items = new ListBoxModel();
            try {
                for (Plan plan : testrail.getPlans(testrailProject)) {
                    if (plan != null && !plan.is_completed) {
                        items.add(plan.getName(), plan.getStringId());
                    }
                }
            } catch (IOException ignored) {
            }

            return items;
        }

        public FormValidation doCheckTestrailTestPlan(@QueryParameter String value)
                throws IOException, ServletException {
            testrail.setHost(getTestrailHost());
            testrail.setUser(getTestrailUser());
            testrail.setPassword(getTestrailPassword());

            if (getTestrailHost().isEmpty() || getTestrailUser().isEmpty() || getTestrailPassword().isEmpty() || !testrail.serverReachable() || testrail.isNotAuthenticationWorks()) {
                return FormValidation.warning("Please fix your TestRail configuration in Manage Jenkins -> Configure System.");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckJunitResultsGlob(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.warning("Please select test result path.");
            // TODO: Should we check to see if the files exist? Probably not.
            return FormValidation.ok();
        }

        public FormValidation doCheckTestrailHost(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.warning("Please add your TestRail host URI.");
            }
            // TODO: There is probably a better way to do URL validation.
            if (!value.startsWith("http://") && !value.startsWith("https://")) {
                return FormValidation.error("Host must be a valid URL.");
            }
            testrail.setHost(value);
            testrail.setUser("");
            testrail.setPassword("");
            if (!testrail.serverReachable()) {
                return FormValidation.error("Host is not reachable.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTestrailUser(@QueryParameter String value,
                                                  @QueryParameter String testrailHost,
                                                  @QueryParameter String testrailPassword)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.warning("Please add your user's email address.");
            }
            if (testrailPassword.length() > 0) {
                testrail.setHost(testrailHost);
                testrail.setUser(value);
                testrail.setPassword(testrailPassword);
                if (testrail.serverReachable() && testrail.isNotAuthenticationWorks()) {
                    return FormValidation.error("Invalid user/password combination.");
                }
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTestrailPassword(@QueryParameter String value,
                                                      @QueryParameter String testrailHost,
                                                      @QueryParameter String testrailUser)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.warning("Please add your password.");
            }
            if (testrailUser.length() > 0) {
                testrail.setHost(testrailHost);
                testrail.setUser(testrailUser);
                testrail.setPassword(value);
                if (testrail.serverReachable() && testrail.isNotAuthenticationWorks()) {
                    return FormValidation.error("Invalid user/password combination.");
                }
            }
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "TestRail Plugin";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            testrailHost = formData.getString("testrailHost");
            testrailUser = formData.getString("testrailUser");
            testrailPassword = formData.getString("testrailPassword");

            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setTestrailHost)
            save();
            return super.configure(req, formData);
        }

        public String getTestrailHost() {
            return testrailHost;
        }

        public void setTestrailHost(String host) {
            this.testrailHost = host;
        }

        public String getTestrailUser() {
            return testrailUser;
        }

        public void setTestrailUser(String user) {
            this.testrailUser = user;
        }

        public String getTestrailPassword() {
            return testrailPassword;
        }

        public void setTestrailPassword(String password) {
            this.testrailPassword = password;
        }

        public TestRailClient getTestrailInstance() {
            return testrail;
        }

        public void setTestrailInstance(TestRailClient trc) {
            testrail = trc;
        }
    }
}
