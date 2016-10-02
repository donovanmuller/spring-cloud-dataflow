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

package org.springframework.cloud.dataflow.server.controller;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StandaloneDefinition;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.rest.resource.StandaloneDeploymentResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.DeploymentKey;
import org.springframework.cloud.dataflow.server.repository.NoSuchStandaloneDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StandaloneDefinitionRepository;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for deployment operations on {@link StandaloneDefinition}.
 *
 * @author Donovan Muller
 */
@RestController
@RequestMapping("/standalone/deployments")
@ExposesResourceFor(StandaloneDeploymentResource.class)
public class StandaloneDeploymentController {

	private static Log logger = LogFactory.getLog(StandaloneDeploymentController.class);

	private static final String INSTANCE_COUNT_PROPERTY_KEY = "count";

	/**
	 * The repository this controller will use for standalone CRUD operations.
	 */
	private final StandaloneDefinitionRepository repository;

	/**
	 * The repository this controller will use for deployment IDs.
	 */
	private final DeploymentIdRepository deploymentIdRepository;

	/**
	 * The app registry this controller will use to lookup apps.
	 */
	private final AppRegistry registry;

	/**
	 * The deployer this controller will use to deploy standalone applications.
	 */
	private final AppDeployer deployer;

	private final ApplicationConfigurationMetadataResolver metadataResolver;

	/**
	 * General properties to be applied to applications on deployment.
	 */
	private final CommonApplicationProperties commonApplicationProperties;

	/**
	 * Create a {@code StandaloneDeploymentController} that delegates
	 * <ul>
	 * <li>CRUD operations to the provided {@link StandaloneDefinitionRepository}</li>
	 * <li>app retrieval to the provided {@link AppRegistry}</li>
	 * <li>deployment operations to the provided {@link AppDeployer}</li>
	 * </ul>
	 * @param repository             the repository this controller will use for standalone CRUD operations
	 * @param deploymentIdRepository the repository this controller will use for deployment IDs
	 * @param registry               the registry this controller will use to lookup apps
	 * @param deployer               the deployer this controller will use to deploy standalone apps
	 * @param commonProperties       common set of application properties
	 */
    public StandaloneDeploymentController(StandaloneDefinitionRepository repository,
            DeploymentIdRepository deploymentIdRepository,
            AppRegistry registry,
            AppDeployer deployer,
            ApplicationConfigurationMetadataResolver metadataResolver,
            CommonApplicationProperties commonProperties) {
        Assert.notNull(repository, "StandaloneDefinitionRepository must not be null");
        Assert.notNull(deploymentIdRepository, "DeploymentIdRepository must not be null");
        Assert.notNull(registry, "AppRegistry must not be null");
        Assert.notNull(deployer, "AppDeployer must not be null");
        Assert.notNull(metadataResolver, "MetadataResolver must not be null");
        Assert.notNull(commonProperties, "CommonApplicationProperties must not be null");
        this.repository = repository;
        this.deploymentIdRepository = deploymentIdRepository;
        this.registry = registry;
        this.deployer = deployer;
        this.metadataResolver = metadataResolver;
        this.commonApplicationProperties = commonProperties;
    }

