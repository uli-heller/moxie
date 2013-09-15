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
package org.moxie;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.moxie.utils.StringUtils;


/**
 * Dependency represents a retrievable artifact.
 */
public class Dependency implements Serializable {

	private static final long serialVersionUID = 1L;

	public String groupId;
	public String artifactId;
	public String version;
	public String revision;
	public String type;
	public String extension;
	public String classifier;
	public boolean optional;	
	public boolean resolveDependencies;
	public Set<String> exclusions;
	public Set<String> tags;

	public int ring;
	public String origin;
    public Scope definedScope;
    
	public Dependency() {
		type = "jar";
		extension = type;
		resolveDependencies = true;
		exclusions = new TreeSet<String>();
		tags = new TreeSet<String>();
	}
	
	public Dependency(String def) {
		String [] principals = def.trim().split(" ");
		
		String coordinates = StringUtils.stripQuotes(principals[0]);
		if (coordinates.indexOf('@') > -1) {
			// strip @ext
			extension = coordinates.substring(coordinates.indexOf('@') + 1);
			type = extension;
			coordinates = coordinates.substring(0, coordinates.indexOf('@'));
			resolveDependencies = false;
		} else {
			extension = "jar";
			type = extension;
			resolveDependencies = true;
		}

		// determine Maven artifact coordinates
		String [] fields = { groupId, artifactId, version, classifier, extension };
		
		// append trailing colon for custom splitting algorithm
		coordinates = coordinates + ":";
		
		// custom string split for performance, blanks are considered null
		StringBuilder sb = new StringBuilder();
		int field = 0;
		for (int i = 0, len = coordinates.length(); i < len; i++) {
			char c = coordinates.charAt(i);
			switch(c) {
			case ' ':
				break;
			case ':':
				fields[field] = sb.toString().trim();
				if (fields[field].length() == 0) {
					fields[field] = null;
				}
				sb.setLength(0);
				field++;
				break;
			default:
				sb.append(c);
				break;
			}
		}

		this.groupId = fields[0].replace('/', '.');
		this.artifactId = fields[1];
		this.version = fields[2];
		this.classifier = fields[3];
		this.extension = fields[4];

		// determine dependency options and transitive dependency exclusions
		exclusions = new TreeSet<String>();
		tags = new TreeSet<String>();
		Set<String> options = new TreeSet<String>();
		for (String option : principals) {
			if (option.charAt(0) == '-' || option.charAt(0) == '!') {
				// exclusion
				exclusions.add(option.substring(1));
			} else if (option.charAt(0) == '@') {
				// fixed extension retrieval
				extension = option.substring(1);			
				resolveDependencies = false;
			} else if (option.charAt(0) == ':') {
				// tag
				tags.add(option.substring(1).toLowerCase());			
			} else if (option.charAt(0) == '#') {
				// comment
				break;
			} else {
				// option
				options.add(option.toLowerCase());
			}
		}
		optional = options.contains("optional");
		
		if (!isMavenObject()) {
			// forge dependency, filename is version field
			int dot = version.lastIndexOf('.');
			if (dot > -1) {
				extension = version.substring(dot + 1);
				version = version.substring(0, dot);
			}
		}
	}
	
	public boolean isMavenObject() {
		return groupId.charAt(0) != '<';
	}
	
	public boolean isSnapshot() {
		if (version == null) {
			throw new MoxieException(MessageFormat.format("Version is undefined for \"{0}\"!",  getCoordinates()));
		}
		return version.contains("-SNAPSHOT");
	}
	
	public boolean isMetaVersion() {
		return isRangedVersion()
				|| isSnapshot()
				|| version.equalsIgnoreCase(Constants.RELEASE)
				|| version.equalsIgnoreCase(Constants.LATEST);
	}
	
	public boolean isRangedVersion() {
		return version.indexOf('[') > -1 || version.indexOf('(') > -1;
	}
	
	public boolean isJavaBinary() {
		return Constants.isJavaBinary(extension);
	}

	public Dependency getPomArtifact() {
		Dependency pom = new Dependency(getDetailedCoordinates());
		pom.revision = revision;
		pom.extension = Constants.POM;
		return pom;
	}

	public Dependency getSourcesArtifact() {
		Dependency sources = new Dependency(getDetailedCoordinates());
		sources.revision = revision;
		sources.classifier = "sources";
		return sources;
	}

	public Dependency getJavadocArtifact() {
		Dependency javadoc = new Dependency(getDetailedCoordinates());
		javadoc.revision = revision;
		javadoc.classifier = "javadoc";
		return javadoc;
	}
	
	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public String getMediationId() {
		return groupId + ":" + artifactId + (classifier == null ? "" : (":" + classifier)) + ":" + extension;
	}

	public String getManagementId() {
		return groupId + ":" + artifactId;
	}
	
	public String getCoordinates() {
		return groupId + ":" + artifactId + ":" + version;
	}

