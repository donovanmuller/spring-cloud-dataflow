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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StandaloneDefinition;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.rest.resource.StandaloneDefinitionResource;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.DeploymentKey;
import org.springframework.cloud.dataflow.server.repository.NoSuchStandaloneDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StandaloneDefinitionRepository;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for operations on {@link StandaloneDefinition}. This
 * includes CRUD and optional deployment operations.
 *
 */
@RestController
@RequestMapping("/standalone/definitions")
@ExposesResourceFor(StandaloneDefinitionResource.class)
public class StandaloneDefinitionController {

	private static final Logger logger = LoggerFactory.getLogger(StandaloneDefinitionController.class);

	/**
	 * The repository this controller will use for standalone CRUD operations.
	 */
	private final StandaloneDefinitionRepository repository;

	/**
	 * The repository this controller will use for deployment IDs.
	 */
	private final DeploymentIdRepository deploymentIdRepository;

	/**
	 * This deployment controller is used as a delegate when standalone application creation is immediately followed by deployment.
	 */
	private final StandaloneDeploymentController deploymentController;

	/**
	 * The deployer this controller will use to compute standalone application deployment status.
	 */
	private final AppDeployer deployer;

	/**
	 * The app registry this controller will use to lookup apps.
	 */
	private final AppRegistry appRegistry;

	/**
	 * Assembler for {@link StandaloneDefinitionResource} objects.
	 */
	private final Assembler standaloneDefinitionAssembler = new Assembler();

	/**
	 * Create a {@code StandaloneDefinitionController} that delegates
	 * <ul>
	 * <li>CRUD operations to the provided {@link StandaloneDefinitionRepository}</li>
	 * <li>deployment ID operations to the provided {@link DeploymentIdRepository}</li>
	 * <li>deployment operations to the provided {@link StandaloneDefinitionRepository}</li>
	 * <li>deployment status computation to the provided {@link AppDeployer}</li>
	 * </ul>
	 * @param repository           the repository this controller will use for standalone application CRUD operations
	 * @param deploymentIdRepository the repository this controller will use for deployment IDs
	 * @param deploymentController the deployment controller to delegate deployment operations
	 * @param deployer             the deployer this controller will use to compute deployment status
	 * @param appRegistry          the app registry to look up registered apps
	 */
    public StandaloneDefinitionController(StandaloneDefinitionRepository repository,
            DeploymentIdRepository deploymentIdRepository,
            StandaloneDeploymentController deploymentController,
            AppDeployer deployer,
            AppRegistry appRegistry) {
        Assert.notNull(repository, "StandaloneDefinitionRepository must not be null");
        Assert.notNull(deploymentIdRepository, "DeploymentIdRepository must not be null");
        Assert.notNull(deploymentController, "StandaloneDeploymentController must not be null");
        Assert.notNull(deployer, "AppDeployer must not be null");
        Assert.notNull(appRegistry, "AppRegistry must not be null");
        this.deploymentController = deploymentController;
        this.deploymentIdRepository = deploymentIdRepository;
        this.repository = repository;
        this.deployer = deployer;
        this.appRegistry = appRegistry;
    }

