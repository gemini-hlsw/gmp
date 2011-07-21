package edu.gemini.aspen.gds.web.ui.keywords.model

import com.vaadin.data.Item
import edu.gemini.aspen.gds.api.{Instrument, GDSConfiguration}
import com.vaadin.ui.Label

/**
 * PropertyItemWrapperFactory for Instrument that is read only
 */
class InstrumentPropertyItemWrapperFactory extends PropertyItemWrapperFactory(classOf[Instrument], classOf[String]) {
  override val width = 30

  override def createItemAndWrapper(config: GDSConfiguration) = {
    val label = new Label(config.instrument.name)

    def wrapper(config: GDSConfiguration): GDSConfiguration = {
      config
    }

    (label, wrapper)
  }
}













