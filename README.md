# cpuhogtd (CPU Hog Thread Dump)

## config.json

The default values are,

```
{
  "CPU_THRESHOLD": 50.0,
  "CHECK_INTERVAL_MS": 60000,
  "AGGRESSIVE_INTERVAL_MS": 10000,
  "CONCERN_THRESHOLD": 12
}
```

# How to test

In order to test with [system-load-generator](https://github.com/pradykaushik/system-load-generator) was very helpful for this project during the development. However it can only be run on LINUX, as it used `lscpu`, I forked it at https://github.com/omnine/system-load-generator/tree/develop.

 You need to reduce the intervals.

`system-load-generator.bat --load-type cpuload`

```
{
	"CPU_THRESHOLD": 20,
	"CHECK_INTERVAL_MS": 6000,
	"AGGRESSIVE_INTERVAL_MS": 1000,
	"CONCERN_THRESHOLD": 3
}
```

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