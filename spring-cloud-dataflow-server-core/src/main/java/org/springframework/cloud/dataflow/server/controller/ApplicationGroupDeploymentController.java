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

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.ApplicationGroupAppDefinition;
import org.springframework.cloud.dataflow.core.ApplicationGroupDefinition;
import org.springframework.cloud.dataflow.core.StandaloneDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.rest.resource.ApplicationGroupDeploymentResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.repository.ApplicationGroupDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.NoSuchApplicationGroupDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StandaloneDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for deployment operations on {@link ApplicationGroupDefinition}.
 *
 */
@RestController
@RequestMapping("/application-groups/deployments")
@ExposesResourceFor(ApplicationGroupDeploymentResource.class)
public class ApplicationGroupDeploymentController {

	private static Log logger = LogFactory.getLog(ApplicationGroupDeploymentController.class);

	/**
	 * The repository this controller will use for application group CRUD operations.
	 */
	private final ApplicationGroupDefinitionRepository repository;

	private final StreamDefinitionRepository streamDefinitionRepository;

	private final TaskDefinitionRepository taskDefinitionRepository;

	private final StandaloneDefinitionRepository standaloneDefinitionRepository;

	/**
	 * The repository this controller will use for deployment IDs.
	 */
	private final DeploymentIdRepository deploymentIdRepository;

	/**
	 * The app registry this controller will use to lookup apps.
	 */
	private final AppRegistry registry;

	/**
	 * The deployer this controller will use to deploy stream apps.
	 */
	private final AppDeployer deployer;

	private final ApplicationConfigurationMetadataResolver metadataResolver;

	/**
	 * General properties to be applied to applications on deployment.
	 */
	private final CommonApplicationProperties commonApplicationProperties;

	private final StreamDeploymentController streamDeploymentController;

	private final TaskDeploymentController taskDeploymentController;

	private final StandaloneDeploymentController standaloneDeploymentController;

	/**
	 * Create a {@code StandaloneDeploymentController} that delegates
	 * <ul>
	 * <li>CRUD operations to the provided {@link ApplicationGroupDefinitionRepository}</li>
	 * <li>app retrieval to the provided {@link AppRegistry}</li>
	 * <li>deployment operations to the provided {@link AppDeployer}</li>
	 * </ul>
	 * @param repository             the repository this controller will use for application group CRUD operations
	 * @param deploymentIdRepository the repository this controller will use for deployment IDs
	 * @param registry               the registry this controller will use to lookup apps
	 * @param deployer               the deployer this controller will use to deploy standalone apps
	 * @param commonProperties       common set of application properties
	 * @param streamDeploymentController
	 * @param taskDeploymentController
	 * @param standaloneDeploymentController
	 */
    public ApplicationGroupDeploymentController(ApplicationGroupDefinitionRepository repository,
            StreamDefinitionRepository streamDefinitionRepository,
            TaskDefinitionRepository taskDefinitionRepository,
            StandaloneDefinitionRepository standaloneDefinitionRepository,
            DeploymentIdRepository deploymentIdRepository,
            AppRegistry registry,
            AppDeployer deployer,
            ApplicationConfigurationMetadataResolver metadataResolver,
            CommonApplicationProperties commonProperties,
            StreamDeploymentController streamDeploymentController,
            TaskDeploymentController taskDeploymentController,
            StandaloneDeploymentController standaloneDeploymentController) {
        Assert.notNull(repository, "ApplicationGroupDefinitionRepository must not be null");
        Assert.notNull(deploymentIdRepository, "DeploymentIdRepository must not be null");
        Assert.notNull(registry, "AppRegistry must not be null");
        Assert.notNull(deployer, "AppDeployer must not be null");
        Assert.notNull(metadataResolver, "MetadataResolver must not be null");
        Assert.notNull(commonProperties, "CommonApplicationProperties must not be null");
        this.repository = repository;
        this.streamDefinitionRepository = streamDefinitionRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.standaloneDefinitionRepository = standaloneDefinitionRepository;
        this.deploymentIdRepository = deploymentIdRepository;
        this.registry = registry;
        this.deployer = deployer;
        this.metadataResolver = metadataResolver;
        this.commonApplicationProperties = commonProperties;
        this.streamDeploymentController = streamDeploymentController;
        this.taskDeploymentController = taskDeploymentController;
        this.standaloneDeploymentController = standaloneDeploymentController;
    }

	/**
	 * Request un-deployment of an existing application group.
	 * @param name the name of an existing application group (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void undeploy(@PathVariable("name") String name) {
		ApplicationGroupDefinition applicationGroup = this.repository.findOne(name);
		if (applicationGroup == null) {
			throw new NoSuchApplicationGroupDefinitionException(name);
		}
		undeployApplicationGroup(applicationGroup);
	}

//	/**
//	 * Request un-deployment of all streams.
//	 */
//	@RequestMapping(value = "", method = RequestMethod.DELETE)
//	@ResponseStatus(HttpStatus.OK)
//	public void undeployAll() {
//		for (StreamDefinition stream : this.repository.findAll()) {
//			this.undeployStream(stream);
//		}
//	}

