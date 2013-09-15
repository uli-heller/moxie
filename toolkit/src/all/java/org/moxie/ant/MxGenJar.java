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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.Manifest.Attribute;
import org.apache.tools.ant.taskdefs.ManifestException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Path.PathElement;
import org.apache.tools.ant.types.resources.FileResource;
import org.moxie.Build;
import org.moxie.MoxieException;
import org.moxie.MxLauncher;
import org.moxie.Scope;
import org.moxie.Toolkit;
import org.moxie.Toolkit.Key;
import org.moxie.console.Console;
import org.moxie.maxml.MaxmlMap;
import org.moxie.utils.FileUtils;
import org.moxie.utils.StringUtils;


public class MxGenJar extends GenJar {

	Build build;
	Console console;
	
	LauncherSpec launcher;
	ClassSpec mainclass;
	boolean classResolution;
	boolean fatjar;
	boolean includeResources;
	boolean excludePomFiles;
	String includes;
	boolean packageSources;
	String resourceFolderPrefix;
	String tag;

	String classifier;
	private boolean configured;
	Boolean showtitle;
	
	public MxGenJar() {
		super();
		setTaskName("mx:genjar");
	}
	
	/**
	 * Builds a <mainclass> element.
	 * 
	 * @return A <mainclass> element.
	 */
	public ClassSpec createMainclass() {
		if (mainclass == null) {
			ClassSpec cs = new ClassSpec(getProject());
			mainclass = cs;
			jarSpecs.add(cs);
			return cs;
		}
		throw new MoxieException("Can only specify one main class");
	}
	
	/**
	 * Builds a <launcher> element.
	 * 
	 * @return A <launcher> element.
	 */
	public LauncherSpec createLauncher() {
		if (launcher == null) {
			LauncherSpec cs = new LauncherSpec(getProject());
			launcher = cs;
			jarSpecs.add(cs);
			return cs;
		}
		throw new MoxieException("Can only specify one launcher class");
	}
	
	public boolean getFatjar() {
		return fatjar;
	}

	public void setFatjar(boolean value) {
		this.fatjar = value;
	}
	
	public boolean getExcludeclasspathjars() {
		return excludeClasspathJars;
	}

	public void setExcludeclasspathjars(boolean value) {
		this.excludeClasspathJars = value;
	}
	
	public boolean getExcludepomfiles() {
		return excludePomFiles;
	}

	public void setExcludepomfiles(boolean value) {
		this.excludePomFiles = value;
	}

	public boolean getIncludesresources() {
		return includeResources;
	}
	
	public void setIncluderesources(boolean copy) {
		this.includeResources = copy;
	}
	
	public String getIncludes() {
		return includes;
	}

	public void setIncludes(String includes) {
		this.includes = includes;
	}
	
	public void setResourceFolderPrefix(String resourceFolderPrefix) {
		this.resourceFolderPrefix = resourceFolderPrefix;
	}
	
	public String getExcludes() {
		return excludes;
	}

	public void setExcludes(String excludes) {
		this.excludes = excludes;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getClassifier() {
		return classifier;
	}
	
	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}
	
	public boolean getPackagesources() {
		return packageSources;
	}
	
	public void setPackagesources(boolean sources) {
		this.packageSources = sources;
	}

	public void setShowtitle(boolean value) {
		this.showtitle = value;
	}
	
	public boolean isShowTitle() {
		return showtitle == null || showtitle;
	}

	
	@Override
	public void setProject(Project project) {
		super.setProject(project);
		Build build = (Build) getProject().getReference(Key.build.referenceId());
		if (build != null) {
			configure(build);
		}
	}

	private void configure(Build build) {
		configured = true;
		MaxmlMap attributes = build.getConfig().getTaskAttributes(getTaskName());
		if (attributes == null) {
			build.getConsole().error(getTaskName() + " attributes are null!");
			return;
		}
		
		AttributeReflector.setAttributes(getProject(), this, attributes);
	}

