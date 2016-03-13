/*
 * Copyright (C) 2016 Frank Oltmanns (frank.oltmanns+selma(at)gmail.com)
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

package com.github.federvieh.selma;

/**
 * List the different modes in which the lesson's texts can be shown to the user.
 * ORIGINAL_TEXT: Text in the target language.
 * TRANSLATION: Translation into the user's native language (must be provided by user).
 * LITERAL: Literal translation of the ORIGINAL_TEXT into the user's native language (must be provided by the user)
 * ORIGINAL_TRANSLATION: Shows both the ORIGINAL_TEXT and the TRANSLATION side by side.
 * ORIGINAL_LITERAL: Shows bot the ORIGINAL_TEXT and the LITERAL translation side by side.
 */
public enum DisplayMode {
    ORIGINAL_TEXT,
    TRANSLATION,
    LITERAL,
    ORIGINAL_TRANSLATION,
    ORIGINAL_LITERAL
}
