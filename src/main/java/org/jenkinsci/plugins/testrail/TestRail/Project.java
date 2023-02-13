package org.jenkinsci.plugins.testrail.TestRail;

public class Project {
    private int _id;
    private String _name;

    public int getId() {
        return _id;
    }

    public void setId(int id) {
        _id = id;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name.trim();
    }

    public String getStringId() {
        return Integer.toString(_id);
    }
}
