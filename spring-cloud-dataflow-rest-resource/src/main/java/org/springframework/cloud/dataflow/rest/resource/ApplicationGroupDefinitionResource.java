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
 * A HATEOAS representation of a {@link ApplicationGroupDefinition}.
 * This class also includes a description of the
 * {@link #status application group status}.
 * <p>
 * Note: this implementation is not thread safe.
 *
 * @author Donovan Muller
 */
public class ApplicationGroupDefinitionResource extends ResourceSupport {

	/**
	 * Application group name.
	 */
	private String name;

	/**
	 * Application group definition DSL text.
	 */
	private String dslText;

	/**
	 * Application group status (i.e. deployed, undeployed, etc).
	 */
	private String status;

	/**
	 * Default constructor for serialization frameworks.
	 */
	protected ApplicationGroupDefinitionResource() {
	}

	/**
	 * Construct a {@code ApplicationGroupDefinitionResource}.
	 *
	 * @param name application group name
	 * @param dslText application group definition DSL text
	 */
	public ApplicationGroupDefinitionResource(String name, String dslText) {
		this.name = name;
		this.dslText = dslText;
	}

	/**
	 * Return the name of this application group.
	 *
	 * @return application group name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the DSL definition for this application group.
	 *
	 * @return application group definition DSL
	 */
	public String getDslText() {
		return this.dslText;
	}

	/**
	 * Return the status of this application group (i.e. deployed, undeployed, etc).
	 *
	 * @return application group status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Set the status of this application group (i.e. deployed, undeployed, etc).
	 *
	 * @param status application group status
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	public static class Page extends PagedResources<ApplicationGroupDefinitionResource> {

	}

}