	@Override
	public void execute() throws BuildException {
		build = (Build) getProject().getReference(Key.build.referenceId());
		console = build.getConsole();
		
		if (!configured) {
			// called from moxie.package
			configure(build);
		}
		
		if (fatjar && excludeClasspathJars) {
			throw new BuildException("Can not specify fatjar and excludeClasspathJars!");
		}
		
		// automatic manifest entries from Moxie metadata
		configureManifest(mft);

		if (mainclass == null) {
			String mc = build.getConfig().getProjectConfig().getMainclass();
			if (!StringUtils.isEmpty(mc)) {
				ClassSpec cs = new ClassSpec(getProject());
				mainclass = cs;
				mainclass.setName(mc);
				jarSpecs.add(cs);
			}
		}
		
		if (mainclass != null) {
			String mc = mainclass.getName().replace('/', '.');
			if (mc.endsWith(".class")) {
				mc = mc.substring(0, mc.length() - ".class".length());
			}
			if (launcher == null) {
				// use specified mainclass
				setManifest(mft, "Main-Class", mc);
			} else {
				// inject Moxie Launcher class
				String mx = launcher.getName().replace('/', '.');
				if (mx.endsWith(".class")) {
					mx = mx.substring(0, mx.length() - ".class".length());
				}
				setManifest(mft, "Main-Class", mx);
				setManifest(mft, "mxMain-Class", mc);
				String paths = launcher.getPaths();
				if (!StringUtils.isEmpty(paths)) {
					setManifest(mft, "mxMain-Paths", paths);	
				}
			}
		}

		// automatic classpath resolution, if not manually specified
		if (classpath == null) {
			Path cp = buildClasspath(build, Scope.compile, tag);
			if (fatjar) {
				// FatJar generation
				classpath = createClasspath();					
				for (String path : cp.list()) {
					if (path.toLowerCase().endsWith(".jar")) {
						LibrarySpec lib = createLibrary();
						lib.setJar(path);
					} else {
						PathElement element = classpath.createPathElement();
						element.setPath(path);
					}
				}
			} else {
				// standard GenJar class dependency resolution
				classpath = cp;
			}
		}
		
		if (destFile == null) {
			setDestfile(build.getBuildArtifact(classifier));
		}
		
		if (destFile.getParentFile() != null) {
			destFile.getParentFile().mkdirs();
		}
		
		version = build.getPom().version;
		
		File outputFolder = build.getConfig().getOutputDirectory(Scope.compile);

		if (excludes == null) {
			excludes = Toolkit.DEFAULT_RESOURCE_EXCLUDES;
		}

		// include resources from the project source folders
		Resource resources = createResource();
		if (!StringUtils.isEmpty(resourceFolderPrefix)) {
			resources.setPrefix(resourceFolderPrefix);
		}
		for (File dir : build.getConfig().getSourceDirectories(Scope.compile, tag)) {
			FileSet res = resources.createFileset();
			res.setDir(dir);
			res.setExcludes(excludes);
		}
		
		if (includeResources) {
			// include resources from the project resource folders
			for (File dir : build.getConfig().getResourceDirectories(Scope.compile, tag)) {
				FileSet res = resources.createFileset();
				res.setExcludes(excludes);
				res.setDir(dir);
			}

			for (Build module : build.getSolver().getLinkedModules()) {
				// include resources from module source folders
				File dir = module.getConfig().getOutputDirectory(Scope.compile);
				FileSet res = resources.createFileset();
				res.setDir(dir);
				res.setExcludes(excludes);
			
				// include resources from the module resource folders
				for (File resDir : module.getConfig().getResourceDirectories(Scope.compile)) {
					FileSet resSet = resources.createFileset();
					res.setExcludes(Toolkit.DEFAULT_RESOURCE_EXCLUDES);
					resSet.setDir(resDir);
				}
			}
		}

		if (isShowTitle()) {
			console.title(getClass(), destFile.getName());
		}
		
		console.debug(getTaskName() + " configuration");

		// display specified mxgenjar attributes
		MaxmlMap attributes = build.getConfig().getTaskAttributes(getTaskName());
		AttributeReflector.logAttributes(this, attributes, console);

		// optionally inject MxLauncher utility
		if (launcher != null) {
			if (launcher.getName().equals(MxLauncher.class.getName().replace('.', '/') + ".class")) {
				// inject MxLauncher into the output folder of the project
				for (String cn : Arrays.asList(MxLauncher.class.getName(), MxLauncher.class.getName() + "$1")) {
					try {
						String fn = cn.replace('.', '/') + ".class";
						InputStream is = MxLauncher.class.getResourceAsStream("/" + fn);
						if (is == null) {
							continue;
						}
						build.getConsole().log("Injecting {0} into output folder", cn);
						File file = new File(outputFolder, fn.replace('/', File.separatorChar));
						if (file.exists()) {
							file.delete();
						}
						file.getParentFile().mkdirs();
						FileOutputStream os = new FileOutputStream(file, false);
						byte [] buffer = new byte[4096];
						int len = 0;
						while ((len = is.read(buffer)) > 0) {
							os.write(buffer,  0,  len);
						}
						is.close();
						os.flush();
						os.close();
						
						// add these files to the jarSpecs
						ClassSpec cs = new ClassSpec(getProject());
						cs.setName(cn);
						jarSpecs.add(cs);
					} catch (Exception e) {
						build.getConsole().error(e, "Failed to inject {0} into {1}",
								launcher.getName(), outputFolder);
					}
				}
			}
		}
		
		long start = System.currentTimeMillis();
		try {
			super.execute();
		} catch (ResolutionFailedException e) {
			String msg;
			if (tag == null) {
				String template = "Unable to resolve: {0}\n\n{1} could not be located on the classpath.\n";
				msg = MessageFormat.format(template, e.resolvingclass, e.missingclass, tag == null ? "classpath" : ("\"" + tag + "\" classpath"), tag);
			} else {
				String template = "Unable to resolve: {0}\n\n{1} could not be located on the \"{2}\" classpath.\nPlease add the \":{2}\" tag to the appropriate dependency in your Moxie descriptor file.\n";
				msg = MessageFormat.format(template, e.resolvingclass, e.missingclass, tag);
			}
			throw new MoxieException(msg);
		}

		if (fatjar) {
			// try to merge duplicate META-INF/services files
			JarUtils.mergeMetaInfServices(console, destFile);
		}

		console.log(1, destFile.getAbsolutePath());
		console.log(1, "{0} KB, generated in {1} ms", (destFile.length()/1024), System.currentTimeMillis() - start);
		
		/*
		 * Build sources jar
		 */
		if (packageSources) {
			String name = destFile.getName();
			if (!StringUtils.isEmpty(classifier)) {
				// replace the classifier with "sources"
				name = name.replace(classifier, "sources");
			} else {
				// append -sources to the filename before the extension
				name = name.substring(0, name.lastIndexOf('.')) + "-sources" + name.substring(name.lastIndexOf('.'));
			}
			File sourcesFile = new File(destFile.getParentFile(), name);
			if (sourcesFile.exists()) {
				sourcesFile.delete();
			}
			
			Jar jar = new Jar();
			jar.setTaskName(getTaskName());
			jar.setProject(getProject());
			
			// set the destination file
			jar.setDestFile(sourcesFile);
			
			// use the resolved classes to determine included source files
			List<FileResource> sourceFiles = new ArrayList<FileResource>();
			Map<File, Set<String>> packageResources = new HashMap<File, Set<String>>();
			
			if (resolvedLocal.size() == 0) {
				console.warn(getTaskName() + " has not resolved any class files local to {0}", build.getPom().getManagementId());
			}
			
			List<File> folders = build.getConfig().getSourceDirectories(Scope.compile, tag);
			for (String className : resolvedLocal) {
				String sourceName = className.substring(0, className.length() - ".class".length()).replace('.', '/') + ".java";
				console.debug(sourceName);
				for (File folder : folders) {
					File file = new File(folder, sourceName);
					if (file.exists()) {
						FileResource resource = new FileResource(getProject(), file);
						resource.setBaseDir(folder);
						sourceFiles.add(resource);
						if (!packageResources.containsKey(folder)) {
							// always include default package resources
							packageResources.put(folder, new TreeSet<String>(Arrays.asList( "/*" )));						
						}
						String packagePath = FileUtils.getRelativePath(folder, file.getParentFile());
						packageResources.get(folder).add(packagePath + "/*");
						console.debug(1, file.getAbsolutePath());
						break;
					}
				}
			}
			
			// add the discovered source files for the resolved classes
			jar.add(new FileResourceSet(sourceFiles));
			
			// add the resolved package folders for resource files
			for (Map.Entry<File, Set<String>> entry : packageResources.entrySet()) {
				FileSet res = new FileSet();				
				res.setDir(entry.getKey());
				res.setExcludes(excludes);
				StringBuilder includes = new StringBuilder();
				for (String packageName : entry.getValue()) {
					includes.append(packageName + ",");
				}
				includes.setLength(includes.length() - 1);
				res.setIncludes(includes.toString());
				console.debug("adding resource fileset {0}", entry.getKey());
				console.debug(1, "includes={0}", includes.toString());
				jar.add(res);
			}

			if (includeResources) {
				for (File dir : build.getConfig().getResourceDirectories(Scope.compile, tag)) {
					FileSet res = resources.createFileset();
					res.setDir(dir);
					res.setExcludes(Toolkit.DEFAULT_RESOURCE_EXCLUDES);
					jar.add(res);
				}
			}

			// set the source jar manifest
			try {
				Manifest mft = new Manifest();
				configureManifest(mft);
				jar.addConfiguredManifest(mft);
			} catch (ManifestException e) {
				console.error(e);
			}
			
			start = System.currentTimeMillis();			
			jar.execute();
						
			console.log(1, sourcesFile.getAbsolutePath());
			console.log(1, "{0} KB, generated in {1} ms", (sourcesFile.length()/1024), System.currentTimeMillis() - start);
		}
	}
	
