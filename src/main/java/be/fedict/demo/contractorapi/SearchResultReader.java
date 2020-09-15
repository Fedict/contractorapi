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
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

/**
 * Parse the search result and convert it into an object. 
 * Note that the result is not a full HTML page, but an HTML table embedded in XML which is normally rendered by a browser.
 * 
 * @author Bart Hanssens
 * @see <a href="https://economie.fgov.be/nl/themas/ondernemingen/specifieke-sectoren/kwaliteit-de-bouw/erkenning-van-aannemers">erkenning van aannemers website</a>
 */
@Provider
@Consumes(MediaType.TEXT_XML)
public class SearchResultReader implements MessageBodyReader<ContractorDAO> {
	private static final String HEADERS = "KBO nummer,BTW nummer,aannemer,Erkenningsnummer,straat,Postcode,Gemeente,Beslissingsdatum,vervaldatum,categorie klassen";
	private static final Pattern CLASSCATS = Pattern.compile("([A-Z]\\d{0,2}) \\((\\d)\\) ?");

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] antns, MediaType mt) {
		return genericType.equals(ContractorDAO.class);
	}

	@Override
	public ContractorDAO readFrom(Class<ContractorDAO> type, Type genericType, Annotation[] antns, MediaType mt, 
									MultivaluedMap<String, String> headers, InputStream in) throws IOException, WebApplicationException {
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
	 * Parse the XML response which contains an HTML table with exactly 1 row if a result was found, or an empty table.
	 * 
	 * @param xml
	 * @return
	 * @throws IOException 
	 */
	private ContractorDAO parseOrganization(InputStream xml) throws IOException, WebApplicationException {
		ContractorDAO contractor = new ContractorDAO();

		Document xmlDoc = Jsoup.parse(xml, StandardCharsets.UTF_8.toString(), "", Parser.xmlParser());
		if (xmlDoc == null) {
			throw new WebApplicationException("Could not process XML update response");
		}

		Elements updates = xmlDoc.select("update");
		if (updates == null || updates.isEmpty()){
			throw new WebApplicationException("No update element found");
		}
		String cdata = updates.first().text();
		Document doc = Jsoup.parse(cdata);
		
		// search for the results in the HTML table
		Elements headrow = doc.select("thead[id='mainForm:dataTab_head'] tr");
		if (headrow == null || headrow.size() != 1) {
			throw new WebApplicationException("No header row");
		}
		Elements headers = headrow.select("th span[class='ui-column-title']");
		if (headers == null || headers.isEmpty()) {
			throw new WebApplicationException("No headers in table");
		}
		// check if returned table headers match expected headers 
		String collected = headers.stream().map(Element::text).collect(Collectors.joining(","));
		if (HEADERS.compareToIgnoreCase(collected) != 0) {
			throw new WebApplicationException("Unknown headers");			
		}

		Elements rows = doc.select("tbody[id='mainForm:dataTab_data'] tr");
		if (rows == null || rows.size() != 1) {
			throw new WebApplicationException("Expected exactly 1 result");
		}

		Element row = rows.first();
		Elements columns = row.getElementsByTag("td");
		// a "not found" with colspan 10 will be returned when there is no search result
		if (columns == null || columns.size() == 1) {
			throw new NotFoundException("Not found");
		}

		if (columns.size() != 10) {
			throw new WebApplicationException("Expected 10 columns in result");			
		}
		contractor.setCbeId(columns.get(0).text().replace("ui-button", ""));
		contractor.setVatId(columns.get(1).text());
		contractor.setName(columns.get(2).text());
		contractor.setLicenseNo(columns.get(3).text());
		contractor.setStreet(columns.get(4).text());
		contractor.setPostalCode(columns.get(5).text());
		contractor.setMunicipality(columns.get(6).text());
		try {
			contractor.setFromDate(columns.get(7).text());
			contractor.setTillDate(columns.get(8).text());
		} catch (ParseException pe) {
			throw new WebApplicationException("Error parsing date");
		}
		contractor.setCatClasses(mapToClassCats(columns.get(9).text()));

		return contractor;
	}
}
