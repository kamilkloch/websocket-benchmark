# Scala websocket benchmark

## Test setup

Client and server run on two separate machines. Both share the same setup: 
 - Intel® Core™ i9-13900K CPU @ 3.0GHz (max 5.8GHz, performance-cores only), 
 - RAM 64GB DDR5-4800,
 - 10Gbit network,
 - Ubuntu 23.10 (Linux 6.6.6), 
 - Oracle JDK 17 (Old ZGC) / 22 (ZGC generational with latest patches)

### Server

Server code resides in the `/server` module. Server exposes a single `/ts` websocket endpoint, which emits a timestamp every 100ms.
This targets a scenario in which a websocket channel is used to serve live market updates to the user.  

Tested servers:
 - [http4s] + blaze ([CE] 3.5.4, [fs2] 3.10.0)
 - [http4s] + blaze, via [tapir] fast-path ([CE] 3.5.4, [fs2] 3.10.0, [tapir] 1.10.0) 
 - [http4s] + blaze, via [tapir] ([CE] 3.5.4, [fs2] 3.10.0, [tapir] 1.10.0) 
 - [http4s] + blaze, via [tapir] ([CE] 3.5.4, [fs2] 3.10.0, [tapir] 1.6.3)
 - [http4s] + blaze, via [tapir] ([CE] 3.5.4, [fs2] 3.10.0, [tapir] 1.6.0)

The following configuration of Tapir endpoint out was used for the "fast-path" mode:
```scala
   webSocketBody(Fs2Streams[IO])
      .decodeCloseRequests(true)
      .concatenateFragmentedFrames(false)
      .autoPongOnPing(false)
      .ignorePong(false)
      .autoPing(None)
```
 
### Client

Client code resides in the `/client` module. [Gatling] client ramps up to 25k users within 30s, 
and each user consumes 600 messages from the websocket server (with an update every 100ms this amounts to 60s). 
For each message, an absolute difference between the client timestamp and the timestamp received from the server
is stored into an [HdrHistogram]. With clocks synchronized between the client and server, this value corresponds
to the latency induced by the server.

### Clock synchronization

For precise measurement of latency up to milliseconds need to install, configure, and run `chrony` service.

The following command could be used for installation on Ubuntu:
```sh
sudo apt-get -y install chrony
```

Here is a list of NTP servers that is used in our `/etc/chrony/chrony.conf`:
```
server time5.facebook.com iburst
server tempus1.gum.gov.pl
server tempus2.gum.gov.pl
server ntp1.tp.pl
server ntp2.tp.pl 
```

