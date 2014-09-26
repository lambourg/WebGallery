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

import java.util.ArrayList;

class SimpleAutoCompletionItems implements CompletionItems {
    private String[] completions;

    public SimpleAutoCompletionItems(String[] items)
    {
      this.completions = items;
    }

    public String[] getCompletionItems(String match) {
      ArrayList<String> matches = new ArrayList<String>();
      for (String completion : this.completions) {
        if (completion.toLowerCase().startsWith(match.toLowerCase())) {
          matches.add(completion);
        }
      }
      String[] returnMatches = new String[matches.size()];
      for(int i = 0; i < matches.size(); i++)
      {
        returnMatches[i] = (String)matches.get(i);
      }
      return returnMatches;
    }
  } 