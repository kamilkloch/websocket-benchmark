import com.aitusoftware.babl.config.PropertiesLoader
import com.aitusoftware.babl.user.{Application, ContentType}
import com.aitusoftware.babl.websocket.{BablServer, DisconnectReason, SendResult, Session}
import org.agrona.DirectBuffer
import org.agrona.concurrent.{Agent, ShutdownSignalBarrier, UnsafeBuffer}

import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

object Babl extends App {
  private[this] val maxConnections = 50000
  private[this] val sessionByIdMap = new ConcurrentHashMap[Long, Session](maxConnections * 2, 0.5f)
  private[this] val config = PropertiesLoader.configure(Paths.get("babl-performance.properties"))
  config.applicationConfig.application(new Application {
    def onSessionConnected(session: Session): Int = {
      sessionByIdMap.put(session.id, session)
      SendResult.OK
    }

    override def onSessionDisconnected(session: Session, reason: DisconnectReason): Int = {
      sessionByIdMap.remove(session.id)
      SendResult.OK
    }

    override def onSessionMessage(session: Session, contentType: ContentType, msg: DirectBuffer, offset: Int, length: Int): Int =
      SendResult.OK
  })
  config.applicationConfig.additionalWork(new Agent {
    var lastTs: Long = System.currentTimeMillis()
    val sessions = new Array[Session](maxConnections)

    override val roleName: String = "Timestamp broadcast"

    override def doWork(): Int = {
      val ts = System.currentTimeMillis()
      if (ts >= lastTs + 500) {
        sessionByIdMap.values().toArray(sessions)
        val buffer = new UnsafeBuffer(ts.toString.getBytes)
        var s: Session = null
        var i, j = 0
        while ( {
          while ( {
            s = sessions(i)
            s == null
          } && {
            i += 1
            if (i == maxConnections) {
              i = 0
            }
            i != j
          }) ()
          s != null && {
            if (s.send(ContentType.TEXT, buffer, 0, buffer.capacity) != SendResult.BACK_PRESSURE) {
              sessions(i) = null
              j = i
            }
            true
          }
        }) ()
        lastTs = ts
      }
      SendResult.OK
    }
  })
  val containers = BablServer.launch(config)
  try {
    containers.start()
    new ShutdownSignalBarrier().await()
  } finally {
    containers.close()
  }
}
