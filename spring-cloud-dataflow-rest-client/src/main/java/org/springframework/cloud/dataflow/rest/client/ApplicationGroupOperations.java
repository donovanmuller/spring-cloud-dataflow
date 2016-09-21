/*
 * Copyright 2015 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.rest.client;

import java.util.Map;

import org.springframework.cloud.dataflow.rest.resource.ApplicationGroupDefinitionResource;
import org.springframework.hateoas.PagedResources;

/**
 * Interface defining operations available against application groups.
 *
 */
public interface ApplicationGroupOperations {

	/**
	 * List application groups known to the system.
	 */
	public PagedResources<ApplicationGroupDefinitionResource> list();

	/**
	 * Import a new application group, optionally deploying it.
	 */
	public ApplicationGroupDefinitionResource importApplicationGroup(String name,
			String uri, boolean force, boolean deploy, Map<String, String> properties);

	/**
	 * Create a new application group, optionally deploying it.
	 */
	public ApplicationGroupDefinitionResource createApplicationGroup(String name,
			String definition, boolean deploy);

	/**
	 * Return the {@link ApplicationGroupDefinitionResource} for the application group
	 * specified.
	 *
	 * @param name name of the application group definition
	 * @return {@link ApplicationGroupDefinitionResource}
	 */
	public ApplicationGroupDefinitionResource display(String name);

	/**
	 * Deploy an already created application group.
	 */
	public void deploy(String name, Map<String, String> properties);

	/**
	 * Redeploy an already deployed application group.
	 */
	public void redeploy(String name, Map<String, String> properties);

	/**
	 * Undeploy a deployed application group, retaining its definition.
	 */
	public void undeploy(String name);

	/**
	 * Undeploy all currently deployed application groups.
	 */
	public void undeployAll();

	/**
	 * Destroy an existing application group.
	 */
	public void destroy(String name);

	/**
	 * Destroy all application groups known to the system.
	 */
	public void destroyAll();

}
