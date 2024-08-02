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

In order to test with [system-load-generator](https://github.com/pradykaushik/system-load-generator), You need to reduce the intervals.

```
{
	"CPU_THRESHOLD": 20,
	"CHECK_INTERVAL_MS": 6000,
	"AGGRESSIVE_INTERVAL_MS": 1000,
	"CONCERN_THRESHOLD": 3
}
```

## Usage

`-javaagent:path/to/java-agent-sample-1.0-SNAPSHOT.jar`

## References

[CPU Load Generator in Java](https://blog.caffinc.com/2016/03/cpu-load-generator/)
[system-load-generator](https://github.com/pradykaushik/system-load-generator)

https://github.com/c2nes/jtopthreads

https://github.com/msigwart/fakeload