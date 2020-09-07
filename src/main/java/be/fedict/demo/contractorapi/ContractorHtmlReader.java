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

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Parse HTML result page and convert it into a ContractorDAO object
 * 
 * @author Bart Hanssens
 * @see <a href="https://economie.fgov.be/nl/themas/ondernemingen/specifieke-sectoren/kwaliteit-de-bouw/erkenning-van-aannemers">erkenning van aannemers website</a>
 */
@Provider
@Consumes(MediaType.TEXT_HTML)
public class ContractorHtmlReader implements MessageBodyReader<ContractorDAO> {
	private static final String BASEURL = "https://economie.fgov.be/";
	private static final String TABLE_RESULT = "mainForm:dataTab_data";
	private static final String HEADERS = "KBO nummer,BTW nummer,aannemer,Erkenningsnummer,straat,Postcode,Gemeente,Beslissingsdatum,vervaldatum,categorie klassen";
	private static final Pattern CLASSCATS = Pattern.compile("([A-Z]\\d{0,2}) \\((\\d)\\) ?");

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] antns, MediaType mt) {
		return genericType.equals(ContractorDAO.class);
	}

	@Override
	public ContractorDAO readFrom(Class<ContractorDAO> type, Type genericType, Annotation[] antns, MediaType mt, MultivaluedMap<String, String> mm, InputStream in) throws IOException, WebApplicationException {
		return parseOrganization(in);
	}
	
	/**
	 * Convert the list of contractor categories and classes to a map.
	 * 
	 * @param str categories as string
	 * @return map with category as key and class as value
	 */
	private Map<String, String> mapToClassCats(String str) {
		Matcher match = CLASSCATS.matcher(str);
		Map<String, String> map = new HashMap();
		while (match.find()) {
			map.put(match.group(1), match.group(2));
		}
		return map;
	}

	/**
	 * The HTML result table should contain exactly 1 row if a result was found, or an empty table.
	 * 
	 * @param in
	 * @return
	 * @throws IOException 
	 */
	private ContractorDAO parseOrganization(InputStream in) throws IOException {
		ContractorDAO contractor = new ContractorDAO();

		Document doc = Jsoup.parse(in, StandardCharsets.UTF_8.toString(), BASEURL);
		Element table = doc.getElementById(TABLE_RESULT);
		if (table == null) {
			throw new WebApplicationException("No results table found", Response.Status.INTERNAL_SERVER_ERROR);
		}
		Elements headrow = table.select("thead tr");
		if (headrow == null || headrow.size() != 1) {
			throw new WebApplicationException("No header row", Response.Status.INTERNAL_SERVER_ERROR);
		}
		Elements headers = headrow.select("th");
		if (headers == null) {
			throw new WebApplicationException("No headers in table", Response.Status.INTERNAL_SERVER_ERROR);
		}
		Elements tds = headers.first().select("td");
		String collected = tds.stream().map(Element::text).collect(Collectors.joining(","));
		if (HEADERS.compareToIgnoreCase(collected) != 0) {
			throw new WebApplicationException("Unknown headers", Response.Status.INTERNAL_SERVER_ERROR);			
		}

		Elements rows = table.select("tbody tr");
		if (rows == null || rows.size() != 1) {
			throw new WebApplicationException("Expected exactly 1 result", Response.Status.NOT_FOUND);
		}

		Element row = table.select("tr").first();
		Elements columns = row.getElementsByTag("td");
		if (columns == null || columns.size() != 10) {
			throw new WebApplicationException("Expected 10 columns in result", Response.Status.INTERNAL_SERVER_ERROR);			
		}
		contractor.setCbeId(columns.get(0).text());
		contractor.setVatId(columns.get(1).text());
		contractor.setName(columns.get(2).text());
		contractor.setLicenseNo(columns.get(3).text());
		contractor.setStreet(columns.get(4).text());
		contractor.setPostalCode(columns.get(5).text());
		contractor.setMunicipality(columns.get(6).text());
		contractor.setFromDate(columns.get(7).text());
		contractor.setTillDate(columns.get(8).text());
		contractor.setCatClasses(mapToClassCats(columns.get(9).text()));

		return contractor;
	}
}
