/***********************************************************************
 * Copyright (c) 2013-2017 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.tools

import com.beust.jcommander.{JCommander, ParameterException}
import org.geotools.data.{DataStore, DataStoreFinder}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConversions._

/**
 * Abstract superclass for all top-level GeoMesa JCommander commands
 */
trait Command {
  val name: String
  def params: Any
  def execute(): Unit
}

object Command {
  // send messages to the user - status, errors, etc
  val user: Logger = LoggerFactory.getLogger("org.locationtech.geomesa.tools.user")
  // send output from a command
  val output: Logger = LoggerFactory.getLogger("org.locationtech.geomesa.tools.output")
}

trait CommandWithSubCommands extends Command {

  def jc: JCommander
  def subCommands: Seq[Command]

  protected def runner: Runner

  override def execute(): Unit = {
    val sub = Option(jc.getCommands.get(name)).map(_.getParsedCommand).orNull
    subCommands.find(_.name == sub) match {
      case Some(command) => command.execute()
      case None =>
        Command.user.error(s"no sub-command listed...run as: ${runner.name} $name <sub-command>")
        Command.user.info(runner.usage(jc, name))
    }
  }
}

trait DataStoreCommand[DS <: DataStore] extends Command {

  def connection: Map[String, String]

  @throws[ParameterException]
  def withDataStore[T](method: (DS) => T): T = {
    val ds = Option(DataStoreFinder.getDataStore(connection).asInstanceOf[DS])
      .getOrElse(throw new ParameterException("Unable to create data store, please check your connection parameters."))
    try { method(ds) } finally {
      ds.dispose()
    }
  }
}
