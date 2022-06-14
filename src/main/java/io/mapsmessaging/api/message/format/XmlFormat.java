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
import org.xml.sax.SAXException;

public class XmlFormat implements Format{

  @Override
  public String getName() {
    return "XML";
  }

  @Override
  public String getDescription() {
    return "Processes XML formatted payloads";
  }

  @Override
  public byte[] toByteArray(Object obj) throws IOException {
    if(obj instanceof Node){
      return obj.toString().getBytes();
    }
    return null;
  }

  @Override
  public Object fromByteArray(byte[] payload) throws IOException {
    XmlParser xmlParser = new XmlParser();
    try {
      return xmlParser.parse(new ByteArrayInputStream(payload));
    } catch (SAXException e) {
      throw new IOException(e);
    }
  }
  @Override
  public IdentifierResolver getResolver(byte[] payload) throws IOException {
    return new XmlIdentifierResolver((Node)fromByteArray(payload));
  }

  public static final class XmlIdentifierResolver implements IdentifierResolver{

    private final Node node;

    public XmlIdentifierResolver(Node node){
      this.node = node;
    }

    @Override
    public Object get(String s) {
      return node.get(s);
    }

    @Override
    public byte[] getOpaqueData() {
      return IdentifierResolver.super.getOpaqueData();
    }
  }
}
