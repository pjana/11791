package org.lappsgrid.example;

// JUnit modules for unit tests
import org.junit.*;

// more APIs for testing code
import org.lappsgrid.api.WebService;

import static org.junit.Assert.*;
import static org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TestUniqueTerms {

    // this will be the sandbag
    protected WebService service;

    // initiate the service before each test
    @Before
    public void setUp() throws IOException {
        service = new UniqueTerms();
    }

    // then destroy it after the test
    @After
    public void tearDown() {
        service = null;
    }

    @Test
    public void testMetadata() {
        WebService service = new UniqueTerms();

        // Retrieve metadata, remember `getMetadata()` returns a serialized JSON string
        String json = service.getMetadata();
        assertNotNull("service.getMetadata() returned null", json);

        // Instantiate `Data` object with returned JSON string
        Data data = Serializer.parse(json, Data.class);
        assertNotNull("Unable to parse metadata json.", data);
        assertNotSame(data.getPayload().toString(), Uri.ERROR, data.getDiscriminator());

        // Then, convert it into `Metadata` datastructure
        ServiceMetadata metadata = new ServiceMetadata((Map) data.getPayload());
        IOSpecification produces = metadata.getProduces();
        IOSpecification requires = metadata.getRequires();

        // Now, see each field has correct value
        assertEquals("Name is not correct", UniqueTerms.class.getName(), metadata.getName());

        List<String> list = requires.getFormat();
        assertTrue("LIF format not accepted.", list.contains(Uri.LAPPS));
        assertTrue("Text not accepted", list.contains(Uri.TEXT));

        assertEquals("Too many annotation types produced", 1, produces.getAnnotations().size());
        assertEquals("Tokens not produced", Uri.TOKEN, produces.getAnnotations().get(0));
    }

    @Test
    public void testExecute() {
        final String text = "apple pie apple";

        // wrap plain text into `Data`
        Data input = new Data<>(Uri.TEXT, text);

        // call `execute()` with jsonized input,
        String tokenized = this.service.execute(input.asJson());

        // store the payload from what is returned into a `Container`, the main wrapper for LIF
        Container container = Serializer.parse(tokenized, DataContainer.class).getPayload();
        assertEquals("Text not set correctly", text, container.getText());

        // Now, see all annotations in current view is correct
        List<View> views = container.getViews();
        if (views.size() != 1) {
            fail(String.format("Expected 1 view. Found: %d", views.size()));
        }
        View view = views.get(0);
        assertTrue("View does not contain tokens", view.contains(Uri.TOKEN));
        List<Annotation> annotations = view.getAnnotations();
        if (annotations.size() != 2) {
            fail(String.format("Expected 2 unique words. Found %d", annotations.size()));
        }
        Annotation tok1 = annotations.get(0);
        assertEquals("Token 1: wrong @type", Uri.TOKEN, tok1.getAtType());
        assertEquals("Token 1: wrong start", 0L, tok1.getStart().longValue());
        assertEquals("Token 1: wrong word", "apple", tok1.getFeature(Features.Token.WORD));

        Annotation tok2 = annotations.get(1);
        assertEquals("Token 2: wrong end", 9L, tok2.getEnd().longValue());
        assertEquals("Token 2: wrong word", "pie", tok2.getFeature(Features.Token.WORD));


    }
}