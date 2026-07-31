package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Kuaidi100SignerTest {

    private final Kuaidi100Signer signer = new Kuaidi100Signer();

    @Test
    void verifiesCallbackUsingParamAndSalt() {
        String signature = signer.signCallback("{\"status\":\"polling\"}", "fixture-salt");

        assertTrue(signer.verifyCallback("{\"status\":\"polling\"}", "fixture-salt", signature));
        assertFalse(signer.verifyCallback("{\"status\":\"shutdown\"}", "fixture-salt", signature));
    }
}
