/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.target.occi.internal;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import net.roboconf.core.utils.Utils;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class OcciVMUtils {

	/**
	 * Create a VM (OCCI / VMWare).
	 * @param hostIpPort IP and port of OCCI server (eg. "172.16.225.91:8080")
	 * @param id Unique VM ID
	 * @param template VM image ID (null means no image specified)
	 * @param title VM title
	 * @param summary VM summary
	 * @return The VM ID
	 */
	public static String createVM(String hostIpPort, String id, String template, String title, String summary) {

		String ret = null;
		URL url = null;
		try {
			CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
			//System.setProperty("http.maxRedirects", "100");
			url = new URL("http://" + hostIpPort + "/compute/" + id);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		if(Utils.isEmptyOrWhitespaces(title)) title = "Roboconf";
		if(Utils.isEmptyOrWhitespaces(summary)) summary = "Generated by Roboconf";

		HttpURLConnection httpURLConnection = null;
		DataInputStream in = null;
		try {
			httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setRequestMethod("PUT");
			httpURLConnection.setRequestProperty("Content-Type", "text/occi");
			httpURLConnection.setRequestProperty("Accept", "*/*");
			StringBuffer category = new StringBuffer("compute; scheme=\"http://schemas.ogf.org/occi/infrastructure#\"; class=\"kind\"");
			if(template != null) {
				category.append(", medium; scheme=\"http://schemas.ogf.org/occi/infrastructure/compute/template/1.1#\"; class=\"mixin\""
						+ ", vmaddon; scheme=\"http://occiware.org/occi/vmwarecrtp#\"; class=\"mixin\""
						+ ", vmwarefolders; scheme=\"http://occiware.org/occi/vmwarecrtp#\"; class=\"mixin\"");
			}
			category.append(";");
			httpURLConnection.setRequestProperty("Category", category.toString());
			httpURLConnection.addRequestProperty("X-OCCI-Attribute",
					"occi.core.id=\"" + id + "\"");
			httpURLConnection.addRequestProperty("X-OCCI-Attribute",
					"occi.core.title=\"" + title + "\"");
			httpURLConnection.addRequestProperty("X-OCCI-Attribute",
					"occi.core.summary=\"" + summary + "\"");
			httpURLConnection.addRequestProperty("X-OCCI-Attribute",
					"occi.compute.architecture=\"x64\"");
			httpURLConnection.addRequestProperty("X-OCCI-Attribute",
					"occi.compute.cores=2");
			httpURLConnection.addRequestProperty("X-OCCI-Attribute",
					"occi.compute.memory=3.0");
			httpURLConnection.addRequestProperty("X-OCCI-Attribute",
					"occi.compute.state=\"active\"");
			if(template != null) {
				httpURLConnection.addRequestProperty("X-OCCI-Attribute",
						"imagename=\"" + template + "\"");
			}

			in = new DataInputStream(httpURLConnection.getInputStream());
			byte buf[] = new byte[in.available()];
			in.readFully(buf);
			ret = new String(buf);

		} catch (IOException exception) {
			exception.printStackTrace();
		}  finally {

			if(in != null) {
				try { in.close(); } catch (IOException e) { /* ignore */ }
			}
			if (httpURLConnection != null) {
				httpURLConnection.disconnect();
			}
		}
		System.out.println(ret);
		return ("OK".equalsIgnoreCase(ret.trim()) ? id : null);
	}


	/**
	 * Delete a VM (OCCI / VMWare).
	 * @param hostIpPort IP and port of OCCI server (eg. "172.16.225.91:8080")
	 * @param id Unique VM ID
	 * @return true if deletion OK, false otherwise
	 */
	public static boolean deleteVM(String hostIpPort, String id) {
		String ret = null;
		URL url = null;
		try {
			CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
			//System.setProperty("http.maxRedirects", "100");
			url = new URL("http://" + hostIpPort + "/compute/" + id);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		HttpURLConnection httpURLConnection = null;
		DataInputStream in = null;
		try {
			httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setRequestMethod("DELETE");
			httpURLConnection.setRequestProperty("Content-Type", "text/occi");
			httpURLConnection.setRequestProperty("Accept", "*/*");

			in = new DataInputStream(httpURLConnection.getInputStream());
			byte buf[] = new byte[in.available()];
			in.readFully(buf);
			ret = new String(buf);

		} catch (IOException exception) {
			exception.printStackTrace();
		}  finally {

			if(in != null) {
				try { in.close(); } catch (IOException e) { /* ignore */ }
			}
			if (httpURLConnection != null) {
				httpURLConnection.disconnect();
			}
		}

		return ("OK".equalsIgnoreCase(ret));
	}

	/**
	 * Test main program.
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {
		System.out.println("Create VM: " +
			createVM("172.16.225.91:8080",
			//createVM("localhost:8888",
				"6157c4d2-08b3-4204-be85-d1828df74c22", null, "javaTest", "Java Test"));
		Thread.sleep(40000);
		System.out.println("Delete VM: " + deleteVM("172.16.225.91:8080", "6157c4d2-08b3-4204-be85-d1828df74c22"));
	}

}
