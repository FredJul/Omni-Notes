/*
 Copyright 2014 Federico Iosue (federico.iosue@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this

file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under

the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 

ANY KIND, either express or implied. See the License for the specific language governing

permissions and limitations under the License.
 */
package it.feio.android.pixlui.links;

import android.text.style.ClickableSpan;
import android.view.View;

//This is class which gives us the clicks on the links which we then can
// use.

public class InternalURLSpan extends ClickableSpan {
    private String clickedSpan;
    private TextLinkClickListener mTextLinkClickListener;

    public InternalURLSpan(String clickedString) {
        clickedSpan = clickedString;
    }

    public void setTextLinkClickListener(TextLinkClickListener mTextLinkClickListener) {
        this.mTextLinkClickListener = mTextLinkClickListener;
    }

    @Override
    public void onClick(View view) {
        mTextLinkClickListener.onTextLinkClick(view, clickedSpan, UrlCompleter.complete(clickedSpan));
    }
}
