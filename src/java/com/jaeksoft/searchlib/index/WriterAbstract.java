/**   
 * License Agreement for Jaeksoft OpenSearchServer
 *
 * Copyright (C) 2008-2010 Emmanuel Keller / Jaeksoft
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

package com.jaeksoft.searchlib.index;

import java.security.NoSuchAlgorithmException;

import com.jaeksoft.searchlib.util.Md5Spliter;

public abstract class WriterAbstract implements WriterInterface {

	private Md5Spliter md5spliter = null;
	private String keyField = null;

	protected WriterAbstract(IndexConfig indexConfig) {
		this.md5spliter = null;
		this.keyField = indexConfig.getKeyField();
		if (indexConfig.getKeyMd5RegExp() != null)
			md5spliter = new Md5Spliter(indexConfig.getKeyMd5RegExp());
	}

	protected boolean acceptDocument(IndexDocument document)
			throws NoSuchAlgorithmException {
		if (keyField == null)
			return true;
		if (md5spliter == null)
			return true;
		FieldContent fieldContent = document.getField(keyField);
		if (fieldContent == null)
			return false;
		return md5spliter.acceptAnyKey(fieldContent.getValues());
	}

}
