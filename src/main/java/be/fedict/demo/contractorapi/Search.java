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

import javax.inject.Singleton;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * HTTP client sending the request to the webform
 * 
 * @author Bart Hanssens
 */
@Singleton
@RegisterRestClient(baseUri = "https://weblists.economie.fgov.be")
public interface Search {
	@GET
	@Path("/weblists/dataDisplay.xhtml")
	@Produces(MediaType.TEXT_HTML)
	public FormDAO getSearchForm(@QueryParam("app") int app, @QueryParam("list") int lst, @QueryParam("lang") String lang);

	@POST
	@Path("/weblists/dataDisplay.xhtml")
	@Produces(MediaType.TEXT_XML)
	@ClientHeaderParam(name="Faces-Request", value="partial/ajax")
	public ContractorDAO getContractorById(@FormParam("mainForm:crit1465:crit767") String id,
											@FormParam("javax.faces.ViewState") String viewState,
											@CookieParam("JSESSIONID") Cookie cookieJS,
											@CookieParam("MY_SESSION") Cookie cookieMS,
											@FormParam("javax.faces.partial.ajax") boolean partial,
											@FormParam("javax.faces.source") String source,
											@FormParam("javax.faces.partial.execute") String exec,
											@FormParam("javax.faces.partial.render") String render,
											@FormParam("mainForm:searchButton") String button,
											@FormParam("mainForm_SUBMIT") int submit);
}
