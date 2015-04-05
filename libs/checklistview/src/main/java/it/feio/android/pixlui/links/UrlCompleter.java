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

import java.util.Locale;


public class UrlCompleter {

    public static final String HASHTAG_SCHEME = "hashtag:";

    public static String complete(String s) {

        if (RegexPatternsConstants.EMAIL.matcher(s).matches()) {
            s = "mailto:" + s;
        } else if (RegexPatternsConstants.HYPER_LINK.matcher(s).matches() && !s.toLowerCase(Locale.getDefault()).startsWith("http")) {
            s = "http://" + s;
        } else if (RegexPatternsConstants.PHONE.matcher(s).matches()) {
            s = "tel:" + s;
        } else if (RegexPatternsConstants.HASH_TAG.matcher(s).matches()) {
            s = HASHTAG_SCHEME + s;
        }

        return s;
    }
}
