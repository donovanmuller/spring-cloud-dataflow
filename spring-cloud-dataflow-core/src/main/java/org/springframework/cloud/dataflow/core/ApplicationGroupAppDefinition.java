/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.core;

import org.springframework.util.Assert;

/**
 * Representation of an application group, including properties provided via the DSL
 * definition. This does not include any information required at deployment
 * time (such as the number of app instances).
 *
 */
public class ApplicationGroupAppDefinition {

	/**
	 * Name of application definition
	 */
	private final String definitionName;

	/**
	 * Name of application definition
	 */
	private final DefinitionType definitionType;

	/**
	 * Name of application group unit this app instance belongs to.
	 */
	private final String applicationGroupName;

	/**
	 * Construct a {@code ApplicationGroupAppDefinition}. This constructor is private; use
	 * {@link ApplicationGroupAppDefinition.Builder} to create a new instance.
	 *
	 * @param definitionName name of the application definition
	 * @param definitionType definition type
	 * @param applicationGroupName name of the application group this application definition belongs to
	 */
	private ApplicationGroupAppDefinition(String definitionName, DefinitionType definitionType,
			String applicationGroupName) {
		Assert.notNull(definitionName, "application definition name must not be null");
		Assert.notNull(definitionType, "definition type must not be null");
		Assert.notNull(applicationGroupName, "application group name must not be null");
		this.definitionName = definitionName;
		this.definitionType = definitionType;
		this.applicationGroupName = applicationGroupName;
	}

	/**
	 * Return name of the application group this application definition belongs to.
	 *
	 * @return application group name
	 */
	public String getApplicationGroupName() {
		return applicationGroupName;
	}

	public String getDefinitionName() {
		return definitionName;
	}

	public DefinitionType getDefinitionType() {
		return definitionType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((applicationGroupName == null) ? 0 : applicationGroupName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ApplicationGroupAppDefinition other = (ApplicationGroupAppDefinition) obj;
		if (applicationGroupName == null) {
			if (other.applicationGroupName != null)
				return false;
		} else if (!applicationGroupName.equals(other.applicationGroupName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ApplicationGroupAppDefinition [definitionName=" + definitionName
				+ ", definitionType=" + definitionType
				+ ", applicationGroupName=" + applicationGroupName + "]";
	}

	/**
	 * Builder object for {@code ApplicationGroupAppDefinition}.
	 * This object is mutable to allow for flexibility in specifying application
	 * fields/properties during parsing.
	 */
	public static class Builder {

		/**
		 * @see ApplicationGroupAppDefinition#definitionName
		 */
		private String definitionName;

		/**
		 * @see ApplicationGroupAppDefinition#definitionType
		 */
		private DefinitionType definitionType;

		/**
		 * @see ApplicationGroupAppDefinition#applicationGroupName
		 */
		private String applicationGroupName;

		/**
		 * Create a new builder that is initialized with properties of the given definition.
		 * Useful for "mutating" a definition by building a slightly different copy.
		 */
		public static Builder from(ApplicationGroupAppDefinition definition) {
			Builder builder = new Builder();
			builder.setDefinitionName(definition.getDefinitionName())
				.setDefinitionType(definition.getDefinitionType())
				.setApplicationGroupName(definition.getApplicationGroupName());
			return builder;
		}

		public String getDefinitionName() {
			return definitionName;
		}

		public Builder setDefinitionName(final String definitionName) {
			this.definitionName = definitionName;
			return this;
		}

		public DefinitionType getDefinitionType() {
			return definitionType;
		}

		public Builder setDefinitionType(final DefinitionType definitionType) {
			this.definitionType = definitionType;
			return this;
		}

		public Builder setDefinitionType(final String definitionType) {
			this.setDefinitionType(DefinitionType.valueOf(definitionType));
			return this;
		}

		/**
		 * Set the application group name this app belongs to.
		 *
		 * @param applicationGroupName name
		 * @return this builder object
		 *
		 * @see ApplicationGroupAppDefinition#applicationGroupName
		 */
		public Builder setApplicationGroupName(String applicationGroupName) {
			this.applicationGroupName = applicationGroupName;
			return this;
		}

		/**
		 * Return name of application group this app definition belongs to.
		 *
		 * @return application group name
		 */
		public String getApplicationGroupName() {
			return applicationGroupName;
		}

		/**
		 * Return a new instance of {@link ApplicationGroupAppDefinition}.
		 *
		 * @return new instance of {@code ApplicationGroupAppDefinition}
		 */
		public ApplicationGroupAppDefinition build() {
			return new ApplicationGroupAppDefinition(this.definitionName,
					this.definitionType, applicationGroupName);
		}
	}

}
