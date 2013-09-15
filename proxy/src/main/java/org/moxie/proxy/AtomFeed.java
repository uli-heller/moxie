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
package org.moxie.proxy;

import java.util.Date;
import java.util.List;

import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.ext.atom.Category;
import org.restlet.ext.atom.Content;
import org.restlet.ext.atom.Entry;
import org.restlet.ext.atom.Feed;
import org.restlet.ext.atom.Generator;
import org.restlet.ext.atom.Link;
import org.restlet.ext.atom.Relation;
import org.restlet.ext.atom.Text;
import org.restlet.representation.StringRepresentation;

/**
 * Store a list of entries which may be retrieved as an Atom Feed representation
 * for being served by a restlet server.
 */
public class AtomFeed {

	final MoxieProxy app;
	final String baseUrl;
	final String title;
	
	public AtomFeed(MoxieProxy app, String baseUrl, String title) {
		this.app = app;
		this.baseUrl = baseUrl;
		this.title = title;
	}

	public Feed getFeed(String repository, int count) {
		Feed feed = getConfiguredFeed();
		List<SearchResult> results = app.getRecentArtifacts(repository, 1, count);
		for (SearchResult result : results) {
			Entry entry = toEntry(result);
			feed.getEntries().add(entry);
		}
		feed.setUpdated(new Date());
		return feed;
	}

	private Feed getConfiguredFeed() {
		Feed feed = new Feed();
		feed.setGenerator(getGenerator());
		feed.setTitle(new Text(MediaType.TEXT_PLAIN, title));
		return feed;
	}
	
	private Entry toEntry(SearchResult result) {
		Entry entry = new Entry();
		entry.getCategories().add(newCategory(result.repository));
		entry.setContent(getContent(result.getDescription()));
		entry.setPublished(result.date);
		entry.setTitle(new Text(MediaType.TEXT_PLAIN, result.getCoordinates()));
		entry.setSummary(result.getName());
		entry.getLinks().add(newLink(result.getPath()));
		return entry;
	}
	
	private Category newCategory(String label) {
		Category c = new Category();
		c.setLabel(label);
		return c;
	}
	
	private Link newLink(String path) {
		Link l = new Link();
		l.setHref(new Reference(baseUrl + "/" + path));
		l.setRel(Relation.VIA);
		return l;
	}

	private Content getContent(String txt) {
		Content content = new Content();
		content.setInlineContent(new StringRepresentation(txt, MediaType.TEXT_PLAIN));
		return content;
	}

	private Generator getGenerator() {
		Generator generator = new Generator();
		generator.setName(Constants.getName());
		generator.setUri(new Reference(Constants.getUrl()));
		generator.setVersion(Constants.getVersion());
		return generator;
	}
}
