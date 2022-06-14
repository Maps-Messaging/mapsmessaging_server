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
