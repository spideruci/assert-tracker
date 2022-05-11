# Assert Tracker
### Running instructions
For running this project's test cases:

1. generate jar file:
mvn clean compile assembly:single test-compile

2. instrumentation:
java -jar "Path………………/target/assert-tracker-1.0-SNAPSHOT-jar-with-dependencies.jar" target/test-classes

3. mvn test
mvn  test 1>result.txt 2>out.txt

4. the output(please comment tryLong test case, since it leads to a bug that leads to nothing in the output)

result.txt contains test suite statistics
out.txt contains method entry and exit info

xmlOutput directory contains xml files

InstrumentationUtils.java includes helper functions that can be invoked in instrumentation part so that no more new local variables would be introduced. Changing the direct java code when using XStream to dump objects is easier than changing bytecode.
However, the bad part is, it's necessary to move this file to other projects to be tested.

About the bug:
testLong method in AppTest triggers the bug. After instrumentation, the "LLOAD index" is changed for long type variable start
either changing long type to int type or removing Person p would make the bug disapppear.


For running other prejects' test cases
copy and make sure that InstrumentationUtils.java file is within the test directory in maven project
include XStream dependency in other projects' POM file,
```
<dependencies>
    <dependency>
      <groupId>xpp3</groupId>
      <artifactId>xpp3</artifactId>
      <version>1.1.4c</version>
    </dependency>
    <dependency>
      <groupId>com.thoughtworks.xstream</groupId>
      <artifactId>xstream</artifactId>
      <version>1.4.19</version>
    </dependency>
<dependencies>
```





make sure the program tested contain InstrumentationUtils


### Demo

The commands and ouput below shows how to apply the Assert Tracker on its own tests.
The project has a failing and passing test.

```
assert-tracker % mvn clean compile assembly:single test-compile
assert-tracker % mvn surefire:test                             ## <---------- run the test
[INFO] Scanning for projects...
[INFO] 
[INFO] -------------< org.spideruci.asserttracker:assert-tracker >-------------
[INFO] Building assert-tracker 1.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-surefire-plugin:2.22.1:test (default-cli) @ assert-tracker ---
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.spideruci.asserttracker.AppTest
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.016 s <<< FAILURE! - in org.spideruci.asserttracker.AppTest
[ERROR] shouldFail(org.spideruci.asserttracker.AppTest)  Time elapsed: 0.001 s  <<< FAILURE! // <---- vijay: test that we expect to fail
java.lang.AssertionError
	at org.spideruci.asserttracker.AppTest.shouldFail(AppTest.java:24)

[INFO] 
[INFO] Results:
[INFO] 
[ERROR] Failures: 
[ERROR]   AppTest.shouldFail:24
[INFO] 
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0
...
...
...
assert-tracker % java -jar target/assert-tracker-1.0-SNAPSHOT-jar-with-dependencies.jar target/test-classes/org/spideruci/asserttracker/AppTest.class ## <--- run the instrumenter
File: target/test-classes/org/spideruci/asserttracker/AppTest.class
	 + Compiled at 1647911290534 start:shouldAnswerWithTrue assertTrue
	 end:shouldAnswerWithTrue assertTrue
	 + Compiled at 1647911290549 start:shouldFail assertTrue
	 end:shouldFail assertTrue
	 + Compiled at 1647911290555 start:shouldAnswerWithTrue assertTrue
	 end:shouldAnswerWithTrue assertTrue
	 + Compiled at 1647911290555 start:shouldFail assertTrue
	 end:shouldFail assertTrue

assert-tracker % mvn surefire:test            ## <--- run the test again
[INFO] Scanning for projects...
[INFO] 
[INFO] -------------< org.spideruci.asserttracker:assert-tracker >-------------
[INFO] Building assert-tracker 1.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-surefire-plugin:2.22.1:test (default-cli) @ assert-tracker ---
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.spideruci.asserttracker.AppTest
	1647911325693//1545818955652791	 + Compiled at1647911290555 start:shouldAnswerWithTrue assertTrue  ## <--- logs from the probe...
	1647911325693//1545818955791125	 end:shouldAnswerWithTrue assertTrue
	1647911325693//1545818956148833	 + Compiled at1647911290555 start:shouldFail assertTrue
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.015 s <<< FAILURE! - in org.spideruci.asserttracker.AppTest
[ERROR] shouldFail(org.spideruci.asserttracker.AppTest)  Time elapsed: 0.001 s  <<< FAILURE!
java.lang.AssertionError
	at org.spideruci.asserttracker.AppTest.shouldFail(AppTest.java:24)

[INFO] 
[INFO] Results:
[INFO] 
[ERROR] Failures: 
[ERROR]   AppTest.shouldFail:24
[INFO] 
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0

```

