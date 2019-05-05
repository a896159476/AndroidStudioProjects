package com.example.administrator.myapplication;

public class User {
    public int id;
    public String name;
    public String pass;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public User(int id, String name, String pass) {
        this.id = id;
        this.name = name;
        this.pass = pass;
    }
}
