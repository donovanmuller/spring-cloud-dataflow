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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.dataflow.core.ApplicationGroupAppDefinition;
import org.springframework.cloud.dataflow.core.ApplicationGroupDefinition;
import org.springframework.cloud.dataflow.core.StandaloneDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.rest.resource.ApplicationGroupDefinitionResource;
import org.springframework.cloud.dataflow.server.repository.ApplicationGroupDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.NoSuchApplicationGroupDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StandaloneDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.impl.ApplicationGroupDescriptor;
import org.springframework.cloud.dataflow.server.service.impl.ApplicationGroupRegistryService;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
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
 * Controller for operations on {@link ApplicationGroupDefinition}. This
 * includes CRUD and optional deployment operations.
 *
 */
@RestController
@RequestMapping("/application-groups/definitions")
@ExposesResourceFor(ApplicationGroupDefinitionResource.class)
public class ApplicationGroupDefinitionController {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationGroupDefinitionController.class);

	/**
	 * The repository this controller will use for application group CRUD operations.
	 */
	private final ApplicationGroupDefinitionRepository repository;

	private final StreamDefinitionRepository streamDefinitionRepository;

	private final TaskDefinitionRepository taskDefinitionRepository;

	private final StandaloneDefinitionRepository standaloneDefinitionRepository;

	/**
	 * Assembler for {@link ApplicationGroupDefinitionResource} objects.
	 */
	private final Assembler assembler = new Assembler();

	/**
	 * This deployment controller is used as a delegate when the application group creation is immediately followed by deployment.
	 */
	private final ApplicationGroupDeploymentController deploymentController;

	private final ResourceLoader resourceLoader;

	private final ApplicationGroupRegistryService applicationGroupRegistryService;

	/**
	 * Create a {@code ApplicationGroupDefinitionController} that delegates
	 * <ul>
	 * <li>CRUD operations to the provided {@link ApplicationGroupDefinitionRepository}</li>
	 * <li>Stream CRUD operations to the provided {@link StreamDefinitionRepository}</li>
	 * <li>Task CRUD operations to the provided {@link TaskDefinitionRepository}</li>
	 * <li>Standalone deployment ID operations to the provided {@link StandaloneDefinitionRepository}</li>
	 * <li>deployment operations to the provided {@link ApplicationGroupDeploymentController}</li>
	 * <li>resourceLoader resourceLoader to validate Maven artifacts
	 * <li>service to facilitate application group registration via {@link ApplicationGroupRegistryService}</li>
	 * </ul>
	 * @param repository           the repository this controller will use for application group CRUD operations
	 * @param streamDefinitionRepository the stream repository this controller will use for application group CRUD operations
	 * @param taskDefinitionRepository the task repository this controller will use for application group CRUD operations
	 * @param standaloneDefinitionRepository the standalone repository this controller will use for application group CRUD operations
	 * @param deploymentController the deployment controller to delegate deployment operations
	 * @param resourceLoader resourceLoader to validate Maven artifacts
	 * @param applicationGroupRegistryService service to facilitate application group registration
	 */
	public ApplicationGroupDefinitionController(
			ApplicationGroupDefinitionRepository repository,
			StreamDefinitionRepository streamDefinitionRepository,
			TaskDefinitionRepository taskDefinitionRepository,
			StandaloneDefinitionRepository standaloneDefinitionRepository,
			ApplicationGroupDeploymentController deploymentController,
			ResourceLoader resourceLoader,
			ApplicationGroupRegistryService applicationGroupRegistryService) {
		Assert.notNull(repository, "StandaloneDefinitionRepository must not be null");
		Assert.notNull(streamDefinitionRepository, "StreamDefinitionRepository must not be null");
		Assert.notNull(taskDefinitionRepository, "TaskDefinitionRepository must not be null");
		Assert.notNull(standaloneDefinitionRepository, "StandaloneDefinitionRepository must not be null");
		Assert.notNull(deploymentController, "StandaloneDeploymentController must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		Assert.notNull(applicationGroupRegistryService,	"ApplicationGroupRegistryService must not be null");
		this.repository = repository;
		this.streamDefinitionRepository = streamDefinitionRepository;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.standaloneDefinitionRepository = standaloneDefinitionRepository;
		this.deploymentController = deploymentController;
		this.resourceLoader = resourceLoader;
		this.applicationGroupRegistryService = applicationGroupRegistryService;
	}

	/**
	 * Return a page-able list of {@link ApplicationGroupDefinitionResource} defined application groups.
	 * @param pageable  page-able collection of {@code ApplicationGroupDefinitionResource}s.
	 * @param assembler assembler for {@link ApplicationGroupDefinition}
	 * @return list of application group definitions
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<ApplicationGroupDefinitionResource> list(Pageable pageable,
			PagedResourcesAssembler<ApplicationGroupDefinition> assembler) {
		return assembler.toResource(repository.findAll(pageable), this.assembler);
	}

	/**
	 * Create a new application group.
	 *
	 * @param name   application group name
	 * @param uri    the URI of a Maven resource that contains a application group descriptor
	 * @param dsl    DSL definition for an application group
	 * @param deploy if {@code true}, the application group is deployed upon creation (default is {@code false})
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void save(@RequestParam("name") String name,
					 @RequestParam(value = "uri", required = false) String uri,
					 @RequestParam(value = "definition", required = false) String dsl,
					 @RequestParam(value = "force", defaultValue = "false") boolean force,
					 @RequestParam(value = "deploy", defaultValue = "false") boolean deploy,
					 @RequestParam(required = false) String properties) {
		if (repository.exists(name) && force) {
			update(name, uri, dsl, deploy, properties);
		} else {
			List<String> errorMessages = new ArrayList<>();
			if (StringUtils.hasText(uri)) {
				Resource resource = this.resourceLoader.getResource(uri);
				if (!(resource instanceof MavenResource)) {
					throw new IllegalArgumentException(String.format("uri '%s' does not reference a valid Maven resource. " +
									"Only application group definitions from maven resources supported.",
							uri));
				}
				try {
					ApplicationGroupDescriptor descriptor = applicationGroupRegistryService
							.extractAndParseDescriptor((MavenResource) resource);
					applicationGroupRegistryService.registerAndCreateApps(name, descriptor, force);
					dsl = descriptor.toDsl();
				} catch (Exception e) {
					throw new IllegalArgumentException(String.format("Application group URI '%s' could not be extracted and parsed '%s'",
							uri, e));
				}
			}
			ApplicationGroupDefinition applicationGroup = new ApplicationGroupDefinition(name, dsl);
			for (ApplicationGroupAppDefinition appDefinition : applicationGroup.getAppDefinitions()) {
				switch (appDefinition.getDefinitionType()) {
					case stream: {
						if (streamDefinitionRepository.findOne(appDefinition.getDefinitionName()) == null) {
							errorMessages.add(String.format("Stream definition '%s' does not exist.",
									appDefinition.getDefinitionName()));
						}
					}
					break;
					case task: {
						if (taskDefinitionRepository.findOne(appDefinition.getDefinitionName()) == null) {
							errorMessages.add(String.format("Task definition '%s' does not exist.",
									appDefinition.getDefinitionName()));
						}
					}
					break;
					case standalone: {
						if (standaloneDefinitionRepository.findOne(appDefinition.getDefinitionName()) == null) {
							errorMessages.add(String.format("Standalone definition '%s' does not exist.",
									appDefinition.getDefinitionName()));
						}
					}
					break;
				}
			}
			if (!errorMessages.isEmpty()) {
				throw new IllegalArgumentException(StringUtils.collectionToDelimitedString(errorMessages, System.lineSeparator()));
			}
			this.repository.save(applicationGroup);
			if (deploy) {
				deploymentController.deploy(name, properties);
			}
		}
	}

	/**
	 * Update an existing application group.
	 *
	 * @param name   	existing application group name
	 * @param dsl    	new DSL definition for application group
	 * @param redeploy 	if {@code true}, the application group is deployed/undeployed, then redeploy it
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.PUT)
	@ResponseStatus(HttpStatus.OK)
	public void update(@PathVariable("name") String name,
			@RequestParam(value = "uri", required = false) String uri,
			@RequestParam("definition") String dsl,
			@RequestParam(value = "redeploy", defaultValue = "false") boolean redeploy,
			@RequestParam(required = false) String properties) {
		if (!repository.exists(name)) {
			throw new NoSuchApplicationGroupDefinitionException(name,
					String.format("An existing application group '%s' is not defined.", name));
		}
		if (StringUtils.hasText(uri)) {
			Resource resource = this.resourceLoader.getResource(uri);
			if (!(resource instanceof MavenResource)) {
				throw new IllegalArgumentException(String.format("uri '%s' does not reference a valid Maven resource. " +
								"Only application group definitions from maven resources supported.",
						uri));
			}
			try {
				ApplicationGroupDescriptor descriptor = applicationGroupRegistryService
						.extractAndParseDescriptor((MavenResource) resource);
				applicationGroupRegistryService.registerAndUpdateApps(name, descriptor);
				dsl = descriptor.toDsl();
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format("Application group URI '%s' could not be extracted and parsed '%s'",
						uri, e));
			}
		}
		ApplicationGroupDefinition applicationGroup = new ApplicationGroupDefinition(name, dsl);
		this.repository.update(applicationGroup);
		if (redeploy) {
			deploymentController.redeploy(name, properties);
		}
	}

	/**
	 * Request removal of an existing application group definition.
	 * @param name the name of an existing application group definition (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable("name") String name) {
		ApplicationGroupDefinition applicationGroup = repository.findOne(name);
		if (applicationGroup == null) {
			throw new NoSuchApplicationGroupDefinitionException(name);
		}
		deploymentController.undeploy(name);
		for (ApplicationGroupAppDefinition appDefinition : applicationGroup.getAppDefinitions()) {
			switch (appDefinition.getDefinitionType()) {
				case stream: {
					StreamDefinition streamDefinition = streamDefinitionRepository.findOne(appDefinition.getDefinitionName());
					streamDefinitionRepository.delete(streamDefinition);
				} break;
				case task: {
				} break;
				case standalone: {
					StandaloneDefinition standaloneDefinition = standaloneDefinitionRepository.findOne(appDefinition.getDefinitionName());
					standaloneDefinitionRepository.delete(standaloneDefinition);
				} break;
			}
		}
		this.repository.delete(name);
	}

	/**
	 * Return a given application group definition resource.
	 * @param name the name of an existing application group definition (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public ApplicationGroupDefinitionResource display(@PathVariable("name") String name) {
		ApplicationGroupDefinition definition = repository.findOne(name);
		if (definition == null) {
			throw new NoSuchApplicationGroupDefinitionException(name);
		}
		return assembler.toResource(definition);
	}

	/**
	 * Request removal of all application group definitions.
	 */
	@RequestMapping(value = "", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void deleteAll() throws Exception {
		deploymentController.undeployAll();
		Iterable<ApplicationGroupDefinition> applicationGroups = this.repository.findAll();
		for (ApplicationGroupDefinition applicationGroup : applicationGroups) {
			delete(applicationGroup.getName());
		}
	}

	/**
	 * Return a string that describes the state of the given application group.
	 * @param name name of application group to determine state for
	 * @return application group state
	 * @see DeploymentState
	 */
	private String calculateApplicationGroupState(String name) {
		return deploymentController.calculateApplicationGroupState(name);
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation
	 * that converts {@link ApplicationGroupDefinition}s to {@link ApplicationGroupDefinitionResource}s.
	 */
	class Assembler extends ResourceAssemblerSupport<ApplicationGroupDefinition, ApplicationGroupDefinitionResource> {

		public Assembler() {
			super(ApplicationGroupDefinitionController.class, ApplicationGroupDefinitionResource.class);
		}

		@Override
		public ApplicationGroupDefinitionResource toResource(ApplicationGroupDefinition applicationGroup) {
			return createResourceWithId(applicationGroup.getName(), applicationGroup);
		}

		@Override
		public ApplicationGroupDefinitionResource instantiateResource(ApplicationGroupDefinition standalone) {
			ApplicationGroupDefinitionResource resource = new ApplicationGroupDefinitionResource(standalone.getName(), standalone.getDslText());
			resource.setStatus(calculateApplicationGroupState(standalone.getName()));
			return resource;
		}
	}

}
