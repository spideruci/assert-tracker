package org.spideruci.asserttracker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
public class UtilsTest {

    @ParameterizedTest
    @CsvSource({"(ILjava/lang/Boolean;)V, 2",
                "(I[LHang/Person;I[[C[LHang/Person;)V, 5"})
    public void shouldCalculateParamNum(String methodDesc, int expectedResult){
        assertEquals(expectedResult, Utils.calculateParaNum(methodDesc));
    }


}
