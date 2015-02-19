/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.fred.taskgame.utils;

import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;

import net.fred.taskgame.model.Task;

import java.util.Locale;


public class TextHelper {
    /**
     * @param task
     * @return
     */
    public static Spanned[] parseTitleAndContent(Task task) {

        final int CONTENT_SUBSTRING_LENGTH = 300;
        final int TITLE_SUBSTRING_OF_CONTENT_LIMIT = 50;

        // Defining title and content texts
        String titleText, contentText;

        String content = task.content.trim();

        if (task.title.length() > 0) {
            titleText = task.title;
            contentText = limit(task.content.trim(), 0, CONTENT_SUBSTRING_LENGTH, false, true);
        } else {
            titleText = limit(content, 0, TITLE_SUBSTRING_OF_CONTENT_LIMIT, true, false);
            contentText = limit(content.replace(titleText, "").trim(), 0, CONTENT_SUBSTRING_LENGTH, false, false);
        }

        // Replacing checkmarks symbols with html entities
        Spanned titleSpanned, contentSpanned;
        if (task.isChecklist) {
            titleText = titleText.replace(it.feio.android.checklistview.interfaces.Constants.CHECKED_SYM,
                    it.feio.android.checklistview.interfaces.Constants.CHECKED_ENTITY).replace(
                    it.feio.android.checklistview.interfaces.Constants.UNCHECKED_SYM,
                    it.feio.android.checklistview.interfaces.Constants.UNCHECKED_ENTITY);
            titleSpanned = Html.fromHtml(titleText);
            contentText = contentText
                    .replace(it.feio.android.checklistview.interfaces.Constants.CHECKED_SYM,
                            it.feio.android.checklistview.interfaces.Constants.CHECKED_ENTITY)
                    .replace(it.feio.android.checklistview.interfaces.Constants.UNCHECKED_SYM,
                            it.feio.android.checklistview.interfaces.Constants.UNCHECKED_ENTITY)
                    .replace(System.getProperty("line.separator"), "<br/>");
            contentSpanned = Html.fromHtml(contentText);
        } else {
            titleSpanned = new SpannedString(titleText);
            contentSpanned = new SpannedString(contentText);
        }

        return new Spanned[]{titleSpanned, contentSpanned};
    }


    public static String limit(String value, int start, int length, boolean singleLine, boolean elipsize) {
        if (start > value.length()) {
            return null;
        }
        StringBuilder buf = new StringBuilder(value.substring(start));
        int indexNewLine = buf.indexOf(System.getProperty("line.separator"));
        int endIndex = singleLine && indexNewLine < length ? indexNewLine : length < buf.length() ? length : -1;
        if (endIndex != -1) {
            buf.setLength(endIndex);
            if (elipsize) {
                buf.append("...");
            }
        }
        return buf.toString();
    }


    public static String capitalize(String string) {
        return string.substring(0, 1).toUpperCase(Locale.getDefault()) + string.substring(1, string.length()).toLowerCase(Locale.getDefault());
    }

}