	/**
	 * Request un-deployment of an existing standalone application.
	 * @param name the name of an existing standalone application (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void undeploy(@PathVariable("name") String name) {
		StandaloneDefinition standalone = this.repository.findOne(name);
		if (standalone == null) {
			throw new NoSuchStandaloneDefinitionException(name);
		}
		undeployStandalone(standalone);
	}

	/**
	 * Request un-deployment of all standalone applications.
	 */
	@RequestMapping(value = "", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void undeployAll() {
		for (StandaloneDefinition standaloneDefinition : this.repository.findAll()) {
			this.undeployStandalone(standaloneDefinition);
		}
	}

	/**
	 * Request deployment of an existing standalone application definition.
	 * @param name       the name of an existing standalone application definition (required)
	 * @param properties the deployment properties for the standalone application as a comma-delimited list of
	 *                      key=value pairs
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void deploy(@PathVariable("name") String name,
					   @RequestParam(required = false) String properties) {
		StandaloneDefinition standalone = this.repository.findOne(name);
		if (standalone == null) {
			throw new NoSuchStandaloneDefinitionException(name);
		}
		String status = calculateStandaloneState(name);
		if (DeploymentState.deployed.equals(DeploymentState.valueOf(status))) {
			throw new StandaloneAlreadyDeployedException(name);
		}
		else if (DeploymentState.deploying.equals(DeploymentState.valueOf(status))) {
			throw new StandaloneAlreadyDeployingException(name);
		}
		deployStandalone(standalone, DeploymentPropertiesUtils.parse(properties));
	}

	String calculateStandaloneState(String name) {
		Set<DeploymentState> appStates = EnumSet.noneOf(DeploymentState.class);
		StandaloneDefinition standalone = this.repository.findOne(name);
		String key = DeploymentKey.forStandaloneAppDefinition(standalone);
		String id = deploymentIdRepository.findOne(key);
		if (id != null) {
			AppStatus status = this.deployer.status(id);
			appStates.add(status.getState());
		}
		else {
			appStates.add(DeploymentState.undeployed);
		}

		return StandaloneDefinitionController.aggregateState(appStates).toString();
	}

	/**
	 * Deploy a standalone application as defined by its {@link StandaloneDefinition} and optional deployment properties.
	 * @param standalone                 	 the standalone application to deploy
	 * @param standaloneDeploymentProperties the deployment properties for the standalone application
	 */
	void deployStandalone(StandaloneDefinition standalone, Map<String, String> standaloneDeploymentProperties) {
		AppRegistration registration = this.registry.find(standalone.getName(), ApplicationType.standalone);
		Assert.notNull(registration, String.format("no application '%s' of type '%s' exists in the registry",
				standalone.getName(), ApplicationType.standalone));
		Resource resource = registration.getResource();
		standalone = qualifyParameters(standalone, resource);

		AppDeploymentRequest request = standalone.createDeploymentRequest(resource, extractDeploymentProperties(
				standalone, standaloneDeploymentProperties
		));
		try {
			String id = this.deployer.deploy(request);
			this.deploymentIdRepository.save(DeploymentKey.forStandaloneAppDefinition(standalone), id);
		}
		catch (Exception e) {
			logger.warn(String.format("Exception when deploying the app %s: %s", standalone, e.getMessage()));
		}
	}

    Map<String, String> extractDeploymentProperties(StandaloneDefinition standalone,
            Map<String, String> standaloneDeploymentProperties) {
        if (standaloneDeploymentProperties == null) {
            standaloneDeploymentProperties = Collections.emptyMap();
        }

        Map<String, String> appDeploymentProperties = extractAppDeploymentProperties(standalone, standaloneDeploymentProperties);
        appDeploymentProperties.put(AppDeployer.GROUP_PROPERTY_KEY, standalone.getRegisteredAppName());
        if (appDeploymentProperties.containsKey(INSTANCE_COUNT_PROPERTY_KEY)) {
            appDeploymentProperties.put(AppDeployer.COUNT_PROPERTY_KEY,
                    appDeploymentProperties.get(INSTANCE_COUNT_PROPERTY_KEY));
        }

        return appDeploymentProperties;
    }

	/**
	 * Return a copy of a given app definition where short form parameters have been expanded to their long form
	 * (amongst the whitelisted supported properties of the app) if applicable.
	 */
	/*default*/ StandaloneDefinition qualifyParameters(StandaloneDefinition original, Resource resource) {
		MultiValueMap<String, ConfigurationMetadataProperty> whiteList = new LinkedMultiValueMap<>();
		Set<String> allProps = new HashSet<>();

		for (ConfigurationMetadataProperty property : this.metadataResolver.listProperties(resource, false)) {
			whiteList.add(property.getName(), property);// Use names here
		}
		for (ConfigurationMetadataProperty property : this.metadataResolver.listProperties(resource, true)) {
			allProps.add(property.getId()); // But full ids here
		}

		StandaloneDefinition.Builder builder = StandaloneDefinition.Builder.from(original);
		Map<String, String> mutatedProps = new HashMap<>(original.getProperties().size());
		for (Map.Entry<String, String> entry : original.getProperties().entrySet()) {
			if (!allProps.contains(entry.getKey())) {
				List<ConfigurationMetadataProperty> longForms = whiteList.get(entry.getKey());
				if (longForms != null) {
					assertNoAmbiguity(longForms);
					mutatedProps.put(longForms.iterator().next().getId(), entry.getValue());
				}
				// Note that we also leave the original property
				mutatedProps.put(entry.getKey(), entry.getValue());
			}
			else {
				mutatedProps.put(entry.getKey(), entry.getValue());
			}
		}
		return builder.setProperties(mutatedProps).build(original.getName());
	}

	private void assertNoAmbiguity(List<ConfigurationMetadataProperty> longForms) {
		if (longForms.size() > 1) {
			Set<String> ids = new HashSet<>(longForms.size());
			for (ConfigurationMetadataProperty pty : longForms) {
				ids.add(pty.getId());
			}
			throw new IllegalArgumentException(String.format("Ambiguous short form property '%s' could mean any of %s",
					longForms.iterator().next().getName(), ids));
		}
	}

	/**
	 * Extract and return a map of properties for a specific app within the
	 * deployment properties of a standalone application.
	 * @param appDefinition              the {@link StandaloneDefinition} for which to return a map of properties
	 * @param standaloneDeploymentProperties deployment properties for the standalone application that the app is defined in
	 * @return map of properties for an app
	 */
    private Map<String, String> extractAppDeploymentProperties(StandaloneDefinition appDefinition,
            Map<String, String> standaloneDeploymentProperties) {
        Map<String, String> appDeploymentProperties = new HashMap<>();
        String wildCardPrefix = "app.*.";
        parseAndPopulateProperties(standaloneDeploymentProperties, appDeploymentProperties, wildCardPrefix);
        String appPrefix = String.format("app.%s.", appDefinition.getName());
        parseAndPopulateProperties(standaloneDeploymentProperties, appDeploymentProperties, appPrefix);
        return appDeploymentProperties;
    }

	private void parseAndPopulateProperties(Map<String, String> standaloneDeploymentProperties,
			Map<String, String> appDeploymentProperties, String appPrefix) {
		for (Map.Entry<String, String> entry : standaloneDeploymentProperties.entrySet()) {
			if (entry.getKey().startsWith(appPrefix)) {
				appDeploymentProperties.put(entry.getKey().substring(appPrefix.length()), entry.getValue());
			}
		}
	}

	/**
	 * Undeploy the given standalone.
	 * @param standalone standalone application to undeploy
	 */
	void undeployStandalone(StandaloneDefinition standalone) {
		String key = DeploymentKey.forStandaloneAppDefinition(standalone);
		String id = this.deploymentIdRepository.findOne(key);
		// if id is null, assume nothing is deployed
		if (id != null) {
			AppStatus status = this.deployer.status(id);
			if (!EnumSet.of(DeploymentState.unknown, DeploymentState.undeployed)
					.contains(status.getState())) {
				this.deployer.undeploy(id);
			}
			this.deploymentIdRepository.delete(key);
		}
	}
}
