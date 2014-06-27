package com.coinport.coinex.api.service

import com.coinport.coinex.data._
import com.coinport.coinex.api.model._
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global

object NotificationService extends AkkaService {
  def getNotifications(lang: Language) = {
    backend ? QueryNotification(getRemoved = Some(false), cur = Cursor(0, 10)) map {
      case rv: QueryNotificationResult =>
        ApiResult(data = Some(rv.notifications.map(fromNotification)))
    }
  }

  def adminGetNotifications(id: Option[Long], ntype: Option[NotificationType], getRemoved: Option[Boolean], cursor: Cursor) = {
    backend ? QueryNotification(id, ntype, getRemoved, None, cursor) map {
      case rv: QueryNotificationResult =>
        ApiResult(data = Some(ApiPagingWrapper(cursor.skip, cursor.limit, rv.notifications.map(fromNotification), rv.count.toInt)))
    }
  }

  def updateNotification(n: Notification) = {
    backend ! SetNotification(n)
  }
}
