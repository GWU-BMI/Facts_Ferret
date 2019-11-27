/*
 *  Copyright 2015 United States Department of Veterans Affairs
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */
package gov.va.research.ir.model;

import gov.va.vinci.nlp.qeUtils.domain.TermWeight;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * @author vhaislreddd
 */
public class QueryExpander {

	private String baseAddress;
	private String path;

	public List<TermWeight> findRelatedTerms(final Collection<String> terms) throws IOException {
		WebClient webClient = WebClient.create(baseAddress);
		webClient.reset();
		webClient.path(path);
		for (String t : terms) {
			webClient.query("qt", t);
		}
		webClient.accept(MediaType.APPLICATION_XML_TYPE);
		Response response = webClient.get();
		if (200 != response.getStatus()) {
			throw new IOException("Response returned error status " + response.getStatus());
		}
		StringBuilder sb = new StringBuilder();
		try (InputStreamReader isr = new InputStreamReader((InputStream)response.getEntity())) {
			int ch = -1;
			while ((ch = isr.read()) != -1) {
				sb.append((char) ch);
			}
		}
		String responseXml = sb.toString();
		XStream xs = new XStream();
		xs.alias("TermWeight", TermWeight.class);
		xs.alias("term", String.class);
		xs.alias("weight", Double.class);
//		List<TermWeight> relatedTerms = (List<TermWeight>) xs.fromXML((InputStream)response.getEntity());
		List<TermWeight> relatedTerms = (List<TermWeight>) xs.fromXML(responseXml);
		Collections.sort(relatedTerms);
		return relatedTerms;
	}

	public String getBaseAddress() {
		return baseAddress;
	}

	public void setBaseAddress(String baseAddress) {
		this.baseAddress = baseAddress;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}
