/*
 * Copyright 2023 Moedog
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package com.hippo.yorozuya

import org.jsoup.parser.Parser

fun String.cleanAsDirname(): String = this
    .replace(Regex("[^\\p{L}\\p{N}\\p{P}\\p{Z}]"), "")
    .replace(Regex("\\s+"), " ")
    .trim()

fun String.unescapeXml(): String = Parser.unescapeEntities(this, true)

inline infix fun <T> CharSequence.trimAnd(block: CharSequence.() -> T): T = block(trim())
