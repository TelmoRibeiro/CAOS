package caos.frontend.widgets

import org.scalajs.dom.{Event, document, html}
import caos.frontend.{Configurator, Documentation, Setting}

import scala.annotation.tailrec

abstract class SettingWidget[A](title: String, doc: Documentation, config: Configurator[A]) extends Widget[Setting](title, doc):
  protected var setting: Setting = config.setting

  protected val buttons: List[(Either[String, String], (() => Unit, String))]

  def reload(): Unit

  def partialReload(): Unit

  override def init(div: Block, visible: Boolean): Unit =
    panelBox(div, visible, buttons = buttons)
      .append("div")
      .attr("id", "setting-container")

    update()
  end init

  override def get: Setting = setting

  def set(setting: Setting): Unit = this.setting = setting

  override def update(): Unit =
    val settingContainerDiv = document.getElementById("setting-container").asInstanceOf[html.Div]
    settingContainerDiv.innerHTML = ""

    if setting != Setting() then renderSetting(setting, settingContainerDiv)
  end update

  private def setCheckedUpstream(currentSetting: Setting, value: Boolean = true): Setting = setting.parentOf(currentSetting) match
    case Some(parentSetting) =>
      setCheckedUpstream(parentSetting, value)
      setting = setting.setChecked(parentSetting, value)
      setting
    case None =>
      setting
  end setCheckedUpstream

  private def setCheckedDownstream(currentSetting: Setting, value: Boolean = false): Setting =
    Setting.allFromOrderedInclusive(currentSetting).foreach(child => setting = setting.setChecked(child, value))
    setting
  end setCheckedDownstream

  private def renderSetting(currentSetting: Setting, parentDiv: html.Div, indentationLevel: Int = 0): Unit =
    val currentSuperDiv = document.createElement("div").asInstanceOf[html.Div]
    currentSuperDiv.style.paddingLeft = s"${indentationLevel * 20}px"

    val currentDiv = document.createElement("div").asInstanceOf[html.Div]
    currentDiv.setAttribute("class", "setting-container")
    currentDiv.style.display = "flex"
    // currentDiv.style.alignmentBaseline = "center"
    currentDiv.style.columnGap = "15px"

    val title = document.createElement("h4").asInstanceOf[html.Heading]
    title.textContent = s"${currentSetting.name}"
    title.style.margin = "0"
    title.style.fontFamily = "monospace"
    title.style.fontSize   = "15px"
    val checkbox = document.createElement("input").asInstanceOf[html.Input]
    checkbox.setAttribute("type", "checkbox")
    checkbox.setAttribute("name", currentSetting.name)
    checkbox.checked = currentSetting.checked

    checkbox.onchange = (_: Event) => {
      val isChecked = checkbox.checked

      setting.parentOf(currentSetting) match
        case Some(parentSetting) if parentSetting.options.contains("allowOne") && isChecked =>
          setting = setCheckedUpstream(currentSetting)
          parentSetting.children.foreach(childSetting =>
            if (childSetting != currentSetting) setting = setting.setChecked(childSetting, false)
            setting = setting.setChecked(currentSetting, isChecked)
          )
        case _ if isChecked =>
          setting = setCheckedUpstream(currentSetting).setChecked(currentSetting, isChecked)
        case _ =>
          setting = setCheckedDownstream(currentSetting)

      val settingContainerDiv = document.getElementById("setting-container").asInstanceOf[html.Div]
      settingContainerDiv.innerHTML = ""
      renderSetting(setting, settingContainerDiv)
    }

    currentDiv.appendChild(checkbox)
    currentDiv.appendChild(title)

    if indentationLevel > 0 then currentSuperDiv.appendChild(currentDiv) // avoids rendering the initial root
    parentDiv.appendChild(currentSuperDiv)

    if (currentSetting.children.nonEmpty) {
      val childrenContainerDiv = document.createElement("div").asInstanceOf[html.Div]
      childrenContainerDiv.setAttribute("class", "children-container") // ns
      currentSetting.children.foreach(childSetting => renderSetting(childSetting, childrenContainerDiv, indentationLevel + 1))
      currentSuperDiv.appendChild(childrenContainerDiv)
    }
  end renderSetting
end SettingWidget
