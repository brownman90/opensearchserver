/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2011-2012 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of OpenSearchServer.
 *
 * OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.web.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.bind.annotation.AfterCompose;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.zul.Messagebox;

import com.jaeksoft.searchlib.Client;
import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.renderer.Renderer;
import com.jaeksoft.searchlib.renderer.RendererField;
import com.jaeksoft.searchlib.renderer.RendererFieldType;
import com.jaeksoft.searchlib.renderer.RendererLogField;
import com.jaeksoft.searchlib.renderer.RendererLogParameterEnum;
import com.jaeksoft.searchlib.renderer.RendererManager;
import com.jaeksoft.searchlib.renderer.RendererSort;
import com.jaeksoft.searchlib.renderer.RendererWidgets;
import com.jaeksoft.searchlib.renderer.plugin.AuthPluginEnum;
import com.jaeksoft.searchlib.request.AbstractSearchRequest;
import com.jaeksoft.searchlib.request.RequestTypeEnum;

@AfterCompose(superclass = true)
public class RendererController extends CommonController {

	private transient Renderer selectedRenderer;
	private transient Renderer currentRenderer;
	private transient boolean isTestable;
	private transient RendererField currentRendererField;
	private transient RendererField selectedRendererField;
	private transient RendererLogField currentRendererLogField;
	private transient RendererSort currentRendererSort;
	private transient RendererSort selectedRendererSort;

	private class DeleteAlert extends AlertController {

		private Renderer deleteRenderer;

		protected DeleteAlert(Renderer deleteRenderer)
				throws InterruptedException {
			super("Please, confirm that you want to delete the renderer: "
					+ deleteRenderer.getName(), Messagebox.YES | Messagebox.NO,
					Messagebox.QUESTION);
			this.deleteRenderer = deleteRenderer;
		}

		@Override
		protected void onYes() throws SearchLibException, IOException {
			Client client = getClient();
			client.getRendererManager().remove(deleteRenderer);
			client.delete(deleteRenderer);
			onCancel();
		}
	}

	public RendererController() throws SearchLibException {
		super();
		reset();
	}

	@Override
	protected void reset() throws SearchLibException {
		currentRenderer = null;
		currentRendererField = null;
		currentRendererSort = null;
		selectedRenderer = null;
		isTestable = false;
		currentRendererLogField = null;
	}

	public Renderer[] getRenderers() throws SearchLibException {
		Client client = getClient();
		if (client == null)
			return null;
		return client.getRendererManager().getArray();
	}

	public List<String> getRequestList() throws SearchLibException {
		Client client = getClient();
		if (client == null)
			return null;
		List<String> requestList = new ArrayList<String>(0);
		client.getRequestMap().getNameList(requestList,
				RequestTypeEnum.SearchRequest,
				RequestTypeEnum.SearchFieldRequest);
		return requestList;
	}

	public String getCurrentEditMode() throws SearchLibException {
		return selectedRenderer == null ? "Create a new renderer"
				: "Edit the renderer: " + selectedRenderer.getName();
	}

	public boolean isEditing() {
		return currentRenderer != null;
	}

	public boolean isNotEditing() {
		return !isEditing();
	}

	public boolean isSelected() {
		return selectedRenderer != null;
	}

	public boolean isNotSelected() {
		return !isSelected();
	}

	public boolean isFieldSelected() {
		return selectedRendererField != null;
	}

	public boolean isNotFieldSelected() {
		return !isFieldSelected();
	}

	public boolean isSortSelected() {
		return selectedRendererSort != null;
	}

	public AuthPluginEnum[] getAuthTypeList() {
		return AuthPluginEnum.values();
	}

	@Command
	public void doEdit(@BindingParam("item") Renderer renderer)
			throws SearchLibException {
		selectedRenderer = renderer;
		currentRenderer = new Renderer(renderer);
		currentRendererField = new RendererField();
		currentRendererLogField = new RendererLogField();
		currentRendererSort = new RendererSort();
		reload();
	}

	@Command
	public void doDelete(@BindingParam("item") Renderer renderer)
			throws InterruptedException {
		new DeleteAlert(renderer);
	}

