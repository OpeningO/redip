/*
 * MIT License
 *
 * Copyright (c) 2021 OpeningO Co.,Ltd.
 *
 *    https://openingo.org
 *    contactus(at)openingo.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.openingo.redip.helper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * StringHelper
 *
 * @author Qicz
 * @since 2021/7/9 14:39
 */
public final class StringHelper {

	public static boolean nonBlank(String string) {
		return Objects.nonNull(string) && !"".equals(string.trim());
	}

	public static List<String> filterBlank(Collection<String> strings) {
		if (Objects.isNull(strings)) {
			return Collections.emptyList();
		}
		return strings.stream().filter(StringHelper::nonBlank).map(String::trim).collect(Collectors.toList());
	}

	public static Set<String> filterBlank(Set<String> strings) {
		if (Objects.isNull(strings)) {
			return Collections.emptySet();
		}
		return strings.stream().filter(StringHelper::nonBlank).map(String::trim).collect(Collectors.toSet());
	}
}
