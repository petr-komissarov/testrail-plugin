package org.jenkinsci.plugins.testrail;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.testrail.JUnit.TestCase;
import org.jenkinsci.plugins.testrail.TestRail.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.awaitility.Duration.ONE_MINUTE;
import static org.jenkinsci.plugins.testrail.Utils.log;

public class TestRailClient {
    private String host;
    private String user;
    private String password;

    public TestRailClient(String host, String user, String password) {
        this.host = host;
        this.user = user;
        this.password = password;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private HttpClient setUpHttpClient(HttpMethod method) {
        HttpClient httpclient = new HttpClient();
        httpclient.getParams().setAuthenticationPreemptive(true);
        httpclient.getState().setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(this.user, this.password)
        );
        method.setDoAuthentication(true);
        method.addRequestHeader("Content-Type", "application/json");
        return httpclient;
    }

    private TestRailResponse httpGet(String path) {
        final TestRailResponse[] response = new TestRailResponse[1];
        await().atMost(ONE_MINUTE).pollInterval(ONE_HUNDRED_MILLISECONDS).until(() -> {
            response[0] = httpGetInt(path);
            return response[0].getStatus() != 429;
        });

        return response[0];
    }

    private TestRailResponse httpGetInt(String path) throws IOException {
        TestRailResponse result;
        GetMethod get = new GetMethod(host + "/" + path);
        HttpClient httpclient = setUpHttpClient(get);

        try {
            Integer status = httpclient.executeMethod(get);
            String body = new String(get.getResponseBody(), get.getResponseCharSet());
            result = new TestRailResponse(status, body);
        } finally {
            get.releaseConnection();
        }

        return result;
    }

    private TestRailResponse httpPost(String path, String payload)
            throws TestRailException {
        final TestRailResponse[] response = new TestRailResponse[1];
        await().atMost(ONE_MINUTE).pollInterval(ONE_HUNDRED_MILLISECONDS).until(() -> {
            response[0] = httpPostInt(path, payload);
            return response[0].getStatus() != 429;
        });

        if (response[0].getStatus() != 200) {
            throw new TestRailException("Posting to " + path + " returned an error! Response from TestRail is: \n" + response[0].getBody());
        }
        return response[0];
    }

    private TestRailResponse httpPostInt(String path, String payload)
            throws IOException {
        TestRailResponse result;
        PostMethod post = new PostMethod(host + "/" + path);
        HttpClient httpclient = setUpHttpClient(post);

        try {
            StringRequestEntity requestEntity = new StringRequestEntity(
                    payload,
                    "application/json",
                    "UTF-8"
            );
            post.setRequestEntity(requestEntity);
            Integer status = httpclient.executeMethod(post);
            String body = new String(post.getResponseBody(), post.getResponseCharSet());
            result = new TestRailResponse(status, body);
        } finally {
            post.releaseConnection();
        }

        return result;
    }

    public boolean serverReachable() throws IOException {
        boolean result = false;
        HttpClient httpclient = new HttpClient();
        GetMethod get = new GetMethod(host);
        try {
            httpclient.executeMethod(get);
            result = true;
        } catch (java.net.UnknownHostException e) {
            // nop - we default to result == false
        } finally {
            get.releaseConnection();
        }
        return result;
    }

    public boolean isNotAuthenticationWorks() {
        TestRailResponse response = httpGet("/index.php?/api/v2/get_projects");
        return (200 != response.getStatus());
    }

    public Project[] getProjects() throws IOException, ElementNotFoundException {
        String body = httpGet("/index.php?/api/v2/get_projects").getBody();
        JSONArray json = new JSONArray(body);
        Project[] projects = new Project[json.length()];
        for (int i = 0; i < json.length(); i++) {
            JSONObject o = json.getJSONObject(i);
            Project p = new Project();
            p.setName(o.getString("name"));
            p.setId(o.getInt("id"));
            projects[i] = p;
        }
        return projects;
    }

    public Suite[] getSuites(int projectId) throws IOException, ElementNotFoundException {
        String body = httpGet("/index.php?/api/v2/get_suites/" + projectId).getBody();

        JSONArray json;
        try {
            json = new JSONArray(body);
        } catch (JSONException e) {
            return new Suite[0];
        }

        Suite[] suites = new Suite[json.length()];
        for (int i = 0; i < json.length(); i++) {
            JSONObject o = json.getJSONObject(i);
            Suite s = new Suite();
            s.setName(o.getString("name"));
            s.setId(o.getInt("id"));
            suites[i] = s;
        }

        return suites;
    }

