package com.example.administrator.myapplication;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AdderImplTest {

    private AdderImpl adder;
    @Before
    public void setUp() throws Exception {
        adder=new AdderImpl();
    }

    @Test
    public void add() {
        assertEquals(3,adder.add(1,2));
    }
}