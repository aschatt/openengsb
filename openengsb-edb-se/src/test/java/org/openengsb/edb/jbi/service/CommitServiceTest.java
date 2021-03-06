/**

   Copyright 2009 OpenEngSB Division, Vienna University of Technology

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
   
 */

package org.openengsb.edb.jbi.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.jbi.JBIException;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openengsb.edb.core.api.EDBHandler;
import org.openengsb.edb.core.api.EDBHandlerFactory;
import org.openengsb.edb.core.entities.GenericContent;
import org.openengsb.edb.jbi.endpoints.EdbEndpoint;
import org.openengsb.util.IO;
import org.openengsb.util.Prelude;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.SAXException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:testBeans.xml" })
public class CommitServiceTest extends SpringTestSupport {

    @Resource
    private EDBHandlerFactory config;

    /* test-parameters */
    private static String TEST_NAMESPACE = "urn:test";
    private static final UUID UUID_1 = UUID.fromString("5ff89772-0e20-44bd-9a97-d022ec2680db");
    private static final UUID UUID_2 = UUID.fromString("5ff89773-0e20-44bd-9a97-d022ec2680db");
    private static final String USER = "andreas";
    private static final String ABSTRACT_PATH = "/first/second/";
    private static final String PATH_1 = "d2sdf000";
    private static final String PATH_2 = "any key";

    private static final String EDB_SERVICE_NAME = "edb";
    private static GenericContent gc1;
    private static GenericContent gc2;
    private static Document persistMessage;
    private static Document resetMessage;
    private static Document invalidResetMessage;
    private static Document validQueryMessage;
    private static Document invalidQueryMessage;

    /* end test-parameters */

