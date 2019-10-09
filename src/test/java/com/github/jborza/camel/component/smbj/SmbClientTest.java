package com.github.jborza.camel.component.smbj;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SmbClientTest {

    private SmbClient client;
    private final SmbShareFactory factory = mock(SmbShareFactory.class);
    private final SmbShare share = mock(SmbShare.class);

    @Before
    public void setup() {
        client = new SmbClient(factory);
        reset(factory);
    }

    @Test
    public void retrieveFileAsStreamTest() throws Exception {
        DefaultCamelContext ctx = new DefaultCamelContext();
        DefaultExchange exchange = new DefaultExchange(ctx);
        ProducerTemplate producerTemplate = ctx.createProducerTemplate();
        ctx.addRoutes(createRoute());

        when(share.retrieveFileStream(eq("file"))).thenReturn(new ByteArrayInputStream("blaat".getBytes()));
        when(factory.makeSmbShare()).thenReturn(share);

        client.retrieveFileAsStream("file", exchange);

        producerTemplate.send("direct:test", exchange);

        verify(share, times(1)).retrieveFileStream(eq("file"));
        verify(factory, times(1)).makeSmbShare();
        verify(share, times(1)).close();
    }

    @Test
    public void retrieveFileAsStreamFailureTest() throws Exception {
        DefaultCamelContext ctx = new DefaultCamelContext();
        DefaultExchange exchange = new DefaultExchange(ctx);
        ProducerTemplate producerTemplate = ctx.createProducerTemplate();
        ctx.addRoutes(createRouteFailed());

        when(share.retrieveFileStream(eq("file"))).thenReturn(new ByteArrayInputStream("blaat".getBytes()));
        when(factory.makeSmbShare()).thenReturn(share);

        client.retrieveFileAsStream("file", exchange);

        producerTemplate.send("direct:test", exchange);

        verify(share, times(1)).retrieveFileStream(eq("file"));
        verify(factory, times(1)).makeSmbShare();
        verify(share, times(1)).close();
    }

    private RouteBuilder createRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:test");
            }
        };
    }

    private RouteBuilder createRouteFailed() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:test")
                        .process(exchange -> {
                            throw new RuntimeException("KABOOM");
                        });
            }
        };
    }

}