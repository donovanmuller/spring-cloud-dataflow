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

package org.springframework.cloud.dataflow.core;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.springframework.cloud.dataflow.core.dsl.AppNode;
import org.springframework.cloud.dataflow.core.dsl.ApplicationGroupNode;
import org.springframework.cloud.dataflow.core.dsl.ApplicationGroupParser;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

/**
 * Representation of a defined application group. An application group consists of an
 * list of streams, tasks or standalone apps.
 * <p>
 * This application group definition does not include any deployment
 * or runtime configuration for a application group.
 *
 * @see ApplicationGroupDefinition
 *
 */
public class ApplicationGroupDefinition {

	/**
	 * Name of application group.
	 */
	private final String name;

	/**
	 * DSL definition for application group.
	 */
	private final String dslText;

	/**
	 * List of {@link ApplicationGroupAppDefinition}s comprising this application group.
	 */
	private final List<ApplicationGroupAppDefinition> applicationGroupAppDefinitions;

	/**
	 * Construct a {@code ApplicationGroupDefinition}.
	 *
	 * @param name     name of application group
	 * @param dslText  DSL definition for application group
	 */
	public ApplicationGroupDefinition(String name, String dslText) {
		Assert.hasText(name, "name is required");
		Assert.hasText(dslText, "dslText is required");
		this.name = name;
		this.dslText = dslText;
		this.applicationGroupAppDefinitions = new LinkedList<>();
		ApplicationGroupNode applicationGroupNode = new ApplicationGroupParser(name, dslText).parse();
		for (AppNode definition : applicationGroupNode.getAppNodes()) {
			ApplicationGroupAppDefinition.Builder builder = new ApplicationGroupAppDefinition.Builder();
			builder.setDefinitionName(definition.getName())
					.setDefinitionType(definition.getLabelName())
					.setApplicationGroupName(name);
			this.applicationGroupAppDefinitions.add(builder.build());
		}
	}

	/**
	 * Return the name of this application group.
	 *
	 * @return application group name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the DSL definition for this application group.
	 *
	 * @return application group definition DSL
	 */
	public String getDslText() {
		return dslText;
	}

	/**
	 * Return the ordered list of application definitions for this application group as a {@link List}.
	 * This allows for retrieval of application definitions in the application group by index.
	 *
	 * @return list of application definitions for this application group definition
	 */
	public List<ApplicationGroupAppDefinition> getAppDefinitions() {
		return Collections.unmodifiableList(this.applicationGroupAppDefinitions);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dslText == null) ? 0 : dslText.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ApplicationGroupDefinition other = (ApplicationGroupDefinition) obj;
		if (dslText == null) {
			if (other.dslText != null)
				return false;
		} else if (!dslText.equals(other.dslText))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("name", this.name)
				.append("definition", this.dslText)
				.toString();
	}


	/**
	 * Iterator that prevents mutation of its backing data structure.
	 *
	 * @param <T> the type of elements returned by this iterator
	 */
	private static class ReadOnlyIterator<T> implements Iterator<T> {
		private final Iterator<T> wrapped;

		public ReadOnlyIterator(Iterator<T> wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public boolean hasNext() {
			return wrapped.hasNext();
		}

		@Override
		public T next() {
			return wrapped.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

}
