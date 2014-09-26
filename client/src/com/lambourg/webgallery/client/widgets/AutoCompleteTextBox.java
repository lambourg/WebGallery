/*
Auto-Completion Textbox for GWT
Copyright (C) 2006 Oliver Albers http://gwt.components.googlepages.com/

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA

*/
package com.lambourg.webgallery.client.widgets;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;



class AutoCompleteTextBox extends TextBox
    implements KeyUpHandler, ClickHandler {
   
  protected PopupPanel choicesPopup = new PopupPanel(true);
  protected ListBox choices = new ListBox();
  protected CompletionItems items;
  protected boolean popupAdded = false;
  protected boolean visible = false;
   
  /**
   * Default Constructor
   *
   */
  public AutoCompleteTextBox(CompletionItems items)
  {
    super();
    this.items = items;
    
    this.addKeyUpHandler(this);
    choices.addClickHandler(this);
    this.setStyleName("AutoCompleteTextBox");

    choicesPopup.add(choices);
    choicesPopup.addStyleName("AutoCompleteChoices");
       
    choices.setStyleName("list");
  }

  /**
   * Sets an "algorithm" returning completion items
   * You can define your own way how the textbox retrieves autocompletion items
   * by implementing the CompletionItems interface and setting the according object
   * @see SimpleAutoCompletionItem
   * @param items CompletionItem implementation
   */
  public void setCompletionItems(CompletionItems items)
  {
    this.items = items;
  }
   
  /**
   * Returns the used CompletionItems object
   * @return CompletionItems implementation
   */
  public CompletionItems getCompletionItems()
  {
    return this.items;
  }
   
  /**
   * A key was released, start autocompletion
   */
  @Override
  public void onKeyUp(KeyUpEvent event) {
    if(event.getNativeKeyCode() == KeyCodes.KEY_DOWN)
    {
      int selectedIndex = choices.getSelectedIndex();
      selectedIndex++;
      if(selectedIndex > choices.getItemCount())
      {
        selectedIndex = 0;
      }
      choices.setSelectedIndex(selectedIndex);
           
      return;
    }
       
    if(event.getNativeKeyCode() == KeyCodes.KEY_UP)
    {
      int selectedIndex = choices.getSelectedIndex();
      selectedIndex--;
      if(selectedIndex < 0)
      {
        selectedIndex = choices.getItemCount();
      }
      choices.setSelectedIndex(selectedIndex);
           
      return;        
    }
       
    if(event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
    {
      if(visible)
      {
        complete();
      }
           
      return;
    }
       
    if(event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE)
    {
      choices.clear();
      choicesPopup.hide();
      visible = false;
           
      return;
    }
       
    String text = this.getText();
    String[] matches = new String[]{};
    if(text.length() > 0)
    {
      matches = items.getCompletionItems(text);
    }
       
    if(matches.length > 0)
    {
      choices.clear();
           
      for(int i = 0; i < matches.length; i++)
      {
        choices.addItem((String) matches[i]);
      }
           
      // if there is only one match and it is what is in the
      // text field anyways there is no need to show autocompletion
      if(matches.length == 1 && matches[0].compareTo(text) == 0)
      {
        choicesPopup.hide();
      } else {
        choices.setSelectedIndex(0);
        choices.setVisibleItemCount(matches.length + 1);
               
        if(!popupAdded)
        {
          RootPanel.get().add(choicesPopup);
          popupAdded = true;
        }
        choicesPopup.show();
        visible = true;
        choicesPopup.setPopupPosition(this.getAbsoluteLeft(),
        this.getAbsoluteTop() + this.getOffsetHeight());
        //choicesPopup.setWidth(this.getOffsetWidth() + "px");
        choices.setWidth(this.getOffsetWidth() + "px");
      }

    } else {
      visible = false;
      choicesPopup.hide();
    }
  }

  /**
   * A mouseclick in the list of items
   */
  public void onChange(Widget arg0) {
    complete();
  }
 
  @Override
  public void onClick(ClickEvent event) {
      complete();
  }
   
  // add selected item to textbox
  protected void complete()
  {
    if(choices.getItemCount() > 0)
    {
      this.setText(choices.getItemText(choices.getSelectedIndex()));
    }
       
    choices.clear();
    choicesPopup.hide();
  }

}
 
