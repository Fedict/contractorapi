/*
 * Copyright (c) 2020, Bart Hanssens <bart.hanssens@bosa.fgov.be>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package be.fedict.demo.contractorapi;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;

/**
 *
 * @author Bart Hanssens
 */
public class WiremockContractors implements QuarkusTestResourceLifecycleManager {
	private WireMockServer server;	

	private String getAsString(String name) {
		try {
			return IOUtils.toString(
				WiremockContractors.class.getClassLoader().getResourceAsStream(name), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			return "";
		}
	}

	@Override
	public Map<String, String> start() {
		server = new WireMockServer();
		server.start();

		stubFor(post("/weblists/dataDisplay.xhtml")
					.withRequestBody(new ContainsPattern("0123456789"))
					.willReturn(ok(getAsString("/found.html"))
									.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML))
		);

		stubFor(post("/weblists/dataDisplay.xhtml")
					.withRequestBody(new ContainsPattern("000000000"))
					.willReturn(ok(getAsString("/notfound.html"))
									.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML))
		);

		stubFor(post("/weblists/dataDisplay.xhtml")
					.withRequestBody(new ContainsPattern("987"))
					.willReturn(badRequest())
		);

		return Collections.singletonMap("be.fedict.demo.contractorapi.ContractorSearch/mp-rest/url", server.baseUrl());
	}

	@Override
	public void stop() {
		if (server != null) {
			server.stop();
		}
	}
}
