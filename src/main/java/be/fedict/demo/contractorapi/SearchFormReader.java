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

import be.fedict.demo.contractorapi.helper.FormDAO;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * Parse HTML search form and convert it into an object
 *
 * @author Bart Hanssens
 * @see
 * <a href="https://economie.fgov.be/nl/themas/ondernemingen/specifieke-sectoren/kwaliteit-de-bouw/erkenning-van-aannemers">erkenning
 * van aannemers website</a>
 */
@Provider
@Consumes(MediaType.TEXT_HTML)
public class SearchFormReader implements MessageBodyReader<FormDAO> {

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] antns, MediaType mt) {
		return genericType.equals(FormDAO.class);
	}

	@Override
	public FormDAO readFrom(Class<FormDAO> type, Type genericType, Annotation[] antns, MediaType mt,
		MultivaluedMap<String, String> headers, InputStream in) throws IOException, WebApplicationException {
		return parseForm(in, headers);
	}

	/**
	 * Parse the webform to obtain cookies, session IDs etc.
	 *
	 * @param html form as HTML
	 * @return form object
	 * @throws IOException
	 */
	private FormDAO parseForm(InputStream html, MultivaluedMap<String, String> headers) throws IOException, WebApplicationException {
		FormDAO form = new FormDAO();

		Document doc = Jsoup.parse(html, StandardCharsets.UTF_8.toString(), "");

		Elements inputs = doc.select("input[name='javax.faces.ViewState']");
		if (inputs == null || inputs.isEmpty()) {
			throw new WebApplicationException("No viewstate found");
		}
		List<String> vals = headers.get(HttpHeaders.SET_COOKIE);
		if (vals == null || vals.isEmpty()) {
			throw new WebApplicationException("No cookie found");
		}
		Map<String, Cookie> cookies = vals.stream().map(Cookie::valueOf).collect(Collectors.toMap(c -> c.getName(), c -> c));

		form.setViewState(inputs.first().val());
		form.setCookies(cookies);

		return form;
	}
}
