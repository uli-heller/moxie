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
package com.maxtk;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Link implements Serializable {

	private static final long serialVersionUID = 1L;

	String name;
	String src;
	List<Link> sublinks;

	boolean isLink;
	boolean isPage;
	boolean isMenu;
	boolean isDivider;
	boolean sidebar;

	public void setName(String name) {
		this.name = name;
	}

	public void setSrc(String src) {
		this.src = src;
	}
	
	public void setSidebar(boolean value) {
		this.sidebar = value;
	}

	public Link createPage() {
		Link link = newLink();
		link.isPage = true;
		return link;
	}

	public Link createMenu() {
		Link link = newLink();
		link.isMenu = true;
		return link;
	}

	public Link createDivider() {
		Link link = newLink();
		link.isDivider = true;
		return link;
	}

	public Link createLink() {
		Link link = newLink();
		link.isLink = true;
		return link;
	}

	private Link newLink() {
		Link link = new Link();
		if (sublinks == null) {
			sublinks = new ArrayList<Link>();
		}
		sublinks.add(link);
		return link;
	}
}
