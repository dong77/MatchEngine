package com.coinport.coinex.users

import akka.event.LoggingReceive
import com.coinport.coinex.common.ExtendedView
import akka.persistence.Persistent
import com.coinport.coinex.data._
import com.mongodb.casbah.MongoDB
import com.coinport.coinex.common.mongo.SimpleJsonMongoCollection
import com.coinport.coinex.common.PersistentId._
import Implicits._

// This view persists user manager state into MongoDB but also keeps an inmemory copy of the state.
// THis view shall not serve any queries.
class UserWriter(db: MongoDB, secret: String) extends ExtendedView {
  override val processorId = USER_PROCESSOR <<
  override val viewId = USER_WRITER_VIEW <<

  val totpAuthenticator = new GoogleAuthenticator
  val manager = new UserManager(totpAuthenticator, secret)

  def receive = LoggingReceive {
    case Persistent(m, seq) => updateState(m)
  }

  def updateState(event: Any) = event match {
    case m: DoRegisterUser => profiles.put(manager.registerUser(m.userProfile))
    case DoUpdateUserProfile(profile) => profiles.put(manager.updateUser(profile))
    case DoRequestPasswordReset(email, token) => profiles.put(manager.requestPasswordReset(email, token.get))
    case DoResetPassword(password, token) => profiles.put(manager.resetPassword(password, token))
    case VerifyEmail(token) => profiles.put(manager.verifyEmail(token))
  }

  val profiles = new SimpleJsonMongoCollection[UserProfile, UserProfile.Immutable] {
    val coll = db("user_profiles")
    def extractId(profile: UserProfile) = profile.id
  }
}