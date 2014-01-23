/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.roboconf.core.model.parsing;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Group common code for facet and component instructions.
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractPropertiesHolder extends AbstractRegion {

	/**
	 * The element's name.
	 */
	private String name;

	/**
	 * The in-line comment after a closing bracket.
	 */
	private String closingInlineComment;

	/**
	 * The instructions within this element.
	 */
	private final Collection<AbstractRegion> internalInstructions = new ArrayList<AbstractRegion> ();


	/**
	 * Constructor.
	 * @param declaringFile not null
	 */
	public AbstractPropertiesHolder( FileDefinition declaringFile ) {
		super( declaringFile );
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName( String name ) {
		this.name = name;
	}

	/**
	 * @return the closingInlineComment
	 */
	public String getClosingInlineComment() {
		return this.closingInlineComment;
	}

	/**
	 * @param closingInlineComment the closingInlineComment to set
	 */
	public void setClosingInlineComment( String closingInlineComment ) {
		this.closingInlineComment = closingInlineComment;
	}

	/**
	 * Finds a property by its name among the one this instance owns.
	 * @param propertyName not null
	 * @return the associated property, or null if it was not found
	 */
	public RegionProperty findPropertyByName( String propertyName ) {

		RegionProperty result = null;
		for( AbstractRegion region : this.internalInstructions ) {
			if( region.getInstructionType() == PROPERTY
					&& propertyName.equals(((RegionProperty) region).getName())) {
				result = (RegionProperty) region;
				break;
			}
		}

		return result;
	}

	/**
	 * @return the internalInstructions
	 */
	public Collection<AbstractRegion> getInternalInstructions() {
		return this.internalInstructions;
	}

	/**
	 * @return the supported properties (not null)
	 * @see Constants
	 */
	public abstract String[] getSupportedPropertyNames();
}
