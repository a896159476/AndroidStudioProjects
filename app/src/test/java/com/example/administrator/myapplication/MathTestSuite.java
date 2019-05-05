package com.example.administrator.myapplication;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;


public class MathTestSuite {

    public static TestSuite suite(){
        TestSuite suite = new TestSuite("com.book.jtm");
        suite.addTest(new JUnit4TestAdapter(AdderImplTest.class));
        return suite;
    }
}
