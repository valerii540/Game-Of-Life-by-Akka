package monitoring

import kamon.Kamon
import kamon.metric.Timer

object KamonReporter {
  val mutationLatencyTimer: Timer =
    Kamon.timer(name = "app.mutation.latency", description = "Epoch updating latency").withoutTags()
}
