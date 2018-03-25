package CoSimulation;
 
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
 
public class xmlImpl implements xmlInterface {
 
    Document basedata;
    Node ModelConfig;
    public  synchronized void init(String fileName) {        
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            basedata = db.parse(fileName);
            NodeList root = basedata.getChildNodes();
            ModelConfig = root.item(0);
            System.out.println("len: "+root.getLength()+"::"+ModelConfig.getNodeName()+"");
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (ParserConfigurationException e) {
            System.out.println(e.getMessage());
        } catch (SAXException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }   
    }
 
    public void createXml(String fileName) {
    }

    public Node findIED(String name, String cnt) {
        Node iedNode;
        iedNode = null;
        NodeList nodetmp = ModelConfig.getChildNodes();
        Node ndtmp,attr;
        for (int i = 0; i < nodetmp.getLength(); i++)
        {
            ndtmp = nodetmp.item(i);
            if(ndtmp.getNodeName().equals(name))
            {
                if(cnt == null)
                {
                    iedNode = ndtmp;
                    break;
                }else{
                    attr = ndtmp.getAttributes().getNamedItem("id");
                    if(attr.getNodeValue().equals(cnt))
                    {
                        iedNode = ndtmp;
                        break;
                    }
                }
            }
        }
        return iedNode;
    }
    
    public Node findNode(Node pNode, String name, String cnt) {
        Node iedNode;
        iedNode = null;
        NodeList nodetmp = pNode.getChildNodes();
        Node ndtmp,attr;
        for (int i = 0; i < nodetmp.getLength(); i++)
        {
            ndtmp = nodetmp.item(i);
            if(ndtmp.getNodeName().equals(name))
            {
                if(cnt == null)
                {
                    iedNode = ndtmp;
                    break;
                }else{
                    attr = ndtmp.getAttributes().getNamedItem("id");
                    if(attr.getNodeValue().equals(cnt))
                    {
                        iedNode = ndtmp;
                        break;
                    }
                }
            }
        }
        return iedNode;
    }
    
    public String GetValue(Node iedNode, String Name, String cnt) {
        NodeList nodetmp = iedNode.getChildNodes();
        String restmp = "";
        Node ndtmp,attr;
        for (int i = 0; i < nodetmp.getLength(); i++)
        {
            ndtmp = nodetmp.item(i);
            if(ndtmp.getNodeName().equals(Name))
            {
                if(cnt == null)
                {
                    restmp = ndtmp.getTextContent();
                    break;
                }else
                {
                    attr = ndtmp.getAttributes().getNamedItem("id");
                    if(attr.getNodeValue().equals(cnt))
                    {
                        restmp = ndtmp.getTextContent();
                        break;
                    }
                }
            }
        }     
        return restmp;
    }
    
    public String GetAttr(Node iedNode, String Name, String Attr, String cnt) {
        NodeList nodetmp = iedNode.getChildNodes();
        String restmp = "";
        Node ndtmp,attrt,attrtt;
        for (int i = 0; i < nodetmp.getLength(); i++)
        {
            ndtmp = nodetmp.item(i);
            if(ndtmp.getNodeName().equals(Name))
            {
                if(cnt == null)
                {
                    attrt = ndtmp.getAttributes().getNamedItem(Attr);
                    restmp = attrt.getNodeValue();
                    break;
                }else
                {
                    attrt = ndtmp.getAttributes().getNamedItem("id");
                    if(attrt.getNodeValue().equals(cnt))
                    {
                        attrtt = ndtmp.getAttributes().getNamedItem(Attr);
                        if(attrtt != null)
                            restmp = attrtt.getNodeValue();
                        break;
                    }
                }
            }
        }     
        return restmp;
    }
    
    public int getCount(Node iedNode, String name) {
        NodeList nodetmp = iedNode.getChildNodes();
        Node ndtmp;
        int cnt = 0;
        for (int i = 0; i < nodetmp.getLength(); i++)
        {
            ndtmp = nodetmp.item(i);
            if(ndtmp.getNodeName().equals(name))
            {
                cnt = cnt + 1;
            }
        }
        return cnt;
    }
   
    public void parserXml(String fileName) {
    }
}