package dev.stiemannkj1.collection.util;

import dev.stiemannkj1.util.StringUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static dev.stiemannkj1.util.Assert.assertPositive;
import static dev.stiemannkj1.util.Assert.assertTrue;
import static dev.stiemannkj1.util.StringUtils.appendHexString;

public class NumberUtilsTest {

    @Test
    void test() {
        final String number = "1234567890";
        number.getBytes(StandardCharsets.UTF_8);


        for (int i = 0; i < (1 << Byte.SIZE); i++) {
            sb.append('"');
            appendHexString(sb, i, 2);
            sb.append('"')
                .append(',')
                .append('\n');
        }


    }


}
