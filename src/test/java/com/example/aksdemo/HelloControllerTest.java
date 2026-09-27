package com.example.aksdemo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class HelloControllerTest {
    @Test
    void returnsExpectedMessage() {
        assertEquals("Hello from AKS!", new HelloController().hello().get("message"));
    }
}