For non-Poland regions [other servers could be preferred](https://gist.github.com/mutin-sa/eea1c396b1e610a2da1e5550d94b0453).

Finally, need to restart the service after (re)configuration by:
```
sudo systemctl restart chrony
```

[Here](https://engineering.fb.com/2020/03/18/production-engineering/ntp-service/) is a great article about time synchronization in Facebook.

## OS tuning

Use transparent huge pages by starting JVM services with the `-XX:+UseTransparentHugePages` option and the following 
`transparent_hugepage` configuration in Linux:

```sh
echo madvise | sudo tee /sys/kernel/mm/transparent_hugepage/enabled
echo advise | sudo tee /sys/kernel/mm/transparent_hugepage/shmem_enabled
echo defer | sudo tee /sys/kernel/mm/transparent_hugepage/defrag
echo 1 | sudo tee /sys/kernel/mm/transparent_hugepage/khugepaged/defrag
```

Add the following configuration to `/etc/sysctl.conf`:
```txt
# Memory (all processes)
vm.swappiness = 0
vm.stat_interval=120
vm.min_free_kbytes = 4194304
vm.zone_reclaim_mode = 0
kernel.numa_balancing = 0

# File System (http/ws/db servers)
fs.file-max = 10000000
fs.nr_open = 10000000
vm.dirty_ratio = 80
vm.dirty_background_ratio = 5
vm.dirty_expire_centisecs = 12000

# Networking (aeron, http/ws servers) 
net.core.somaxconn = 65535
net.core.rmem_max = 4194304
net.core.rmem_default = 65536
net.core.wmem_max = 4194304
net.core.wmem_default = 65536
net.ipv4.ip_local_port_range = 1024 65535
net.ipv4.tcp_mem = 786432 1697152 1945728
net.ipv4.tcp_rmem = 4096 65536 4194304
net.ipv4.tcp_wmem = 4096 65536 4194304
net.ipv4.tcp_max_syn_backlog = 8192
net.ipv4.tcp_slow_start_after_idle = 0
net.ipv4.tcp_syn_retries = 2
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_max_orphans = 65536
net.ipv4.tcp_fin_timeout = 10
net.ipv4.tcp_timestamps = 1
net.ipv4.tcp_sack = 1
net.ipv4.tcp_congestion_control = htcp
net.ipv4.tcp_no_metrics_save = 1
net.ipv4.tcp_window_scaling = 1

# Profiling (perf, async-profiler)
kernel.perf_event_paranoid = -1
kernel.kptr_restrict = 0
kernel.perf_event_max_stack = 1024
kernel.perf_event_mlock_kb = 8096
```

And then run:
```sh
sudo sysctl -p
```

Install and configure an after boot service that will turn on `performance` mode for scaling governor:
```sh
sudo apt install cpufrequtils
```

Then, perform a restart and set scaling governor to performance:

```sh
sudo cpufreq-set -g performance
```

Note: If you ran the above command without restarting, then you'd have to edit the file in `/etc/init.d/cpufrequtils`
and change the line that says `GOVERNOR=ondemand` to `GOVERNOR=performance` then run:

```sh
sudo sh /etc/init.d/cpufrequtils start
```

It'd probably prompt you to restart your systemctl daemon, so do it like this:

```sh
sudo systemctl daemon-reload
```

Then view your CPU frequency with:

```sh
cat /proc/cpuinfo | grep -i mhz
```

## Benchmarks

Benchmark results reside in `/results`. 
```
 results
 ├── http4s          (JDK 22 with Generational ZGC, THP, and other optimizations, CE 3.5.4, fs2 3.10.0)
 ├── http4s-no       (JDK 17 without optimizations, CE 3.5.4, fs2 3.10.0)
 ├── http4s-co       (coordinated ommision, JDK 22 with Generational ZGC, THP, and other optimizations, CE 3.5.4, fs2 3.10.0)
 ├── tapir-1.6.0     (JDK 22 with Generational ZGC, THP, and other optimizations, CE 3.5.4, fs2 3.10.0, tapir 1.6.0)
 ├── tapir-1.6.0-no  (JDK 17 without optimizations, CE 3.5.4, fs2 3.10.0, tapir 1.6.0)
 ├── tapir-1.6.3     (JDK 22 with Generational ZGC, THP, and other optimizations, CE 3.5.4, fs2 3.10.0, tapir 1.6.3)
 ├── tapir-1.10.0    (JDK 22 with Generational ZGC, THP, and other optimizations, CE 3.5.4, fs2 3.10.0, tapir 1.10.0)
 ├── tapir-1.10.0-fp (JDK 22 with Generational ZGC, THP, and other optimizations, CE 3.5.4, fs2 3.10.0, tapir 1.10.0, fast-path)
```

Each folder contains:
  - [HdrHistogram] latency,
  - [Gatling] html report (useful to see variance in the expected 100ms between the updates across time),
  - [async-profiler] interactive flame-graph with per-thread aggregation of CPU cycles for stack frames.

## How to run benchmarks

Note: you need Java 21 to build and run the benchmarks. 

1. Build server binaries via 
   ```bash
   sbt stage
   ```
2. Start the desired server using binaries found in `server/target/universal/stage/bin`
3. Start [gatling] web socket client via
   ```bash
    sbt client/Gatling/test
   ```
## Acknowledgements

The majority of the work behind the tests is carried out by [Andriy Plokhotnyuk](https://github.com/plokhotnyuk).
Thank, you Andriy!

[tapir]: https://github.com/softwaremill/tapir
[gatling]: https://github.com/gatling/gatling
[http4s]: https://github.com/http4s/http4s
[zio-http]: https://github.com/zio/zio-http
[zio]: https://github.com/zio/zio
[CE]: https://github.com/typelevel/cats-effect
[fs2]: https://github.com/typelevel/fs2
[HdrHistogram]: https://github.com/HdrHistogram/HdrHistogram
[async-profiler]: https://github.com/async-profiler/async-profiler
