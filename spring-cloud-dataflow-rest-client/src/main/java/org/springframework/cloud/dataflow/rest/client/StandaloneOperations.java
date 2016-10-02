/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.rest.client;

import java.util.Map;

import org.springframework.cloud.dataflow.rest.resource.StandaloneDefinitionResource;
import org.springframework.hateoas.PagedResources;

/**
 * Interface defining operations available against standalone applications.
 *
 * @author Donovan Muller
 */
public interface StandaloneOperations {

	/**
	 * List standalone applications known to the system.
	 */
	public PagedResources<StandaloneDefinitionResource> list();

	/**
	 * Create a new standalone application, optionally deploying it.
	 */
	public StandaloneDefinitionResource createStandalone(String name, String definition, boolean force, boolean deploy);

	/**
	 * Return the {@link StandaloneDefinitionResource} for the standalone application name specified.
	 *
	 * @param name name of the standalone definition
	 * @return {@link StandaloneDefinitionResource}
	 */
	StandaloneDefinitionResource display(String name);

	/**
	 * Deploy an already registered standalone application.
	 */
	public void deploy(String name, Map<String, String> properties);

	/**
	 * Undeploy a deployed standalone application.
	 */
	public void undeploy(String name);

	/**
	 * Undeploy all currently deployed standalone applications.
	 */
	public void undeployAll();

	/**
	 * Destroy an existing standalone application.
	 */
	public void destroy(String name);

	/**
	 * Destroy all standalone applications known to the system.
	 */
	public void destroyAll();

}
