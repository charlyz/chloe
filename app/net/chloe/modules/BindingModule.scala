package net.chloe.modules

import javax.inject.Singleton
import play.api.Configuration
import play.api.Environment
import com.google.inject.AbstractModule

@Singleton
class BindingModule(
  environment: Environment,
  configuration: Configuration
) extends AbstractModule {

  override def configure(): Unit = {

  }

}