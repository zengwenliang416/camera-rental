package cn.iocoder.yudao.module.rental.service.device;

import cn.iocoder.yudao.module.rental.config.RentalDeviceProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RentalDeviceQrCodecTest {

    @Test
    void encodeIsPermanentAndDeterministic() {
        RentalDeviceProperties properties = new RentalDeviceProperties();
        properties.setQrSecret("test-secret");
        RentalDeviceQrCodec codec = new RentalDeviceQrCodec(properties);

        String a = codec.encode("A7M4-0001", "A7M4");
        String b = codec.encode("A7M4-0001", "A7M4");
        assertEquals(a, b);
        assertTrue(a.startsWith("CRD1|A7M4-0001|A7M4|"));
        assertEquals(4, a.split("\\|", -1).length);
        assertTrue(a.split("\\|", -1)[3].length() == 16);
    }

    @Test
    void decodeVerifiesModelSignature() {
        RentalDeviceProperties properties = new RentalDeviceProperties();
        properties.setQrSecret("test-secret");
        RentalDeviceQrCodec codec = new RentalDeviceQrCodec(properties);

        String payload = codec.encode("A7M4-0001", "A7M4");
        RentalDeviceQrCodec.ParsedPayload parsed = codec.decode(payload);
        assertEquals("A7M4-0001", parsed.deviceNo());
        assertEquals("A7M4", parsed.equipmentModelCode());
        assertTrue(parsed.signed());

        String tampered = payload.replace("|A7M4|", "|A7C|");
        assertThrows(IllegalArgumentException.class, () -> codec.decode(tampered));
    }

    @Test
    void unsignedModeStillPermanent() {
        RentalDeviceProperties properties = new RentalDeviceProperties();
        properties.setQrSecret("");
        RentalDeviceQrCodec codec = new RentalDeviceQrCodec(properties);

        String payload = codec.encode("A7M4-0001", "A7M4");
        assertEquals("CRD1|A7M4-0001|A7M4|", payload);
        RentalDeviceQrCodec.ParsedPayload parsed = codec.decode(payload);
        assertEquals("A7M4-0001", parsed.deviceNo());
        assertEquals("A7M4", parsed.equipmentModelCode());
        assertFalse(parsed.signed());
    }

}
