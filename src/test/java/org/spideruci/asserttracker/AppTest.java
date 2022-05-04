package org.spideruci.asserttracker;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit test for simple App.
 * run mvn test 1> out.txt 2> result.txt
 * instrumentation error printing results are in result.txt file.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void shouldFail()
    {
        assertTrue( false );
    }

    @Test
    public void shouldDumpInt(){
        int i =10;
        assertEquals(10,i);
    }

    @Test
    public void shouldDumpByteCharShort(){
        byte b = 'd';
        assertEquals('d',b);
        char c = 's';
        assertEquals('s',c);
        short s = 1;
        assertEquals(1, s);
    }

    @Test
    public void shouldDumpChar(){

    }

    @Test
    public void shouldDumpLong(){
        long k = 3;
        assertEquals(3, k);
    }

    @Test
    public void shouldDumpFloat(){
        float k = 3;
        assertEquals(3, k,0.00001);
    }

    @Test
    public void shouldDumpDouble(){
        double k = 3;
        assertEquals(3, k,0.0001);
    }

    @Test
    public void shouldDumpObject(){
        Person p = new Person(19, true);
        assertNotEquals(new Person(19,true), p);
    }

    @Test
    public void shouldDumpArray(){
        Person[] persons =new Person[2];
        persons[0]=new Person(13,false);
        persons[1]= new Person(10,true);
        assertTrue(persons[1].sex);
    }

    @Test
    public void shouldPassAndFailWithLocalVariables()
    {

        assertTrue( true );

        int i = 0;
        double k = 9;

        if (i == 9) {
            String j1 = "y";
            System.out.println(j1);
            assertTrue(true);
            k += 1;
        }

        if (i == 0) {
            float a = 3;
            assertTrue(true);
            int j2 = 9;
            assertTrue(true);
            long y = 99;
            assertTrue(true);
            System.out.println(j2);
            System.out.println(y);
        }

        System.out.println(k);

        assertTrue( false );
    }
}

class Person{
    public int age;
    public Boolean sex;
    public Person(int age, Boolean sex){
        this.age = age;
        this.sex = sex;
    }
}
