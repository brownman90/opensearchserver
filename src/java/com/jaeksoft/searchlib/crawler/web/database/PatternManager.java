/**   
 * License Agreement for Jaeksoft OpenSearchServer
 *
 * Copyright (C) 2008 Emmanuel Keller / Jaeksoft
 * 
 * http://www.jaeksoft.com
 * 
 * This file is part of Jaeksoft OpenSearchServer.
 *
 * Jaeksoft OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Jaeksoft OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Jaeksoft OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.crawler.web.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.util.DomUtils;
import com.jaeksoft.searchlib.util.XPathParser;
import com.jaeksoft.searchlib.util.XmlWriter;

public class PatternManager {

	private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock(true);
	private final Lock r = rwl.readLock();
	private final Lock w = rwl.writeLock();

	private Map<String, List<PatternItem>> patternMap = null;

	private File patternFile;

	public PatternManager(File patternFile) throws SearchLibException {
		this.patternFile = patternFile;
		try {
			load();
		} catch (ParserConfigurationException e) {
			throw new SearchLibException(e);
		} catch (SAXException e) {
			throw new SearchLibException(e);
		} catch (IOException e) {
			throw new SearchLibException(e);
		} catch (XPathExpressionException e) {
			throw new SearchLibException(e);
		}
	}

	private void load() throws ParserConfigurationException, SAXException,
			IOException, XPathExpressionException, SearchLibException {
		if (!patternFile.exists())
			return;
		XPathParser xpp = new XPathParser(patternFile);
		NodeList nodeList = xpp.getNodeList("/patterns/pattern");
		int l = nodeList.getLength();
		List<PatternItem> patternList = new ArrayList<PatternItem>(l);
		for (int i = 0; i < l; i++)
			patternList
					.add(new PatternItem(DomUtils.getText(nodeList.item(i))));
		addListWithoutStoreAndLock(patternList, true);
	}

	private void store() throws IOException, TransformerConfigurationException,
			SAXException {
		if (!patternFile.exists())
			patternFile.createNewFile();
		PrintWriter pw = new PrintWriter(patternFile);
		try {
			XmlWriter xmlWriter = new XmlWriter(pw, "UTF-8");
			AttributesImpl atts = new AttributesImpl();
			TransformerHandler hd = xmlWriter.getTransformerHandler();
			hd.startElement("", "", "patterns", atts);
			Iterator<List<PatternItem>> it = patternMap.values().iterator();
			while (it.hasNext()) {
				for (PatternItem item : it.next()) {
					hd.startElement("", "", "pattern", atts);
					xmlWriter.textNode(item.getClass());
					hd.endElement("", "", "pattern");
				}
			}
			hd.endElement("", "", "pattern");
			xmlWriter.endDocument();
		} finally {
			pw.close();
		}
	}

	private void addListWithoutStoreAndLock(List<PatternItem> patternList,
			boolean bDeleteAll) throws SearchLibException,
			MalformedURLException {
		if (bDeleteAll)
			patternMap.clear();

		// First pass: Identify already present
		for (PatternItem item : patternList) {
			String pattern = item.getPattern();
			if (!bDeleteAll && findPattern(new URL(pattern)) != null)
				item.setStatus(PatternItem.Status.ALREADY);
			else {
				addPatternWithoutLock(item);
				item.setStatus(PatternItem.Status.INJECTED);
			}
		}
	}

	public void addList(List<PatternItem> patternList, boolean bDeleteAll)
			throws SearchLibException {
		w.lock();
		try {
			addListWithoutStoreAndLock(patternList, bDeleteAll);
			store();
		} catch (IOException e) {
			throw new SearchLibException(e);
		} catch (TransformerConfigurationException e) {
			throw new SearchLibException(e);
		} catch (SAXException e) {
			throw new SearchLibException(e);
		} finally {
			w.unlock();
		}
	}

	private void delPatternWithoutLock(String sPattern)
			throws MalformedURLException {
		String host = new PatternItem(sPattern).extractUrl(true).getHost();
		List<PatternItem> itemList = patternMap.get(host);
		if (itemList == null)
			return;
		itemList.remove(sPattern);
	}

	public void delPattern(Collection<String> patterns)
			throws SearchLibException {
		w.lock();
		try {
			for (String pattern : patterns)
				delPatternWithoutLock(pattern);
			store();
		} catch (MalformedURLException e) {
			throw new SearchLibException(e);
		} catch (TransformerConfigurationException e) {
			throw new SearchLibException(e);
		} catch (IOException e) {
			throw new SearchLibException(e);
		} catch (SAXException e) {
			throw new SearchLibException(e);
		} finally {
			w.unlock();
		}
	}

	private void addPatternWithoutLock(PatternItem patternItem)
			throws MalformedURLException {
		String host = patternItem.extractUrl(true).getHost();
		List<PatternItem> itemList = patternMap.get(host);
		if (itemList == null) {
			itemList = new ArrayList<PatternItem>();
			patternMap.put(host, itemList);
		}
		itemList.add(patternItem);
	}

	public void addPattern(PatternItem patternItem) throws SearchLibException {
		w.lock();
		try {
			addPatternWithoutLock(patternItem);
			store();
		} catch (TransformerConfigurationException e) {
			throw new SearchLibException(e);
		} catch (IOException e) {
			throw new SearchLibException(e);
		} catch (SAXException e) {
			throw new SearchLibException(e);
		} finally {
			w.unlock();
		}
	}

	public int getPatterns(String startsWith, long start, long rows,
			List<PatternItem> list) throws SearchLibException {
		r.lock();
		try {
			Iterator<List<PatternItem>> it = patternMap.values().iterator();
			long end = start + rows;
			int pos = 0;
			int total = 0;
			while (it.hasNext())
				for (PatternItem item : it.next()) {
					if (startsWith != null) {
						if (!item.getPattern().startsWith(startsWith)) {
							pos++;
							continue;
						}
					}
					if (pos >= start && pos < end)
						list.add(item);
					total++;
					pos++;
				}
			return total;
		} finally {
			r.unlock();
		}
	}

	public PatternItem findPattern(URL url) {
		r.lock();
		try {
			List<PatternItem> patternList = patternMap.get(url.getHost());
			if (patternList == null)
				return null;
			String sUrl = url.toExternalForm();
			for (PatternItem patternItem : patternList)
				if (patternItem.match(sUrl))
					return patternItem;
			return null;
		} finally {
			r.unlock();
		}
	}

	public void injectUrl(List<InjectUrlItem> urlListItems)
			throws SearchLibException {
		Iterator<InjectUrlItem> it = urlListItems.iterator();
		List<PatternItem> patternList = new ArrayList<PatternItem>();
		while (it.hasNext()) {
			InjectUrlItem item = it.next();
			if (findPattern(item.getURL()) != null)
				continue;
			patternList.add(new PatternItem(item.getURL()));
		}
		addList(patternList, false);
	}

	private static void addLine(List<PatternItem> list, String pattern) {
		pattern = pattern.trim();
		if (pattern.length() == 0)
			return;
		if (pattern.indexOf(':') == -1)
			pattern = "http://" + pattern;
		PatternItem item = new PatternItem();
		item.setPattern(pattern);
		list.add(item);
	}

	public static List<PatternItem> getPatternList(String pattern) {
		List<PatternItem> patternList = new ArrayList<PatternItem>();
		for (String sPattern : pattern.split("\n"))
			addLine(patternList, sPattern);
		return patternList;
	}

	public static List<PatternItem> getPatternList(BufferedReader reader)
			throws IOException {
		List<PatternItem> patternList = new ArrayList<PatternItem>();
		String line;
		while ((line = reader.readLine()) != null)
			addLine(patternList, line);
		return patternList;
	}

	public static String getStringPatternList(List<PatternItem> patternList) {
		StringBuffer sPattern = new StringBuffer();
		for (PatternItem item : patternList) {
			sPattern.append(item.getPattern());
			sPattern.append("\n");
		}
		return sPattern.toString();
	}

}
