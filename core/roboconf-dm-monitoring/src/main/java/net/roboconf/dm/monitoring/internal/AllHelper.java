/**
 * Copyright 2013-2015 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of the joint LINAGORA -
 * Université Joseph Fourier - Floralis research program and is designated
 * as a "Result" pursuant to the terms and conditions of the LINAGORA
 * - Université Joseph Fourier - Floralis research program. Each copyright
 * holder of Results enumerated here above fully & independently holds complete
 * ownership of the complete Intellectual Property rights applicable to the whole
 * of said Results, and may freely exploit it in any manner which does not infringe
 * the moral rights of the other copyright holders.
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

package net.roboconf.dm.monitoring.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import org.apache.commons.lang3.StringUtils;

/**
 * Helper needed to select all the instances matching a given criterion.
 *
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class AllHelper implements Helper<Object> {

	/**
	 * The name of this helper.
	 */
	public static final String NAME = "all";

	/**
	 * A singleton instance of this helper.
	 */
	public static final Helper<Object> INSTANCE = new AllHelper();

	/**
	 * Select instances according to a selection path.
	 * For example:
	 *
	 * <pre>
	 * {{#all children path="/VM/Apache" installer="script"}}
	 *   {{path}}
	 * {{/all}}
	 * </pre>
	 */
	@Override
	public CharSequence apply( final Object context, final Options options ) throws IOException {
		final CharSequence result;

		if (context instanceof ApplicationContextBean) {
			// Implicit: all instances of the application.
			result = safeApply( ((ApplicationContextBean) context).instances, options );
		} else if (context instanceof InstanceContextBean) {
			// Implicit: all descendants of the instance.
			result = safeApply( descendantInstances((InstanceContextBean) context), options );
		} else if (context instanceof Collection<?>) {
			// Collection of instances. We must ensure type-safety.
			final Collection<InstanceContextBean> safeContext = new ArrayList<InstanceContextBean>();
			for (final Object element : (Collection<?>) context) {
				if (element instanceof InstanceContextBean) {
					safeContext.add( (InstanceContextBean) element );
				}
			}
			result = safeApply( safeContext, options );
		} else {
			result = StringUtils.EMPTY;
		}


		return result;
	}

	/**
	 * Return all the descendant instances of the given instance.
	 *
	 * @param instance the instance which descendants must be retrieved.
	 * @return the descendants of the given instance.
	 */
	private static Collection<InstanceContextBean> descendantInstances( final InstanceContextBean instance ) {
		final Collection<InstanceContextBean> result = new ArrayList<InstanceContextBean>();
		for (final InstanceContextBean child : instance.children) {
			result.add( child );
			result.addAll( descendantInstances( child ) );
		}
		return result;
	}

	/**
	 * Same as above, but with type-safe arguments.
	 *
	 * @param instances the instances to which this helper is applied.
	 * @param options   the options of this helper invocation.
	 * @return a string result.
	 * @throws IOException if a template cannot be loaded.
	 */
	private String safeApply( final Collection<InstanceContextBean> instances, final Options options ) throws IOException {
		final String path = options.param( 0, InstanceFilter.JOKER );

		// Parse the filter.
		final InstanceFilter filter = InstanceFilter.createFilter( path );

		// Apply the filter.
		final Collection<InstanceContextBean> selectedInstances = filter.apply( instances );

		// Apply the content template of the helper to each selected instance.
		final StringBuilder buffer = new StringBuilder();
		final Context parent = options.context;
		int index = 0;
		final int last = selectedInstances.size() - 1;
		for (final InstanceContextBean instance : selectedInstances) {
			final Context current = Context.newBuilder( parent, instance )
					.combine( "@index", index )
					.combine( "@first", index == 0 ? "first" : "" )
					.combine( "@last", index == last ? "last" : "" )
					.combine( "@odd", index % 2 == 0 ? "" : "odd" )
					.combine( "@even", index % 2 == 0 ? "even" : "" )
					.build();
			index++;
			buffer.append( options.fn( current ) );
		}
		return buffer.toString();
	}

}
