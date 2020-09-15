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

import be.fedict.demo.contractorapi.helper.ContractorDAO;
import be.fedict.demo.contractorapi.helper.FormDAO;

import javax.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Main entry for the API
 * 
 * @author Bart Hanssens
 */
@Path("/contractor")
@Produces(MediaType.APPLICATION_JSON)
public class ContractorResource {
	@Inject
    @RestClient
	Search search;
	
	@GET
	@Path("/{id}")
	@Operation(summary = "Get contractor", description = "Get one contractor by enterprise ID")
	@APIResponses(value = {
		@APIResponse(responseCode = "200", description = "Success"),
		@APIResponse(responseCode = "404", description = "Not Found"),
		@APIResponse(responseCode = "500", description = "Other error")
	})
	public ContractorDAO getContractorById(@PathParam("id") String str) {
		// remove "BE", spaces, dots ...
		String id = str.replaceAll("\\D", "");
		if (id.isEmpty() || id.length() < 9) {
			throw new WebApplicationException("ID too short", Response.Status.BAD_REQUEST);
		}

		// mimic manual form entry
		FormDAO form = search.getSearchForm(5, 8, "NL");
		return search.getContractorById(id, form.getViewState(), 
				form.getCookies().get("JSESSIONID"), form.getCookies().get("MY_SESSION"),
				true, "mainForm:searchButton", "@all", "mainForm:dataTab","mainForm:searchButton", 1);
    }
}
