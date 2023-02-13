package org.jenkinsci.plugins.testrail.TestRail;


public class Plan {
    public int id;
    public String name;
    public String description;
    public Object milestone_id;
    public Object assignedto_id;
    public boolean is_completed;
    public Object completed_on;
    public int passed_count;
    public int blocked_count;
    public int untested_count;
    public int retest_count;
    public int failed_count;
    public int custom_status1_count;
    public int custom_status2_count;
    public int custom_status3_count;
    public int custom_status4_count;
    public int custom_status5_count;
    public int custom_status6_count;
    public int custom_status7_count;
    public int project_id;
    public int created_on;
    public int created_by;
    public String url;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public String getStringId() {
        return Integer.toString(id);
    }

    public void setId(int id) {
        this.id = id;
    }
}