	void configureManifest(Manifest manifest) {
		// set manifest entries from Moxie metadata
		Manifest mft = new Manifest();
		setManifest(mft, "Created-By", "Moxie v" + Toolkit.getVersion());
		setManifest(mft, "Build-Jdk", System.getProperty("java.version"));
		setManifest(mft, "Build-Date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));

		setManifest(mft, "Implementation-Title", Key.name);
		setManifest(mft, "Implementation-Vendor", Key.organization);
		setManifest(mft, "Implementation-Vendor-Id", Key.groupId);
		setManifest(mft, "Implementation-Vendor-URL", Key.url);
		setManifest(mft, "Implementation-Version", Key.version);

		setManifest(mft, "Bundle-Name", Key.name);
		setManifest(mft, "Bundle-SymbolicName", Key.artifactId);
		setManifest(mft, "Bundle-Version", Key.version);
		setManifest(mft, "Bundle-Vendor", Key.organization);
		
		setManifest(mft, "Maven-Url", Key.mavenUrl);
		setManifest(mft, "Commit-Id", Key.commitId);
		
		try {
			manifest.merge(mft, true);
		} catch (ManifestException e) {
			console.error(e, "Failed to configure manifest!");
		}
	}

	void setManifest(Manifest man, String key, Key prop) {
		// try project property
		String value = getProject().getProperty(prop.projectId());
		if (value == null) {
			return;
		}
		if (!StringUtils.isEmpty(value)) {
			setManifest(man, key, value);
		}
	}
	
	void setManifest(Manifest man, String key, String value) {
		if (!StringUtils.isEmpty(value)) {
			try {
				man.addConfiguredAttribute(new Attribute(key, value));
			} catch (ManifestException e) {
				console.error(e, "Failed to set manifest attribute \"{0}\"!", key);
			}
		}
	}
	
	/**
	 * Add the Maven META-INF files. 
	 */
	@Override
	protected void writeJarEntries(JarOutputStream jos) {
		if (excludePomFiles) {
			return;
		}
		
		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(jos));
		
		Properties properties = new Properties();
		properties.put(Key.groupId.name(), build.getPom().groupId);
		properties.put(Key.artifactId.name(), build.getPom().artifactId);
		properties.put(Key.version.name(), version);
		
		try {
			ZipEntry entry = new ZipEntry(MessageFormat.format("META-INF/maven/{0}/{1}/pom.properties", build.getPom().groupId, build.getPom().artifactId));
			jos.putNextEntry(entry);		
			properties.store(dos, "Generated by Moxie");
			dos.flush();
			jos.closeEntry();
		} catch (IOException e) {
			console.error(e, "failed to write pom.properties!");
		}
		
		try {
			ZipEntry entry = new ZipEntry(MessageFormat.format("META-INF/maven/{0}/{1}/pom.xml", build.getPom().groupId, build.getPom().artifactId));
			jos.putNextEntry(entry);
			dos.write(build.getPom().toXML(false).getBytes("UTF-8"));
			dos.flush();
			jos.closeEntry();
		} catch (IOException e) {
			console.error(e, "failed to write pom.xml!");
		}
	}

	private Path buildClasspath(Build build, Scope scope, String tag) {
		List<File> jars = build.getSolver().getClasspath(scope, tag);
		Path cp = new Path(getProject());
		// output folder
		PathElement of = cp.createPathElement();
		of.setLocation(build.getConfig().getOutputDirectory(scope));
		if (!scope.isDefault()) {
			of.setLocation(build.getConfig().getOutputDirectory(Scope.compile));
		}
		
		// add project dependencies 
		for (File folder : buildDependentProjectsClasspath(build)) {
			PathElement element = cp.createPathElement();
			element.setLocation(folder);
		}
		
		// jars
		for (File jar : jars) {
			PathElement element = cp.createPathElement();
			element.setLocation(jar);
		}

		return cp;
	}
	
	private List<File> buildDependentProjectsClasspath(Build build) {
		List<File> folders = new ArrayList<File>();
		List<Build> libraryProjects = build.getSolver().getLinkedModules();
		for (Build project : libraryProjects) {
			File outputFolder = project.getConfig().getOutputDirectory(Scope.compile);
			folders.add(outputFolder);
		}		
		return folders;
	}
}