A closer look at the probe's own logs:

```
## ---> logs from the execution of the passing test
	1647911325693//1545818955652791	 + Compiled at1647911290555 start:shouldAnswerWithTrue assertTrue  ## <--- probe reports assert to be executed in start:shouldAnswerWithTrue
	1647911325693//1545818955791125	 end:shouldAnswerWithTrue assertTrue ## <--- probe reports assert finished executing in start:shouldAnswerWithTrue
## ---> logs from the execution of the passing test 
	1647911325693//1545818956148833	 + Compiled at1647911290555 start:shouldFail assertTrue ## <--- probe reports assert to be executed in start:shouldAnswerWithFalse
    ## ---> logs from the end of the failing test never shows up because of the AssertionError.
```

The logs above above tell us:
- shouldAnswerWithTrue was exected before shouldAnswerWithFalse
- shouldAnswerWithTrue had a single assertion executed
- shouldAnswerWithTrue's assertion passed
- shouldAnswerWithFalse's only assertion failed

The logs above collect timestamps from both Instance.now().toEpochMilli() and System.nanoTime(), separated by two fwd-slashes (//)
```
toEpochMilli⤵
	1647911325693//1545818955652791
    System.nanoTime()⤴
```


### Updated instrumentation:
#### without mutation
when running with Pitest, the coverage collecting information would look like this:
1. for each test method, we know if it is started, finished.
2. for each test method, we know how assertion statements are executed
3. for each assertion statement, we know the direct method that holds the statement, and the type of assertion statement
4. during the coverage collecting period, Pitest also indicates the start of executing a test method
```
10:08:14 PM PIT >> INFO : MINION : 10:08:14 PM PIT >> FINE : Gathering coverage for test Description [testClass=org.jsoup.helper.DataUtilTest, name=discardsSpuriousByteOrderMark()]
10:08:14 PM PIT >> INFO : MINION : recognize an @Test Annotation
10:08:14 PM PIT >> INFO : MINION : Start executing outer test method: discardsSpuriousByteOrderMark TestClassName: org.jsoup.helper.DataUtilTest
10:08:15 PM PIT >> INFO : MINION :      1649308095062//360279131500      + Compiled at 1649308030176 start:discardsSpuriousByteOrderMark assertEquals  testClassName: org.jsoup.helper.DataUtilTest
10:08:15 PM PIT >> INFO : MINION :      1649308095078//360292788400      end:discardsSpuriousByteOrderMark assertEquals testClassName: org.jsoup.helper.DataUtilTest
10:08:15 PM PIT >> INFO : MINION : No crash or assertion failure! Finish executing outer test method: discardsSpuriousByteOrderMark TestClassName: org.jsoup.helper.DataUtilTest
10:08:15 PM PIT >> INFO : MINION : 10:08:15 PM PIT >> FINE : Gathering coverage for test Description [testClass=org.jsoup.helper.DataUtilTest, name=discardsSpuriousByteOrderMarkWhenNoCharsetSet()]
```

#### with mutation
when running with Pitest, the mutation execution output information would look like this:

a passing test case for one mutation would look like these

passing situation 1:

```
stderr  : 10:09:33 PM PIT >> FINE : mutating method scanBufferForNewlines
stderr  : 10:09:33 PM PIT >> FINE : 22 relevant test for scanBufferForNewlines
stderr  : 10:09:33 PM PIT >> FINE : replaced class with mutant in 63 ms
stderr  : 10:09:33 PM PIT >> FINE : Running 22 units
stderr  : recognize an @Test Annotation
stderr  : Start executing outer test method: canEnableAndDisableLineNumberTracking TestClassName: org.jsoup.parser.CharacterReaderTest
stderr  : 	1649308173223//438434445300	 + Compiled at 1649308031724 start:canEnableAndDisableLineNumberTracking assertFalse  testClassName: org.jsoup.parser.CharacterReaderTest
stderr  : 	1649308173223//438434509300	 end:canEnableAndDisableLineNumberTracking assertFalse testClassName: org.jsoup.parser.CharacterReaderTest
stderr  : 	1649308173223//438434555300	 + Compiled at 1649308031724 start:canEnableAndDisableLineNumberTracking assertTrue  testClassName: org.jsoup.parser.CharacterReaderTest
stderr  : 	1649308173223//438434585600	 end:canEnableAndDisableLineNumberTracking assertTrue testClassName: org.jsoup.parser.CharacterReaderTest
stderr  : 	1649308173223//438434607100	 + Compiled at 1649308031724 start:canEnableAndDisableLineNumberTracking assertFalse  testClassName: org.jsoup.parser.CharacterReaderTest
stderr  : 	1649308173223//438434622400	 end:canEnableAndDisableLineNumberTracking assertFalse testClassName: org.jsoup.parser.CharacterReaderTest
stderr  : No crash or assertion failure! Finish executing outer test method: canEnableAndDisableLineNumberTracking TestClassName: org.jsoup.parser.CharacterReaderTest
```


passing situation 2:

It's just because some test methods do not contain any assertion statements, they just want to test if it does not lead to crash.
```
stderr  : recognize an @Test Annotation
stderr  : Start executing outer test method: testUnconsumeAfterBufferUp TestClassName: org.jsoup.parser.TokeniserStateTest
stderr  : No crash or assertion failure! Finish executing outer test method: testUnconsumeAfterBufferUp TestClassName: org.jsoup.parser.TokeniserStateTest
stderr  : recognize an @Test Annotation
```

a failing test case would look like these. There is no "No crash or assertion failure" before the next recognition of "@Test" annotation

failure situation 1: 
Here, we notice that there is one assertion statement starting executing, but no end. 
It indicates that the test method ends because of assertion failure instead of "crash"
```
stderr  : recognize an @Test Annotation
stderr  : Start executing outer test method: linenumbersAgreeWithEditor TestClassName: org.jsoup.parser.CharacterReaderTest
stderr  : 	1649308173255//438470449600	 + Compiled at 1649308031740 start:linenumbersAgreeWithEditor assertEquals  testClassName: org.jsoup.parser.CharacterReaderTest
stderr  : 	1649308173255//438470497300	 end:linenumbersAgreeWithEditor assertEquals testClassName: org.jsoup.parser.CharacterReaderTest
stderr  : 	1649308173255//438470517600	 + Compiled at 1649308031740 start:linenumbersAgreeWithEditor assertEquals  testClassName: org.jsoup.parser.CharacterReaderTest
stderr  : recognize an @Test Annotation
stderr  : Start executing outer test method: canTrackNewlines TestClassName: org.jsoup.parser.CharacterReaderTest
```

failure situation 2:
Here, each assertion statement is paired with "start" and "end".
But there is no "No crash or assertion failure" info.
It indicates there is some kind of "crash" instead of "assertion failure"
Actaully, if manually check the test method, we see there are four assertion statements within the test method, and only one assertion statements assert successfully, and then there is crash during execution.

```
stderr  : recognize an @Test Annotation
stderr  : Start executing outer test method: countsColumnsOverBufferWhenNoNewlines TestClassName: org.jsoup.parser.CharacterReaderTest
stderr  : 	1649308173223//438440342800	 + Compiled at 1649308031740 start:countsColumnsOverBufferWhenNoNewlines assertEquals  testClassName: org.jsoup.parser.CharacterReaderTest
stderr  : 	1649308173223//438440375700	 end:countsColumnsOverBufferWhenNoNewlines assertEquals testClassName: org.jsoup.parser.CharacterReaderTest
stderr  : recognize an @Test Annotation
stderr  : Start executing outer test method: linenumbersAgreeWithEditor TestClassName: org.jsoup.parser.CharacterReaderTest
```

failure situation 3:
the program just meets a crash before executing any assertion statements
```
stderr  : recognize an @Test Annotation
stderr  : Start executing outer test method: testXwikiExpanded TestClassName: org.jsoup.integration.ParseTest
```
