/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.core.dsl;

import java.util.List;

/**
 * @author Donovan Muller
 */
public class ApplicationGroupNode extends AstNode {

	private final String applicationGroupText;

	private final String applicationGroupName;

	private final List<AppNode> appNodes;

	public ApplicationGroupNode(String applicationGroupText, String applicationGroupName, List<AppNode> appNodes) {
		super(appNodes.get(0).getStartPos(), appNodes.get(appNodes.size() - 1).getEndPos());
		this.applicationGroupText = applicationGroupText;
		this.applicationGroupName = applicationGroupName;
		this.appNodes = appNodes;
	}

	/** @inheritDoc */
	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		s.append("[");
		if (getApplicationGroupName() != null) {
			s.append(getApplicationGroupName()).append(" = ");
		}
		s.append("]");
		return s.toString();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (getApplicationGroupName() != null) {
			s.append(getApplicationGroupName()).append(" = ");
		}
		for (int m = 0; m < appNodes.size(); m++) {
			AppNode appNode = appNodes.get(m);
			s.append(appNode.toString());
			if (m + 1 < appNodes.size()) {
				s.append(" | ");
			}
		}
		return s.toString();
	}

	public List<AppNode> getAppNodes() {
		return appNodes;
	}

	public String getApplicationGroupName() {
		return applicationGroupName;
	}

	/**
	 * Find the first reference to the named app in the application group. If the same app is referred to multiple times the
	 * secondary references cannot be accessed via this method.
	 *
	 * @return the first occurrence of the named app in the application group
	 */
	public AppNode getApp(String appName) {
		for (AppNode appNode : appNodes) {
			if (appNode.getName().equals(appName)) {
				return appNode;
			}
		}
		return null;
	}

	public int getIndexOfLabel(String labelOrAppName) {
		for (int m = 0; m < appNodes.size(); m++) {
			AppNode appNode = appNodes.get(m);
			if (appNode.getLabelName().equals(labelOrAppName)) {
				return m;
			}
		}
		return -1;
	}

	public String getApplicationGroupData() {
		return toString();
	}

	public String getApplicationGroupText() {
		return this.applicationGroupText;
	}

	public String getName() {
		return this.applicationGroupName;
	}

}