	/**
	 * Request deployment of an existing application group definition.
	 * @param name       the name of an existing application group definition (required)
	 * @param properties the deployment properties for the application group as a comma-delimited list of key=value pairs
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void deployApplicationGroup(@PathVariable("name") String name,
									   @RequestParam(required = false) String properties) {
		ApplicationGroupDefinition applicationGroup = this.repository.findOne(name);
		if (applicationGroup == null) {
			throw new NoSuchApplicationGroupDefinitionException(name);
		}
		String status = calculateApplicationGroupState(name);
		if (DeploymentState.deployed.equals(DeploymentState.valueOf(status))) {
			throw new ApplicationGroupAlreadyDeployedException(name);
		}
		else if (DeploymentState.deploying.equals(DeploymentState.valueOf(status))) {
			throw new ApplicationGroupAlreadyDeployedException(name);
		}
		deployApplicationGroup(applicationGroup, DeploymentPropertiesUtils.parse(properties));
	}

	String calculateApplicationGroupState(String name) {
		Set<DeploymentState> appStates = EnumSet.noneOf(DeploymentState.class);
		ApplicationGroupDefinition applicationGroup = this.repository.findOne(name);
		for (ApplicationGroupAppDefinition appDefinition : applicationGroup.getAppDefinitions()) {
			switch (appDefinition.getDefinitionType()) {
				case stream: {
					appStates.add(DeploymentState.valueOf(
							streamDeploymentController.calculateStreamState(appDefinition.getDefinitionName())));
				} break;
				case task: {
					// TODO
				} break;
				case standalone: {
					appStates.add(DeploymentState.valueOf(
							standaloneDeploymentController.calculateStandaloneState(appDefinition.getDefinitionName())));
				} break;
			}
		}

		return aggregateState(appStates).toString();
	}

	/**
	 * Aggregate the set of app states into a single state for an application group.
	 * @param states set of states for apps of an application group
	 * @return application group state based on app states
	 */
	DeploymentState aggregateState(Set<DeploymentState> states) {
		if (states.size() == 1) {
			DeploymentState state = states.iterator().next();
			return state == DeploymentState.unknown ? DeploymentState.undeployed : state;
		}
		if (states.isEmpty() || states.contains(DeploymentState.error)) {
			return DeploymentState.error;
		}
		if (states.contains(DeploymentState.failed)) {
			return DeploymentState.failed;
		}
		if (states.contains(DeploymentState.deploying)) {
			return DeploymentState.deploying;
		}

		return DeploymentState.partial;
	}

	/**
	 * Deploy a application group as defined by its {@link ApplicationGroupDefinition} and optional deployment properties.
	 * @param applicationGroup the application group definition to deploy
	 * @param deploymentProperties the deployment properties for the applications in this application group
	 */
	private void deployApplicationGroup(ApplicationGroupDefinition applicationGroup, Map<String, String> deploymentProperties) {
		for (ApplicationGroupAppDefinition appDefinition : applicationGroup.getAppDefinitions()) {
			switch (appDefinition.getDefinitionType()) {
				case stream: {
					StreamDefinition streamDefinition = streamDefinitionRepository.findOne(appDefinition.getDefinitionName());
					streamDeploymentController.deployStream(streamDefinition, deploymentProperties);
				} break;
				case task: {
					// TODO does it make sense to "deploy" a task? Probably not.
				} break;
				case standalone: {
					StandaloneDefinition standaloneDefinition = standaloneDefinitionRepository.findOne(appDefinition.getDefinitionName());
					standaloneDeploymentController.deployStandalone(standaloneDefinition, deploymentProperties);
				} break;
			}
		}

		this.deploymentIdRepository.save(applicationGroup.getName(), UUID.randomUUID().toString());
	}

	/**
	 * Undeploy the given application group.
	 * @param applicationGroup application group to undeploy
	 */
	private void undeployApplicationGroup(ApplicationGroupDefinition applicationGroup) {
		String id = this.deploymentIdRepository.findOne(applicationGroup.getName());
		// if id is null, assume nothing is deployed
		if (id != null) {
			for (ApplicationGroupAppDefinition appDefinition : applicationGroup.getAppDefinitions()) {
				switch (appDefinition.getDefinitionType()) {
					case stream: {
						StreamDefinition streamDefinition = streamDefinitionRepository.findOne(appDefinition.getDefinitionName());
						streamDeploymentController.undeployStream(streamDefinition);
					} break;
					case task: {
					} break;
					case standalone: {
						StandaloneDefinition standaloneDefinition = standaloneDefinitionRepository.findOne(appDefinition.getDefinitionName());
						standaloneDeploymentController.undeployStandalone(standaloneDefinition);
					} break;
				}
			}

			this.deploymentIdRepository.delete(applicationGroup.getName());
		}
	}

}
