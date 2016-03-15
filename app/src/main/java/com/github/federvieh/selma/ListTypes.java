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
 * List the different modes in which the lesson's texts can be listed.
 * ALL: All texts (lesson texts and translation exercises).
 * NO_TRANSLATE: Only lesson texts (i.e. without translation exercises).
 * ONLY_TRANSLATE: Only translation exercises (i.e. without lesson texts).
 */
public enum ListTypes {
	ALL,
	NO_TRANSLATE,
	ONLY_TRANSLATE
}
