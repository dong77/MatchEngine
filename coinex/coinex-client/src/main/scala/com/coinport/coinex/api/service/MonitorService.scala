package com.coinport.coinex.api.service

import akka.pattern.ask
import com.coinport.coinex.data.{ QueryActiveActorsResult, QueryActiveActors }
import com.coinport.coinex.api.model.{ ApiResult, ApiActorsInfo, ApiActorsPath }
import scala.concurrent.ExecutionContext.Implicits.global

object MonitorService extends AkkaService {
  def getActorsPath() = {
    backend ? QueryActiveActors() map {
      case rv: QueryActiveActorsResult =>
        val pathSeq = rv.actorPaths.map {
          case (ip, pathSeq) =>
            ApiActorsPath(ip, pathSeq)
        }
        ApiResult(data = Some(ApiActorsInfo(pathSeq.toSeq)))
    }
  }
}
