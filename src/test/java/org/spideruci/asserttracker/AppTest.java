package org.spideruci.asserttracker;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test for simple App.
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
