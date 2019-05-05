package com.example.administrator.myapplication;

public class MockLoginImpl implements Login{
    @Override
    public User login(String name, String pass) {
        return new User(1,"123",name);
    }
}