	@Command
	public void onNew() throws SearchLibException {
		currentRenderer = new Renderer();
		currentRendererField = new RendererField();
		currentRendererLogField = new RendererLogField();
		currentRendererSort = new RendererSort();
		reload();
	}

	@Command
	public void onRendererFieldSave() throws SearchLibException {
		if (selectedRendererField == null)
			currentRenderer.addField(currentRendererField);
		else
			currentRendererField.copyTo(selectedRendererField);
		onRendererFieldCancel();
		reload();
	}

	@Command
	public void onRendererSortSave() throws SearchLibException {
		if (selectedRendererSort == null)
			currentRenderer.addSort(currentRendererSort);
		else
			currentRendererSort.copyTo(selectedRendererSort);
		onRendererSortCancel();
		reload();
	}

	@Command
	public void onLogFieldAdd() throws SearchLibException, InterruptedException {
		if (currentRendererLogField.getLogParameterEnum() == null
				|| currentRendererLogField.getCustomlogItem().equals("")
				|| RendererLogParameterEnum.find(currentRendererLogField
						.getLogParameterEnum().name()) == null)
			new AlertController("FieldName / Parameter cannot be null");
		else {
			if (currentRendererLogField != null)
				currentRenderer.addLogField(currentRendererLogField);
		}
		onRendererLogFieldCancel();
		reload();
	}

	@Command
	public void onRendererLogFieldCancel() throws SearchLibException {
		currentRendererLogField = new RendererLogField();
		reload();
	}

	@Command
	public void onRendererSortCancel() throws SearchLibException {
		currentRendererSort = new RendererSort();
		selectedRendererSort = null;
		reload();
	}

	@Command
	public void onLogFieldRemove(
			@BindingParam("renderLogFieldItem") RendererLogField rendererlogField)
			throws SearchLibException, InterruptedException {
		currentRenderer.removeLogField(rendererlogField);
		reload();
	}

	@Command
	public void onRendererSortRemove(
			@BindingParam("rendererSort") RendererSort rendererSort)
			throws SearchLibException, InterruptedException {
		currentRenderer.removeSort(rendererSort);
		reload();
	}

	@Command
	public void onRendererFieldRemove(
			@BindingParam("rendererFieldItem") RendererField rendererField)
			throws SearchLibException {
		currentRenderer.removeField(rendererField);
		reload();
	}

	@Command
	public void onRendererFieldUp(
			@BindingParam("rendererFieldItem") RendererField rendererField)
			throws SearchLibException {
		currentRenderer.fieldUp(rendererField);
		reload();
	}

	@Command
	public void onRendererFieldDown(
			@BindingParam("rendererFieldItem") RendererField rendererField)
			throws SearchLibException {
		currentRenderer.fieldDown(rendererField);
		reload();
	}

	@Command
	public void onRendererSortUp(
			@BindingParam("rendererSort") RendererSort rendererSort)
			throws SearchLibException {
		currentRenderer.sortUp(rendererSort);
		reload();
	}

	@Command
	public void onRendererSortDown(
			@BindingParam("rendererSort") RendererSort rendererSort)
			throws SearchLibException {
		currentRenderer.sortDown(rendererSort);
		reload();
	}

	@Command
	public void onRendererFieldCancel() throws SearchLibException {
		currentRendererField = new RendererField();
		selectedRendererField = null;
		reload();
	}

	@Command
	public void onCancel() throws SearchLibException {
		currentRenderer = null;
		selectedRenderer = null;
		isTestable = false;
		reload();
	}

	@Command
	public void onCssDefault() throws SearchLibException {
		currentRenderer.setDefaultCss();
		reload();
	}

	@Command
	public void onSave() throws SearchLibException, IOException {
		Client client = getClient();
		if (client == null)
			return;
		RendererManager manager = client.getRendererManager();
		if (selectedRenderer != null) {
			manager.replace(selectedRenderer, currentRenderer);
		} else
			manager.add(currentRenderer);
		client.save(currentRenderer);
	}

	@Command
	public void onSaveAndClose() throws SearchLibException, IOException {
		onSave();
		onCancel();
	}

	@Command
	public void onTest() throws SearchLibException {
		isTestable = true;
		reload();
	}

