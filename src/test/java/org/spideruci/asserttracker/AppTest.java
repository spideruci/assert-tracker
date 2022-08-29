package org.spideruci.asserttracker;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

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


    @ParameterizedTest
    @CsvSource({"1,true","4,false"})
    public void parmeterizedTest(int age, Boolean sex){
        Person p = new Person(age,sex);
        assertTrue(true);
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

    @Test
    public void noLocalVariable(){
        assertEquals(3, new Person(3,true).age);
    }

    @Test
    public void forLoopGenerator(){
        Person[] persons =new Person[2];
        persons[0]=new Person(13,false);
        persons[1]= new Person(10,true);
        for(Person p: persons){
            System.out.println("do something");
            assertTrue(true);
        }
        assertTrue(true);
    }

    @Test
    public void longForLoops(){
        for(int i =0; i<20; i++){
            System.out.println(i);
            assertTrue(true);
        }
    }

    @Test
    public void testTryCatch(){
        Person person1 = new Person(2,false);
        try{
            Person person = new Person(1,true);
            assertTrue(true);
        }catch(Exception e){
            Person person2 = new Person(3,true);
            assertTrue(true);
        }
        assertTrue(true);
    }

    @Test
    public void testNoTryCatch(){
        Person person1 = new Person(2,false);
        
            Person person = new Person(1,true);
            assertTrue(true);
        
            Person person2 = new Person(3,true);
            assertTrue(true);
        
        assertTrue(true);
    }

    @Test
    public void testNoTryCatchEarlyReturn(){
        Person person1 = new Person(2,false);

        if (person1.age > 2) { return; }
        
            Person person = new Person(1,true);
            assertTrue(true);
        
            Person person2 = new Person(3,true);
            assertTrue(true);
        
        assertTrue(true);
    }

    @Test
    public void testOuterTryCatch(){
        try {
        Person person1 = new Person(2,false);
        
            Person person = new Person(1,true);
            assertTrue(true);
        
            Person person2 = new Person(3,true);
            assertTrue(true);
        
        assertTrue(true);
        } catch(Throwable t) { throw t; }
    }

    @Test
    public void testOuterTryCatchEarlyReturn(){
        try {

        Person person1 = new Person(2,false);
        
        if (person1.age > 2) { return; }

            Person person = new Person(1,true);
            assertTrue(true);
        
            Person person2 = new Person(3,true);
            assertTrue(true);
        
        assertTrue(true);
        } catch(Throwable t) { throw t; } 
    }

    public void testNestedOuterTryCatch(){
        try {
        try {
        Person person1 = new Person(2,false);
        
            Person person = new Person(1,true);
            assertTrue(true);
        
            Person person2 = new Person(3,true);
            assertTrue(true);
        
        assertTrue(true);
        } catch(Throwable t) { throw t; }
        } catch(Throwable t) { throw t; }
    }

    @Test
    public void testLong(){
        //this: ALOAD 0
        for (int i = 0; i < 3; i++) {//25000
            //i: ILOAD 1
        }
        //wierd the index of  variable below changed when instrumenting something without introducing local variable;
        //without instrumenting, it's LLOAD 1;after instrmention, it's LLOAD2
        long start = System.currentTimeMillis();

        //after removing p local variable below, it's back to LLOAD1
        Person p = new Person(1,true);
        assertEquals(50000, 50000);//50000
        System.out.println(start);
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
