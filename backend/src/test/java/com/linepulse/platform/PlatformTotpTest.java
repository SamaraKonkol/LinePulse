package com.linepulse.platform;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PlatformTotpTest {
    @Test void matchesRfc6238Sha1VectorsTruncatedToSixDigits() {
        String secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
        assertEquals("287082", PlatformTotp.codeAt(secret,59 / 30));
        assertEquals("081804", PlatformTotp.codeAt(secret,1111111109L / 30));
        assertEquals("050471", PlatformTotp.codeAt(secret,1111111111L / 30));
        assertEquals("005924", PlatformTotp.codeAt(secret,1234567890L / 30));
        assertEquals("353130", PlatformTotp.codeAt(secret,20000000000L / 30));
    }
}