    public Plan[] getPlans(int projectId) throws IOException {
        String body = httpGet("/index.php?/api/v2/get_plans/" + projectId).getBody();

        JSONArray json;
        try {
            json = new JSONArray(body);
        } catch (JSONException e) {
            return new Plan[0];
        }

        Plan[] plans = new Plan[json.length()];
        for (int i = 0; i < json.length(); i++) {
            JSONObject o = json.getJSONObject(i);
            Plan p = new Plan();
            p.setName(o.getString("name"));
            p.setId(o.getInt("id"));
            plans[i] = p;
        }

        return plans;
    }

    public Case[] getCases(int projectId, int suiteId) throws ElementNotFoundException {
        String body = httpGet("index.php?/api/v2/get_cases/" + projectId + "&suite_id=" + suiteId).getBody();

        JSONArray json;

        try {
            json = new JSONArray(body);
        } catch (JSONException e) {
            throw new ElementNotFoundException("No cases for project " + projectId + " and suite " + suiteId + "! Response from TestRail is: \n" + body);
        }

        Case[] cases = new Case[json.length()];
        for (int i = 0; i < json.length(); i++) {
            JSONObject o = json.getJSONObject(i);
            cases[i] = createCaseFromJson(o);
        }

        return cases;
    }

    public Section[] getSections(int projectId, int suiteId) {
        String body = httpGet("index.php?/api/v2/get_sections/" + projectId + "&suite_id=" + suiteId).getBody();
        JSONArray json = new JSONArray(body);

        Section[] sects = new Section[json.length()];
        for (int i = 0; i < json.length(); i++) {
            JSONObject o = json.getJSONObject(i);
            sects[i] = createSectionFromJSON(o);
        }

        return sects;
    }

    private Section createSectionFromJSON(JSONObject o) {
        Section s = new Section();

        s.setName(o.getString("name"));
        s.setId(o.getInt("id"));

        if (!o.isNull("parent_id")) {
            s.setParentId(String.valueOf(o.getInt("parent_id")));
        } else {
            s.setParentId("null");
        }

        s.setSuiteId(o.getInt("suite_id"));

        return s;
    }

    public Section addSection(String sectionName, int projectId, int suiteId, String parentId)
            throws TestRailException {
        String payload = new JSONObject().put("name", sectionName).put("suite_id", suiteId).put("parent_id", parentId).toString();
        String body = httpPost("index.php?/api/v2/add_section/" + projectId, payload).getBody();
        JSONObject o = new JSONObject(body);

        return createSectionFromJSON(o);
    }

    private Case createCaseFromJson(JSONObject o) {
        Case s = new Case();

        s.setTitle(o.getString("title"));
        s.setId(o.getInt("id"));
        s.setSectionId(o.getInt("section_id"));
        s.setRefs(o.optString("refs"));

        return s;
    }

    public Case addCase(TestCase caseToAdd, int sectionId)
            throws TestRailException {
        JSONObject payload = new JSONObject().put("title", caseToAdd.getName());
        if (!StringUtils.isEmpty(caseToAdd.getRefs())) {
            payload.put("refs", caseToAdd.getRefs());
        }

        String body = httpPost("index.php?/api/v2/add_case/" + sectionId, payload.toString()).getBody();
        return createCaseFromJson(new JSONObject(body));
    }

    public TestRailResponse addResultsForCases(int runId, TestRailResults results)
            throws TestRailException {
        JSONArray a = new JSONArray();
        for (int i = 0; i < results.getResults().size(); i++) {
            JSONObject o = new JSONObject();
            TestRailResult r = results.getResults().get(i);
            o.put("case_id", r.getCaseId()).put("status_id", r.getStatus().getValue()).put("comment", r.getComment()).put("elapsed", r.getElapsedTimeString());
            a.put(o);
        }

        String payload = new JSONObject().put("results", a).toString();
        log(payload);
        return httpPost("index.php?/api/v2/add_results_for_cases/" + runId, payload);
    }

    public int addRun(int projectId, int suiteId, String description)
            throws TestRailException {
        String runName = "Test Run " + new SimpleDateFormat("M/d/yy").format(new Date()) + " by Jenkins";
        String payload = new JSONObject()
                .put("suite_id", suiteId)
                .put("name", runName)
                .put("description", description)
                .toString();
        String body = httpPost("index.php?/api/v2/add_run/" + projectId, payload).getBody();
        return new JSONObject(body).getInt("id");
    }

    public int addPlanEntry(int planId, int suiteId, String description)
            throws TestRailException {
        String runName = "Test Run " + new SimpleDateFormat("M/d/yy").format(new Date()) + " by Jenkins";
        String payload = new JSONObject()
                .put("suite_id", suiteId)
                .put("name", runName)
                .put("description", description)
                .put("runs", new JSONArray())
                .toString();
        String body = httpPost("index.php?/api/v2/add_plan_entry/" + planId, payload).getBody();
        return new JSONObject(body).getJSONArray("runs").getJSONObject(0).getInt("id");
    }
}
