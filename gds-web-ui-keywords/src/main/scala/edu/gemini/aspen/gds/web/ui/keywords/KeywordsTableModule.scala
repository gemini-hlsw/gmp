package edu.gemini.aspen.gds.web.ui.keywords

import edu.gemini.aspen.gds.web.ui.api.GDSWebModule
import edu.gemini.aspen.gds.api.configuration.GDSConfigurationService
import edu.gemini.aspen.gds.web.ui.api.Preamble._
import model.{WritableGDSKeywordsDataSource, ReadOnlyGDSKeywordsDataSource}
import scala.collection.JavaConversions._
import com.vaadin.ui.Table.ColumnGenerator
import com.vaadin.terminal.ThemeResource
import com.vaadin.ui._
import themes.BaseTheme
import com.vaadin.Application
import org.vaadin.dialogs.ConfirmDialog
import com.vaadin.ui.Window.Notification

/**
 * Module for the table to edit the keywords
 */
class KeywordsTableModule(configService: GDSConfigurationService) extends GDSWebModule {
  val title = "Keyword Configuration"
  val order = 0
  lazy val dataSource = new WritableGDSKeywordsDataSource(configService.getFullConfiguration)

  val tabLayout = new VerticalLayout
  val table = new Table("Keywords")

  var deleteIcon = new ThemeResource("../runo/icons/16/document-delete.png")
  var deleteTooltip = "Delete row"
  val deleteProperty = "DEL"

  val saveButton = new Button("Save..")
  val newRowButton = new Button("New row...")

  def visibleColumns(user: AnyRef): Array[AnyRef] = {
    val prop = Option(user) map {
      _ => deleteProperty
    } toList
    val cols = getDataSource(user).propertyIds
    val p = cols ++ prop
    (p toArray).asInstanceOf[Array[AnyRef]]
  }

  private def getDataSource(user: AnyRef) = Option(user) map {
    _ => dataSource
  } getOrElse {
    new ReadOnlyGDSKeywordsDataSource(configService.getFullConfiguration)
  }

  private def updateTableHeaders(user: AnyRef) = {
    val ds = getDataSource(user)
    ds.propertyIds foreach {
      p => table.setColumnHeader(p, ds.propertyHeader(p))
    }
  }

  override def userChanged(user: AnyRef) = {
    table.setContainerDataSource(getDataSource(user))
    updateTableHeaders(user)
    // Update user dependant parts
    updateSaveButton(user)
    updateNewButton(user)
    table.setVisibleColumns(visibleColumns(user))
    table.requestRepaintAll()
    tabLayout.replaceComponent(table, table)
  }

  private def updateNewButton(user: AnyRef) {
    newRowButton.setVisible(Option(user).isDefined)
  }

  private def updateSaveButton(user: AnyRef) {
    saveButton.setVisible(Option(user).isDefined)
  }

  override def buildTabContent(app: Application) = {
    table.setContainerDataSource(getDataSource(app.getUser))
    table.setNullSelectionAllowed(false)
    table.setImmediate(true)
    table.addStyleName("keywords-table")
    table.setSizeFull()
    table.setPageLength(25)
    table.setCacheRate(0.1)
    table.setColumnCollapsingAllowed(true)
    table.setColumnReorderingAllowed(true)

    setupDeleteColumn(table)
    setupNewButton(table)

    tabLayout.addComponent(table)
    tabLayout.setExpandRatio(table, 1.0f)

    tabLayout.addComponent(statusRow(app))
    updateSaveButton(app.getUser)
    updateNewButton(app.getUser)
    table.setVisibleColumns(visibleColumns(app.getUser))
    dataSource.propertyIds map {
      c => table.setColumnWidth(c, dataSource.propertyWidth(c))
    }

    tabLayout.setSizeFull
    tabLayout
  }

  def statusRow(app: Application) = {
    val layout = new HorizontalLayout
    layout.addStyleName("keywords-control")
    layout.setMargin(false)
    layout.setWidth("100%")
    layout.addStyleName("keywords-control")
    val label = new Label("Keywords count: " + table.getContainerDataSource.size)

    layout.addComponent(label)
    layout.setComponentAlignment(label, Alignment.MIDDLE_LEFT)
    layout.setExpandRatio(label, 1.0f)

    val saveButton = buildSaveButton(app)
    layout.addComponent(newRowButton)
    layout.addComponent(saveButton)
    layout.setComponentAlignment(newRowButton, Alignment.MIDDLE_RIGHT)
    layout.setComponentAlignment(saveButton, Alignment.MIDDLE_RIGHT)

    layout
  }

  def setupDeleteColumn(table: Table) {
    table.addGeneratedColumn(deleteProperty, new ColumnGenerator {
      def generateCell(source: Table, itemId: AnyRef, columnId: AnyRef) = {
        val deleteButton = new Button("")
        deleteButton.setStyleName(BaseTheme.BUTTON_LINK)
        deleteButton.setIcon(deleteIcon)
        deleteButton.setDescription(deleteTooltip)
        deleteButton.addListener((e: Button#ClickEvent) => {
          val message = "Do you want to delete the item " + itemId + "?\n" +
            "Keyword: %s".format(dataSource.getItem(itemId).getItemProperty("FitsKeyword"))
          ConfirmDialog.show(table.getApplication.getMainWindow, "Please Confirm:", message,
            "Yes", "No", new ConfirmDialog.Listener() {

              def onClose(dialog: ConfirmDialog) {
                if (dialog.isConfirmed()) {
                  // Confirmed to continue
                  dataSource.removeItem(itemId)
                }
              }
            })
        })
        deleteButton
      }
    })
    table.setColumnIcon(deleteProperty, deleteIcon)
    table.setColumnHeader(deleteProperty, "")
    table.setColumnAlignment(deleteProperty, Table.ALIGN_CENTER)
    table.setColumnWidth(deleteProperty, 20)
  }

  private def setupNewButton(table: Table) {
    newRowButton.addListener((e: Button#ClickEvent) => {
      table.getApplication.getMainWindow.addWindow(new NewRowWindow(getDataSource(table.getApplication.getUser)))
    })
    newRowButton
  }

  private def buildSaveButton(app: Application): Button = {
    saveButton.addListener((e: Button#ClickEvent) => {
      configService.updateConfiguration(dataSource.toGDSConfiguration)
      app.getMainWindow.showNotification("Saving...", Notification.TYPE_HUMANIZED_MESSAGE)
    })
    saveButton
  }

}