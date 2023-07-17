# Scala web servers benchmark

Current benchmarks are only for web sockets.

Tested server setups:
 - http4s + blaze (CE 3.5.1, fs2 3.7.0)
 - http4s + ember (CE 3.5.1, fs2 3.7.0)
 - http4s + blaze (CE 3.6-e9aeb8c, fs2 3.8-1af22dd)
 - http4s + ember (CE 3.6-e9aeb8c, fs2 3.8-1af22dd)
 - http4s + blaze, via [tapir] (CE 3.5.1, fs2 3.7.0) 
 - http4s + ember, via [tapir] (CE 3.5.1, fs2 3.7.0)
 - zio-http

[tapir]: https://github.com/softwaremill/tapir