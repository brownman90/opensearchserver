/**   
 * License Agreement for Jaeksoft OpenSearchServer
 *
 * Copyright (C) 2010 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
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

package com.jaeksoft.searchlib.crawler.file.process;

import java.net.URI;
import java.net.URISyntaxException;

import com.jaeksoft.searchlib.crawler.file.database.FilePathItem;
import com.jaeksoft.searchlib.crawler.file.database.FileTypeEnum;

public abstract class FileInstanceAbstract {

	protected FileInstanceAbstract parent;

	protected FilePathItem filePathItem;

	protected String path;

	private URI uri;

	final public static FileInstanceAbstract create(FilePathItem filePathItem,
			FileInstanceAbstract parent, String path)
			throws InstantiationException, IllegalAccessException,
			URISyntaxException {
		FileInstanceAbstract fileInstance = filePathItem.getType()
				.getNewInstance();
		fileInstance.init(filePathItem, parent, path);
		return fileInstance;
	}

	protected FileInstanceAbstract() {
	}

	protected void init(FilePathItem filePathItem, FileInstanceAbstract parent,
			String path) throws URISyntaxException {
		this.filePathItem = filePathItem;
		this.parent = parent;
		this.path = path;
		uri = new URI(filePathItem.getType().getScheme(),
				filePathItem.getUsername(), filePathItem.getHost(), -1, path,
				null, null);
	}

	public abstract FileTypeEnum getFileType();

	public abstract FileInstanceAbstract[] listFilesAndDirectories()
			throws URISyntaxException;

	public abstract FileInstanceAbstract[] listFilesOnly()
			throws URISyntaxException;

	public abstract Long getLastModified();

	public abstract Long getFileSize();

	public FilePathItem getFilePathItem() {
		return filePathItem;
	}

	public FileInstanceAbstract getParent() {
		return parent;
	}

	public String getPath() {
		return path;
	}

	public URI getURI() {
		return uri;
	}
}