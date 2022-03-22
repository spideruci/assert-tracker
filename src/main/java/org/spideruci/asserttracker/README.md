# Assert Tracker

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