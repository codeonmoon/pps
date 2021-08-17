package com.hsse.pps.routes;

import com.hsse.pps.exception.PaymentProcessorException;
import com.hsse.pps.model.Payment;
import com.hsse.pps.model.Response;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jsonvalidator.JsonValidationException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PaymentProcessingSystemRoute extends RouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessingSystemRoute.class);

    @Override
    public void configure() throws Exception {

        onException(JsonValidationException.class)
                .handled(true)
                .log("Error occurred while validating JSON!")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                        logger.error("Exception occurred while validation {} ", cause.getMessage());
                    }
                })
                .to("{{validation.failed}}");

        // Perform basic check for ISO country and ISO Currency
        from("{{pps.in}}")
                .to("json-validator:payment.json")
                .unmarshal().json(JsonLibrary.Jackson, Payment.class)
                .log(LoggingLevel.INFO, "transactionId from pps.in", "${body.transactionId}")
                .marshal().json(JsonLibrary.Jackson, Payment.class)
                .to("{{bs.in}}");

        from("{{pps.out}}")
                .unmarshal().json(JsonLibrary.Jackson, Response.class)
                .log(LoggingLevel.INFO, "transactionId from pps.out", "${body.transactionId}")
                .marshal()
                .json(JsonLibrary.Jackson, Response.class)
                .to("{{final}}")
                .end();

    }
}