	/**
	 * Return a page-able list of {@link StandaloneDefinitionResource} defined standalone applications.
	 * @param pageable  page-able collection of {@code StandaloneDefinitionResource}s.
	 * @param assembler assembler for {@link StandaloneDefinition}
	 * @return list of standalone application definitions
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<StandaloneDefinitionResource> list(Pageable pageable,
			PagedResourcesAssembler<StandaloneDefinition> assembler) {
		return assembler.toResource(repository.findAll(pageable), standaloneDefinitionAssembler);
	}

	/**
	 * Create a new standalone application.
	 *
	 * @param name   standalone application name
	 * @param dsl    DSL definition for standalone application
	 * @param deploy if {@code true}, the standalone application is deployed upon creation (default is {@code false})
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void save(@RequestParam("name") String name,
					 @RequestParam("definition") String dsl,
					 @RequestParam(value = "force", defaultValue = "false") boolean force,
					 @RequestParam(value = "deploy", defaultValue = "false") boolean deploy) {
		StandaloneDefinition standalone = new StandaloneDefinition(name, dsl);
		List<String> errorMessages = new ArrayList<>();
		ApplicationType appType = ApplicationType.standalone;
		if (appRegistry.find(standalone.getName(), appType) == null) {
			errorMessages.add(String.format("Application name '%s' with type '%s' does not exist in the app registry.",
					name, appType));
		}
		if (!errorMessages.isEmpty()) {
			throw new IllegalArgumentException(StringUtils.collectionToDelimitedString(errorMessages, System.lineSeparator()));
		}
		this.repository.save(standalone);
		if (deploy) {
			deploymentController.deploy(name, null);
		}
	}

	/**
	 * Request removal of an existing standalone application definition.
	 * @param name the name of an existing standalone application definition (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable("name") String name) {
		if (repository.findOne(name) == null) {
			throw new NoSuchStandaloneDefinitionException(name);
		}
		deploymentController.undeploy(name);
		this.repository.delete(name);
	}

	/**
	 * Return a given standalone application definition resource.
	 * @param name the name of an existing standalone application definition (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public StandaloneDefinitionResource display(@PathVariable("name") String name) {
		StandaloneDefinition definition = repository.findOne(name);
		if (definition == null) {
			throw new NoSuchStandaloneDefinitionException(name);
		}
		return standaloneDefinitionAssembler.toResource(definition);
	}

	/**
	 * Request removal of all standalone application definitions.
	 */
	@RequestMapping(value = "", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void deleteAll() throws Exception {
		deploymentController.undeployAll();
		this.repository.deleteAll();
	}

	/**
	 * Return a string that describes the state of the given standalone application.
	 * @param name name of standalone application to determine state for
	 * @return standalone application state
	 * @see DeploymentState
	 */
	private String calculateStandaloneState(String name) {
		Set<DeploymentState> appStates = EnumSet.noneOf(DeploymentState.class);
		StandaloneDefinition standalone = repository.findOne(name);
		String key = DeploymentKey.forStandaloneAppDefinition(standalone);
		String id = deploymentIdRepository.findOne(key);
		if (id != null) {
			AppStatus status = deployer.status(id);
			appStates.add(status.getState());
		}
		else {
			appStates.add(DeploymentState.undeployed);
		}

		logger.debug("Application states for standalone application {}: {}", name, appStates);
		return aggregateState(appStates).toString();
	}

	/**
	 * Aggregate the set of app states into a single state for a standalone application.
	 * @param states set of states for apps of a standalone application
	 * @return standalone application state based on app states
	 */
	static DeploymentState aggregateState(Set<DeploymentState> states) {
		if (states.size() == 1) {
			DeploymentState state = states.iterator().next();

			// a standalone application which is known to the standalone application definition repository
			// but unknown to deployers is undeployed
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
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation
	 * that converts {@link StandaloneDefinition}s to {@link StandaloneDefinitionResource}s.
	 */
	class Assembler extends ResourceAssemblerSupport<StandaloneDefinition, StandaloneDefinitionResource> {

		public Assembler() {
			super(StandaloneDefinitionController.class, StandaloneDefinitionResource.class);
		}

		@Override
		public StandaloneDefinitionResource toResource(StandaloneDefinition standalone) {
			return createResourceWithId(standalone.getRegisteredAppName(), standalone);
		}

		@Override
		public StandaloneDefinitionResource instantiateResource(StandaloneDefinition standalone) {
			StandaloneDefinitionResource resource = new StandaloneDefinitionResource(standalone.getRegisteredAppName(), standalone.getDslText());
			resource.setStatus(calculateStandaloneState(standalone.getRegisteredAppName()));
			return resource;
		}
	}

}
