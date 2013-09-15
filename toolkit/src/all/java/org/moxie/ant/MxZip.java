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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.ZipFileSet;
import org.moxie.Build;
import org.moxie.Toolkit.Key;
import org.moxie.utils.StringUtils;


public class MxZip extends Zip {
	
	public MxZip() {
		super();
		setTaskName("mx:zip");
	}
	
	private List<ZipDependencies> dependencies = new ArrayList<ZipDependencies>();
	
	public ZipDependencies createDependencies() {
		ZipDependencies deps = new ZipDependencies();
		dependencies.add(deps);
		return deps;
	}

	private List<ZipArtifact> artifacts = new ArrayList<ZipArtifact>();
	
	public ZipArtifact createArtifact() {
		ZipArtifact artifact = new ZipArtifact();
		artifacts.add(artifact);
		return artifact;
	}

	public void execute() {
		Build build = (Build) getProject().getReference(Key.build.referenceId());
		if (zipFile == null) {
			// default output jar if file unspecified
			String name = build.getPom().artifactId;
			if (!StringUtils.isEmpty(build.getPom().version)) {
				name += "-" + build.getPom().version;
			}
			zipFile = new File(build.getConfig().getTargetDirectory(), name + ".zip");
		}
		
		if (zipFile.getParentFile() != null) {
			zipFile.getParentFile().mkdirs();
		}
		
		for (ZipArtifact artifact : artifacts) {
			ZipFileSet fs = new ZipFileSet();
			fs.setProject(getProject());
			File file = artifact.getFile();
			if (file == null) {
				file = build.getBuildArtifact(artifact.getClassifier());
			}
			fs.setDir(file.getParentFile());
			fs.setIncludes(file.getName());
			if (!StringUtils.isEmpty(artifact.getPrefix())) {
				fs.setPrefix(artifact.getPrefix());
			}
			addZipfileset(fs);
		}
		
		for (ZipDependencies deps : dependencies) {
			for (File jar : build.getSolver().getClasspath(deps.getScope(), deps.getTag())) {
				ZipFileSet fs = new ZipFileSet();
				fs.setProject(getProject());
				if (!StringUtils.isEmpty(deps.getPrefix())) {
					fs.setPrefix(deps.getPrefix());
				}
				fs.setDir(jar.getParentFile());
				fs.setIncludes(jar.getName());
				addZipfileset(fs);
			}
		}
		super.execute();
	}
}