	public String getDetailedCoordinates() {
		return groupId + ":" + artifactId + ":" + version + ":" + (classifier == null ? "" : classifier) + ":" + extension;
	}
	
	public String getPrefix() {
		String [] chunks = groupId.split("\\.");
		if (chunks.length < 2) {
			// single path
			return "/" + chunks[0];
		} else {
			// add first two paths
			return "/" + chunks[0] + "/" + chunks[1];
		}
	}
	
	public boolean excludes(Dependency dependency) {
		return exclusions.contains(dependency.getMediationId()) 
				|| exclusions.contains(dependency.getManagementId())
				|| exclusions.contains(dependency.groupId)
				|| exclusions.contains("*:*")
				|| exclusions.contains("*");
	}
	
	public String getOrigin() {
		return origin;
	}
	
	public void setOrigin(String origin) {
		this.origin = origin;
	}

	@Override
	public int hashCode() {
		return getDetailedCoordinates().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Dependency) {
			return hashCode() == o.hashCode();
		}
		return false;
	}
	
	@Override
	public String toString() {
		return getDetailedCoordinates() + (resolveDependencies ? " transitive":"") + (optional ? " optional":"");
	}
	
	public String toXML(Scope scope) {
		StringBuilder sb = new StringBuilder();
		sb.append("<dependency>\n");
		sb.append(StringUtils.toXML("groupId", groupId));
		sb.append(StringUtils.toXML("artifactId", artifactId));
		sb.append(StringUtils.toXML("version", version));
		sb.append(StringUtils.toXML("type", type));
		if (!StringUtils.isEmpty(classifier)) {
			sb.append(StringUtils.toXML("classifier", classifier));
		}
		sb.append(StringUtils.toXML("scope", scope));
		if (optional) {
			sb.append(StringUtils.toXML("optional", true));
		}
		Set<String> excludes = new TreeSet<String>(exclusions);
		if (!resolveDependencies) {
			excludes.add("*:*");
		}
		if (excludes.size() > 0) {
			StringBuilder nodelist = new StringBuilder();
			nodelist.append("<exclusions>\n");
			for (String exclusion : excludes) {
				StringBuilder node = new StringBuilder();
				node.append("<exclusion>\n");
				String [] e = exclusion.split(":");
				node.append(StringUtils.toXML("groupId", e[0]));
				if (e.length > 1) {
					node.append(StringUtils.toXML("artifactId", e[1]));
				}
				node.append("</exclusion>\n");
				nodelist.append(StringUtils.insertHalfTab(node.toString()));
			}
			nodelist.append("</exclusions>\n");
			sb.append(StringUtils.insertHalfTab(nodelist.toString()));
		}
		sb.append("</dependency>\n");
		return sb.toString();
	}
	
	public static String getArtifactPath(Dependency dep, String ext, String pattern) {
		return getPath(dep,  ext, pattern, true);
	}
	
	public static String getFilename(Dependency dep, String ext, String pattern) {
		return getPath(dep, ext, pattern, false);
	}
	
	private static String getPath(Dependency dep, String ext, String pattern, boolean splitGroupId) {
		Map<String, String> optionals = new HashMap<String, String>();
		String newpattern = pattern;
		int op = -1;
		while ((op = pattern.indexOf('(', op + 1)) > -1) {
			int cp = pattern.indexOf(')', op) + 1;
			if (cp > 0) {
				String s = pattern.substring(op, cp);
				int ob = s.indexOf('[');
				int cb = s.indexOf(']', ob) + 1;
				String field = s.substring(ob, cb);
				optionals.put(field, s.substring(1, s.length() - 1));
				newpattern = newpattern.replace(s, field);
			}
		}
		
		String url = newpattern;
		if (splitGroupId) {
			// Maven-style: groupId is split into paths
			url = replace(url, "[groupId]", dep.groupId.replace('.', '/'), optionals);
		} else {
			// Ivy-style: groupId is left in dot-notation
			url = replace(url, "[groupId]", dep.groupId, optionals);
		}
		url = replace(url, "[artifactId]", dep.artifactId, optionals);
		url = replace(url, "[version]", dep.version, optionals);
		url = replace(url, "[revision]", StringUtils.isEmpty(dep.revision) ? dep.version : dep.revision, optionals);

		if (ext != null && ext.equalsIgnoreCase(Constants.POM)) {
			// POMs do not have classifiers
			url = url.replace("[classifier]", "");
		} else {
			url = replace(url, "[classifier]", dep.classifier, optionals);
		}
		if (ext != null) {
			url = replace(url, "[ext]", ext, optionals);
		}
		return url;
	}
	
	private static String replace(String target, String key, String value, Map<String, String> substitutes) {
		String newtarget;
		if (StringUtils.isEmpty(value)) {
			newtarget = target.replace(key, "");
		} else if (substitutes.containsKey(key)) {
			String sub = substitutes.get(key).replace(key, value);
			newtarget = target.replace(key, sub);
		} else {
		  newtarget = target.replace(key, value);
		}
		return newtarget;
	}
}