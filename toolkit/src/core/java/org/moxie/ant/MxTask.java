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
package org.moxie.ant;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import org.apache.tools.ant.Task;
import org.moxie.Build;
import org.moxie.Toolkit;
import org.moxie.Toolkit.Key;
import org.moxie.console.Console;
import org.moxie.utils.FileUtils;
import org.moxie.utils.StringUtils;


public abstract class MxTask extends Task {

	private Console console;

	private Boolean verbose;

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	public boolean isVerbose() {
		if (verbose == null) {
			String mxvb = System.getProperty(Toolkit.MX_VERBOSE);
			if (StringUtils.isEmpty(mxvb)) {
				Build build = getBuild();
				if (build == null) {
					return false;
				} else {
					return build.isVerbose();
				}
			} else {
				verbose = Boolean.parseBoolean(mxvb);
			}
		}
		return verbose;
	}
	
	protected Build getBuild() {
		Build build = (Build) getProject().getReference(Key.build.refId());
		return build;
	}

	protected Console getConsole() {
		if (console == null) {
			Build build = getBuild();
			if (build == null) {
				console = new Console();
			} else {
				console = build.getConsole();
			}
		}
		return console;
	}

	protected void setProperty(Key prop, String value) {
		if (!StringUtils.isEmpty(value)) {
			getProject().setProperty(prop.propId(), value);
			log(prop.propId(), value, false);
		}
	}

	protected void setProperty(String prop, String value) {
		if (!StringUtils.isEmpty(value)) {
			getProject().setProperty(prop, value);
			log(prop, value, false);
		}
	}

	protected void addReference(Key prop, Object obj, boolean split) {
		getProject().addReference(prop.refId(), obj);
		log(prop.refId(), obj.toString(), split);
	}
	
	protected void log(String key, String value, boolean split) {
		if (isVerbose()) {
			int indent = 26;
			if (split) {
				String [] paths = value.split(File.pathSeparator);
				getConsole().key(StringUtils.leftPad(key, indent, ' '), paths[0]);
				for (int i = 1; i < paths.length; i++) {
					getConsole().key(StringUtils.leftPad("", indent, ' '), paths[i]);	
				}
			} else {
				getConsole().key(StringUtils.leftPad(key, indent, ' '), value);
			}
		}
	}
	
	protected void extractResource(File outputFolder, String resource) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			InputStream is = getClass().getResourceAsStream("/" + resource);
			
			byte [] buffer = new byte[32767];
			int len = 0;
			while ((len = is.read(buffer)) > -1) {
				os.write(buffer, 0, len);
			}			
		} catch (Exception e) {
			getConsole().error(e, "Can't extract {0}!", resource);
		}
		File file = new File(outputFolder, resource);
		file.getParentFile().mkdirs();
		FileUtils.writeContent(file, os.toByteArray());
	}
}
