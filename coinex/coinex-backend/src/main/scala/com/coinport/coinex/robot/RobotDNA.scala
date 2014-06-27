package com.coinport.coinex.robot

import com.coinport.coinex.common.Constants._

case class RobotDNA(dnaId: Long,
  dna: Map[String, Action] = Map.empty[String, Action],
  dnaStringMap: Map[String, String] = Map.empty[String, String])
