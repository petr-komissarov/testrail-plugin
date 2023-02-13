package org.jenkinsci.plugins.testrail;

/**
 * Created by Drew on 3/20/14.
 */
public class TestRailResponse {
    private Integer status;
    private String body;

    public TestRailResponse(Integer status, String body) {
        this.status = status;
        this.body = body;
    }

    public Integer getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }
}
