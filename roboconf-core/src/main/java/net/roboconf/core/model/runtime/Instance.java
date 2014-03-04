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

package net.roboconf.core.model.runtime;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;

import net.roboconf.core.model.helpers.InstanceHelpers;

/**
 * An instance object represents a running component instance.
 * @author Vincent Zurczak - Linagora
 */
public class Instance implements Serializable {

	/**
	 * A constant to store the IP address in {@link #getData()}.
	 * <p>Storing this information in a root instance is enough.</p>
	 */
	public static final String IP_ADDRESS = "ip.address";

	/**
	 * A constant to store the machine ID in {@link #getData()}.
	 * <p>Storing this information in a root instance is enough.</p>
	 */
	public static final String MACHINE_ID = "machine.id";



	private static final long serialVersionUID = -3320865356277185064L;

	private String name, channel;
	private Component component;
	private transient Instance parent;
	private final transient Collection<Instance> children = new LinkedHashSet<Instance> ();

	private InstanceStatus status = InstanceStatus.NOT_DEPLOYED;
	private final Map<String,String> data = new LinkedHashMap<String,String>( 0 );
	private final Map<String,String> overridenExports = new HashMap<String,String> ();

	// FIXME: change the structure of this?
	private final Map<String,Collection<Import>> parameters = new HashMap<String,Collection<Import>> ();


	/**
	 * Constructor.
	 */
	public Instance() {
		// nothing
	}

	/**
	 * Constructor.
	 * @param name the instance name
	 */
	public Instance( String name ) {
		this.name = name;
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
	 * @return the channel
	 */
	public String getChannel() {
		return this.channel;
	}

	/**
	 * @param channel the channel to set
	 */
	public void setChannel( String channel ) {
		this.channel = channel;
	}

	/**
	 * @return the status
	 */
	public InstanceStatus getStatus() {
		return this.status;
	}

	/**
	 * @param status the status to set
	 */
	public synchronized void setStatus( InstanceStatus status ) {
		this.status = status;
	}

	/**
	 * @return the component
	 */
	public Component getComponent() {
		return this.component;
	}

	/**
	 * @param component the component to set
	 */
	public void setComponent( Component component ) {
		this.component = component;
	}

	/**
	 * @return the parent
	 */
	public Instance getParent() {
		return this.parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent( Instance parent ) {
		this.parent = parent;
	}

	/**
	 * @return the children
	 */
	public Collection<Instance> getChildren() {
		return this.children;
	}

	/**
	 * @return the data
	 */
	public Map<String, String> getData() {
		return this.data;
	}

	/**
	 * @return the overridenExports
	 */
	public Map<String,String> getOverriddenExports() {
		return this.overridenExports;
	}

	@Override
	public int hashCode() {
		return InstanceHelpers.computeInstancePath( this ).hashCode();
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof Instance
				&& InstanceHelpers.haveSamePath( this, (Instance) obj);
	}

	@Override
	public String toString() {
		return getName();
	}


	//

	/**
	 * Updates the imports with new values.
	 * @param newImports the new imports (can be null)
	 */
	public void updateImports( Map<String,Collection<Import>> newImports ) {
		this.parameters.clear();
		if( newImports != null )
			this.parameters.putAll( newImports );
	}


	public Map<String, Collection<Import>> getImports() {
		return this.parameters;
	}


	public void addImport(String component, Import imp) {

		Collection<Import> imports = this.parameters.get(component);
		if(imports == null) {
			imports = new LinkedList<Import>();
			this.parameters.put(component, imports);
		}

		if(! imports.contains(imp))
			imports.add(imp);
	}


	/**
	 * @author Noël - LIG
	 */
	public enum InstanceStatus implements Serializable {
		NOT_DEPLOYED( true ),
		DEPLOYING( false ),
		DEPLOYED_STOPPED( true ),
		STARTING( false ),
		DEPLOYED_STARTED( true ),
		STOPPING( false ),
		UNDEPLOYING( false ),
		PROBLEM( false );


		private final boolean stable;

		/**
		 * Constructor.
		 * @param stable
		 */
		InstanceStatus( boolean stable ) {
			this.stable = stable;
		}


		/**
		 * A secured alternative to {@link InstanceStatus#valueOf(String)}.
		 * @param s a string (can be null)
		 * @return the associated runtime status, or {@link InstanceStatus#NOT_DEPLOYED} otherwise
		 */
		public static InstanceStatus wichStatus( String s ) {

			InstanceStatus result = InstanceStatus.NOT_DEPLOYED;
			for( InstanceStatus status : InstanceStatus.values()) {
				if( status.toString().equalsIgnoreCase( s )) {
					result = status;
					break;
				}
			}

			return result;
		}


		/**
		 * @return the stable
		 */
		public boolean isStable() {
			return this.stable;
		}
	}
}