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

package net.roboconf.dm.templating.internal;

import java.io.File;
import java.io.FileFilter;

import net.roboconf.dm.templating.TemplatingService;

import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.CanReadFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;

import com.github.jknack.handlebars.Template;

/**
 * Entry for a template being watched by the {@link TemplateWatcher}.
 * @author Pierre Bourret - Université Joseph Fourier
 */
public final class TemplateEntry {

	/**
	 * The template file.
	 */
	final File file;

	/**
	 * The identifier of this template entry.
	 */
	final String id;

	/**
	 * The name of the application scoped by this template, or {@code null} for a global template entry.
	 */
	final String appName;

	/**
	 * The compiled Handlebars template.
	 */
	final Template template;

	/**
	 * File filter that only selects template files that are in the root template directory, or in a first-level
	 * sub-directory.
	 */
	private static class TemplateFileFilter extends AbstractFileFilter {
		/**
		 * The root template directory.
		 */
		private final File rootTemplateDir;

		/**
		 * Create a template file filter.
		 *
		 * @param rootTemplateDir the root template directory.
		 */
		TemplateFileFilter( final File rootTemplateDir ) {
			this.rootTemplateDir = rootTemplateDir;
		}

		@Override
		public boolean accept( File file ) {
			final File parentDir = file.getParentFile();
			return this.rootTemplateDir.equals(parentDir) || this.rootTemplateDir.equals(parentDir.getParentFile());
		}
	}

	/**
	 * File filter that only selects sub-template directories. It is guaranteed that this filter is only called with
	 * directory files.
	 */
	private static class TemplateSubDirectoryFileFilter extends AbstractFileFilter {
		/**
		 * The root template directory.
		 */
		private final File rootTemplateDir;

		/**
		 * Create a template sub-directory file filter.
		 *
		 * @param rootTemplateDir the root template directory.
		 */
		TemplateSubDirectoryFileFilter( final File rootTemplateDir ) {
			this.rootTemplateDir = rootTemplateDir;
		}

		@Override
		public boolean accept( File file ) {
			return this.rootTemplateDir.equals(file.getParentFile());
		}
	}

	/**
	 * Get the file filter for templates selection inside the given template directory.
	 *
	 * Templates must:
	 * <ol>
	 * <li>be regular templates,</li>
	 * <li>have the template file extension,</li>
	 * <li>be readable,</li>
	 * <li>be :
	 * <ul>
	 * <li>either <em>direct children</em> of the template directory. In that case the templates are global
	 * to all applications.</li>
	 * <li>or be grandchildren of the template directory. In that case, the templates are specific to one
	 * application, the name of the intermediate directory being the name of the application.</li>
	 * </ul>
	 * </li>
	 * </ol>
	 *
	 * @param templateDir the templates directory, <em>MUST</em> be canonical.
	 * @return the file filter for template selection inside the given directory.
	 */
	public static FileFilter getTemplateFileFilter( final File templateDir ) {
		return FileFilterUtils.or(
				FileFilterUtils.and(
						FileFilterUtils.fileFileFilter(),
						FileFilterUtils.suffixFileFilter(TemplatingService.TEMPLATE_FILE_EXTENSION),
						CanReadFileFilter.CAN_READ,
						new TemplateFileFilter(templateDir)),
				FileFilterUtils.and(
						FileFilterUtils.directoryFileFilter(),
						CanReadFileFilter.CAN_READ,
						new TemplateSubDirectoryFileFilter(templateDir))
		);
	}

	/**
	 * Get the name of the application targeted by a template from its filename.
	 *
	 * @param templateDir  the templates root directory, the one being watched by the TemplateWatcher.
	 * @param templateFile the template file
	 * @return name of the application targeted by the given template file, or {@code null} for a template global to all
	 * applications.
	 * @throws IllegalStateException if the provided {@code templateFile} does not match
	 */
	public static String getTemplateApplicationName( final File templateDir, final File templateFile ) {
		if (!templateFile.getName().endsWith(TemplatingService.TEMPLATE_FILE_EXTENSION)) {
			throw new IllegalArgumentException("not a template file name: " + templateFile.getName());
		}
		final File parentDir = templateFile.getParentFile();
		final String appName;
		if (templateDir.equals(parentDir)) {
			// No intermediate directory, the template is global!
			appName = null;
		} else if (templateDir.equals(parentDir.getParentFile())) {
			// One intermediate directory: the template is specific to one application.
			// The name of the application is the name of the intermediate directory.
			appName = parentDir.getName();
		} else {
			// Too many levels!
			throw new IllegalArgumentException("not a template file: " + templateFile);
		}
		return appName;
	}

	/**
	 * Create a template entry.
	 *
	 * @param file     the template file.
	 * @param id       tThe identifier of this template entry.
	 * @param appName  the name of the application scoped by this template, or {@code null} for a global template
	 *                 entry.
	 * @param template the compiled Handlebars template.
	 */
	public TemplateEntry( final File file, final String id, final String appName, final Template template ) {
		this.file = file;
		this.id = id;
		this.appName = appName;
		this.template = template;
	}

	/**
	 * Get the identifier of a template from its filename.
	 *
	 * @param templateFile the template file
	 * @return the identifier for the given template file.
	 * @throws IllegalArgumentException if the provided template file does not end with the template file extension.
	 */
	public static String getTemplateId( final File templateFile ) {
		final String name = templateFile.getName();
		if (!name.endsWith(TemplatingService.TEMPLATE_FILE_EXTENSION)) {
			throw new IllegalArgumentException("not a template file name: " + name);
		}
		return name.substring(0, name.length() - TemplatingService.TEMPLATE_FILE_EXTENSION.length());
	}

	@Override
	public String toString() {
		return super.toString() + "[appName=" + this.appName + ", id=" + this.id + ", file=" + this.file + "]";
	}
}