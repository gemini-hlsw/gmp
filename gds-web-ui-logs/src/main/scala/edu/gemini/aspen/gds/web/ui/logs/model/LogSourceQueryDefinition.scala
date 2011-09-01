package edu.gemini.aspen.gds.web.ui.logs.model

import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition
import edu.gemini.aspen.gds.web.ui.logs.LogSource

/**
 * Placeholder class needed to let LoggingEventBeanQuery access logSource */
class LogSourceQueryDefinition(val logSource: LogSource, compositeItems: Boolean, batchSize: Int) extends LazyQueryDefinition(compositeItems, batchSize)