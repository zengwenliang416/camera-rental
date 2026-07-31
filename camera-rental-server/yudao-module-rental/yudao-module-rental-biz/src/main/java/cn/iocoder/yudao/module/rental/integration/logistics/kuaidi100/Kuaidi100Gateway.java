package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

import java.io.IOException;
import java.util.Map;

public interface Kuaidi100Gateway {

    String subscribe(Map<String, String> form) throws IOException;

    String query(Map<String, String> form) throws IOException;
}
