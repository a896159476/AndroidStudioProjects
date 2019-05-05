package com.example.administrator.myapplication;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JavaTest {

    private AdderImpl adder;

    /**
     * 初始化注解@Before，每次都会运行
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        adder=new AdderImpl();
    }

    @Test
    public void add() throws Exception{
        Assert.assertEquals(3, adder.add(1,2));
    }
}