	public String getIframeHtmlCode() throws UnsupportedEncodingException,
			InterruptedException {
		if (currentRenderer == null)
			return null;
		return currentRenderer.getIFrameHtmlCode(getIframeWidthPx(),
				getIframeHeightPx());
	}

	public boolean isTestable() {
		return isTestable;
	}

	public Renderer getCurrentRenderer() {
		return currentRenderer;
	}

	public RendererField getCurrentRendererField() {
		return currentRendererField;
	}

	public RendererField getSelectedRendererField() {
		return selectedRendererField;
	}

	public void setSelectedRendererField(RendererField field)
			throws SearchLibException {
		selectedRendererField = field;
		currentRendererField = new RendererField(field);
		reload();
	}

	public RendererSort getCurrentRendererSort() {
		return currentRendererSort;
	}

	public RendererSort getSelectedRendererSort() {
		return selectedRendererSort;
	}

	public void setSelectedRendererSort(RendererSort sort)
			throws SearchLibException {
		selectedRendererSort = sort;
		currentRendererSort = new RendererSort(sort);
		reload();
	}

	public Renderer getSelectedClassifier() {
		return selectedRenderer;
	}

	public Integer getIframeWidth() {
		return (Integer) getAttribute(ScopeAttribute.RENDERER_IFRAME_WIDTH,
				new Integer(700));
	}

	public Integer getIframeHeight() {
		return (Integer) getAttribute(ScopeAttribute.RENDERER_IFRAME_HEIGHT,
				new Integer(400));
	}

	public String getIframeWidthPx() {
		return getIframeWidth() + "px";
	}

	public String getIframeHeightPx() {
		return getIframeHeight() + "px";
	}

	public void setIframeWidth(Integer width) {
		setAttribute(ScopeAttribute.RENDERER_IFRAME_WIDTH, width);
	}

	public void setIframeHeight(Integer height) {
		setAttribute(ScopeAttribute.RENDERER_IFRAME_HEIGHT, height);
	}

	public RendererFieldType[] getFieldTypeList() {
		return RendererFieldType.values();
	}

	public RendererWidgets[] getWidgetList() {
		return RendererWidgets.values();
	}

	public List<String> getFieldList() throws SearchLibException {
		if (currentRendererField == null || currentRenderer == null)
			return null;
		Client client = getClient();
		if (client == null)
			return null;
		AbstractSearchRequest request = (AbstractSearchRequest) client
				.getRequestMap().get(currentRenderer.getRequestName());
		if (request == null)
			return null;
		List<String> nameList = new ArrayList<String>();
		nameList.add(null);
		request.getReturnFieldList().toNameList(nameList);
		return nameList;
	}

	public List<String> getFieldOrSnippetList() throws SearchLibException {
		if (currentRendererField == null || currentRenderer == null)
			return null;
		Client client = getClient();
		if (client == null)
			return null;
		AbstractSearchRequest request = (AbstractSearchRequest) client
				.getRequestMap().get(currentRenderer.getRequestName());
		if (request == null)
			return null;
		List<String> nameList = new ArrayList<String>();
		nameList.add(null);
		if (currentRendererField.getFieldType() == RendererFieldType.FIELD)
			request.getReturnFieldList().toNameList(nameList);
		else if (currentRendererField.getFieldType() == RendererFieldType.SNIPPET)
			request.getSnippetFieldList().toNameList(nameList);
		return nameList;
	}

	public String[] getCustomLogList() {
		String customLogs[] = new String[10];
		for (int i = 0; i < 10; i++)
			customLogs[i] = "customField" + i;
		return customLogs;
	}

	public List<String> getAutocompletionList() throws SearchLibException {
		Client client = getClient();
		if (client == null)
			return null;
		List<String> nameList = new ArrayList<String>();
		nameList.add(null);
		client.getAutoCompletionManager().toNameList(nameList);
		return nameList;
	}

	public RendererLogParameterEnum[] getLogParameterList() {
		return RendererLogParameterEnum.values();
	}

	public RendererLogField getCurrentRendererLogField() {
		return currentRendererLogField;
	}

	public void setCurrentRendererLogField(
			RendererLogField currentRendererLogField) {
		this.currentRendererLogField = currentRendererLogField;
	}
}
