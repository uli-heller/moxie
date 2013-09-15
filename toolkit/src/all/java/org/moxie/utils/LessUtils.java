/*
 * Copyright 2012 James Moger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.moxie.utils;
import java.io.File;

import com.asual.lesscss.LessException;

/**
 * Compiles a LESS file into CSS and optionally minifies it.
 * 
 * @author James Moger
 *
 */
public class LessUtils {

	public static String compile(File source, boolean minify) throws LessException {
		// compile less into css
		com.asual.lesscss.LessEngine engine = new com.asual.lesscss.LessEngine();
		String css = engine.compile(source, minify);
		return css;
	}
}
