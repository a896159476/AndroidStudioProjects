package com.example.administrator.myapplication;

import org.junit.Test;

import static org.junit.Assert.*;

public class NoteDAOTest {

    @Test
    public void saveNote() {
        Login login = new MockLoginImpl();
        NoteDAO noteDAO=new NoteDAO();
        noteDAO.saveNote(login.login("",""),"");

    }
}