    /* test-variables */
    private DefaultServiceMixClient client;
    private static EDBHandler handler;

    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("testXBean.xml");
    }

    /**
     * Creates a new ServiceMixClieant
     * 
     * @return The new ServiceMixClient
     */
    private DefaultServiceMixClient createClient() throws JBIException {
        return new DefaultServiceMixClient(this.jbi);
    }

    /*
     * It is required to override these methods for some reason.
     */

    public static void makeParameters(EDBHandlerFactory config) {

        CommitServiceTest.handler = config.loadDefaultRepository();

        /* generic content */
        CommitServiceTest.gc1 = new GenericContent(CommitServiceTest.handler.getRepositoryBase().toString(), Prelude
                .dePathize(CommitServiceTest.ABSTRACT_PATH), new String[] { CommitServiceTest.PATH_1,
                CommitServiceTest.PATH_2, }, CommitServiceTest.UUID_1);

        CommitServiceTest.gc2 = new GenericContent(CommitServiceTest.handler.getRepositoryBase().toString(), Prelude
                .dePathize(CommitServiceTest.ABSTRACT_PATH), new String[] { CommitServiceTest.PATH_1,
                CommitServiceTest.PATH_2, }, CommitServiceTest.UUID_2);
        CommitServiceTest.gc2.setProperty("third", "some other value");

        /* Request-messages */
        /* valid commit */
        Element root = DocumentHelper.createElement("acmPersistMessage");
        Element body = root.addElement("body");
        addGCToMessagePart(CommitServiceTest.gc1, body, CommitServiceTest.USER);
        addGCToMessagePart(CommitServiceTest.gc2, body, CommitServiceTest.USER);
        CommitServiceTest.persistMessage = DocumentHelper.createDocument(root);

        /* valid/invalid reset */
        root = DocumentHelper.createElement("acmResetRequestMessage");
        Element resetBody = root.addElement("body");
        resetBody.addElement("repoId").setText("");
        resetBody.addElement("headId").setText("something-invalid");
        resetBody.addElement("depth").setText("0");
        CommitServiceTest.resetMessage = DocumentHelper.createDocument(root);
        CommitServiceTest.invalidResetMessage = DocumentHelper.createDocument(root.createCopy());

        /* valid query */
        root = DocumentHelper.createElement("acmQueryRequestMessage");
        Element querybody = root.addElement("body");
        querybody.addElement("query").setText("*");
        CommitServiceTest.validQueryMessage = DocumentHelper.createDocument(root.createCopy());

        /* invalid query */
        querybody.element("query").setText("does_not_exist");
        CommitServiceTest.invalidQueryMessage = DocumentHelper.createDocument(root);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        makeParameters(this.config);
        CommitServiceTest.handler = this.config.loadDefaultRepository();
        this.client = createClient();
        CommitServiceTest.resetMessage.getRootElement().element("body").element("headId").setText(
                CommitServiceTest.handler.getHeadInfo());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        IO.deleteStructure(CommitServiceTest.handler.getRepositoryBase().getParentFile());
        super.tearDown();
    }

    private static void addGCToMessagePart(final GenericContent gc, final Element elem, final String user) {
        final Element gcElem = elem.addElement("acmMessageObjects");
        gcElem.addElement("user").setText(user);
        gcElem.addElement("uuid").setText(gc.getUUID());
        gcElem.addElement("path").setText(gc.getPath());
        for (final Entry<Object, Object> entry : gc.getEntireContent()) {
            final Element pairs = gcElem.addElement("acmMessageObject");
            pairs.addElement("key").setText(entry.getKey().toString());
            pairs.addElement("value").setText(entry.getValue().toString());
        }
    }

    private InOut createInOutMessage(String message) throws MessagingException {
        InOut inOut = this.client.createInOutExchange();
        inOut.setService(new QName(CommitServiceTest.TEST_NAMESPACE, CommitServiceTest.EDB_SERVICE_NAME));
        inOut.getInMessage().setContent(new StringSource(message));
        return inOut;
    }

    private Document parseResponse(MessageExchange response) throws IOException, SAXException, TransformerException {
        SourceTransformer st = new SourceTransformer();
        SAXSource rSource = st.toSAXSource(response.getMessage("out").getContent());
        SAXReader saxreader = new SAXReader(rSource.getXMLReader());
        try {
            return saxreader.read(rSource.getInputSource());
        } catch (DocumentException e) {
            // TODO Auto-generated catch block
            StreamSource rawSource = st.toStreamSource(response.getMessage("out").getContent());
            BufferedReader br = new BufferedReader(rawSource.getReader());
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            this.log.error(e.getMessage());
            this.log.error(sb.toString());
            return null;
        }
    }

    private void assertNoErrorMessage(Element body) {
        Element errorObject = body.element("acmErrorObject");
        if (errorObject != null) {
            fail(errorObject.asXML());
        }
    }

    @Test
    @Ignore
    public void testCommit1() throws Exception {
        InOut inout = createInOutMessage(CommitServiceTest.persistMessage.asXML());
        inout.setOperation(new QName(EdbEndpoint.OPERATION_COMMIT));
        this.client.sendSync(inout);
        Document doc = parseResponse(this.client.receive());
        Element root = doc.getRootElement();
        assertEquals("acmResponseMessage", root.getName());
        Element body = root.element("body");
        assertNotNull(body);
        @SuppressWarnings("unchecked")
        List<Element> messageObjects = body.elements("acmMessageObjects");
        assertEquals(2, messageObjects.size());
        assertNoErrorMessage(body);
    }

    @Test
    @Ignore
    public void testInvalidCommit() throws Exception {
        InOut inout = createInOutMessage(CommitServiceTest.invalidResetMessage.asXML());
        inout.setOperation(new QName(EdbEndpoint.OPERATION_COMMIT));
        this.client.sendSync(inout);
        Document doc = parseResponse(this.client.receive());
        Element root = doc.getRootElement();
        assertEquals("acmResponseMessage", root.getName());
        Element body = root.element("body");
        assertNotNull(body);
        Element errorObject = body.element("acmErrorObject");
        assertNotNull(errorObject);
    }

    @Test
    @Ignore
    public void testResetInvalidHead() throws Exception {
        InOut inout = createInOutMessage(CommitServiceTest.invalidResetMessage.asXML());
        inout.setOperation(new QName(EdbEndpoint.OPERATION_RESET));
        this.client.sendSync(inout);
        Document doc = parseResponse(this.client.receive());
        Element root = doc.getRootElement();
        assertEquals("acmResponseMessage", root.getName());
        Element body = root.element("body");
        assertNotNull(body);
        Element errorObject = body.element("acmErrorObject");
        assertNotNull("reset on invalid head was successful", errorObject);
    }

    @Test
    @Ignore
    public void testResetDepth0() throws Exception {
        InOut inout = createInOutMessage(CommitServiceTest.resetMessage.asXML());
        inout.setOperation(new QName(EdbEndpoint.OPERATION_RESET));
        this.client.sendSync(inout);
        Document doc = parseResponse(this.client.receive());
        Element root = doc.getRootElement();
        assertEquals("acmResponseMessage", root.getName());
        Element body = root.element("body");
        assertNotNull(body);
        assertNoErrorMessage(body);
    }

    @Test
    @Ignore
    public void testValidQuery() throws Exception {
        testCommit1();
        InOut inout = createInOutMessage(CommitServiceTest.validQueryMessage.asXML());
        inout.setOperation(new QName(EdbEndpoint.OPERATION_QUERY));
        this.client.sendSync(inout);
        Document doc = parseResponse(this.client.receive());
        System.out.println(doc.asXML());
        Element root = doc.getRootElement();
        assertEquals("acmResponseMessage", root.getName());
        Element body = root.element("body");
        assertNotNull(body);
        assertNoErrorMessage(body);
        Element object = body.element("acmMessageObjects");
        assertNotNull("query result was empty", object);
    }

    @Test
    @Ignore
    public void testInvalidQuery() throws Exception {
        testCommit1();
        InOut inout = createInOutMessage(CommitServiceTest.invalidQueryMessage.asXML());
        inout.setOperation(new QName(EdbEndpoint.OPERATION_QUERY));
        this.client.sendSync(inout);
        Document doc = parseResponse(this.client.receive());
        System.out.println(doc.asXML());
        Element root = doc.getRootElement();
        assertEquals("acmResponseMessage", root.getName());
        Element body = root.element("body");
        assertNotNull(body);
        assertNoErrorMessage(body);
        Element object = body.element("acmMessageObjects");
        assertNull("query result was not empty", object);
    }
}
