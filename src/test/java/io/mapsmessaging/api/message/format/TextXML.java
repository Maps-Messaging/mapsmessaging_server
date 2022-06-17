/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */
package io.mapsmessaging.api.message.format;

import io.mapsmessaging.selector.IdentifierResolver;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.eclipse.jetty.xml.XmlParser;
import org.eclipse.jetty.xml.XmlParser.Node;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

public class TextXML {

  @Test
  public void testXMLFiltering() throws IOException, SAXException {
    Format format = new XmlFormat();
    XmlParser p = new XmlParser(false);
    Node node = p.parse(new ByteArrayInputStream(XMLString.getBytes()));
    String xml = node.toString();
    IdentifierResolver resolver = format.getResolver(xml.getBytes());
    Assertions.assertEquals("cardigan.jpg", resolver.get("catalog.product.product_image"));
    Assertions.assertEquals("Medium", resolver.get("catalog.product.catalog_item[0].size[0].description"));
  }

  private static final String XMLString = "<?xml version=\"1.0\"?>\n"
      + "<?xml-stylesheet href=\"catalog.xsl\" type=\"text/xsl\"?>\n"
      + "<!DOCTYPE catalog  >\n"
      + "<catalog>\n"
      + "   <product description=\"Cardigan Sweater\" product_image=\"cardigan.jpg\">\n"
      + "      <catalog_item gender=\"Men's\">\n"
      + "         <item_number>QWZ5671</item_number>\n"
      + "         <price>39.95</price>\n"
      + "         <size description=\"Medium\">\n"
      + "            <color_swatch image=\"red_cardigan.jpg\">Red</color_swatch>\n"
      + "            <color_swatch image=\"burgundy_cardigan.jpg\">Burgundy</color_swatch>\n"
      + "         </size>\n"
      + "         <size description=\"Large\">\n"
      + "            <color_swatch image=\"red_cardigan.jpg\">Red</color_swatch>\n"
      + "            <color_swatch image=\"burgundy_cardigan.jpg\">Burgundy</color_swatch>\n"
      + "         </size>\n"
      + "      </catalog_item>\n"
      + "      <catalog_item gender=\"Women's\">\n"
      + "         <item_number>RRX9856</item_number>\n"
      + "         <price>42.50</price>\n"
      + "         <size description=\"Small\">\n"
      + "            <color_swatch image=\"red_cardigan.jpg\">Red</color_swatch>\n"
      + "            <color_swatch image=\"navy_cardigan.jpg\">Navy</color_swatch>\n"
      + "            <color_swatch image=\"burgundy_cardigan.jpg\">Burgundy</color_swatch>\n"
      + "         </size>\n"
      + "         <size description=\"Medium\">\n"
      + "            <color_swatch image=\"red_cardigan.jpg\">Red</color_swatch>\n"
      + "            <color_swatch image=\"navy_cardigan.jpg\">Navy</color_swatch>\n"
      + "            <color_swatch image=\"burgundy_cardigan.jpg\">Burgundy</color_swatch>\n"
      + "            <color_swatch image=\"black_cardigan.jpg\">Black</color_swatch>\n"
      + "         </size>\n"
      + "         <size description=\"Large\">\n"
      + "            <color_swatch image=\"navy_cardigan.jpg\">Navy</color_swatch>\n"
      + "            <color_swatch image=\"black_cardigan.jpg\">Black</color_swatch>\n"
      + "         </size>\n"
      + "         <size description=\"Extra Large\">\n"
      + "            <color_swatch image=\"burgundy_cardigan.jpg\">Burgundy</color_swatch>\n"
      + "            <color_swatch image=\"black_cardigan.jpg\">Black</color_swatch>\n"
      + "         </size>\n"
      + "      </catalog_item>\n"
      + "   </product>\n"
      + "</catalog>";
}
