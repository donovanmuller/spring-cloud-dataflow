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

package org.springframework.cloud.dataflow.rest.resource;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;

/**
 * A HATEOAS representation of a {@link StandaloneDefinition}.
 * This class also includes a description of the
 * {@link #status standalone status}.
 * <p>
 * Note: this implementation is not thread safe.
 *
 */
public class StandaloneDefinitionResource extends ResourceSupport {

	/**
	 * Standalone application name.
	 */
	private String name;

	/**
	 * Standalone application definition DSL text.
	 */
	private String dslText;

	/**
	 * Standalone application status (i.e. deployed, undeployed, etc).
	 */
	private String status;

	/**
	 * Default constructor for serialization frameworks.
	 */
	protected StandaloneDefinitionResource() {
	}

	/**
	 * Construct a {@code StandaloneDefinitionResource}.
	 *
	 * @param name standalone application name
	 * @param dslText standalone application definition DSL text
	 */
	public StandaloneDefinitionResource(String name, String dslText) {
		this.name = name;
		this.dslText = dslText;
	}

	/**
	 * Return the name of this standalone application.
	 *
	 * @return standalone application name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the DSL definition for this standalone application.
	 *
	 * @return standalone application definition DSL
	 */
	public String getDslText() {
		return this.dslText;
	}

	/**
	 * Return the status of this standalone application (i.e. deployed, undeployed, etc).
	 *
	 * @return standalone application status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Set the status of this standalone application (i.e. deployed, undeployed, etc).
	 *
	 * @param status standalone application status
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	public static class Page extends PagedResources<StandaloneDefinitionResource> {

	}

}
