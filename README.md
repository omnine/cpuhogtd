# cpuhogtd (CPU Hog Thread Dump)

## config.json

```
{
  "LateStart": 300000,
  "IgnoreUnder": 5.0,
  "CPUThreshold": 60.0,
  "CheckInterval": 60000,
  "AggressiveInterval": 1000,
  "ConcernThreshold": 10
}
```

`LateStart`, the default value is `5` minutes (= 300,000 milliseconds). The monitoring will begin in this time after the targeted application starts up. Generally the start-up is CPU intensive, we can skip this period.

`IgnoreUnder`, the default value is `5.0%`. The stack trace of the thread with CPU under this value will not be printed out.

`CPUThreshold`, the default value is `60.0%`. The agent will activate the alarm when the CPU of the monitored process is over this amount. 

`CheckInterval`, the default value is `60,000` ms (=1 minute). This has to be carefully selected. It is recommended as half of the peak range.

`AggressiveInterval`: the default value is `1,000` ms (=1 sec).  
`ConcernThreshold`: the default value is `10`.  
These two are hard to pick. They depend on the peak pattern. Once the alarm is activated, the agent enters to the aggressive mode, check the CPU more frequently.The frequency will be once per second, and the concern will be added 1 up if the CPU is over the `CPUThreshold`. Once the total of the concern is equal to `ConcernThreshold`, The agent will dump the thread. 


# How to test

In order to test with [system-load-generator](https://github.com/pradykaushik/system-load-generator) was very helpful for this project during the development. However it can only be run on LINUX, as it used `lscpu`, I forked it at https://github.com/omnine/system-load-generator/tree/develop.

 You need to reduce the intervals.

`system-load-generator.bat --load-type cpuload`




In order to simulate a real case, CPU spike, we can use https://github.com/msigwart/fakeload

```
import com.martensigwart.fakeload.*;
import java.util.concurrent.TimeUnit;
FakeLoad fakeload = FakeLoads.create()
        .lasting(10, TimeUnit.SECONDS)
        .withCpu(70)
        .withMemory(30, MemoryUnit.MB);

// Execute FakeLoad synchronously
FakeLoadExecutor executor = FakeLoadExecutors.newDefaultExecutor();
executor.execute(fakeload);

```

## Usage

`-javaagent:path/to/java-agent-sample-1.0-SNAPSHOT.jar`

## References

[CPU Load Generator in Java](https://blog.caffinc.com/2016/03/cpu-load-generator/)
[system-load-generator](https://github.com/pradykaushik/system-load-generator)

https://github.com/c2nes/jtopthreads

https://github.com/msigwart/fakeload