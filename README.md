# Scala websocket benchmark

## Test setup

Client and server run on two separate machines. Both share the same setup: 
 - Intel® Core™ i9-13900K CPU @ 3.0GHz (max 5.8GHz, performance-cores only), 
 - RAM 64GB DDR5-4800,
 - 10Gbit network,
 - Ubuntu 23.10 (Linux 6.6.6), 
 - Oracle JDK 21.0.2 (ZGC generational)

### Server

Server code resides in the `/server` module. Server exposes a single `/ts` websocket endpoint, which emits a timestamp every 100ms.
This targets a scenario in which a websocket channel is used to serve live market updates to the user.  

Tested servers:
 - [http4s] + blaze ([CE] 3.5.4, [fs2] 3.9.4)
 - [http4s] + blaze, via [tapir] fast-path ([CE] 3.5.4, [fs2] 3.9.4, [tapir] 1.6.11) 
 - [http4s] + blaze, via [tapir] slow-path ([CE] 3.5.4, [fs2] 3.9.4, [tapir] 1.6.11) 
 - [http4s] + blaze, via [tapir] old-1 ([CE] 3.5.4, [fs2] 3.9.4, [tapir] 1.6.0) 
 - [http4s] + blaze, via [tapir] old-2 ([CE] 3.5.4, [fs2] 3.9.4, [tapir] 1.6.3)
 
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

## Benchmarks

Benchmark results reside in `/results`. 
```
 results
 ├── http4s-blaze          (CE 3.5.4, fs2 3.9.4)
 ├── tapir-blaze           (CE 3.5.4, fs2 3.9.4, tapir 1.6.11)
 ├── tapir-blaze-fast-path (CE 3.5.4, fs2 3.9.4, tapir 1.6.11)
 ├── tapir-blaze-slow-path (CE 3.5.4, fs2 3.9.4, tapir 1.6.11)
 ├── tapir-blaze-old-1     (CE 3.5.4, fs2 3.9.4, tapir 1.6.0)
 ├── tapir-blaze-old-2     (CE 3.5.4, fs2 3.9.4, tapir 1.6.3)
```

Each folder contains:
  - [HdrHistogram] latency,
  - [Gatling] html report (useful to see variance in the expected 100ms between the updates across time),
  - [async-profiler] flame graphs in 2 flavours: per-thread and aggregated.

![websocket-benchmark-25k](results/websocket-benchmark-25k.png)

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
