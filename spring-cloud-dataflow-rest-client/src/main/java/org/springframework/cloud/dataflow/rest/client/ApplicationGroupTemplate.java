/*
 * Copyright 2015-2016 the original author or authors.
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

import org.springframework.cloud.dataflow.rest.resource.ApplicationGroupDefinitionResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation for {@link ApplicationGroupOperations}.
 *
 */
public class ApplicationGroupTemplate implements ApplicationGroupOperations {

	public static String DEFINITIONS_REL = "application-groups/definitions";

	private static String DEFINITION_REL = "application-groups/definitions/definition";

	private static String DEPLOYMENTS_REL = "application-groups/deployments";

	private static String DEPLOYMENT_REL = "application-groups/deployments/deployment";

	private RestTemplate restTemplate;

	private Link definitionsLink;

	private Link definitionLink;

	private Link deploymentsLink;

	private Link deploymentLink;

	ApplicationGroupTemplate(RestTemplate restTemplate, ResourceSupport resources) {
		Assert.notNull(resources, "URI Resources can't be null");
		Assert.notNull(resources.getLink(DEFINITIONS_REL),
				"Definitions relation is required");
		Assert.notNull(resources.getLink(DEFINITION_REL),
				"Definition relation is required");
		Assert.notNull(resources.getLink(DEPLOYMENTS_REL),
				"Deployments relation is required");
		Assert.notNull(resources.getLink(DEPLOYMENT_REL),
				"Deployment relation is required");
		this.restTemplate = restTemplate;
		this.definitionsLink = resources.getLink(DEFINITIONS_REL);
		this.deploymentsLink = resources.getLink(DEPLOYMENTS_REL);
		this.definitionLink = resources.getLink(DEFINITION_REL);
		this.deploymentLink = resources.getLink(DEPLOYMENT_REL);
	}

	@Override
	public ApplicationGroupDefinitionResource.Page list() {
		String uriTemplate = definitionsLink.expand().getHref();
		uriTemplate = uriTemplate + "?size=10000";
		return restTemplate.getForObject(uriTemplate,
				ApplicationGroupDefinitionResource.Page.class);
	}

	@Override
	public ApplicationGroupDefinitionResource importApplicationGroup(String name,
			String uri, boolean force, boolean deploy, Map<String, String> properties) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("name", name);
		values.add("uri", uri);
		values.add("force", Boolean.toString(force));
		values.add("deploy", Boolean.toString(deploy));
		values.add("properties", DeploymentPropertiesUtils.format(properties));
		ApplicationGroupDefinitionResource applicationGroup = restTemplate.postForObject(
				definitionsLink.expand().getHref(), values,
				ApplicationGroupDefinitionResource.class);
		return applicationGroup;
	}

	@Override
	public ApplicationGroupDefinitionResource createApplicationGroup(String name,
			String definition, boolean deploy) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("name", name);
		values.add("definition", definition);
		values.add("deploy", Boolean.toString(deploy));
		ApplicationGroupDefinitionResource applicationGroup = restTemplate.postForObject(
				definitionsLink.expand().getHref(), values,
				ApplicationGroupDefinitionResource.class);
		return applicationGroup;
	}

	@Override
	public ApplicationGroupDefinitionResource display(final String name) {
		return restTemplate.getForObject(definitionLink.expand(name).getHref(),
				ApplicationGroupDefinitionResource.class);
	}

	@Override
	public void deploy(String name, Map<String, String> properties) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("properties", DeploymentPropertiesUtils.format(properties));
		restTemplate.postForObject(deploymentLink.expand(name).getHref(), values,
				Object.class);
	}

	@Override
	public void redeploy(String name, Map<String, String> properties) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("properties", DeploymentPropertiesUtils.format(properties));
		restTemplate.put(deploymentLink.expand(name).getHref(), values, Object.class);
	}

	@Override
	public void undeploy(String name) {
		restTemplate.delete(deploymentLink.expand(name).getHref());
	}

	@Override
	public void undeployAll() {
		restTemplate.delete(deploymentsLink.getHref());
	}

	@Override
	public void destroy(String name) {
		restTemplate.delete(definitionLink.expand(name).getHref());
	}

	@Override
	public void destroyAll() {
		restTemplate.delete(definitionsLink.getHref());
	}
}
